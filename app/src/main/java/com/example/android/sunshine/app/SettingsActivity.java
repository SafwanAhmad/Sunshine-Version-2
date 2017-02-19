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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener
{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_icon_packs_key)));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    // We want to be notified about the change in shared preferences as long as we are inside
    // the SettingActivity. So register and unregister accordingly.
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    // Override this method so that we can get notifications when the value of location status
    // gets changed. The location status value corresponds to status of server.
    // This method gets called after the preference is changed, which is important because we
    // start our synchronization here.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // We entered a new location
        if (key.equals(getString(R.string.pref_location_key))) {
            //Reset the value of location status (server status) to unknown
            Utility.resetLocationStatus(this);

            //Also launch service to download data, this will also update the location
            //status preference. So we expect a future call with key = key_location_status
            SunshineSyncAdapter.syncImmediately(this);
        }

        // If unit value changes, we need to update weather accordingly.
        else if (key.equals(getString(R.string.pref_units_key))) {
            // This will force the attached loader to perform load, which will tell the
            // attached adapter to update views.
            getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }

        // If value of location status is changed, we perform update to summary as well as
        // bind this preference so that in future if the value gets changed it is reflected
        // inside setting activity also.
        else if (key.equals(getString(R.string.key_location_status))) {
            Preference locationStatus = findPreference(getString(R.string.pref_location_key));
            bindPreferenceSummaryToValue(locationStatus);
        }
    }

    // This is called before the state of the Preference is about to be updated and before the
    // state is persisted.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // We will only change the summary here. As preference is still not written to persistent
        // memory.
        setPreferenceSummary(preference, value);
        return true;
    }

    //Helper method to set summary for different scenarios
    private void setPreferenceSummary(Preference preference, Object value) {
        // Take out the key and string representation of value
        String key = preference.getKey();
        String stringValue = value.toString();

        // Check if the change is for the list preference
        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (key.equals(getString(R.string.pref_location_key))) {
            @SunshineSyncAdapter.LocationStatus int statusCode = Utility.getLocationStatus(this);

            switch (statusCode) {
                case SunshineSyncAdapter.LOCATION_STATUS_OK:
                    preference.setSummary(stringValue);
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                    preference.setSummary(getString(R.string.pref_location_unknown_description, stringValue));
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                    preference.setSummary(getString(R.string.pref_location_error_description, stringValue));
                    break;
                default:
                    //If the server is down we still assume the value is valid
                    preference.setSummary(stringValue);
            }
        }
        // For other preferences, set the summary to the value's simple string representation.
        else {
            preference.setSummary(stringValue);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

}