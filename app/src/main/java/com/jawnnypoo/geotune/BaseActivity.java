package com.jawnnypoo.geotune;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import com.jawnnypoo.geotune.data.GeoTune;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * Base activity for all others to derive
 */
public class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_GEOFENCE = 819;
    protected static final int REQUEST_AUDIO = 67;
    protected static final int REQUEST_NOTIFICATION = 68;

    public static final String EXTRA_GEOTUNE = "extra_geotune";

    protected void navigateToMap(int[] location, ArrayList<GeoTune> geoTunes) {
        startActivityForResult(GeoMapActivity.newIntent(this, location, geoTunes), REQUEST_GEOFENCE);
        overridePendingTransition(R.anim.do_nothing, R.anim.do_nothing);
    }

    protected void navigateToMap(int[] location, ArrayList<GeoTune> geoTunes, GeoTune startingGeoTune) {
        startActivityForResult(GeoMapActivity.newIntent(this, location, geoTunes, startingGeoTune),
                REQUEST_GEOFENCE);
        overridePendingTransition(R.anim.do_nothing, R.anim.do_nothing);
    }

    protected void navigateToAbout() {
        startActivity(AboutActivity.newInstance(this));
        overridePendingTransition(R.anim.fade_in, R.anim.do_nothing);
    }

    protected void chooseNotification() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        this.startActivityForResult(intent, REQUEST_NOTIFICATION);
    }

    protected void chooseRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        this.startActivityForResult(intent, REQUEST_NOTIFICATION);
    }

    protected void chooseMedia() {
        Timber.d("ChooseAudio called");
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Audio "), REQUEST_AUDIO);
    }

    @Override
    public void finish() {
        super.finish();
        if (this instanceof MainActivity) {

        } else {
            overridePendingTransition(R.anim.do_nothing, R.anim.fade_out);
        }
    }
}
