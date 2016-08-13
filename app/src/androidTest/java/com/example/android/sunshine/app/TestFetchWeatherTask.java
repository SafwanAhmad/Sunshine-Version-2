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
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.test.AndroidTestCase;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.service.SunshineService;

public class TestFetchWeatherTask extends AndroidTestCase{
    static final String LOCATION_ID = "110025";

    /*
        Students: uncomment testAddLocation after you have written the AddLocation function.
        This test will only run on API level 11 and higher because of a requirement in the
        content provider.
     */
    @TargetApi(11)
    public void testIntentService(){
        // start from a clean state
        getContext().getContentResolver().delete(WeatherContract.LocationEntry.CONTENT_URI,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{LOCATION_ID});

        //Send an intent to SunshineService
        Intent intent = new Intent(getContext(), SunshineService.class);

        //Add location information
        intent.putExtra(SunshineService.LOCATION_QUERY_EXTRA, LOCATION_ID);

        //Launch the intent
        getContext().startService(intent);

        try {
            Thread.sleep(1000);
        }catch (InterruptedException iEx)
        {
        }


        // test all this twice
        for (int i = 0; i < 2; i++) {

            // does the ID point to our location?
            Cursor locationCursor = getContext().getContentResolver().query(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    new String[]{
                            WeatherContract.LocationEntry._ID,
                            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
                            WeatherContract.LocationEntry.COLUMN_CITY_NAME,
                            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                            WeatherContract.LocationEntry.COLUMN_COORD_LONG
                    },
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                    new String[]{LOCATION_ID},
                    null);


            // these match the indices of the projection
            if (locationCursor.moveToFirst()) {
                assertEquals("Error: the queried value of location setting is incorrect",
                        locationCursor.getString(1), LOCATION_ID);


            } else {
                fail("Error: the id you used to query returned an empty cursor");
            }

            // there should be no more records
            assertFalse("Error: there should be only one record returned from a location query",
                    locationCursor.moveToNext());

        }

        // reset our state back to normal
        getContext().getContentResolver().delete(WeatherContract.LocationEntry.CONTENT_URI,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{LOCATION_ID});

        // clean up the test so that other tests can use the content provider
        getContext().getContentResolver().
                acquireContentProviderClient(WeatherContract.LocationEntry.CONTENT_URI).
                getLocalContentProvider().shutdown();
    }
}
