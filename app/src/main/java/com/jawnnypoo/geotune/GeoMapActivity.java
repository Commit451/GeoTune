package com.jawnnypoo.geotune;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.misc.AnimUtils;
import com.jawnnypoo.geotune.misc.LocationUtils;
import com.jawnnypoo.geotune.service.GeoTuneModService;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.ArrayList;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Maps are so much fun
 */
public class GeoMapActivity extends BaseActivity {

    private static final String MAP_TAG = "MAP_TAG";

    private static final String EXTRA_REVEAL_POINT = "EXTRA_REVEAL_POINT";
    private static final String EXTRA_GEOTUNES = "EXTRA_GEOTUNES";
    private static final String EXTRA_STARTING_GEOTUNE = "EXTRA_STARTING_GEOTUNE";

    private static final int ANIMATION_DURATION = 800;
    private static final float LIME_HUE = 69.0f;

    private static final int REQUEST_PLACE = 1;

    private MapFragment mMapFragment;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker mCurrentMarker;
    private Circle mCurrentRadius;
    //Views
    @BindView(R.id.activity_root)
    View mRoot;
    @BindView(R.id.map_overlay)
    View mMapOverlay;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.edit_name)
    EditText mEditName;
    @BindView(R.id.radius_bar)
    DiscreteSeekBar mRadiusBar;

    private GeoTune mStartingGeoTune;
    private ArrayList<GeoTune> mGeoTunes;
    private int mRadiusColor;
    private int mScreenHeight;

    public static Intent newIntent(Context context, int[] point, ArrayList<GeoTune> geoTunes) {
        Intent intent = new Intent(context, GeoMapActivity.class);
        intent.putExtra(EXTRA_REVEAL_POINT, point);
        intent.putExtra(EXTRA_GEOTUNES, geoTunes);
        return intent;
    }

    public static Intent newIntent(Context context, int[] point, ArrayList<GeoTune> geoTunes, GeoTune startingGeoTune) {
        Intent intent = new Intent(context, GeoMapActivity.class);
        intent.putExtra(EXTRA_REVEAL_POINT, point);
        intent.putExtra(EXTRA_GEOTUNES, geoTunes);
        intent.putExtra(EXTRA_STARTING_GEOTUNE, startingGeoTune);
        return intent;
    }

    private final View.OnClickListener mOnDoneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            GeoTune geoTune = createGeoTuneFromValues();
            GeoTuneModService.addGeoTune(GeoMapActivity.this, geoTune.toContentValues());
            Intent intent = new Intent();
            intent.putExtra(MainActivity.EXTRA_GEOTUNE, geoTune);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private final DiscreteSeekBar.OnProgressChangeListener mOnProgressChangedListener = new DiscreteSeekBar.OnProgressChangeListener() {
        @Override
        public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
            if (mCurrentRadius != null) {
                mCurrentRadius.setRadius(value);
            }
        }
    };

    private View.OnClickListener mNavigationIconClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_map);
        ButterKnife.bind(this);
        mToolbar.setNavigationIcon(R.drawable.ic_back);
        mToolbar.setNavigationOnClickListener(mNavigationIconClickListener);
        mToolbar.inflateMenu(R.menu.map);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_search:
                        findPlace();
                        return true;
                }
                return false;
            }
        });

        mMapOverlay.setTranslationY(2000);
        mMapOverlay.setVisibility(View.GONE);

        mRadiusBar.setOnProgressChangeListener(mOnProgressChangedListener);

        findViewById(R.id.fab).setOnClickListener(mOnDoneClickListener);

        mGeoTunes = getIntent().getParcelableArrayListExtra(EXTRA_GEOTUNES);
        mStartingGeoTune = getIntent().getParcelableExtra(EXTRA_STARTING_GEOTUNE);
        if (savedInstanceState != null) {
        } else {
            final int[] revealPoint = getIntent().getIntArrayExtra(EXTRA_REVEAL_POINT);
            if (Build.VERSION.SDK_INT >= 21) {
                mRoot.post(new Runnable() {
                    @Override
                    public void run() {
                        AnimUtils.circleReveal(mRoot, revealPoint[0], revealPoint[1], 0, mRoot.getWidth() * 2, ANIMATION_DURATION);
                    }
                });
            } else {
                mRoot.setAlpha(0.0f);
                mRoot.animate().alpha(1.0f).setDuration(ANIMATION_DURATION);
            }
            Snackbar.make(mRoot, getString(R.string.map_hint), Snackbar.LENGTH_LONG)
                    .show();
        }

        mRadiusColor = getResources().getColor(R.color.teal_50);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenHeight = size.y;
        setUpMapIfNeeded();
    }

    public void findPlace() {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .build(GeoMapActivity.this);
            startActivityForResult(intent, REQUEST_PLACE);
        } catch (GooglePlayServicesRepairableException e) {
            Timber.e(e, null);
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            Timber.e(e, null);
            // TODO: Handle the error.
        }
    }

    private GeoTune createGeoTuneFromValues() {
        LatLng loc = mCurrentMarker.getPosition();
        double radius = mCurrentRadius.getRadius();
        String name = mEditName.getText().toString();
        return new GeoTune(TextUtils.isEmpty(name) ? getString(R.string.geotune) : name,
                UUID.randomUUID().toString(),
                loc.latitude, loc.longitude,
                (float) radius,
                Geofence.GEOFENCE_TRANSITION_ENTER, null, null, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:
                if (resultCode == RESULT_OK) {
                    //TODO figure out if we need to do anything based
                    //on the fact that we resolved the issue
                    Timber.d("resolved the issue");
                }
                break;
            case REQUEST_PLACE:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    if (place != null) {
                        addPotentialPointToMap(place.getLatLng());
                    }
                }
                break;
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMapFragment = (MapFragment) getFragmentManager().findFragmentByTag(MAP_TAG);
            if (mMapFragment == null) {
                mMapFragment = new MapFragment();
                getFragmentManager().beginTransaction()
                        .add(R.id.map_root, mMapFragment, MAP_TAG)
                        .commit();
            }
            // Try to obtain the map from the SupportMapFragment.
            mMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mMap = googleMap;
                    setUpMap();
                }
            });
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setOnMapLongClickListener(mMapLongClickListener);
        mMap.setOnMarkerClickListener(mMarkerClickListener);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        addGeoTunesToMap();

        if (mStartingGeoTune != null) {
            LatLng geoTuneLatLng = new LatLng(mStartingGeoTune.getLatitude(), mStartingGeoTune.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(geoTuneLatLng, LocationUtils.ZOOM_LEVEL));
        }
    }

    private final GoogleMap.OnMapLongClickListener mMapLongClickListener = new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng latLng) {
            Timber.d("onMapLongClick");
            addPotentialPointToMap(latLng);
        }
    };

    private final GoogleMap.OnMarkerClickListener mMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            mCurrentMarker = marker;
            if (isAGeoTuneMarker(marker.getTitle())) {
            } else {
                //TODO distinguish between an edit and a new thing and if edit then prefil with existing stuff
                showEditOverlay();
            }
            return false;
        }
    };

    private boolean isAGeoTuneMarker(String title) {
        for (GeoTune geoTune : mGeoTunes) {
            if (geoTune.getName().equals(title)) {
                return true;
            }
        }
        return false;
    }

    private void addGeoTunesToMap() {
        for (GeoTune geoTune : mGeoTunes) {
            LatLng loc = new LatLng(geoTune.getLatitude(), geoTune.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(geoTune.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(LIME_HUE)));
            mMap.addCircle(new CircleOptions().fillColor(mRadiusColor).strokeColor(Color.TRANSPARENT).radius(geoTune.getRadius()).center(loc));
        }
    }

    private void showEditOverlay() {
        mMapOverlay.setVisibility(View.VISIBLE);
        mMapOverlay.animate().translationY(0).setDuration(ANIMATION_DURATION);
    }

    @Override
    public void onBackPressed() {
        if (mMapOverlay.getTranslationY() == 0) {
            mMapOverlay.animate().translationY(mScreenHeight).setDuration(ANIMATION_DURATION);
            mMapOverlay.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMapOverlay.setVisibility(View.GONE);
                }
            }, ANIMATION_DURATION);
        } else {
            super.onBackPressed();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Moves the camera to the user's current location. Call after a location has been set
     */
    private void moveCameraToCurrentLocation(Location location) {
        mMap.animateCamera(CameraUpdateFactory
                .newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude())
                        , LocationUtils.ZOOM_LEVEL));

    }

    private void moveCameraToCurrentLocation(LatLng location) {
        mMap.animateCamera(CameraUpdateFactory
                .newLatLngZoom(location
                        , LocationUtils.ZOOM_LEVEL));

    }

    private void addPotentialPointToMap(LatLng position) {
        if (mCurrentMarker == null) {
            mCurrentMarker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(getString(R.string.new_geotune))
                    .icon(BitmapDescriptorFactory.defaultMarker(LIME_HUE))
                    .alpha(0.5f));
        } else {
            mCurrentMarker.setVisible(true);
            mCurrentMarker.setPosition(position);
        }
        //Set the radius
        if (mCurrentRadius == null) {
            mCurrentRadius = mMap.addCircle(new CircleOptions()
                    .fillColor(mRadiusColor)
                    .strokeColor(Color.TRANSPARENT)
                    .radius(LocationUtils.DEFAULT_RADIUS)
                    .center(mCurrentMarker.getPosition()));
        } else {
            mCurrentRadius.setVisible(true);
            mCurrentRadius.setCenter(mCurrentMarker.getPosition());
        }
        hideKeyboard();
        showEditOverlay();
        moveCameraToCurrentLocation(mCurrentMarker.getPosition());
    }
}
