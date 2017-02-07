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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {

    //TODO DATE_FORMAT is never used

    //Format used for storing dates in the database. Also used for converting those strings
    //back into date object for comparison/processing
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }

    public static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    /**
     * Helper method to convert the database representation of the date into something to
     * display to user. As classy and polished a user experience as "20160102" is.
     *
     * @param context Context to use for resource localization.
     * @param dateInMilliseconds The date in milli seconds.
     * @return user-friendly representation of the date.
     */

    public static String getFriendlyDayString(Context context, long dateInMilliseconds) {

        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Time time = new Time();
        time.setToNow();

        long currentTime = System.currentTimeMillis();

        int julianDay = Time.getJulianDay(dateInMilliseconds, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if(julianDay == currentJulianDay) {
            //Find the string defined in res for today
            String today = context.getString(R.string.today);

            int formatId = R.string.format_full_friendly_date;

            return context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context,dateInMilliseconds)
            );
        }
        //If the input date is less than a week in future just return the day name
        else if(julianDay < currentJulianDay + 7) {
            return getDayName(context, dateInMilliseconds);
        }

        //Otherwise use the form "Mon Jun 3"
        else {
            SimpleDateFormat shortedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortedDateFormat.format(dateInMilliseconds);
        }

    }


    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInMilliseconds The date in milliseconds
     * @return
     */
    public static String getDayName(Context context, long dateInMilliseconds) {
        //If the date is today,return the localized version of "Today" instead of the actual
        //day name
        Time time = new Time();
        time.setToNow();

        int julianDay = Time.getJulianDay(dateInMilliseconds, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), time.gmtoff);

        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        }
        else if (julianDay == currentJulianDay + 1) {
            return context.getString(R.string.tomorrow);
        } else {
            time.setToNow();
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMilliseconds);
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMilliseconds The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMilliseconds) {
        Time time = new Time();
        time.setToNow();

        //SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");

        String monthDayString = monthDayFormat.format(dateInMilliseconds);
        return monthDayString;

    }

    private static String getFriendlyWindDirection(float degrees, boolean useAbb)
    {
        //From wind direction in degrees find out the compass based direction e.g NW
        String direction = "Unknown";

        if(degrees >= 337.5 || degrees < 22.5) {
            direction = (useAbb == true ? "N" : "From North");
        }
        else if (degrees >= 22.5 && degrees < 67.5) {
            direction = (useAbb == true ? "NE" : "From North-East");
        }
        else if (degrees >= 67.5 && degrees < 112.5) {
            direction = (useAbb == true ? "E" : "From East");
        }
        else if (degrees >= 112.5 && degrees < 157.5) {
            direction = (useAbb == true ? "SE" : "From South-East");
        }
        else if (degrees >= 157.5 && degrees < 202.5) {
            direction = (useAbb == true ? "S" : "From South");
        }
        else if (degrees >= 202.5 && degrees < 247.5) {
            direction = (useAbb == true ? "SW" : "From South-West");
        }
        else if (degrees >= 247.5 && degrees < 292.5) {
            direction = (useAbb == true ? "W" : "From West");
        }
        else if (degrees >= 292.5 && degrees < 337.5) {
            direction = (useAbb == true ? "NW" : "From North-West");
        }
        return  direction;
    }

    public static String getFormattedWind(Context context, float windSpeed, float degrees, boolean useAbb) {

        int windFormat;
        final float KPH_TO_MPH = 0.621371f;

        if(Utility.isMetric(context))
        {
            windFormat = R.string.format_wind_kph;
        }
        else
        {
            windFormat = R.string.format_wind_mph;
            windSpeed = KPH_TO_MPH * windSpeed;
        }

        return context.getString(windFormat, windSpeed, getFriendlyWindDirection(degrees, useAbb));
    }


    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_rain;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }

    /**
     * Helper method to fetch the status code for server. This is saved inside <code>{@link SunshineSyncAdapter}</code>
     * based on the type of issue with the server.
     * The status code is one of the codes defined inside <code>{@link SunshineSyncAdapter}</code>.
     *
     * @param context Current context.
     * @return A constant int value representing status code for server.
     */
    @SuppressWarnings("ResourceType")
    public static @SunshineSyncAdapter.LocationStatus int getLocationStatus(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int status = preferences.getInt(
                context.getString(
                        R.string.key_location_status),
                SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
        return status;
    }

    /**
     * Helper method to reset the location status for server to unknown. This method is called
     * from {@link SettingsActivity} when there is a change in the location setting.
     * @param context Current context.
     */
    public static void resetLocationStatus(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        //We need an Editor object to make preference changes.
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt(context.getString(R.string.key_location_status),
                SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);

        editor.apply();
    }
}