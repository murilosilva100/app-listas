package com.example.applistas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapPickerActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var selectedLocationText: TextView
    private var selectedPoint: GeoPoint? = null
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(OSMDROID_PREFS_NAME, MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_map_picker)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mapPickerRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mapView = findViewById(R.id.mapView)
        selectedLocationText = findViewById(R.id.selectedMapLocationText)

        setupInitialLocation()
        setupMap()
        setupConfirmButton()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    private fun setupMap() {
        val initialPoint = selectedPoint ?: DEFAULT_POINT

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.controller.setCenter(initialPoint)
        selectedPoint?.let { updateMarker(it) }

        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                event: android.view.MotionEvent,
                mapView: MapView
            ): Boolean {
                val point = mapView.projection.fromPixels(
                    event.x.toInt(),
                    event.y.toInt()
                ) as GeoPoint
                updateMarker(point)
                return true
            }
        })
    }

    private fun setupInitialLocation() {
        val latitude = intent.getDoubleExtra(EXTRA_INITIAL_LATITUDE, Double.NaN)
        val longitude = intent.getDoubleExtra(EXTRA_INITIAL_LONGITUDE, Double.NaN)

        if (!latitude.isNaN() && !longitude.isNaN()) {
            selectedPoint = GeoPoint(latitude, longitude)
            updateLocationText(latitude, longitude)
        }
    }

    private fun setupConfirmButton() {
        findViewById<View>(R.id.confirmLocationButton).setOnClickListener {
            val point = selectedPoint
            if (point == null) {
                Toast.makeText(this, "Toque em um ponto do mapa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent().apply {
                putExtra(EXTRA_SELECTED_LATITUDE, point.latitude)
                putExtra(EXTRA_SELECTED_LONGITUDE, point.longitude)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun updateMarker(point: GeoPoint) {
        selectedPoint = point
        selectedMarker?.let { mapView.overlays.remove(it) }
        selectedMarker = Marker(mapView).apply {
            position = point
            title = "Local selecionado"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(selectedMarker)
        mapView.controller.animateTo(point)
        mapView.invalidate()
        updateLocationText(point.latitude, point.longitude)
    }

    private fun updateLocationText(latitude: Double, longitude: Double) {
        selectedLocationText.text = "localiza\u00e7\u00e3o: %.6f, %.6f".format(
            Locale.US,
            latitude,
            longitude
        )
    }

    companion object {
        const val EXTRA_INITIAL_LATITUDE = "extra_initial_latitude"
        const val EXTRA_INITIAL_LONGITUDE = "extra_initial_longitude"
        const val EXTRA_SELECTED_LATITUDE = "extra_selected_latitude"
        const val EXTRA_SELECTED_LONGITUDE = "extra_selected_longitude"

        private const val OSMDROID_PREFS_NAME = "osmdroid_preferences"
        private val DEFAULT_POINT = GeoPoint(-23.55052, -46.633308)
        private const val DEFAULT_ZOOM = 12.0
    }
}
