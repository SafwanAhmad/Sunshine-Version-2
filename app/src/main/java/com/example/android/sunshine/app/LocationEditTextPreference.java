package com.example.android.sunshine.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;

/**
 * This is our custom view to provide extra/custom attributes on {@Link EditTextPreference}.
 * To know more how to build custom view and custom attributes go to the following link:
 * <a href=https://developer.android.com/training/custom-views/create-view.html>Creating Custom Views</a>
 * Created by safwanx on 2/8/17.
 */

public class LocationEditTextPreference extends EditTextPreference {

    public static final int DEFAULT_MIN_LENGTH = 2;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.LocationEditTextPreference,
                0,
                0);

        try {
            int minLength = array.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MIN_LENGTH);

            //Log this value
            Log.d(LocationEditTextPreference.class.getSimpleName(), String.valueOf(minLength));
        } finally {
            array.recycle();
        }
    }
}
