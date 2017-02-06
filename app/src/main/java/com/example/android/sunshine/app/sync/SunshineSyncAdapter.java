package com.example.android.sunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.BuildConfig;
import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    public static final int MINS_IN_HOUR = 60;
    public static final int SECS_IN_MIN = 60;
    public static final int SYNC_INTERVAL = 3 * MINS_IN_HOUR * SECS_IN_MIN;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;


    //Notification data will be pulled from database, and these are the projection
    //for the query and column indices values
    private static final String[] NOTIFY_WEATHER_PROJECTION = {WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    //These indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;


    //We’ll also add some additional constants at the top of SunshineSyncAdapter;
    // DAY_IN_MILLIS is the amount of milliseconds in a day and WEATHER_NOTIFICATION_ID is
    // an id we create that is matched to our notification so that we can reuse it.
    // If we reuse the notification ID, our application will post at most one notification.
    private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    // Annotations to be used to share status of data unavailability
    // this could be because there is some problem at server side.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID, LOCATION_STATUS_UNKNOWN})
    public @interface LocationStatus {
    }

    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_SERVER_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;

    /**
     * Method to set the location status inside shared preferences.
     *
     * @param status One of the constants defined for status
     * @param context Current context.
     */
    private static void setLocationStatus(@LocationStatus int status, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        //We need an Editor object to make preference changes.
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt(context.getString(R.string.key_location_status), status);
        //Don't forget to commit
        editor.commit();
    }

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync Called.");


        //Take out the location query with the help of utility class
        String locationQuery = Utility.getPreferredLocation(getContext());


        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                //Set the status corresponding to server down
                setLocationStatus(LOCATION_STATUS_SERVER_DOWN, getContext());
                return;
            }
            forecastJsonStr = buffer.toString();

            getWeatherDataFromJson(forecastJsonStr, locationQuery);

            //Call the notifyWeather
            notifyWeather();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            //Set the status corresponding to server down
            setLocationStatus(LOCATION_STATUS_SERVER_DOWN, getContext());
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            //Set the status corresponding to server invalid
            setLocationStatus(LOCATION_STATUS_SERVER_INVALID, getContext());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }


    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getWeatherDataFromJson(String forecastJsonStr,
                                        String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for (int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            // add to database
            if (cVVector.size() > 0) {
                // Student: call bulkInsert to add the weatherEntries to the database here
                ContentValues[] contentValues = new ContentValues[cVVector.size()];
                cVVector.toArray(contentValues);
                getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, contentValues);
            }

            //Delete the old weather data
            deleteOldData(julianStartDay, dayTime);

            //Everything seems good, update the location status also
            setLocationStatus(LOCATION_STATUS_OK, getContext());

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {

        long id = 0L;

        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID

        //We use the uri builder for the location from WeatherContact, this uri is
        //required during the query
        Uri uri = WeatherContract.LocationEntry.CONTENT_URI;

        Cursor c = getContext().getContentResolver().query(
                uri,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (c.moveToFirst()) {
            id = c.getLong(c.getColumnIndex(WeatherContract.LocationEntry._ID));
        }

        // Otherwise, insert it using the content resolver and the base URI
        else {

            //Create the ContentValues object to hold the data to be inserted
            ContentValues values = new ContentValues();

            //Add the data to values
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);

            //Insert the data into database using content resolver
            Uri insertedUri = getContext().getContentResolver().insert(
                    uri,
                    values
            );

            //Fetch the id from the uri
            id = ContentUris.parseId(insertedUri);
        }

        //Don't forget to close the cursor
        c.close();

        return id;
    }


    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }


    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);


        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        //AccountManagerFuture<Bundle> bundle = accountManager.getAuthToken(newAccount, AccountManager.KEY_AUTH_TOKEN_LABEL, null, null, null, null);

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }


    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);

        String authority = context.getString(R.string.content_authority);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();

            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
    }

    /**
     * This method is called from <code>MainActivity</code>. The control flow is as follows:
     * <p>
     * 1) <code>MainActivity</code> is created and the sync adapter is initialized.
     * 2) During initialization, <code>getSyncAccount</code> is called.
     * 3) <code>getSyncAccount</code> will create a new account if no <code>sunshine.example.com</code>
     * account exists. If this is the case, <code>onAccountCreated</code> will be called.
     * 4) <code>onAccountCreated</code> configures the periodic sync and calls for an immediate sync.
     * At this point, Sunshine will sync with the Open Weather API either every 3 hours (if the build version
     * is less than KitKat) or everyone 1 hour (if the build version is greater than or equal to KitKat)
     *
     * @param context The context used to access the account service
     */
    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    private void notifyWeather() {

        Context currentContext = getContext();

        //Checks whether you’ve already shown a notification today.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(currentContext);
        String lastNotificationKey = currentContext.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);


        //Find out if the notification are turned on/off
        boolean notificationStatus = prefs.getBoolean(
                currentContext.getString(R.string.pref_enable_notifications_key),
                Boolean.parseBoolean(currentContext.getString(R.string.pref_enable_notifications_default))
        );

        // If the last notification was sent a day before and notifications
        // are turned on for the app then let's send a new notification

        if ((System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) &&
                (notificationStatus)) {

            //1) Take out the current location setting
            String locationSetting = Utility.getPreferredLocation(currentContext);

            //2) Build the URI for the database query
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    locationSetting,
                    System.currentTimeMillis()
            );

            //3) Use content provider to execute the query
            Cursor cursor = currentContext.getContentResolver().query(
                    weatherUri,
                    NOTIFY_WEATHER_PROJECTION,
                    null,
                    null,
                    null
            );

            //4) Check if there is some data in the cursor
            if (cursor.moveToFirst()) {
                //Take out the weather data from cursor
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

                //Close the cursor
                cursor.close();

                //Take out the icon corresponding to this weather
                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                //Take out the title for the notification
                String title = currentContext.getString(R.string.app_name);


                //Define the text for the forecast notification
                String contentText = String.format(
                        currentContext.getString(R.string.format_notification),
                        desc,
                        Utility.formatTemperature(currentContext, high, Utility.isMetric(currentContext)),
                        Utility.formatTemperature(currentContext, low, Utility.isMetric(currentContext)));


                //1) Create the Notification using NotificationCompat.builder.
                NotificationCompat.Builder builder = new NotificationCompat.Builder(currentContext);
                builder.setSmallIcon(iconId);
                builder.setContentTitle(title);
                builder.setContentText(contentText);

                //2) Create an explicit intent for what the notification should open.
                Intent intent = new Intent(currentContext, MainActivity.class);

                //3) Using TaskStackBuilder, create an artificial “backstack” so that when the user
                // clicks the back button, it is clear to Android where the user will go.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(currentContext);
                stackBuilder.addParentStack(MainActivity.class);

                //add the intent that starts the activity at the top of stack
                stackBuilder.addNextIntent(intent);

                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

                //Add the pending intent to the notification
                builder.setContentIntent(resultPendingIntent);

                //Get the notification manager service
                Object object = currentContext.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationManager notificationManager = (NotificationManager) object;

                notificationManager.notify(WEATHER_NOTIFICATION_ID,
                        builder.build());

                //4) Refreshing the last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();

            }
        }
    }

    //This method will delete all weather data that is older than today

    private void deleteOldData(int julianStartDay, Time dayTime) {
        //Create Uri for weather table
        Uri weatherUri = WeatherContract.WeatherEntry.CONTENT_URI;

        getContext().getContentResolver().delete(
                weatherUri,
                WeatherContract.WeatherEntry.COLUMN_DATE + "<= ?",
                new String[]{Long.toString(dayTime.setJulianDay(julianStartDay - 1))}
        );
    }
}