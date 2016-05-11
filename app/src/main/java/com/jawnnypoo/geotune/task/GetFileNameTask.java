package com.jawnnypoo.geotune.task;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.widget.RecyclerView;

import com.jawnnypoo.geotune.R;
import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.service.GeoTuneModService;

import timber.log.Timber;

/**
 * Task to get the name of a file from the system
 */
public class GetFileNameTask extends AsyncTask<Uri, Void, String> {

    private Context mContext;
    private GeoTune mGeotune;
    private RecyclerView.Adapter mAdapterToAlert;

    public GetFileNameTask(Context context, GeoTune geoTune, RecyclerView.Adapter adapter) {
        mContext = context;
        mGeotune = geoTune;
        mAdapterToAlert = adapter;
    }

    protected String doInBackground(Uri... entries) {
        /*
     * Get the file's content URI from the incoming Intent,
     * then query the server app to get the file's display name
     * and size.
     */
        Timber.d("Retrieving tune name...");
        Cursor returnCursor =
                mContext.getContentResolver().query(entries[0], null, null, null, null);
    /*
     * Get the column indexes of the data in the Cursor,
     * move to the first row in the Cursor, get the data,
     * and display it.
     */
        if (returnCursor == null) {
            return mContext.getString(R.string.media_file);
        }
        int nameIndex = returnCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        if (nameIndex == -1) {
            nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        }
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        if (!returnCursor.isClosed()) {
            returnCursor.close();
        }
        return name;
    }

    protected void onProgressUpdate(Void... progress) {
        //We do not track progress and show it to the user, since it will happen so quickly
    }

    protected void onPostExecute(String result) {
        Timber.d("Found file name to be " + result);
        if (mGeotune != null) {
            mGeotune.setTuneName(result);
            ContentValues cv = new ContentValues();
            cv.put(GeoTune.KEY_TUNE_NAME, result);
            GeoTuneModService.updateGeoTune(mContext, cv, mGeotune.getId());
        }
        if (mAdapterToAlert != null) {
            mAdapterToAlert.notifyDataSetChanged();
        }
    }
}
