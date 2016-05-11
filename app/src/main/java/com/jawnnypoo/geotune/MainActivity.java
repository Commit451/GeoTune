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

package com.jawnnypoo.geotune;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.jawnnypoo.geotune.adapter.GeoTuneAdapter;
import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.dialog.ChooseUriDialog;
import com.jawnnypoo.geotune.dialog.EditNameDialog;
import com.jawnnypoo.geotune.loader.GeoTunesLoader;
import com.jawnnypoo.geotune.observable.GetFileNameObservableFactory;
import com.jawnnypoo.geotune.service.GeoTuneModService;
import com.jawnnypoo.geotune.util.NotificationUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Activity that shows all of the GeoTunes
 */
public class MainActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<ArrayList<GeoTune>> {

    private static final String STATE_ACTIVE_GEOTUNE = "active_geotune";

    private static final int LOADER_GEOTUNES = 123;

    private static final int PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE = 1337;

    //Dialogs
    private EditNameDialog mEditNameDialog;
    private ChooseUriDialog mChooseUriDialog;
    //Views
    @BindView(R.id.main_content) View mRoot;
    @BindView(R.id.list) RecyclerView mListGeofences;
    @BindView(R.id.fab) View mFab;
    @BindView(R.id.empty_view) View mEmptyView;
    //Data
    private GeoTune mActiveGeoTune;
    private GeoTuneAdapter mGeoTuneAdapter;

    private final View.OnClickListener mOnAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int[] location = {(int) mFab.getX(), (int) mFab.getY()};
            navigateToMap(location, mGeoTuneAdapter.getGeoTunes());
        }
    };

    private final ChooseUriDialog.OnUriChoiceMadeListener mOnUriChoiceListener = new ChooseUriDialog.OnUriChoiceMadeListener() {
        @Override
        public void onChoiceMade(ChooseUriDialog.UriChoice choice) {
            switch (choice) {
                case NOTIFICATION:
                    chooseNotification();
                    mChooseUriDialog.dismiss();
                    break;
                case MEDIA:
                    chooseMedia();
                    mChooseUriDialog.dismiss();
                    break;
            }
        }
    };

    private final EditNameDialog.OnEditNameDialogListener mEditNameListener = new EditNameDialog.OnEditNameDialogListener() {
        @Override
        public void onNameEdited(String name) {
            mActiveGeoTune.setName(name);
            mGeoTuneAdapter.onGeoTuneChanged(mActiveGeoTune);
            mEditNameDialog.dismiss();
            ContentValues cv = new ContentValues();
            cv.put(GeoTune.KEY_NAME, name);
            GeoTuneModService.updateGeoTune(MainActivity.this, cv, mActiveGeoTune.getId());
            mActiveGeoTune = null;
        }
    };

    private final GeoTuneAdapter.Callback mCallback = new GeoTuneAdapter.Callback() {
        @Override
        public void onSetNotificationClicked(GeoTune geoTune) {
            mActiveGeoTune = geoTune;
            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                mChooseUriDialog.show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE);
            }
        }

        @Override
        public void onRenameClicked(GeoTune geoTune) {
            mActiveGeoTune = geoTune;
            mEditNameDialog.setName(geoTune.getName());
            mEditNameDialog.show();
        }

        @Override
        public void onDeleteClicked(GeoTune geoTune) {
            GeoTuneModService.deleteGeoTune(MainActivity.this, geoTune);
            adapterDataChanged();
        }

        @Override
        public void onGeoTuneSwitched(boolean isChecked, GeoTune geoTune) {
            //Update with GPlay
            if (isChecked) {
                GeoTuneModService.registerGeoTune(getApplicationContext(), geoTune);
            } else {
                GeoTuneModService.unregisterGeoTune(getApplicationContext(), geoTune);
            }
        }

        @Override
        public void onGeoTuneClicked(GeoTune geoTune) {
            int[] location = {(int) mFab.getX(), (int) mFab.getY()};
            navigateToMap(location, mGeoTuneAdapter.getGeoTunes(), geoTune);
        }
    };

    @OnClick(R.id.toolbar_title)
    void onToolbarTitleClick() {
        navigateToAbout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            mActiveGeoTune = savedInstanceState.getParcelable(STATE_ACTIVE_GEOTUNE);
        }

        mFab.setOnClickListener(mOnAddClickListener);
        mEmptyView.setOnClickListener(mOnAddClickListener);

        mListGeofences.setLayoutManager(new LinearLayoutManager(this));
        mGeoTuneAdapter = new GeoTuneAdapter(mCallback);
        mListGeofences.setAdapter(mGeoTuneAdapter);

        setupDialogs();
        getLoaderManager().initLoader(LOADER_GEOTUNES, null, this);
    }

    private void setupDialogs() {
        mEditNameDialog = new EditNameDialog(this);
        mChooseUriDialog = new ChooseUriDialog(this);
        mEditNameDialog.setOnEditNameListener(mEditNameListener);
        mChooseUriDialog.setOnUriChoiceMadeListener(mOnUriChoiceListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GEOFENCE:
                if (resultCode == RESULT_OK && data != null) {
                    GeoTune newGeoTune = data.getParcelableExtra(EXTRA_GEOTUNE);
                    newGeoTune.setActive(true);
                    mGeoTuneAdapter.addGeoTune(newGeoTune);
                    adapterDataChanged();
                    GeoTuneModService.registerGeoTune(getApplicationContext(), newGeoTune);
                    Snackbar.make(mRoot, getString(R.string.reminder_set_tune), Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
            case REQUEST_AUDIO:
                if (resultCode == RESULT_OK) {
                    if (data.getData() != null) {
                        Timber.d("Uri was found: %s", data.getData());
                        updateGeotuneUri(data.getData());
                    }
                }
                break;
            case REQUEST_NOTIFICATION:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    updateGeotuneUri(uri);
                }
                break;
        }
    }

    private void updateGeotuneUri(final Uri uri) {
        if (mActiveGeoTune != null) {
            GetFileNameObservableFactory.create(getApplicationContext(), uri)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String name) {
                            mActiveGeoTune.setTuneUri(uri);
                            mActiveGeoTune.setTuneName(name);
                            mGeoTuneAdapter.onGeoTuneChanged(mActiveGeoTune);
                            NotificationUtils.playTune(MainActivity.this, mActiveGeoTune);
                            ContentValues cv = new ContentValues();
                            cv.put(GeoTune.KEY_TUNE, uri.toString());
                            cv.put(GeoTune.KEY_TUNE_NAME, name);
                            GeoTuneModService.updateGeoTune(MainActivity.this, cv, mActiveGeoTune.getId());
                            mActiveGeoTune = null;
                        }
                    });
        }

    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_SAVE_TONE_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mChooseUriDialog.show();
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_ACTIVE_GEOTUNE, mActiveGeoTune);
    }

    @Override
    public Loader<ArrayList<GeoTune>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_GEOTUNES:
                Timber.d("onCreateLoader");
                return new GeoTunesLoader(getApplicationContext());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<GeoTune>> loader, ArrayList<GeoTune> data) {
        switch (loader.getId()) {
            case LOADER_GEOTUNES:
                if (data != null && mGeoTuneAdapter != null) {
                    Timber.d("onLoadFinished");
                    mGeoTuneAdapter.setGeofences(data);
                }
                adapterDataChanged();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {}

    private ArrayList<GeoTune> getFakeGeofenceList(int number) {
        ArrayList<GeoTune> geofences = new ArrayList<GeoTune>();
        for (int i = 0; i < number; i++) {
            geofences.add(new GeoTune("Neat " + i, String.valueOf(i), i, i, i, i, null, "Hi", true));
        }
        return geofences;
    }

    /**
     * Normally you would do this in an observer, but oh well
     */
    private void adapterDataChanged() {
        if (mGeoTuneAdapter.getItemCount() > 0) {
            mEmptyView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }
}
