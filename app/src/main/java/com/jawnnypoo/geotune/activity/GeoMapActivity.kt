package com.jawnnypoo.geotune.activity

import android.Manifest
import android.app.Activity
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
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.commit451.addendum.parceler.getParcelerParcelableExtra
import com.commit451.addendum.parceler.putParcelerParcelableExtra
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.service.GeoTuneModService
import com.jawnnypoo.geotune.util.AnimUtils
import com.jawnnypoo.geotune.util.LocationUtils
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import timber.log.Timber
import java.util.*

/**
 * Maps are so much fun
 */
class GeoMapActivity : BaseActivity() {

    companion object {

        private val MAP_TAG = "MAP_TAG"

        private val EXTRA_REVEAL_POINT = "EXTRA_REVEAL_POINT"
        private val EXTRA_GEOTUNES = "EXTRA_GEOTUNES"
        private val EXTRA_STARTING_GEOTUNE = "EXTRA_STARTING_GEOTUNE"

        private val ANIMATION_DURATION = 800
        private val LIME_HUE = 69.0f

        private val REQUEST_PLACE = 1
        private val REQUEST_PERMISSION_FINE_LOCATION = 2

        fun newIntent(context: Context, point: IntArray, geoTunes: List<GeoTune>): Intent {
            val intent = Intent(context, GeoMapActivity::class.java)
            intent.putExtra(EXTRA_REVEAL_POINT, point)
            intent.putParcelerParcelableExtra(EXTRA_GEOTUNES, geoTunes)
            return intent
        }

        fun newIntent(context: Context, point: IntArray, geoTunes: List<GeoTune>, startingGeoTune: GeoTune): Intent {
            val intent = Intent(context, GeoMapActivity::class.java)
            intent.putExtra(EXTRA_REVEAL_POINT, point)
            intent.putParcelerParcelableExtra(EXTRA_GEOTUNES, geoTunes)
            intent.putParcelerParcelableExtra(EXTRA_STARTING_GEOTUNE, startingGeoTune)
            return intent
        }
    }

    @BindView(R.id.activity_root) lateinit var root: View
    @BindView(R.id.map_overlay) lateinit var mapOverlay: View
    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
    @BindView(R.id.edit_name) lateinit var textName: EditText
    @BindView(R.id.radius_bar) lateinit var radiusBar: DiscreteSeekBar

    var mapFragment: MapFragment? = null
    var map: GoogleMap? = null
    var currentMarker: Marker? = null
    var currentRadius: Circle? = null

    var startingGeoTune: GeoTune? = null
    lateinit var geoTunes: List<GeoTune>
    var radiusColor: Int = 0
    var screenHeight: Int = 0

    private val mOnProgressChangedListener = object : DiscreteSeekBar.OnProgressChangeListener {
        override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
            if (currentRadius != null) {
                currentRadius!!.radius = value.toDouble()
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

        geoTunes = intent.getParcelerParcelableExtra<List<GeoTune>>(EXTRA_GEOTUNES)!!
        startingGeoTune = intent.getParcelerParcelableExtra<GeoTune>(EXTRA_STARTING_GEOTUNE)

        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        toolbar.inflateMenu(R.menu.map)
        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    findPlace()
                    return@OnMenuItemClickListener true
                }
            }
            false
        })

        mapOverlay.translationY = 2000f
        mapOverlay.visibility = View.GONE

        radiusBar.setOnProgressChangeListener(mOnProgressChangedListener)

        if (savedInstanceState != null) {
        } else {
            val revealPoint = intent.getIntArrayExtra(EXTRA_REVEAL_POINT)
            if (Build.VERSION.SDK_INT >= 21) {
                root.post { AnimUtils.circleReveal(root, revealPoint[0], revealPoint[1], 0f, (root.width * 2).toFloat(), ANIMATION_DURATION) }
            } else {
                root.alpha = 0.0f
                root.animate().alpha(1.0f).duration = ANIMATION_DURATION.toLong()
            }
            Snackbar.make(root, getString(R.string.map_hint), Snackbar.LENGTH_LONG)
                    .show()
        }

        radiusColor = resources.getColor(R.color.teal_50)
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        screenHeight = size.y
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
        intent.putParcelerParcelableExtra(BaseActivity.EXTRA_GEOTUNE, geoTune)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun createGeoTuneFromValues(): GeoTune {
        val loc = currentMarker!!.position
        val radius = currentRadius!!.radius
        val name = textName.text.toString()
        val geoTune = GeoTune()
        geoTune.name = if (TextUtils.isEmpty(name)) getString(R.string.geotune) else name
        geoTune.id = UUID.randomUUID().toString()
        geoTune.latitude = loc.latitude
        geoTune.longitude = loc.longitude
        geoTune.radius = radius.toFloat()
        geoTune.transitionType = Geofence.GEOFENCE_TRANSITION_ENTER
        return geoTune
    }

    override fun onResume() {
        super.onResume()
        setUpMapIfNeeded()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST -> if (resultCode == Activity.RESULT_OK) {
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
            REQUEST_PERMISSION_FINE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                saveGeoTune()
            }
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call [.setUpMap] once when [.map] is not null.
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
        if (map == null) {
            mapFragment = fragmentManager.findFragmentByTag(MAP_TAG) as MapFragment
            if (mapFragment == null) {
                mapFragment = MapFragment()
                fragmentManager.beginTransaction()
                        .add(R.id.map_root, mapFragment, MAP_TAG)
                        .commit()
            }
            // Try to obtain the map from the SupportMapFragment.
            mapFragment!!.getMapAsync { googleMap ->
                map = googleMap
                setUpMap()
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     *
     *
     * This should only be called once and when we are sure that [.map] is not null.
     */
    private fun setUpMap() {
        map!!.setOnMapLongClickListener(mMapLongClickListener)
        map!!.setOnMarkerClickListener(mMarkerClickListener)
        map!!.uiSettings.isZoomControlsEnabled = false
        map!!.uiSettings.isMapToolbarEnabled = false
        addGeoTunesToMap()

        if (startingGeoTune != null) {
            val geoTuneLatLng = LatLng(startingGeoTune!!.latitude, startingGeoTune!!.longitude)
            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(geoTuneLatLng, LocationUtils.ZOOM_LEVEL))
        }
    }

    private val mMapLongClickListener = GoogleMap.OnMapLongClickListener { latLng ->
        Timber.d("onMapLongClick")
        addPotentialPointToMap(latLng)
    }

    private val mMarkerClickListener = GoogleMap.OnMarkerClickListener { marker ->
        currentMarker = marker
        if (isAGeoTuneMarker(marker.title)) {
        } else {
            //TODO distinguish between an edit and a new thing and if edit then prefil with existing stuff
            showEditOverlay()
        }
        false
    }

    private fun isAGeoTuneMarker(title: String): Boolean {
        for (geoTune in geoTunes) {
            if (geoTune.name == title) {
                return true
            }
        }
        return false
    }

    private fun addGeoTunesToMap() {
        for (geoTune in geoTunes) {
            val loc = LatLng(geoTune.latitude, geoTune.longitude)
            map!!.addMarker(MarkerOptions()
                    .position(loc)
                    .title(geoTune.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(LIME_HUE)))
            map!!.addCircle(CircleOptions().fillColor(radiusColor).strokeColor(Color.TRANSPARENT).radius(geoTune.radius.toDouble()).center(loc))
        }
    }

    private fun showEditOverlay() {
        mapOverlay.visibility = View.VISIBLE
        mapOverlay.animate().translationY(0f).duration = ANIMATION_DURATION.toLong()
    }

    override fun onBackPressed() {
        if (mapOverlay.translationY == 0f) {
            mapOverlay.animate().translationY(screenHeight.toFloat()).duration = ANIMATION_DURATION.toLong()
            mapOverlay.postDelayed({ mapOverlay.visibility = View.GONE }, ANIMATION_DURATION.toLong())
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
        map!!.animateCamera(CameraUpdateFactory
                .newLatLngZoom(LatLng(location.latitude, location.longitude), LocationUtils.ZOOM_LEVEL))

    }

    private fun moveCameraToCurrentLocation(location: LatLng) {
        map!!.animateCamera(CameraUpdateFactory
                .newLatLngZoom(location, LocationUtils.ZOOM_LEVEL))

    }

    private fun addPotentialPointToMap(position: LatLng) {
        if (currentMarker == null) {
            currentMarker = map!!.addMarker(MarkerOptions()
                    .position(position)
                    .title(getString(R.string.new_geotune))
                    .icon(BitmapDescriptorFactory.defaultMarker(LIME_HUE))
                    .alpha(0.5f))
        } else {
            currentMarker!!.isVisible = true
            currentMarker!!.position = position
        }
        //Set the radius
        if (currentRadius == null) {
            currentRadius = map!!.addCircle(CircleOptions()
                    .fillColor(radiusColor)
                    .strokeColor(Color.TRANSPARENT)
                    .radius(LocationUtils.DEFAULT_RADIUS.toDouble())
                    .center(currentMarker!!.position))
        } else {
            currentRadius!!.isVisible = true
            currentRadius!!.center = currentMarker!!.position
        }
        hideKeyboard()
        showEditOverlay()
        moveCameraToCurrentLocation(currentMarker!!.position)
    }
}
