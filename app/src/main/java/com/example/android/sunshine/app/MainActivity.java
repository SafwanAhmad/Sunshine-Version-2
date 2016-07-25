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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements ForecastFragment.listItemClickedListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    //Tag for the detail fragment
    public static final String DETAIL_FRAGMENT_TAG = "DFTAG";

    //Store the current location setting so that later it can be used to
    //monitor a location change
    private String mLocationSetting;
    private String mUnitSetting;

    //Flag to check if device is two pane
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(null == findViewById(R.id.weather_detail_container)) {
            mTwoPane = false;
        }
        else
        {
            mTwoPane = true;
            //Attach detail fragment to the weather_detail_container
            getSupportFragmentManager().beginTransaction().add(R.id.weather_detail_container,
                    new DetailFragment(),DETAIL_FRAGMENT_TAG).commit();
        }

        //Get the current value of mLocationSetting
        SharedPreferences sharedPreferences = (SharedPreferences) PreferenceManager.getDefaultSharedPreferences(this);
        mLocationSetting = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        mUnitSetting = sharedPreferences.getString(
                getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric)
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        String location = sharedPrefs.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", location)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + location + ", no receiving apps installed!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Check if the location setting is changed
        SharedPreferences sharedPreferences = (SharedPreferences)PreferenceManager.getDefaultSharedPreferences(this);

        String newLocationSetting = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        String newUnitSetting = sharedPreferences.getString(
                getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric)
        );

        if(!mLocationSetting.equals(newLocationSetting))
        {
            //Get the associated fragment object
            ForecastFragment forecastFragment = (ForecastFragment)getSupportFragmentManager().
                    findFragmentById(R.id.fragment_forecast);

            if(forecastFragment != null)
            {
                //Perform update
                forecastFragment.onLocationChanged();
            }

            //Get the associated detail fragment object
            DetailFragment detailFragment = (DetailFragment)getSupportFragmentManager().
                    findFragmentByTag(DETAIL_FRAGMENT_TAG);
            if (detailFragment != null) {
                detailFragment.onLocationChanged(newLocationSetting);
            }


            mLocationSetting = newLocationSetting;
        }

        if(newUnitSetting != mUnitSetting)
        {
            ForecastFragment forecastFragment = (ForecastFragment)getSupportFragmentManager().
                    findFragmentById(R.id.fragment_forecast);
            forecastFragment.onUnitChanged();

            //Get the associated detail fragment object
            DetailFragment detailFragment = (DetailFragment)getSupportFragmentManager().
                    findFragmentByTag(DETAIL_FRAGMENT_TAG);
            if (detailFragment != null) {
                detailFragment.onUnitChanged();
            }
            mUnitSetting = newUnitSetting;
        }
    }

    @Override
    public void onListItemClicked(Uri uriWithDate) {

        //Replace the fragment in the container
        if (mTwoPane == true) {
            //We want to pass the uri also to this new fragment. We will use bundle for this purpose
            Bundle uriData = new Bundle();
            uriData.putParcelable(DetailFragment.URI_KEY, uriWithDate);

            DetailFragment detailFragment = new DetailFragment();
            detailFragment.setArguments(uriData);

            getSupportFragmentManager().beginTransaction().
            replace(R.id.weather_detail_container, detailFragment   ,DETAIL_FRAGMENT_TAG).commit();
        }
        //if device doesn't support two pane then launch an intent for
        //DetailActivity
        else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.setData(uriWithDate);
            startActivity(intent);
        }

    }
}
