package com.jawnnypoo.geotune.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.data.GeoTuneContentProvider
import com.jawnnypoo.geotune.service.GeoTuneModService
import java.util.*

/**
 * Need your boot receiver to restore the Geofences if you want them to
 * persist across device reboots
 */
class CheckRegistrationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        restoreGeofences(context)
    }

    private fun restoreGeofences(context: Context) {
        val cursor = context.contentResolver.query(GeoTuneContentProvider.CONTENT_URI, null, null, null, null)
        val geoTunes = ArrayList<GeoTune>()
        if (cursor != null) {
            //Iterate on all the entries we got back
            while (cursor.moveToNext()) {
                geoTunes.add(GeoTune.fromCursor(cursor))
            }
            if (!cursor.isClosed) {
                cursor.close()
            }
        }
        if (geoTunes.size > 0) {
            GeoTuneModService.reregisterGeoTunes(context, geoTunes)
        }
    }
}