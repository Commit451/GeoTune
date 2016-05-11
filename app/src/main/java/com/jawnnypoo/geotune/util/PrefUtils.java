package com.jawnnypoo.geotune.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

/**
 * Manage all the preferences
 */
public class PrefUtils {

    private static final String SHARED_PREFS = "com.jawnnypoo.geotune.SharedPrefs";

    private static final String PREF_LOCATION_LAT = "pref_location_lat";
    private static final String PREF_LOCATION_LNG = "pref_location_lng";

    private static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
    }

    public static LatLng getLocation(Context context) {
        float lat = getSharedPrefs(context).getFloat(PREF_LOCATION_LAT, Float.MAX_VALUE);
        float lng = getSharedPrefs(context).getFloat(PREF_LOCATION_LNG, Float.MAX_VALUE);
        if (lat == Float.MAX_VALUE || lng == Float.MAX_VALUE) {
            return null;
        }
        return new LatLng(lat, lng);
    }

    public static void setLocation(Context context, LatLng latLng) {
        getSharedPrefs(context).edit()
                .putFloat(PREF_LOCATION_LAT, (float) latLng.latitude)
                .putFloat(PREF_LOCATION_LNG, (float) latLng.longitude)
                .apply();
    }
}
