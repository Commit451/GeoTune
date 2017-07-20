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

package com.jawnnypoo.geotune.activity

import android.Manifest
import android.annotation.TargetApi
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
import android.support.v7.widget.RecyclerView
import android.view.View

import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.adapter.GeoTuneAdapter
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.dialog.ChooseUriDialog
import com.jawnnypoo.geotune.dialog.EditNameDialog
import com.jawnnypoo.geotune.loader.GeoTunesLoader
import com.jawnnypoo.geotune.observable.GetFileNameObservableFactory
import com.jawnnypoo.geotune.rx.CustomSingleObserver
import com.jawnnypoo.geotune.service.GeoTuneModService
import com.jawnnypoo.geotune.util.NotificationUtils

import java.util.ArrayList

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Activity that shows all of the GeoTunes
 */
class MainActivity : BaseActivity(), LoaderManager.LoaderCallbacks<ArrayList<GeoTune>> {

    //Dialogs
    private var editNameDialog: EditNameDialog? = null
    private var chooseUriDialog: ChooseUriDialog? = null
    //Views
    @BindView(R.id.main_content) internal var root: View? = null
    @BindView(R.id.list) internal var listGeofences: RecyclerView? = null
    @BindView(R.id.fab) internal var fab: View? = null
    @BindView(R.id.empty_view) internal var emptyView: View? = null
    //Data
    private var activeGeotune: GeoTune? = null
    private var adapter: GeoTuneAdapter? = null

    private val mOnUriChoiceListener = ChooseUriDialog.OnUriChoiceMadeListener { choice ->
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

    private val mEditNameListener = EditNameDialog.OnEditNameDialogListener { name ->
        activeGeotune!!.name = name
        adapter!!.onGeoTuneChanged(activeGeotune)
        editNameDialog!!.dismiss()
        val cv = ContentValues()
        cv.put(GeoTune.KEY_NAME, name)
        GeoTuneModService.updateGeoTune(this@MainActivity, cv, activeGeotune!!.id)
        activeGeotune = null
    }

    @OnClick(R.id.toolbar_title)
    fun onToolbarTitleClick() {
        navigateToAbout()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        if (savedInstanceState != null) {
            activeGeotune = savedInstanceState.getParcelable<GeoTune>(STATE_ACTIVE_GEOTUNE)
        }

        fab!!.setOnClickListener { onAddClicked() }
        emptyView!!.setOnClickListener { onAddClicked() }

        listGeofences!!.layoutManager = LinearLayoutManager(this)
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
                val location = intArrayOf(fab!!.x.toInt(), fab!!.y.toInt())
                navigateToMap(location, adapter!!.geoTunes, geoTune)
            }
        })
        listGeofences!!.adapter = adapter

        setupDialogs()
        loaderManager.initLoader(LOADER_GEOTUNES, null, this)
    }

    private fun onAddClicked() {
        val location = intArrayOf(fab!!.x.toInt(), fab!!.y.toInt())
        navigateToMap(location, adapter!!.geoTunes)
    }

    private fun setupDialogs() {
        editNameDialog = EditNameDialog(this)
        chooseUriDialog = ChooseUriDialog(this)
        editNameDialog!!.setOnEditNameListener(mEditNameListener)
        chooseUriDialog!!.setOnUriChoiceMadeListener(mOnUriChoiceListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BaseActivity.Companion.REQUEST_GEOFENCE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val newGeoTune = data.getParcelableExtra<GeoTune>(BaseActivity.Companion.EXTRA_GEOTUNE)
                newGeoTune.isActive = true
                adapter!!.addGeoTune(newGeoTune)
                adapterDataChanged()
                GeoTuneModService.registerGeoTune(applicationContext, newGeoTune)
                Snackbar.make(root!!, getString(R.string.reminder_set_tune), Snackbar.LENGTH_LONG)
                        .show()
            }
            BaseActivity.Companion.REQUEST_AUDIO -> if (resultCode == Activity.RESULT_OK) {
                if (data!!.data != null) {
                    Timber.d("Uri was found: %s", data.data)
                    updateGeotuneUri(data.data)
                }
            }
            BaseActivity.Companion.REQUEST_NOTIFICATION -> if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                updateGeotuneUri(uri)
            }
        }
    }

    private fun updateGeotuneUri(uri: Uri) {
        if (activeGeotune != null) {
            GetFileNameObservableFactory.create(applicationContext, uri)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : CustomSingleObserver<String>() {
                        override fun success(name: String) {
                            activeGeotune!!.tuneUri = uri
                            activeGeotune!!.tuneName = name
                            adapter!!.onGeoTuneChanged(activeGeotune)
                            NotificationUtils.playTune(this@MainActivity, activeGeotune)
                            val cv = ContentValues()
                            cv.put(GeoTune.KEY_TUNE, uri.toString())
                            cv.put(GeoTune.KEY_TUNE_NAME, name)
                            GeoTuneModService.updateGeoTune(this@MainActivity, cv, activeGeotune!!.id)
                            activeGeotune = null
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
        outState.putParcelable(STATE_ACTIVE_GEOTUNE, activeGeotune)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<ArrayList<GeoTune>>? {
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
                if (data != null && adapter != null) {
                    Timber.d("onLoadFinished")
                    adapter!!.setGeofences(data)
                }
                adapterDataChanged()
            }
        }
    }

    override fun onLoaderReset(loader: Loader<*>) {}

    /**
     * Normally you would do this in an observer, but oh well
     */
    private fun adapterDataChanged() {
        if (adapter!!.itemCount > 0) {
            emptyView!!.visibility = View.GONE
        } else {
            emptyView!!.visibility = View.VISIBLE
        }
    }

    companion object {

        private val STATE_ACTIVE_GEOTUNE = "active_geotune"

        private val LOADER_GEOTUNES = 123

        private val PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE = 1337
    }
}
