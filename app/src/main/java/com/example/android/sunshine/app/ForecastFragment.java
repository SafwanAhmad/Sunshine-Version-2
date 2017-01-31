/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

;


/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private ForecastAdapter mForecastAdapter;
    private final int loaderId = 0;
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private static final String LAST_SELECTED_INDEX_KEY = "lastSelectedIndex";

    private int lastSelectedIndex = ListView.INVALID_POSITION;

    private boolean mUseTodayLayout;


    //Used for updating weather data with the help of alarm and pending intents
    private AlarmManager mUpdateWeatherAlarm;
    private PendingIntent mUpdateWeatherIntent;

    //Notification callbacks
    public interface listItemClickedListener {
        public void onListItemClicked(Uri uriWithDate);
    }

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." +
                    WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG,
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }

    //Setter for mUseTodayLayout
    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;

        if (mForecastAdapter != null) {
            mForecastAdapter.setTodayViewSpecial(mUseTodayLayout);
        }
    }

    //Initialize Loader inside this
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(loaderId, savedInstanceState, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null && savedInstanceState.containsKey(LAST_SELECTED_INDEX_KEY)) {
            lastSelectedIndex = savedInstanceState.getInt(LAST_SELECTED_INDEX_KEY);
        }

        //Create a forecast adapter no cursor attached
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        //Get the reference of the view used when no weather information is available.
        View emptyView = rootView.findViewById(R.id.listview_forecast_empty);
        //Set it as the empty view
        listView.setEmptyView(emptyView);

        listView.setAdapter(mForecastAdapter);


        //Note the type of third parameter of onCreateView. It's now made final, as
        //it's being used inside this anonymous class. Still the above method overrides
        //the base class fragment method. (Read closure in java for more information).
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);

                if (cursor != null && cursor.getCount() != 0) {

                    Uri weatherWithDate = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                            cursor.getString(COL_LOCATION_SETTING), cursor.getLong(COL_WEATHER_DATE));

                    try {
                        ((listItemClickedListener) getActivity()).onListItemClicked(weatherWithDate);
                    } catch (ClassCastException cEx) {
                        Log.e(LOG_TAG, getActivity().getClass().getSimpleName() + " must implement interface " + listItemClickedListener.class.getSimpleName());
                    }
                    //Save the current selected item position, in case two pane is supported
                    lastSelectedIndex = position;
                }
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastSelectedIndex != ListView.INVALID_POSITION)
            outState.putInt(LAST_SELECTED_INDEX_KEY, lastSelectedIndex);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void openPreferredLocationInMap() {

        if (mForecastAdapter != null) {
            Cursor cursor = mForecastAdapter.getCursor();

            if (cursor.moveToFirst()) {
                double longitude = cursor.getDouble(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_COORD_LONG));
                double latitude = cursor.getDouble(cursor.getColumnIndex(WeatherContract.LocationEntry.COLUMN_COORD_LAT));

                // Using the URI scheme for showing a location found on a map.  This super-handy
                // intent can is detailed in the "Common Intents" page of Android's developer site:
                // http://developer.android.com/guide/components/intents-common.html#Maps
                Uri geoLocation = Uri.parse("geo:" + latitude + "," + longitude);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't show " + longitude + "," + latitude + ", no receiving apps installed!");
                }
            } else {
                Toast mapToast = Toast.makeText(getActivity(), "No Coordinates received!", Toast.LENGTH_SHORT);
                mapToast.show();
            }
        }
    }

    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void onLocationChanged() {
        //Perform weather updation based on the new location setting
        updateWeather();

        //Restart the loader
        getLoaderManager().restartLoader(loaderId, null, this);
    }

    public void onUnitChanged() {
        mForecastAdapter.notifyDataSetChanged();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String locationSetting = Utility.getPreferredLocation(getActivity());

        //Sort order: Ascending by date
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        //Build the Uri needed for the query
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting,
                System.currentTimeMillis());

        //Create a cursor loader
        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Check if the cursor is empty
        if (data.getCount() == 0) {
            //Check network status
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = (networkInfo != null && networkInfo.isConnectedOrConnecting());

            if (!isConnected) {
                TextView emptyView = (TextView) getView().findViewById(R.id.listview_forecast_empty);
                emptyView.setText(getString(R.string.no_weather_information) + "\n"+
                        getString(R.string.no_network_available));
            }
        } else {
            //Update the adapter
            mForecastAdapter.swapCursor(data);

            //If there is some item in the list that was selected before then scroll to it
            if (lastSelectedIndex != ListView.INVALID_POSITION) {
                //Restore to last scrolled position
                // Get a reference to the ListView, and attach this adapter to it.
                ListView listView = (ListView) getActivity().findViewById(R.id.listview_forecast);

                listView.smoothScrollToPosition(lastSelectedIndex);

                //Also if two pane layout is supported then select the last item
                if (!mUseTodayLayout) {
                    selectItemFromList(lastSelectedIndex);
                }
            }

            //select the first element if two pane is supported and last state doesn't exist
            else if (!mUseTodayLayout) {
                selectItemFromList(ListView.SCROLLBAR_POSITION_DEFAULT);
            }
        }

    }

    private void selectItemFromList(final int position) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ListView listView = (ListView) getActivity().findViewById(R.id.listview_forecast);

                listView.setItemChecked(position, true);

                listView.performItemClick(listView,
                        position,
                        listView.getItemIdAtPosition(position));
            }
        });

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }
}
