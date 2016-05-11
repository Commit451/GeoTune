package com.jawnnypoo.geotune.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;

import com.jawnnypoo.geotune.MainActivity;
import com.jawnnypoo.geotune.R;
import com.jawnnypoo.geotune.data.GeoTune;

import timber.log.Timber;

/**
 * Post all the notifications
 */
public class NotificationUtils {

    public static void postNotification(Context context, String name) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        Intent intentToLaunch = new Intent(context, MainActivity.class);

        // Set the notification contents
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You are in geofence: " + name);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intentToLaunch, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        notificationManager.notify(1337, builder.build());
    }

    public static void playTune(Context context, GeoTune geoTune) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        MediaPlayer mp;
        //TODO make sure we match the name of the geofence in storage with the one that was tripped
        // Set the notification contents
        if(geoTune.getTuneUri() == null) {
            Timber.d("Tone was null, playing default");
            mp = MediaPlayer.create(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        } else {
            Timber.d("Setting tone to uri");
            mp = MediaPlayer.create(context, geoTune.getTuneUri());
            builder.setSound(geoTune.getTuneUri());
        }
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        Timber.d("Playing tone!");
        //NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(1337, builder.build());
    }
}
