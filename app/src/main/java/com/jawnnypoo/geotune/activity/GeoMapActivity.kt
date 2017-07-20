package com.jawnnypoo.geotune.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Display
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.misc.AnimUtils
import com.jawnnypoo.geotune.misc.LocationUtils
import com.jawnnypoo.geotune.service.GeoTuneModService

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

import java.util.ArrayList
import java.util.UUID

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import timber.log.Timber

/**
 * Maps are so much fun
 */
class GeoMapActivity : BaseActivity() {

    @BindView(R.id.activity_root)
    internal var root: View? = null
    @BindView(R.id.map_overlay)
    internal var mapOverlay: View? = null
    @BindView(R.id.toolbar)
    internal var toolbar: Toolbar? = null
    @BindView(R.id.edit_name)
    internal var textName: EditText? = null
    @BindView(R.id.radius_bar)
    internal var radiusBar: DiscreteSeekBar? = null

    internal var mMapFragment: MapFragment? = null
    internal var mMap: GoogleMap? = null
    internal var mCurrentMarker: Marker? = null
    internal var mCurrentRadius: Circle? = null

    internal var mStartingGeoTune: GeoTune? = null
    internal var mGeoTunes: ArrayList<GeoTune>
    internal var mRadiusColor: Int = 0
    internal var mScreenHeight: Int = 0

    private val mOnProgressChangedListener = object : DiscreteSeekBar.OnProgressChangeListener {
        override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
            if (mCurrentRadius != null) {
                mCurrentRadius!!.radius = value.toDouble()
            }
        }

        override fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar) {}

        override fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar) {}
    }

    @OnClick(R.id.fab)
    internal fun onDoneClicked() {
        val permissionCheck = ContextCompat.checkSelfPermission(this@GeoMapActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            saveGeoTune()
        } else {
            ActivityCompat.requestPermissions(this@GeoMapActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_FINE_LOCATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        ButterKnife.bind(this)

        mGeoTunes = intent.getParcelableArrayListExtra<GeoTune>(EXTRA_GEOTUNES)
        mStartingGeoTune = intent.getParcelableExtra<GeoTune>(EXTRA_STARTING_GEOTUNE)

        toolbar!!.setNavigationIcon(R.drawable.ic_back)
        toolbar!!.setNavigationOnClickListener { onBackPressed() }
        toolbar!!.inflateMenu(R.menu.map)
        toolbar!!.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    findPlace()
                    return@OnMenuItemClickListener true
                }
            }
            false
        })

        mapOverlay!!.translationY = 2000f
        mapOverlay!!.visibility = View.GONE

        radiusBar!!.setOnProgressChangeListener(mOnProgressChangedListener)

        if (savedInstanceState != null) {
        } else {
            val revealPoint = intent.getIntArrayExtra(EXTRA_REVEAL_POINT)
            if (Build.VERSION.SDK_INT >= 21) {
                root!!.post { AnimUtils.circleReveal(root, revealPoint[0], revealPoint[1], 0f, (root!!.width * 2).toFloat(), ANIMATION_DURATION) }
            } else {
                root!!.alpha = 0.0f
                root!!.animate().alpha(1.0f).duration = ANIMATION_DURATION.toLong()
            }
            Snackbar.make(root!!, getString(R.string.map_hint), Snackbar.LENGTH_LONG)
                    .show()
        }

        mRadiusColor = resources.getColor(R.color.teal_50)
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        mScreenHeight = size.y
        setUpMapIfNeeded()
    }

    fun findPlace() {
        try {
            val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(this@GeoMapActivity)
            startActivityForResult(intent, REQUEST_PLACE)
        } catch (e: GooglePlayServicesRepairableException) {
            Timber.e(e, null)
            // TODO: Handle the error.
        } catch (e: GooglePlayServicesNotAvailableException) {
            Timber.e(e, null)
            // TODO: Handle the error.
        }

    }

    private fun saveGeoTune() {
        val geoTune = createGeoTuneFromValues()
        GeoTuneModService.addGeoTune(this@GeoMapActivity, geoTune.toContentValues())
        val intent = Intent()
        intent.putExtra(BaseActivity.Companion.EXTRA_GEOTUNE, geoTune)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun createGeoTuneFromValues(): GeoTune {
        val loc = mCurrentMarker!!.position
        val radius = mCurrentRadius!!.radius
        val name = textName!!.text.toString()
        return GeoTune(if (TextUtils.isEmpty(name)) getString(R.string.geotune) else name,
                UUID.randomUUID().toString(),
                loc.latitude, loc.longitude,
                radius.toFloat(),
                Geofence.GEOFENCE_TRANSITION_ENTER, null, null, false)
    }

    override fun onResume() {
        super.onResume()
        setUpMapIfNeeded()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST -> if (resultCode == Activity.RESULT_OK) {
                //TODO figure out if we need to do anything based
                //on the fact that we resolved the issue
                Timber.d("resolved the issue")
            }
            REQUEST_PLACE -> if (resultCode == Activity.RESULT_OK) {
                val place = PlaceAutocomplete.getPlace(this, data)
                if (place != null) {
                    addPotentialPointToMap(place.latLng)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_FINE_LOCATION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                saveGeoTune()
            }
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call [.setUpMap] once when [.mMap] is not null.
     *
     *
     * If it isn't installed [SupportMapFragment] (and
     * [MapView][com.google.android.gms.maps.MapView]) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     *
     *
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), [.onCreate] may not be called again so we should call this
     * method in [.onResume] to guarantee that it will be called.
     */
    private fun setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMapFragment = fragmentManager.findFragmentByTag(MAP_TAG) as MapFragment
            if (mMapFragment == null) {
                mMapFragment = MapFragment()
                fragmentManager.beginTransaction()
                        .add(R.id.map_root, mMapFragment, MAP_TAG)
                        .commit()
            }
            // Try to obtain the map from the SupportMapFragment.
            mMapFragment!!.getMapAsync { googleMap ->
                mMap = googleMap
                setUpMap()
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     *
     *
     * This should only be called once and when we are sure that [.mMap] is not null.
     */
    private fun setUpMap() {
        mMap!!.setOnMapLongClickListener(mMapLongClickListener)
        mMap!!.setOnMarkerClickListener(mMarkerClickListener)
        mMap!!.uiSettings.isZoomControlsEnabled = false
        mMap!!.uiSettings.isMapToolbarEnabled = false
        addGeoTunesToMap()

        if (mStartingGeoTune != null) {
            val geoTuneLatLng = LatLng(mStartingGeoTune!!.latitude, mStartingGeoTune!!.longitude)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(geoTuneLatLng, LocationUtils.ZOOM_LEVEL))
        }
    }

    private val mMapLongClickListener = GoogleMap.OnMapLongClickListener { latLng ->
        Timber.d("onMapLongClick")
        addPotentialPointToMap(latLng)
    }

    private val mMarkerClickListener = GoogleMap.OnMarkerClickListener { marker ->
        mCurrentMarker = marker
        if (isAGeoTuneMarker(marker.title)) {
        } else {
            //TODO distinguish between an edit and a new thing and if edit then prefil with existing stuff
            showEditOverlay()
        }
        false
    }

    private fun isAGeoTuneMarker(title: String): Boolean {
        for (geoTune in mGeoTunes) {
            if (geoTune.name == title) {
                return true
            }
        }
        return false
    }

    private fun addGeoTunesToMap() {
        for (geoTune in mGeoTunes) {
            val loc = LatLng(geoTune.latitude, geoTune.longitude)
            mMap!!.addMarker(MarkerOptions()
                    .position(loc)
                    .title(geoTune.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(LIME_HUE)))
            mMap!!.addCircle(CircleOptions().fillColor(mRadiusColor).strokeColor(Color.TRANSPARENT).radius(geoTune.radius.toDouble()).center(loc))
        }
    }

    private fun showEditOverlay() {
        mapOverlay!!.visibility = View.VISIBLE
        mapOverlay!!.animate().translationY(0f).duration = ANIMATION_DURATION.toLong()
    }

    override fun onBackPressed() {
        if (mapOverlay!!.translationY == 0f) {
            mapOverlay!!.animate().translationY(mScreenHeight.toFloat()).duration = ANIMATION_DURATION.toLong()
            mapOverlay!!.postDelayed({ mapOverlay!!.visibility = View.GONE }, ANIMATION_DURATION.toLong())
        } else {
            super.onBackPressed()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (currentFocus != null && currentFocus!!.windowToken != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    /**
     * Moves the camera to the user's current location. Call after a location has been set
     */
    private fun moveCameraToCurrentLocation(location: Location) {
        mMap!!.animateCamera(CameraUpdateFactory
                .newLatLngZoom(LatLng(location.latitude, location.longitude), LocationUtils.ZOOM_LEVEL))

    }

    private fun moveCameraToCurrentLocation(location: LatLng) {
        mMap!!.animateCamera(CameraUpdateFactory
                .newLatLngZoom(location, LocationUtils.ZOOM_LEVEL))

    }

    private fun addPotentialPointToMap(position: LatLng) {
        if (mCurrentMarker == null) {
            mCurrentMarker = mMap!!.addMarker(MarkerOptions()
                    .position(position)
                    .title(getString(R.string.new_geotune))
                    .icon(BitmapDescriptorFactory.defaultMarker(LIME_HUE))
                    .alpha(0.5f))
        } else {
            mCurrentMarker!!.isVisible = true
            mCurrentMarker!!.position = position
        }
        //Set the radius
        if (mCurrentRadius == null) {
            mCurrentRadius = mMap!!.addCircle(CircleOptions()
                    .fillColor(mRadiusColor)
                    .strokeColor(Color.TRANSPARENT)
                    .radius(LocationUtils.DEFAULT_RADIUS.toDouble())
                    .center(mCurrentMarker!!.position))
        } else {
            mCurrentRadius!!.isVisible = true
            mCurrentRadius!!.center = mCurrentMarker!!.position
        }
        hideKeyboard()
        showEditOverlay()
        moveCameraToCurrentLocation(mCurrentMarker!!.position)
    }

    companion object {

        private val MAP_TAG = "MAP_TAG"

        private val EXTRA_REVEAL_POINT = "EXTRA_REVEAL_POINT"
        private val EXTRA_GEOTUNES = "EXTRA_GEOTUNES"
        private val EXTRA_STARTING_GEOTUNE = "EXTRA_STARTING_GEOTUNE"

        private val ANIMATION_DURATION = 800
        private val LIME_HUE = 69.0f

        private val REQUEST_PLACE = 1
        private val REQUEST_PERMISSION_FINE_LOCATION = 2

        fun newIntent(context: Context, point: IntArray, geoTunes: ArrayList<GeoTune>): Intent {
            val intent = Intent(context, GeoMapActivity::class.java)
            intent.putExtra(EXTRA_REVEAL_POINT, point)
            intent.putExtra(EXTRA_GEOTUNES, geoTunes)
            return intent
        }

        fun newIntent(context: Context, point: IntArray, geoTunes: ArrayList<GeoTune>, startingGeoTune: GeoTune): Intent {
            val intent = Intent(context, GeoMapActivity::class.java)
            intent.putExtra(EXTRA_REVEAL_POINT, point)
            intent.putExtra(EXTRA_GEOTUNES, geoTunes)
            intent.putExtra(EXTRA_STARTING_GEOTUNE, startingGeoTune)
            return intent
        }
    }
}
