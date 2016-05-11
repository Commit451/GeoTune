/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jawnnypoo.geotune.service;

import android.app.IntentService;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.jawnnypoo.geotune.BuildConfig;
import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.loader.GeoTunesLoader;
import com.jawnnypoo.geotune.util.NotificationUtils;

import timber.log.Timber;

/**
 * Listens for geofence transition changes.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent sent by Location Services. This Intent is provided to Location
     * Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("onHandleIntent");
        GeofencingEvent geoFenceEvent = GeofencingEvent.fromIntent(intent);
        if (geoFenceEvent.hasError()) {
            Timber.e("Error with intent " + geoFenceEvent.getErrorCode());
        } else {
            int transitionType = geoFenceEvent.getGeofenceTransition();
            if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType) {
                for (Geofence geofence : geoFenceEvent.getTriggeringGeofences()) {
                    playRegisteredGeoTune(geofence.getRequestId());
                }
            }
        }
    }

    private void playRegisteredGeoTune(String geoTuneId) {
        GeoTune geoTune = GeoTunesLoader.getGeoTune(getApplicationContext(), geoTuneId);
        //Just to make sure
        if (geoTune == null || !geoTune.isActive()) {
            Timber.d("Geofence triggered but geotune null or inactive");
            return;
        }

        Uri tone = geoTune.getTuneUri();
        if (tone == null) {
            tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), tone);
        if (ringtone != null) {
            ringtone.play();
        }
        if (BuildConfig.DEBUG) {
            NotificationUtils.postNotification(getApplicationContext(), geoTune.getName());
        }
    }
}
