package com.example.android.sunshine.app;

/**
 * Created by safwanx on 7/23/16.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int loaderId = 0;
    public static final String URI_KEY = "URI";

    //This is the Uri used to create a new cursor loader, it's sent through an intent or
    // as a fragment argument.
    private Uri contentUri;

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private String mForecastStr = null;

    private ShareActionProvider mShareActionProvider;

    //Views required to set the data received from the loader
    private ImageView mIconView;
    private TextView mFriendlyDateView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;

    //Projection string used to select the required columns from the table
    private static final String [] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." +
                    WeatherContract.WeatherEntry._ID,
            //This contant is used to map weather condition code to appropriate image
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED
    };

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Initialize loader
        getLoaderManager().initLoader(loaderId, savedInstanceState, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Take out the fragment arguments if any
        Bundle uriData = getArguments();
        if (uriData != null) {
            contentUri = uriData.getParcelable(DetailFragment.URI_KEY);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        //Get all the view inside the root view
        mIconView = (ImageView)rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView)rootView.findViewById(R.id.detail_date_textview);
        mFriendlyDateView = (TextView) rootView.findViewById(R.id.detail_day_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        //If onLoadFinished happens before this, we can go ahead and set the share intent now
        if (mForecastStr != null ) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        } else {
            Log.d(LOG_TAG, "Share Action Provider is null?");
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                mForecastStr + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    //Helper method to update the content if location preference is changed
    public void onLocationChanged(String locationSetting) {

        Uri currentUri = contentUri;
        if(currentUri != null)
        {
            //We need to find the weather for new location but same date
            long date = WeatherContract.WeatherEntry.getDateFromUri(contentUri);

            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting,date);

            contentUri = updatedUri;

            getLoaderManager().restartLoader(loaderId, null, this);
        }
    }

    public void onUnitChanged() {
        if(getLoaderManager().getLoader(loaderId) != null)
        {
            getLoaderManager().restartLoader(loaderId, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;

        if(contentUri != null) {
            cursorLoader = new CursorLoader(getActivity(),
                    contentUri,
                    FORECAST_COLUMNS,
                    null,
                    null,
                    null);
        }

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if(data.moveToFirst()) {

            //Set the date and friendlyDate information
            long date = data.getLong(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));
            String dateString = Utility.getFormattedMonthDay(getActivity(), date);
            String friendlyDateString = Utility.getDayName(getActivity(), date);

            //Set date and friendly date
            mFriendlyDateView.setText(friendlyDateString);
            mDateView.setText(dateString);

            //Find the unit
            boolean isMetric = Utility.isMetric(getActivity());

            //Set the max and min temperatures
            float maxTemp = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
            float minTemp = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
            mHighTempView.setText(Utility.formatTemperature(getActivity(), maxTemp, isMetric));
            mLowTempView.setText(Utility.formatTemperature(getActivity(), minTemp, isMetric));

            //Set the icon
            int weatherId = data.getInt(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            //Get the appropriate icon id
            int weatherIconId = Utility.getArtResourceForWeatherCondition(weatherId);
            mIconView.setImageResource(weatherIconId);


            //Set the description of weather
            String description = data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
            mDescriptionView.setText(description);

            //Also update the content description for the image view
            mIconView.setContentDescription(description);


            //Set the humidity
            float humidity = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY));
            mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));

            //Set the wind direction and speed
            float degrees = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES));
            float speed = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED));

            mWindView.setText(Utility.getFormattedWind(getActivity(), speed, degrees));

            //Set the pressure
            float pressure = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE));
            mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));


            //Still needed for the share intent
            mForecastStr = String.format("%s - %s - %s/%s", dateString, description,
                    maxTemp, minTemp);

            //If onCreateOptionMenu has already happened, we need to update the share intent
            //now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastStr = null;
    }
}