package com.jawnnypoo.geotune.data;
/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.Geofence;

/**
 * A GeoTune, which is basically a geofence associated with a tune (ringtone) that plays
 * when you enter the fence
 */
public class GeoTune implements Parcelable {
    // Instance variables
    private String mName;
    private String mId;
    private double mLatitude;
    private double mLongitude;
    private float mRadius;
    private int mTransitionType;
    private Uri mTune;
    private String mTuneName;
    private boolean mActive;

    public GeoTune(
            String geofenceName,
            String id,
            double latitude,
            double longitude,
            float radius,
            int transition,
            Uri tune,
            String tuneName,
            boolean active) {
        // Set the instance fields from the constructor

        mName = geofenceName;
        // An identifier for the geofence
        mId = id;

        // Center of the geofence
        mLatitude = latitude;
        mLongitude = longitude;

        // Radius of the geofence, in meters
        mRadius = radius;

        // Transition type
        mTransitionType = transition;
        mTune = tune;
        mTuneName = tuneName;
        mActive = active;
    }

    // Instance field getters

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    /**
     * Get the GeoTune ID, which acts as our ID in our local DB as well as
     * the request ID for the geofence
     */
    public String getId() {
        return mId;
    }

    /**
     * Get the geofence latitude
     * @return A latitude value
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Get the geofence longitude
     * @return A longitude value
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Get the geofence radius
     * @return A radius value
     */
    public float getRadius() {
        return mRadius;
    }

    /**
     * Get the geofence transition type
     * @return Transition type (see {@link Geofence})
     */
    public int getTransitionType() {
        return mTransitionType;
    }

    public Uri getTuneUri() { return mTune; }

    public void setTuneUri(Uri uri) { mTune = uri; }

    public String getTuneName() { return mTuneName; }

    public void setTuneName(String name) { mTuneName = name; }

    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    /**
     * Creates a Location Services Geofence object from a
     * SimpleGeofence.
     *
     * @return A Geofence object
     */
    public Geofence toGeofence() {
        // Build a new Geofence object
        return new Geofence.Builder()
                .setRequestId(getId())
                .setTransitionTypes(mTransitionType)
                .setCircularRegion(
                        getLatitude(),
                        getLongitude(),
                        getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }

    public static final String KEY_UID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lng";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_TRANSITION_TYPE = "transition";
    public static final String KEY_TUNE = "tune";
    public static final String KEY_TUNE_NAME = "tuneName";
    public static final String KEY_ACTIVE = "active";

    public static GeoTune fromCursor(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(KEY_UID));
        String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
        double latitude = cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE));
        double longitude = cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE));
        float radius = cursor.getFloat(cursor.getColumnIndex(KEY_RADIUS));
        int transitionType = cursor.getInt(cursor.getColumnIndex(KEY_TRANSITION_TYPE));
        String tuneString = cursor.getString(cursor.getColumnIndex(KEY_TUNE));
        Uri tune = tuneString == null ? null : Uri.parse(tuneString);
        String tuneName = cursor.getString(cursor.getColumnIndex(KEY_TUNE_NAME));
        boolean active = cursor.getInt(cursor.getColumnIndex(KEY_ACTIVE)) == 1;
        return new GeoTune(name, id, latitude,longitude,radius,
                transitionType,tune,tuneName,active);
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        // Assign values for each row.
        cv.put(KEY_UID, getId());
        cv.put(KEY_NAME, getName());
        cv.put(KEY_LATITUDE, getLatitude());
        cv.put(KEY_LONGITUDE, getLongitude());
        cv.put(KEY_RADIUS, getRadius());
        cv.put(KEY_TRANSITION_TYPE, getTransitionType());
        cv.put(KEY_TUNE, getTuneUri() == null ? null : getTuneUri().toString());
        cv.put(KEY_TUNE_NAME, getTuneName());
        cv.put(KEY_ACTIVE, isActive());
        return cv;
    }

    protected GeoTune(Parcel in) {
        mName = in.readString();
        mId = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mRadius = in.readFloat();
        mTransitionType = in.readInt();
        String uri = in.readString();
        if (uri.equals("")) {
            mTune = null;
        } else {
            mTune = Uri.parse(uri);
        }
        mTuneName = in.readString();
        mActive = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mId);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeFloat(mRadius);
        dest.writeInt(mTransitionType);
        if (mTune != null) {
            dest.writeString(mTune.toString());
        } else {
            dest.writeString("");
        }
        dest.writeString(mTuneName);
        dest.writeInt(mActive ? 1 : 0);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<GeoTune> CREATOR = new Parcelable.Creator<GeoTune>() {
        @Override
        public GeoTune createFromParcel(Parcel in) {
            return new GeoTune(in);
        }

        @Override
        public GeoTune[] newArray(int size) {
            return new GeoTune[size];
        }
    };
}