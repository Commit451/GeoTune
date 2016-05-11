package com.jawnnypoo.geotune.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.data.GeoTuneContentProvider;

import java.util.ArrayList;

/**
 * Get all the geotunes we have in storage
 */
public class GeoTunesLoader extends AsyncTaskLoader<ArrayList<GeoTune>> {

    private Context mContext;
    private static Uri DB_URI = GeoTuneContentProvider.CONTENT_URI;

    public GeoTunesLoader(Context context) {
        super(context);
        mContext = context;
        forceLoad();
    }

    @Override
    public ArrayList<GeoTune> loadInBackground() {
        return getGeoTunes(mContext);
    }

    public static ArrayList<GeoTune> getGeoTunes(Context context) {
        Cursor cursor = context.getContentResolver().query(DB_URI, null, null, null, null);
        ArrayList<GeoTune> geoTunes = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            //Iterate on all the entries we got back
            while (cursor.moveToNext()) {
                geoTunes.add(GeoTune.fromCursor(cursor));
            }
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
        return geoTunes;
    }

    public static GeoTune getGeoTune(Context context, String id) {
        Cursor cursor = context.getContentResolver().query(
                DB_URI,
                null,
                GeoTune.KEY_UID + " LIKE ?",
                new String[]{ id },
                null);
        cursor.moveToNext();
        GeoTune geoTune = GeoTune.fromCursor(cursor);
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return geoTune;
    }
}
