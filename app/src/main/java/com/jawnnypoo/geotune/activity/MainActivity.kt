package com.jawnnypoo.geotune.activity

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.LoaderManager
import android.content.ContentValues
import android.content.Intent
import android.content.Loader
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.commit451.addendum.parceler.getParcelerParcelable
import com.commit451.addendum.parceler.getParcelerParcelableExtra
import com.commit451.addendum.parceler.putParcelerParcelable
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.adapter.GeoTuneAdapter
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.dialog.ChooseUriDialog
import com.jawnnypoo.geotune.dialog.EditNameDialog
import com.jawnnypoo.geotune.loader.GeoTunesLoader
import com.jawnnypoo.geotune.rx.CustomSingleObserver
import com.jawnnypoo.geotune.service.GeoTuneModService
import com.jawnnypoo.geotune.util.FileNameHelper
import com.jawnnypoo.geotune.util.NotificationUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.*

/**
 * Activity that shows all of the GeoTunes
 */
class MainActivity : BaseActivity(), LoaderManager.LoaderCallbacks<ArrayList<GeoTune>> {

    companion object {

        private const val STATE_ACTIVE_GEOTUNE = "active_geotune"

        private const val LOADER_GEOTUNES = 123

        private const val PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE = 1337
    }

    var editNameDialog: EditNameDialog? = null
    var chooseUriDialog: ChooseUriDialog? = null

    var activeGeotune: GeoTune? = null
    lateinit var adapter: GeoTuneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            activeGeotune = savedInstanceState.getParcelerParcelable<GeoTune>(STATE_ACTIVE_GEOTUNE)
        }

        toolbarTitle.setOnClickListener {
            navigateToAbout()
        }

        fab.setOnClickListener { onAddClicked() }
        emptyView.setOnClickListener { onAddClicked() }

        list.layoutManager = LinearLayoutManager(this)
        adapter = GeoTuneAdapter(object : GeoTuneAdapter.Callback {
            override fun onSetNotificationClicked(geoTune: GeoTune) {
                activeGeotune = geoTune
                val permissionCheck = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    chooseUriDialog!!.show()
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE)
                }
            }

            override fun onRenameClicked(geoTune: GeoTune) {
                activeGeotune = geoTune
                editNameDialog!!.setName(geoTune.name)
                editNameDialog!!.show()
            }

            override fun onDeleteClicked(geoTune: GeoTune) {
                GeoTuneModService.deleteGeoTune(this@MainActivity, geoTune)
                adapterDataChanged()
            }

            override fun onGeoTuneSwitched(isChecked: Boolean, geoTune: GeoTune) {
                //Update with GPlay
                if (isChecked) {
                    GeoTuneModService.registerGeoTune(applicationContext, geoTune)
                } else {
                    GeoTuneModService.unregisterGeoTune(applicationContext, geoTune)
                }
            }

            override fun onGeoTuneClicked(geoTune: GeoTune) {
                val location = intArrayOf(fab.x.toInt(), fab.y.toInt())
                navigateToMap(location, adapter.geoTunes, geoTune)
            }
        })
        list.adapter = adapter

        setupDialogs()
        loaderManager.initLoader(LOADER_GEOTUNES, null, this)
    }

    fun onAddClicked() {
        val location = intArrayOf(fab.x.toInt(), fab.y.toInt())
        navigateToMap(location, adapter.geoTunes)
    }

    fun setupDialogs() {
        editNameDialog = EditNameDialog(this)
        chooseUriDialog = ChooseUriDialog(this)
        editNameDialog!!.setOnEditNameListener { name ->
            activeGeotune!!.name = name
            adapter.onGeoTuneChanged(activeGeotune!!)
            editNameDialog!!.dismiss()
            val cv = ContentValues()
            cv.put(GeoTune.KEY_NAME, name)
            GeoTuneModService.updateGeoTune(this@MainActivity, cv, activeGeotune!!.id!!)
            activeGeotune = null
        }
        chooseUriDialog!!.setOnUriChoiceMadeListener { choice ->
            when (choice) {
                ChooseUriDialog.UriChoice.NOTIFICATION -> {
                    chooseNotification()
                    chooseUriDialog!!.dismiss()
                }
                ChooseUriDialog.UriChoice.MEDIA -> {
                    chooseMedia()
                    chooseUriDialog!!.dismiss()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BaseActivity.REQUEST_GEOFENCE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val newGeoTune = data.getParcelerParcelableExtra<GeoTune>(BaseActivity.Companion.EXTRA_GEOTUNE)!!
                newGeoTune.isActive = true
                adapter.addGeoTune(newGeoTune)
                adapterDataChanged()
                GeoTuneModService.registerGeoTune(applicationContext, newGeoTune)
                Snackbar.make(root, getString(R.string.reminder_set_tune), Snackbar.LENGTH_LONG)
                        .show()
            }
            BaseActivity.REQUEST_AUDIO -> if (resultCode == Activity.RESULT_OK) {
                if (data!!.data != null) {
                    Timber.d("Uri was found: %s", data.data)
                    updateGeotuneUri(data.data)
                }
            }
            BaseActivity.REQUEST_NOTIFICATION -> if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                updateGeotuneUri(uri)
            }
        }
    }

    fun updateGeotuneUri(uri: Uri) {
        if (activeGeotune != null) {
            FileNameHelper.queryFileName(applicationContext, uri)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : CustomSingleObserver<String>() {
                        override fun success(name: String) {
                            val geoTune = activeGeotune!!
                            geoTune.tuneUri = uri
                            geoTune.tuneName = name
                            adapter.onGeoTuneChanged(geoTune)
                            NotificationUtils.playTune(this@MainActivity, geoTune)
                            val cv = ContentValues()
                            cv.put(GeoTune.KEY_TUNE, uri.toString())
                            cv.put(GeoTune.KEY_TUNE_NAME, name)
                            GeoTuneModService.updateGeoTune(this@MainActivity, cv, geoTune.id!!)
                            this@MainActivity.activeGeotune = null
                        }

                        override fun error(throwable: Throwable) {
                            Timber.e(throwable)
                        }
                    })
        }

    }

    @TargetApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseUriDialog!!.show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelerParcelable(STATE_ACTIVE_GEOTUNE, activeGeotune)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<ArrayList<GeoTune>>? {
        when (id) {
            LOADER_GEOTUNES -> {
                Timber.d("onCreateLoader")
                return GeoTunesLoader(applicationContext)
            }
        }
        return null
    }

    override fun onLoadFinished(loader: Loader<ArrayList<GeoTune>>, data: ArrayList<GeoTune>?) {
        when (loader.id) {
            LOADER_GEOTUNES -> {
                if (data != null) {
                    Timber.d("onLoadFinished")
                    adapter.setGeofences(data)
                }
                adapterDataChanged()
            }
        }
    }

    override fun onLoaderReset(loader: Loader<ArrayList<GeoTune>>?) {}

    /**
     * Normally you would do this in an observer, but oh well
     */
    fun adapterDataChanged() {
        if (adapter.itemCount > 0) {
            emptyView.visibility = View.GONE
        } else {
            emptyView.visibility = View.VISIBLE
        }
    }
}
