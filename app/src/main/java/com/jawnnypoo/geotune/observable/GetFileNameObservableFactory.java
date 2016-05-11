package com.jawnnypoo.geotune.observable;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.jawnnypoo.geotune.R;

import rx.Observable;
import rx.functions.Func0;
import timber.log.Timber;

/**
 * Simple Observable that gets a file name
 */
public class GetFileNameObservableFactory {

    public static Observable<String> create(final Context context, final Uri uri) {
        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just(fetchFileName(context, uri));
            }
        });
    }

    private static String fetchFileName(Context context, Uri uri) {
        /*
     * Get the file's content URI from the incoming Intent,
     * then query the server app to get the file's display name
     * and size.
     */
        Timber.d("Retrieving tune name...");
        Cursor returnCursor =
                context.getContentResolver().query(uri, null, null, null, null);
    /*
     * Get the column indexes of the data in the Cursor,
     * move to the first row in the Cursor, get the data,
     * and display it.
     */
        if (returnCursor == null) {
            //This should probably just return null, but oh well
            return context.getString(R.string.media_file);
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
}
