package com.jawnnypoo.geotune.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.jawnnypoo.geotune.R
import io.reactivex.Single

/**
 * Simple Observable that gets a file name
 */
object FileNameHelper {

    fun queryFileName(context: Context, uri: Uri): Single<String> {
        return Single.defer {

            val returnCursor = context.contentResolver.query(uri, null, null, null, null) ?:
                    return@defer Single.just(context.getString(R.string.media_file))

            var nameIndex = returnCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            if (nameIndex == -1) {
                nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            }
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            if (!returnCursor.isClosed) {
                returnCursor.close()
            }

            Single.just(name)
        }
    }
}
