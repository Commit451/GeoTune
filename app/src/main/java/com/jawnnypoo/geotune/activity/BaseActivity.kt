package com.jawnnypoo.geotune.activity

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.data.GeoTune
import timber.log.Timber
import java.util.*

/**
 * Base activity for all others to derive
 */
open class BaseActivity : AppCompatActivity() {

    companion object {

        const val REQUEST_GEOFENCE = 819
        const val REQUEST_AUDIO = 67
        const val REQUEST_NOTIFICATION = 68

        val EXTRA_GEOTUNE = "extra_geotune"
    }

    protected fun navigateToMap(location: IntArray, geoTunes: List<GeoTune>) {
        startActivityForResult(GeoMapActivity.newIntent(this, location, geoTunes), REQUEST_GEOFENCE)
        overridePendingTransition(R.anim.do_nothing, R.anim.do_nothing)
    }

    protected fun navigateToMap(location: IntArray, geoTunes: List<GeoTune>, startingGeoTune: GeoTune) {
        startActivityForResult(GeoMapActivity.newIntent(this, location, geoTunes, startingGeoTune),
                REQUEST_GEOFENCE)
        overridePendingTransition(R.anim.do_nothing, R.anim.do_nothing)
    }

    protected fun navigateToAbout() {
        startActivity(AboutActivity.newInstance(this))
        overridePendingTransition(R.anim.fade_in, R.anim.do_nothing)
    }

    protected fun chooseNotification() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
        this.startActivityForResult(intent, REQUEST_NOTIFICATION)
    }

    protected fun chooseRingtone() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
        this.startActivityForResult(intent, REQUEST_NOTIFICATION)
    }

    protected fun chooseMedia() {
        Timber.d("ChooseAudio called")
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Audio "), REQUEST_AUDIO)
    }

    override fun finish() {
        super.finish()
        if (this is MainActivity) {

        } else {
            overridePendingTransition(R.anim.do_nothing, R.anim.fade_out)
        }
    }
}
