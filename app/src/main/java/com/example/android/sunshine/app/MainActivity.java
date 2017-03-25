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

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.sunshine.app.gcm.RegistrationIntentService;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.GoogleApiAvailability;

import static com.google.android.gms.common.ConnectionResult.SUCCESS;


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

    //Constant used by Google API availability check inside getErrorDialog method
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // Key used to store the status of registration token
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Find the tool bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if(null == findViewById(R.id.weather_detail_container)) {
            mTwoPane = false;

            //Also remove the shadow of action bar
            getSupportActionBar().setElevation(0f);
        }
        else
        {
            mTwoPane = true;
            //Attach detail fragment to the weather_detail_container
            getSupportFragmentManager().beginTransaction().replace(R.id.weather_detail_container,
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

        //Also inform forecastAdapter whether to use special view for today or not
        //through assocaited fragment
        Fragment associatedFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        ((ForecastFragment)associatedFragment).setUseTodayLayout(!mTwoPane);

        //Initialize the Sync Adapter
        SunshineSyncAdapter.initializeSyncAdapter(this);

        // Check if the device has Google Play Services apk available.
        // If Google Play Services is up to date, we'll want to register GCM. If it is not, we'll
        // skip the registration and this device will not receive any downstream messages from
        // our fake server. Because weather alerts are not a core feature of the app, this should
        // not affect the behavior of the app, from a user perspective.
        if (checkPlayService()) {
            // Because this is the initial creation of the app, we'll want to be certain we have
            // a token. If we do not, then we will start the IntentService that will register this
            // application with GCM.
            boolean sentToken = sharedPreferences.getBoolean(MainActivity.SENT_TOKEN_TO_SERVER, false);

            if(!sentToken)
            {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
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

        return super.onOptionsItemSelected(item);
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

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayService() {
        // Get the reference to GoogleApiAvailability singleton object
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        // Check if the service is available
        int isAvailable = apiAvailability.isGooglePlayServicesAvailable(this);

        if (isAvailable != SUCCESS) {
            // Determines whether an error can be resolved via user action
            if (apiAvailability.isUserResolvableError(isAvailable)) {
                // Proceed and get a dialog
                Dialog dialog = apiAvailability.getErrorDialog(this, isAvailable, PLAY_SERVICES_RESOLUTION_REQUEST);
                dialog.show();
            } else {
                // User can not resolve the error
                Log.i(LOG_TAG, "This device is not supported!");
                finish();
            }

            return false;
        }
        return true;
    }

}
