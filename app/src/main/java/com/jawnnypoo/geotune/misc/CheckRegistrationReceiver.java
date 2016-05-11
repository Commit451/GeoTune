package com.jawnnypoo.geotune.misc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.data.GeoTuneContentProvider;
import com.jawnnypoo.geotune.service.GeoTuneModService;

import java.util.ArrayList;

/**
 * Need your boot receiver to restore the Geofences if you want them to
 * persist across device reboots
 */
public class CheckRegistrationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        restoreGeofences(context);
    }

    private void restoreGeofences(Context context) {
        //TODO restore geofences after boot
        Cursor cursor = context.getContentResolver().query(GeoTuneContentProvider.CONTENT_URI,
                null, null, null, null);
        ArrayList<GeoTune> geoTunes = new ArrayList<>();
        if (cursor != null) {
            //Iterate on all the entries we got back
            while (cursor.moveToNext()) {
                geoTunes.add(GeoTune.fromCursor(cursor));
            }
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
        if (geoTunes.size() > 0) {
            GeoTuneModService.reregisterGeoTunes(context, geoTunes);
        }
    }
}