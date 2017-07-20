package com.jawnnypoo.geotune.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.activity.MainActivity
import com.jawnnypoo.geotune.data.GeoTune
import timber.log.Timber

/**
 * Post all the notifications
 */
object NotificationUtils {

    fun postNotification(context: Context, name: String) {

        val builder = NotificationCompat.Builder(context)

        val intentToLaunch = Intent(context, MainActivity::class.java)

        // Set the notification contents
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You are in geofence: " + name)

        val resultPendingIntent = PendingIntent.getActivity(context, 0, intentToLaunch, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(resultPendingIntent)

        // Get an instance of the Notification manager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Issue the notification
        notificationManager.notify(1337, builder.build())
    }

    fun playTune(context: Context, geoTune: GeoTune) {
        val builder = NotificationCompat.Builder(context)

        val mp: MediaPlayer
        //TODO make sure we match the name of the geofence in storage with the one that was tripped
        // Set the notification contents
        if (geoTune.tuneUri == null) {
            Timber.d("Tone was null, playing default")
            mp = MediaPlayer.create(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            Timber.d("Setting tone to uri")
            mp = MediaPlayer.create(context, geoTune.tuneUri)
            builder.setSound(geoTune.tuneUri)
        }
        mp.start()
        mp.setOnCompletionListener { mp -> mp.release() }
        Timber.d("Playing tone!")
        //NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(1337, builder.build());
    }
}
