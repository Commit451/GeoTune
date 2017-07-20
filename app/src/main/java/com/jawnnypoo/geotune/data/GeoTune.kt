package com.jawnnypoo.geotune.data

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

import com.google.android.gms.location.Geofence

import org.parceler.Parcel

/**
 * A GeoTune, which is basically a geofence associated with a tune (ringtone) that plays
 * when you enter the fence
 */
@Parcel
class GeoTune {

    companion object {

        val KEY_UID = "id"
        val KEY_NAME = "name"
        val KEY_LATITUDE = "lat"
        val KEY_LONGITUDE = "lng"
        val KEY_RADIUS = "radius"
        val KEY_TRANSITION_TYPE = "transition"
        val KEY_TUNE = "tune"
        val KEY_TUNE_NAME = "tuneName"
        val KEY_ACTIVE = "active"

        fun fromCursor(cursor: Cursor): GeoTune {
            val geoTune = GeoTune()
            geoTune.id = cursor.getString(cursor.getColumnIndex(KEY_UID))
            geoTune.name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
            geoTune.latitude = cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE))
            geoTune.longitude = cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE))
            geoTune.radius = cursor.getFloat(cursor.getColumnIndex(KEY_RADIUS))
            geoTune.transitionType = cursor.getInt(cursor.getColumnIndex(KEY_TRANSITION_TYPE))
            val tuneString = cursor.getString(cursor.getColumnIndex(KEY_TUNE))
            geoTune.tuneUri = if (tuneString == null) null else Uri.parse(tuneString)
            geoTune.tuneName = cursor.getString(cursor.getColumnIndex(KEY_TUNE_NAME))
            geoTune.isActive = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVE)) == 1
            return geoTune
        }
    }


    var name: String? = null
    /**
     * The GeoTune ID, which acts as our ID in our local DB as well as
     * the request ID for the geofence
     */
    var id: String? = null
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var radius: Float = 0f
    /**
     * Get the geofence transition type
     * @return Transition type (see [Geofence])
     */
    var transitionType: Int = Geofence.GEOFENCE_TRANSITION_ENTER
    var tuneUri: Uri? = null
    var tuneName: String? = null
    var isActive: Boolean = false

    /**
     * Creates a Location Services Geofence object from a
     * SimpleGeofence.

     * @return A Geofence object
     */
    fun toGeofence(): Geofence {
        // Build a new Geofence object
        return Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(transitionType)
                .setCircularRegion(
                        latitude,
                        longitude,
                        radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
    }

    fun toContentValues(): ContentValues {
        val cv = ContentValues()
        // Assign values for each row.
        cv.put(KEY_UID, id)
        cv.put(KEY_NAME, name)
        cv.put(KEY_LATITUDE, latitude)
        cv.put(KEY_LONGITUDE, longitude)
        cv.put(KEY_RADIUS, radius)
        cv.put(KEY_TRANSITION_TYPE, transitionType)
        cv.put(KEY_TUNE, if (tuneUri == null) null else tuneUri!!.toString())
        cv.put(KEY_TUNE_NAME, tuneName)
        cv.put(KEY_ACTIVE, isActive)
        return cv
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val geoTune = o as GeoTune?

        return if (id != null) id == geoTune!!.id else geoTune!!.id == null

    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}