package com.example.android.sunshine.app;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by safwanx on 7/17/16.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
    }
}
