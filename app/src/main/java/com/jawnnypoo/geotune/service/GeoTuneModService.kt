package com.jawnnypoo.geotune.service

import android.app.IntentService
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.commit451.addendum.parceler.getParcelerParcelableExtra
import com.commit451.addendum.parceler.putParcelerParcelableExtra
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.data.GeoTuneContentProvider
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Modify a GeoTune in the background
 */
/**
 * Sets an identifier for this class' background thread
 */
class GeoTuneModService : IntentService(TAG) {

    companion object {
        private val TAG = GeoTuneModService::class.java.simpleName

        private val ACTION_ADD = "ACTION_ADD"
        private val ACTION_UPDATE = "ACTION_MODIFY"
        private val ACTION_DELETE = "ACTION_DELETE"
        private val ACTION_REGISTER = "ACTION_REGISTER"
        private val ACTION_REREGISTER = "ACTION_REREGISTER"
        private val ACTION_UNREGSITER = "ACTION_UNREGISTER"

        private val EXTRA_GEOTUNE = "EXTRA_GEOTUNE"
        private val EXTRA_GEOTUNE_ARRAY = "EXTRA_GEOTUNE_ARRAY"
        private val EXTRA_GEOTUNE_ID = "EXTRA_GEOTUNE_ID"
        private val EXTRA_CONTENT_VALUES = "EXTRA_CONTENT_VALUES"

        fun addGeoTune(context: Context, contentValues: ContentValues) {
            val modIntent = Intent(context, GeoTuneModService::class.java)
            modIntent.action = ACTION_ADD
            modIntent.putExtra(EXTRA_CONTENT_VALUES, contentValues)
            context.startService(modIntent)
        }

        fun updateGeoTune(context: Context, contentValues: ContentValues, geoTuneId: String) {
            val modIntent = Intent(context, GeoTuneModService::class.java)
            modIntent.action = ACTION_UPDATE
            modIntent.putExtra(EXTRA_CONTENT_VALUES, contentValues)
            modIntent.putExtra(EXTRA_GEOTUNE_ID, geoTuneId)
            context.startService(modIntent)
        }

        fun deleteGeoTune(context: Context, geoTune: GeoTune) {
            val modIntent = Intent(context, GeoTuneModService::class.java)
            modIntent.action = ACTION_DELETE
            modIntent.putParcelerParcelableExtra(EXTRA_GEOTUNE, geoTune)
            context.startService(modIntent)
        }

        fun registerGeoTune(context: Context, geoTune: GeoTune) {
            val modIntent = Intent(context, GeoTuneModService::class.java)
            modIntent.action = ACTION_REGISTER
            modIntent.putParcelerParcelableExtra(EXTRA_GEOTUNE, geoTune)
            context.startService(modIntent)
        }

        fun reregisterGeoTunes(context: Context, geoTunes: List<GeoTune>) {
            val modIntent = Intent(context, GeoTuneModService::class.java)
            modIntent.action = ACTION_REREGISTER
            modIntent.putParcelerParcelableExtra(EXTRA_GEOTUNE_ARRAY, geoTunes)
            context.startService(modIntent)
        }

        fun unregisterGeoTune(context: Context, geoTune: GeoTune) {
            val modIntent = Intent(context, GeoTuneModService::class.java)
            modIntent.action = ACTION_UNREGSITER
            modIntent.putParcelerParcelableExtra(EXTRA_GEOTUNE, geoTune)
            context.startService(modIntent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent!!.action) {
            ACTION_ADD -> createGeoTune(intent)
            ACTION_UPDATE -> updateGeoTune(intent)
            ACTION_DELETE -> deleteGeoTune(intent)
            ACTION_REGISTER -> registerGeoTune(intent)
            ACTION_REREGISTER -> reregisterGeoTunes(intent)
            ACTION_UNREGSITER -> unregisterGeoTune(intent)
        }
    }

    private fun createGeoTune(intent: Intent) {
        contentResolver.insert(GeoTuneContentProvider.CONTENT_URI,
                intent.getParcelableExtra<Parcelable>(EXTRA_CONTENT_VALUES) as ContentValues)
        Timber.d("Created GeoTune")
    }

    private fun updateGeoTune(intent: Intent) {
        val updates = contentResolver.update(GeoTuneContentProvider.CONTENT_URI,
                intent.getParcelableExtra<Parcelable>(EXTRA_CONTENT_VALUES) as ContentValues,
                GeoTune.KEY_UID + " LIKE ?",
                arrayOf(intent.getStringExtra(EXTRA_GEOTUNE_ID)))
        Timber.d("Updated $updates geotune(s)")
    }

    private fun deleteGeoTune(intent: Intent) {
        val geoTune = intent.getParcelerParcelableExtra<GeoTune>(EXTRA_GEOTUNE)!!
        if (geoTune.isActive) {
            unregisterGeoTune(intent)
        }
        val deletions = contentResolver.delete(GeoTuneContentProvider.CONTENT_URI,
                GeoTune.KEY_UID + " LIKE ?",
                arrayOf(intent.getStringExtra(EXTRA_GEOTUNE_ID)))
        Timber.d("Deleted $deletions geotune(s)")
    }

    private fun registerGeoTune(intent: Intent) {
        val geoTune = intent.getParcelerParcelableExtra<GeoTune>(EXTRA_GEOTUNE)!!
        val apiClient = connectedApiClient
        if (apiClient != null) {
            var itWorked = false
            try {
                LocationServices.GeofencingApi.addGeofences(apiClient,
                        GeofencingRequest.Builder()
                                .addGeofence(geoTune.toGeofence())
                                .build(),
                        getGeofenceTransitionPendingIntent())
                itWorked = true
            } catch (e: SecurityException) {
                //They should have already accepted the permission if we got here
                Timber.e(e, null)
            }

            //Now update in the DB
            val cv = ContentValues()
            cv.put(GeoTune.KEY_ACTIVE, itWorked)
            intent.putExtra(EXTRA_CONTENT_VALUES, cv)
            intent.putExtra(EXTRA_GEOTUNE_ID, geoTune.id)
            updateGeoTune(intent)
            Timber.d("Registered geotune " + geoTune.name + " success")
        } else {
            Timber.d("Registered geotune " + geoTune.name + " failed")
        }
    }

    private fun reregisterGeoTunes(intent: Intent) {
        val geoTunes = intent.getParcelerParcelableExtra<List<GeoTune>>(EXTRA_GEOTUNE_ARRAY)!!
        val apiClient = connectedApiClient
        if (apiClient != null) {
            val request = GeofencingRequest.Builder()
            for (geoTune in geoTunes) {
                request.addGeofence(geoTune.toGeofence())
            }
            try {
                LocationServices.GeofencingApi.addGeofences(apiClient,
                        request.build(),
                        getGeofenceTransitionPendingIntent())
            } catch (e: SecurityException) {
                Timber.e(e, null)
            }

            Timber.d("All geofences reregistered")
        } else {
            Timber.e("Reregsiter failed")
        }
    }

    private fun unregisterGeoTune(intent: Intent) {
        val geoTune = intent.getParcelerParcelableExtra<GeoTune>(EXTRA_GEOTUNE)!!
        val apiClient = connectedApiClient
        if (apiClient != null) {
            LocationServices.GeofencingApi.removeGeofences(apiClient,
                    Arrays.asList(geoTune.id))
            //Now update in the DB
            val cv = ContentValues()
            cv.put(GeoTune.KEY_ACTIVE, false)
            intent.putExtra(EXTRA_CONTENT_VALUES, cv)
            intent.putExtra(EXTRA_GEOTUNE_ID, geoTune.id)
            updateGeoTune(intent)
            Timber.d("Unregistered geotune " + geoTune.name + " success")
        } else {
            Timber.d("Unregistered geotune " + geoTune.name + " failed")
        }
    }

    private val connectedApiClient: GoogleApiClient?
        get() {
            val apiClient = GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build()
            val connectionResult = apiClient.blockingConnect(10, TimeUnit.SECONDS)
            if (connectionResult.isSuccess) {
                return apiClient
            } else {
                return null
            }
        }

    /**
     * Create a PendingIntent that triggers GeofenceTransitionIntentService when a geofence
     * transition occurs.
     */
    fun getGeofenceTransitionPendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
