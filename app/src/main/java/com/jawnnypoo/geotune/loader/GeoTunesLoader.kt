package com.jawnnypoo.geotune.loader

import android.content.AsyncTaskLoader
import android.content.Context
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.data.GeoTuneContentProvider
import java.util.*

/**
 * Get all the geotunes we have in storage
 */
class GeoTunesLoader(private val theContext: Context) : AsyncTaskLoader<ArrayList<GeoTune>>(theContext) {

    companion object {
        private val DB_URI = GeoTuneContentProvider.CONTENT_URI

        fun getGeoTunes(context: Context): ArrayList<GeoTune> {
            val cursor = context.contentResolver.query(DB_URI,
                    null, null, null, null)
            val geoTunes = ArrayList<GeoTune>()
            if (cursor != null && cursor.count > 0) {
                //Iterate on all the entries we got back
                while (cursor.moveToNext()) {
                    geoTunes.add(GeoTune.fromCursor(cursor))
                }
                if (!cursor.isClosed) {
                    cursor.close()
                }
            }
            return geoTunes
        }

        fun getGeoTune(context: Context, id: String): GeoTune {
            val cursor = context.contentResolver.query(
                    DB_URI, null,
                    GeoTune.KEY_UID + " LIKE ?",
                    arrayOf(id), null)
            cursor!!.moveToNext()
            val geoTune = GeoTune.fromCursor(cursor)
            if (!cursor.isClosed) {
                cursor.close()
            }
            return geoTune
        }
    }

    init {
        forceLoad()
    }

    override fun loadInBackground(): ArrayList<GeoTune> {
        return getGeoTunes(theContext)
    }
}
