package com.jawnnypoo.geotune.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.data.GeoTuneContentProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Modify a GeoTune in the background
 */
public class GeoTuneModService extends IntentService {
    private static final String TAG = GeoTuneModService.class.getSimpleName();

    private static final String ACTION_ADD = "ACTION_ADD";
    private static final String ACTION_UPDATE = "ACTION_MODIFY";
    private static final String ACTION_DELETE = "ACTION_DELETE";
    private static final String ACTION_REGISTER = "ACTION_REGISTER";
    private static final String ACTION_REREGISTER = "ACTION_REREGISTER";
    private static final String ACTION_UNREGSITER = "ACTION_UNREGISTER";

    private static final String EXTRA_GEOTUNE = "EXTRA_GEOTUNE";
    private static final String EXTRA_GEOTUNE_ARRAY = "EXTRA_GEOTUNE_ARRAY";
    private static final String EXTRA_GEOTUNE_ID = "EXTRA_GEOTUNE_ID";
    private static final String EXTRA_CONTENT_VALUES = "EXTRA_CONTENT_VALUES";
    /**
     * Sets an identifier for this class' background thread
     */
    public GeoTuneModService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_ADD:
                createGeoTune(intent);
                break;
            case ACTION_UPDATE:
                updateGeoTune(intent);
                break;
            case ACTION_DELETE:
                deleteGeoTune(intent);
                break;
            case ACTION_REGISTER:
                registerGeoTune(intent);
                break;
            case ACTION_REREGISTER:
                reregisterGeoTunes(intent);
                break;
            case ACTION_UNREGSITER:
                unregisterGeoTune(intent);
                break;
        }
    }

    private void createGeoTune(Intent intent) {
        getContentResolver().insert(GeoTuneContentProvider.CONTENT_URI,
                (ContentValues) intent.getParcelableExtra(EXTRA_CONTENT_VALUES));
        Timber.d("Created GeoTune");
    }

    private void updateGeoTune(Intent intent) {
        int updates = getContentResolver().update(GeoTuneContentProvider.CONTENT_URI,
                (ContentValues) intent.getParcelableExtra(EXTRA_CONTENT_VALUES),
                GeoTune.KEY_UID + " LIKE ?",
                new String[] {intent.getStringExtra(EXTRA_GEOTUNE_ID)});
        Timber.d("Updated " + updates + " geotune(s)");
    }

    private void deleteGeoTune(Intent intent) {
        GeoTune geoTune = intent.getParcelableExtra(EXTRA_GEOTUNE);
        if (geoTune.isActive()) {
            unregisterGeoTune(intent);
        }
        int deletions = getContentResolver().delete(GeoTuneContentProvider.CONTENT_URI,
                GeoTune.KEY_UID + " LIKE ?",
                new String[] {intent.getStringExtra(EXTRA_GEOTUNE_ID)});
        Timber.d("Deleted " + deletions + " geotune(s)");
    }

    private void registerGeoTune(Intent intent) {
        GeoTune geoTune = intent.getParcelableExtra(EXTRA_GEOTUNE);
        GoogleApiClient apiClient = getConnectedApiClient();
        if (apiClient != null) {
            boolean itWorked = false;
            try {
                LocationServices.GeofencingApi.addGeofences(apiClient,
                        new GeofencingRequest.Builder()
                                .addGeofence(geoTune.toGeofence())
                                .build(),
                        getGeofenceTransitionPendingIntent());
                itWorked = true;
            } catch (SecurityException e) {
                //They should have already accepted the permission if we got here
                Timber.e(e, null);
            }
            //Now update in the DB
            ContentValues cv = new ContentValues();
            cv.put(GeoTune.KEY_ACTIVE, itWorked);
            intent.putExtra(EXTRA_CONTENT_VALUES, cv);
            intent.putExtra(EXTRA_GEOTUNE_ID, geoTune.getId());
            updateGeoTune(intent);
            Timber.d("Registered geotune " + geoTune.getName() + " success");
        } else {
            Timber.d("Registered geotune " + geoTune.getName() + " failed");
        }
    }

    private void reregisterGeoTunes(Intent intent) {
        ArrayList<GeoTune> geoTunes = intent.getParcelableArrayListExtra(EXTRA_GEOTUNE_ARRAY);
        GoogleApiClient apiClient = getConnectedApiClient();
        if (apiClient != null) {
            GeofencingRequest.Builder request = new GeofencingRequest.Builder();
            for (GeoTune geoTune : geoTunes) {
                request.addGeofence(geoTune.toGeofence());
            }
            try {
                LocationServices.GeofencingApi.addGeofences(apiClient,
                        request.build(),
                        getGeofenceTransitionPendingIntent());
            } catch (SecurityException e) {
                Timber.e(e, null);
            }
            Timber.d("All geofences reregistered");
        } else {
            Timber.e("Reregsiter failed");
        }
    }

    private void unregisterGeoTune(Intent intent) {
        GeoTune geoTune = intent.getParcelableExtra(EXTRA_GEOTUNE);
        GoogleApiClient apiClient = getConnectedApiClient();
        if (apiClient != null) {
            LocationServices.GeofencingApi.removeGeofences(apiClient,
                    Arrays.asList(geoTune.getId()));
            //Now update in the DB
            ContentValues cv = new ContentValues();
            cv.put(GeoTune.KEY_ACTIVE, false);
            intent.putExtra(EXTRA_CONTENT_VALUES, cv);
            intent.putExtra(EXTRA_GEOTUNE_ID, geoTune.getId());
            updateGeoTune(intent);
            Timber.d("Unregistered geotune " + geoTune.getName() + " success");
        } else {
            Timber.d("Unregistered geotune " + geoTune.getName() + " failed");
        }
    }

    private GoogleApiClient getConnectedApiClient() {
        GoogleApiClient apiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        ConnectionResult connectionResult = apiClient.blockingConnect(10, TimeUnit.SECONDS);
        if (connectionResult.isSuccess()) {
            return apiClient;
        } else {
            return null;
        }
    }

    /**
     * Create a PendingIntent that triggers GeofenceTransitionIntentService when a geofence
     * transition occurs.
     */
    private PendingIntent getGeofenceTransitionPendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void addGeoTune(Context context, ContentValues contentValues) {
        Intent modIntent = new Intent(context, GeoTuneModService.class);
        modIntent.setAction(ACTION_ADD);
        modIntent.putExtra(EXTRA_CONTENT_VALUES, contentValues);
        context.startService(modIntent);
    }

    public static void updateGeoTune(Context context, ContentValues contentValues, String geoTuneId) {
        Intent modIntent = new Intent(context, GeoTuneModService.class);
        modIntent.setAction(ACTION_UPDATE);
        modIntent.putExtra(EXTRA_CONTENT_VALUES, contentValues);
        modIntent.putExtra(EXTRA_GEOTUNE_ID, geoTuneId);
        context.startService(modIntent);
    }

    public static void deleteGeoTune(Context context, GeoTune geoTune) {
        Intent modIntent = new Intent(context, GeoTuneModService.class);
        modIntent.setAction(ACTION_DELETE);
        modIntent.putExtra(EXTRA_GEOTUNE, geoTune);
        context.startService(modIntent);
    }

    public static void registerGeoTune(Context context, GeoTune geoTune) {
        Intent modIntent = new Intent(context, GeoTuneModService.class);
        modIntent.setAction(ACTION_REGISTER);
        modIntent.putExtra(EXTRA_GEOTUNE, geoTune);
        context.startService(modIntent);
    }

    public static void reregisterGeoTunes(Context context, ArrayList<GeoTune> geoTunes) {
        Intent modIntent = new Intent(context, GeoTuneModService.class);
        modIntent.setAction(ACTION_REREGISTER);
        modIntent.putExtra(EXTRA_GEOTUNE_ARRAY, geoTunes);
        context.startService(modIntent);
    }

    public static void unregisterGeoTune(Context context, GeoTune geoTune) {
        Intent modIntent = new Intent(context, GeoTuneModService.class);
        modIntent.setAction(ACTION_UNREGSITER);
        modIntent.putExtra(EXTRA_GEOTUNE, geoTune);
        context.startService(modIntent);
    }
}
