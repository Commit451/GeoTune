package com.jawnnypoo.geotune;

import android.app.Application;

import timber.log.Timber;

/**
 * Some kinda app
 */
public class App extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
