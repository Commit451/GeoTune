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

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.dialog.ChooseUriDialog;
import com.jawnnypoo.geotune.dialog.EditNameDialog;
import com.jawnnypoo.geotune.loader.GeoTunesLoader;
import com.jawnnypoo.geotune.service.GeoTuneModService;
import com.jawnnypoo.geotune.task.GetFileNameTask;
import com.jawnnypoo.geotune.util.NotificationUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Activity that shows all of the GeoTunes
 */
public class MainActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<ArrayList<GeoTune>> {

    private static final String STATE_ACTIVE_INDEX = "STATE_ACTIVE_INDEX";

    private static final int LOADER_GEOTUNES = 123;

    //Dialogs
    private EditNameDialog mEditNameDialog;
    private ChooseUriDialog mChooseUriDialog;
    //Views
    @BindView(R.id.main_content) View mRoot;
    @BindView(R.id.list) RecyclerView mListGeofences;
    @BindView(R.id.fab) View mFab;
    @BindView(R.id.empty_view) View mEmptyView;
    //Data
    private int mActivePosition = -1;
    private GeofenceAdapter mGeofenceAdapter;

    private final View.OnClickListener mOnAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int[] location = {(int) mFab.getX(), (int) mFab.getY()};
            navigateToMap(location, mGeofenceAdapter.mGeoTunes);
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
        public void onNameEdited(int position, String name) {
            mGeofenceAdapter.setGeoTuneName(position, name);
            mEditNameDialog.dismiss();
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
            mActivePosition = savedInstanceState.getInt(STATE_ACTIVE_INDEX);
        }

        mFab.setOnClickListener(mOnAddClickListener);
        mEmptyView.setOnClickListener(mOnAddClickListener);

        setupGeofenceList();

        setupDialogs();
        getLoaderManager().initLoader(LOADER_GEOTUNES, null, this);
    }

    private void setupDialogs() {
        mEditNameDialog = new EditNameDialog(this);
        mChooseUriDialog = new ChooseUriDialog(this);
        mEditNameDialog.setOnEditNameListener(mEditNameListener);
        mChooseUriDialog.setOnUriChoiceMadeListener(mOnUriChoiceListener);
    }

    private void setupGeofenceList() {
        mListGeofences.setLayoutManager(new LinearLayoutManager(this));
        mGeofenceAdapter = new GeofenceAdapter(new ArrayList<GeoTune>());
        mListGeofences.setAdapter(mGeofenceAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GEOFENCE:
                if (resultCode == RESULT_OK && data != null) {
                    Timber.d("adding geofence onAcitivityResult");
                    GeoTune newGeoTune = data.getParcelableExtra(EXTRA_GEOTUNE);
                    newGeoTune.setActive(true);
                    mGeofenceAdapter.addGeoTune(newGeoTune);
                    GeoTuneModService.registerGeoTune(getApplicationContext(), newGeoTune);
                    Snackbar.make(mRoot, getString(R.string.reminder_set_tune), Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
            case REQUEST_AUDIO:
                if (resultCode == RESULT_OK) {
                    if (data.getData() != null) {
                        Timber.d("Uri was found: " + data.getData());
                        if (mActivePosition != -1) {
                            GeoTune geoTune = mGeofenceAdapter.getGeoTune(mActivePosition);
                            mGeofenceAdapter.setUri(mActivePosition, data.getData());
                            NotificationUtils.playTune(this, geoTune);
                            mActivePosition = -1;
                        }
                    }
                }
                break;
            case REQUEST_NOTIFICATION:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (mActivePosition != -1) {
                        mGeofenceAdapter.setUri(mActivePosition, uri);
                        mActivePosition = -1;
                    }
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_INDEX, mActivePosition);
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
                if (data != null && mGeofenceAdapter != null) {
                    Timber.d("onLoadFinished");
                    mGeofenceAdapter.setGeofences(data);
                }
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
        if (mGeofenceAdapter.getItemCount() > 0) {
            mEmptyView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public class GeofenceAdapter extends RecyclerView.Adapter<GeofenceAdapter.Holder> {

        public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener,
                PopupMenu.OnMenuItemClickListener, CompoundButton.OnCheckedChangeListener {
            View card;
            TextView name;
            TextView tune;
            SwitchCompat datSwitch;
            ImageView overflow;

            public Holder(View view) {
                super(view);
                card = view.findViewById(R.id.card_view);
                name = (TextView) view.findViewById(R.id.geotune_name);
                tune = (TextView) view.findViewById(R.id.geotune_tune);
                card.setOnClickListener(this);
                tune.setOnClickListener(this);
                datSwitch = (SwitchCompat) view.findViewById(R.id.geotune_switch);
                datSwitch.setOnCheckedChangeListener(this);
                overflow = (ImageView) view.findViewById(R.id.geotune_overflow);
                final PopupMenu popup = new PopupMenu(view.getContext(), overflow);
                popup.getMenuInflater().inflate(R.menu.geotune_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(this);
                overflow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popup.show();
                    }
                });
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.geotune_tune:
                        mActivePosition = getPosition();
                        mChooseUriDialog.show();
                        break;
                    default:
                        int[] location = {(int) mFab.getX(), (int) mFab.getY()};
                        navigateToMap(location, mGeofenceAdapter.mGeoTunes, mGeoTunes.get(getPosition()));
                }
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_rename:
                        mEditNameDialog.setPosition(getPosition());
                        mEditNameDialog.setName(getGeoTune(getPosition()).getName());
                        mEditNameDialog.show();
                        return true;
                    case R.id.action_delete:
                        GeoTune geoTune = mGeofenceAdapter.getGeoTune(getPosition());
                        GeoTuneModService.deleteGeoTune(MainActivity.this, geoTune);
                        mGeofenceAdapter.removeGeoTune(getPosition());
                        return true;
                }
                return false;
            }

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                GeoTune geoTune = mGeofenceAdapter.getGeoTune(getPosition());
                if (isChecked == geoTune.isActive()) {
                    return;
                }
                geoTune.setActive(isChecked);
                //Update with GPlay
                if (isChecked) {
                    GeoTuneModService.registerGeoTune(getApplicationContext(), geoTune);
                } else {
                    GeoTuneModService.unregisterGeoTune(getApplicationContext(), geoTune);
                }
            }
        }

        private ArrayList<GeoTune> mGeoTunes;

        public GeofenceAdapter(ArrayList<GeoTune> objects) {
            mGeoTunes = objects;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // create a new view
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_geofence, viewGroup, false);
            // set the view's size, margins, paddings and layout parameters
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder viewHolder, int position) {
            GeoTune geoTune = getGeoTune(position);
            viewHolder.name.setText(geoTune.getName());
            if (geoTune.getTuneUri() == null) {
                viewHolder.tune.setText(getString(R.string.default_notification_tone));
            } else {
                if (geoTune.getTuneName() == null) {
                    viewHolder.tune.setText("");
                    new GetFileNameTask(MainActivity.this, geoTune, this).execute(geoTune.getTuneUri());
                } else {
                    viewHolder.tune.setText(geoTune.getTuneName());
                }
            }
            viewHolder.datSwitch.setChecked(geoTune.isActive());
        }

        @Override
        public int getItemCount() {
            return mGeoTunes.size();
        }

        public void setGeofences(ArrayList<GeoTune> geoTunes) {
            mGeoTunes = geoTunes;
            notifyDataSetChanged();
            adapterDataChanged();
        }

        public void addGeoTune(GeoTune geofence) {
            mGeoTunes.add(geofence);
            notifyItemInserted(mGeoTunes.size() - 1);
            adapterDataChanged();
        }

        public void removeGeoTune(int position) {
            mGeoTunes.remove(position);
            notifyItemRemoved(position);
            adapterDataChanged();
        }

        public GeoTune getGeoTune(int position) {
            return mGeoTunes.get(position);
        }

        public void setUri(int position, Uri uri) {
            Timber.d("Setting uri of item " + position);
            mGeoTunes.get(position).setTuneUri(uri);
            ContentValues cv = new ContentValues();
            cv.put(GeoTune.KEY_TUNE, uri.toString());
            GeoTuneModService.updateGeoTune(MainActivity.this, cv, mGeoTunes.get(position).getId());
            new GetFileNameTask(MainActivity.this, mGeoTunes.get(position), this).execute(uri);
            notifyDataSetChanged();
        }

        public void setGeoTuneName(int position, String name) {
            GeoTune geoTune = mGeoTunes.get(position);
            geoTune.setName(name);
            ContentValues cv = new ContentValues();
            cv.put(GeoTune.KEY_NAME, name);
            GeoTuneModService.updateGeoTune(MainActivity.this, cv, geoTune.getId());
            notifyDataSetChanged();
        }

    }
}
