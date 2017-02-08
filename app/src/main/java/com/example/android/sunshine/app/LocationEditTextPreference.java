package com.example.android.sunshine.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

/**
 * This is our custom view to provide extra/custom attributes on {@Link EditTextPreference}.
 * To know more how to build custom view and custom attributes go to the following link:
 * <a href=https://developer.android.com/training/custom-views/create-view.html>Creating Custom Views</a>
 * Created by safwanx on 2/8/17.
 */

public class LocationEditTextPreference extends EditTextPreference {

    public static final int DEFAULT_MIN_LENGTH = 2;
    private TextWatcher mEditTextWatcher;
    private int mMinLength = 0;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEditTextWatcher = new EditTextWatcher();

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.LocationEditTextPreference,
                0,
                0);

        try {
            mMinLength = array.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MIN_LENGTH);

            //Log this value
            Log.d(LocationEditTextPreference.class.getSimpleName(), String.valueOf(mMinLength));
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        EditText editText = getEditText();
        editText.addTextChangedListener(mEditTextWatcher);
    }

    private class EditTextWatcher implements TextWatcher
    {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.d(LocationEditTextPreference.class.getSimpleName(), s.toString());


            // Check if the length of the input is less than 3 characters long then
            // we will disable the OK (dialog) button.
            Button positive = null;
            Dialog dialog = getDialog();

            //But we must ensure that the associated dialog belongs to alert dialog.
            if (dialog instanceof AlertDialog) {
                positive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            }

            if (positive != null) {
                if (s.length() < mMinLength) {
                    positive.setEnabled(false);
                } else {
                    positive.setEnabled(true);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }
    }
}
