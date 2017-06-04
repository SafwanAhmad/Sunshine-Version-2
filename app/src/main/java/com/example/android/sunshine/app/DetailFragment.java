package com.example.android.sunshine.app;

/**
 * Created by safwanx on 7/23/16.
 */

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
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
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private WindVane mWindVaneView;
    private ImageView mCompassView;
    private TextView mHumidityLabelView;
    private TextView mPressureLabelView;
    private TextView mWindLabelView;

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
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mWindVaneView = (WindVane) rootView.findViewById(R.id.detail_wind_vane_view);
        mCompassView = (ImageView) rootView.findViewById(R.id.detail_compass_view);
        mHumidityLabelView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textview);
        mPressureLabelView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textview);
        mWindLabelView = (TextView) rootView.findViewById(R.id.detail_wind_label_textview);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // If this is for single pane device
        if( getActivity() instanceof DetailActivity) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            finishCreatingMenu(menu);
        }
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareForecastIntent());
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

        if(data != null && data.moveToFirst()) {

            //Set the date and friendlyDate information
            long date = data.getLong(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));
            String dateString = Utility.getFullFriendlyDayString(getActivity(), date);
            //Set date and friendly date
            mDateView.setText(dateString);

            //Find the unit
            boolean isMetric = Utility.isMetric(getActivity());

            //Set the max and min temperatures
            float maxTemp = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
            float minTemp = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
            mHighTempView.setText(Utility.formatTemperature(getActivity(), maxTemp, isMetric));
            mLowTempView.setText(Utility.formatTemperature(getActivity(), minTemp, isMetric));

            //Also set content description for both high and low temperatures
            mHighTempView.setContentDescription(getString(R.string.a11y_temp_high, mHighTempView.getText()));
            mLowTempView.setContentDescription(getString(R.string.a11y_temp_low, mLowTempView.getText()));

            //Set the icon
            int weatherId = data.getInt(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            //Get the appropriate icon id used as back up
            int weatherIconId = Utility.getArtResourceForWeatherCondition(weatherId);

            //Use Glide to load appropriate icon into image view
            Glide.with(this)
                    .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                    .error(weatherIconId)
                    .crossFade()
                    .into(mIconView);

            //Set the description of weather
            String description = data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
            mDescriptionView.setText(description);
            mDescriptionView.setContentDescription(getString(R.string.a11y_forecast, description));

            //Also update the content description for the image view
            // For accessibility, add a content description to the icon field. Because the ImageView
            // is independently focusable, it's better to have a description of the image. Using
            // null is appropriate when the image is purely decorative or when the image already
            // has text describing it in the same UI component.
            mIconView.setContentDescription(getString(R.string.a11y_forecast_icon, description));


            //Set the humidity
            float humidity = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY));
            mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
            mHumidityView.setContentDescription(getString(R.string.a11y_humidity, mHighTempView.getText()));
            mHumidityLabelView.setContentDescription(mHumidityView.getContentDescription());

            //Set the wind direction and speed
            float degrees = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES));
            float speed = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED));


            //Update the wind direction information in WindVane
            mCompassView.setVisibility(View.VISIBLE);
            mWindVaneView.setVisibility((View.VISIBLE));
            mWindVaneView.setVaneDirection(degrees, Utility.getFormattedWind(getActivity(), speed, degrees, false));

            mWindView.setText(Utility.getFormattedWind(getActivity(), speed, degrees, true));
            //Set the content description
            mWindView.setContentDescription(Utility.getFormattedWind(getActivity(), speed, degrees, false));
            // Set the content description for wind label
            mWindLabelView.setContentDescription(mWindView.getContentDescription());

            //Set the pressure
            float pressure = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE));
            mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));
            // Set content description for pressure value
            mPressureView.setContentDescription(getString(R.string.a11y_pressure, mPressureView.getText()));
            // Set content description for pressure label
            mPressureLabelView.setContentDescription(mPressureView.getContentDescription());


            //Still needed for the share intent
            mForecastStr = String.format("%s - %s - %s/%s", dateString, description,
                    maxTemp, minTemp);

            //If onCreateOptionMenu has already happened, we need to update the share intent
            //now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }

            // We will work on tool bar only when data is loaded.
            // And here we assume that data is successfully loaded.
            AppCompatActivity currentActivity = (AppCompatActivity) getActivity();
            // Get the toolbar
            Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

            // For single pane devices
            if (currentActivity instanceof DetailActivity) {
                //https://plus.google.com/+AlexLockwood/posts/FJsp1N9XNLS
                currentActivity.supportStartPostponedEnterTransition();

                if (null != toolbarView) {
                    currentActivity.setSupportActionBar(toolbarView);

                    currentActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                    currentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                if (null != toolbarView) {
                    Menu menu = toolbarView.getMenu();
                    if (null != menu) {
                        menu.clear();
                        toolbarView.inflateMenu(R.menu.detailfragment);
                        finishCreatingMenu(toolbarView.getMenu());
                    }
                }
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastStr = null;
    }
}