package com.jawnnypoo.geotune;

import android.app.Application;

import timber.log.Timber;

/**
 * Some kinda app
 * Created by John on 10/30/2014.
 */
public class GeoTuneApp extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
