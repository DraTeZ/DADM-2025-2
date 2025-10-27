package com.example.maps

import com.example.maps.ApiClient
import com.example.maps.*
import com.example.maps.GooglePlacesApiService
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import com.google.android.gms.maps.model.Marker
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import android.widget.RatingBar
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.Arrays
import android.content.Intent
import android.net.Uri


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheet: LinearLayout
    private lateinit var placeNameTextView: TextView
    private lateinit var placeAddressTextView: TextView
    private lateinit var placeRatingBar: RatingBar
    private lateinit var placeRatingText: TextView
    private lateinit var placeRatingTotal: TextView
    private lateinit var placePhone: TextView
    private lateinit var placeWebsite: TextView
    private lateinit var buttonCall: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonShare: Button
    private var currentPlacePhone: String? = null
    private var currentPlaceWebsite: String? = null
    private var currentPlaceName: String? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocationAndFindPOIs()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocationAndFindPOIs()
            }
            else -> {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val apiKey = "AIzaSyDvxVyPnj5NPDNb0jh92v0q4-ccJwuKJ48"
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        placeNameTextView = findViewById(R.id.place_name)
        placeAddressTextView = findViewById(R.id.place_address)
        placeRatingBar = findViewById(R.id.place_rating_bar)
        placeRatingText = findViewById(R.id.place_rating_text)
        placeRatingTotal = findViewById(R.id.place_rating_total)
        placePhone = findViewById(R.id.place_phone)
        placeWebsite = findViewById(R.id.place_website)
        buttonCall = findViewById(R.id.button_call)
        buttonSave = findViewById(R.id.button_save)
        buttonShare = findViewById(R.id.button_share)
        buttonCall.setOnClickListener {
            if (!currentPlacePhone.isNullOrBlank()) {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:$currentPlacePhone")
                startActivity(dialIntent)
            }
        }

        buttonSave.setOnClickListener {
            Log.i("MainActivity", "Botón 'Guardar' presionado para: $currentPlaceName")
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        }

        buttonShare.setOnClickListener {
            if (!currentPlaceName.isNullOrBlank()) {
                val shareText: String
                shareText = if (!currentPlaceWebsite.isNullOrBlank()) {
                    "¡Mira este lugar! $currentPlaceName: $currentPlaceWebsite"
                } else {
                    "¡Mira este lugar! $currentPlaceName"
                }

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Compartir '$currentPlaceName'")
                startActivity(shareIntent)
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))


        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("MainActivity", "Lugar seleccionado: ${place.name}, ${place.id}")
                if (place.latLng != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng!!, 15f))
                }
                mMap.clear()
                val marker = mMap.addMarker(MarkerOptions()
                    .position(place.latLng!!)
                    .title(place.name))
                marker?.tag = place.id

                if (marker != null) {
                    onMarkerClick(marker)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("MainActivity", "Error en autocompletado: $status")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setPadding(0, 200, 0, 0)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        checkLocationPermission()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val placeId = marker.tag as? String ?: return true
        placeNameTextView.text = marker.title
        placeAddressTextView.text = "Cargando detalles..."
        placeRatingBar.rating = 0f
        placeRatingText.text = ""
        placeRatingTotal.text = ""
        placePhone.text = ""
        placeWebsite.text = ""
        currentPlacePhone = null
        currentPlaceWebsite = null
        currentPlaceName = marker.title
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        lifecycleScope.launch {
            try {
                val apiKey = "AIzaSyDvxVyPnj5NPDNb0jh92v0q4-ccJwuKJ48"
                val fields = "name,vicinity,formatted_phone_number,rating,user_ratings_total,website"
                val response = ApiClient.placesService.getPlaceDetails(placeId, fields, apiKey)

                if (response.isSuccessful && response.body() != null) {
                    val details = response.body()!!.result
                    placeNameTextView.text = details.name
                    placeAddressTextView.text = details.vicinity
                    placeRatingBar.rating = (details.rating ?: 0.0).toFloat()
                    placeRatingText.text = details.rating.toString()
                    placeRatingTotal.text = "(${details.user_ratings_total ?: 0})"
                    placePhone.text = details.formatted_phone_number ?: "Teléfono no disponible"
                    placeWebsite.text = details.website ?: "Sitio web no disponible"
                    currentPlaceName = details.name
                    currentPlacePhone = details.formatted_phone_number
                    currentPlaceWebsite = details.website
                } else {
                    placeAddressTextView.text = "No se pudieron cargar los detalles."
                    Log.e("MainActivity", "Error en Place Details: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Excepción en Place Details: ${e.message}")
                placeAddressTextView.text = "Error de red al cargar detalles."
            }
        }

        return true
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocationAndFindPOIs()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            else -> {
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun getSearchRadiusInMeters(): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val radiusInKm = prefs.getString("radius_km", "5") ?: "5"
        return (radiusInKm.toIntOrNull() ?: 5) * 1000
    }

    private fun saveSearchRadius(km: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("radius_km", km).apply()
    }

    private fun getCurrentLocationAndFindPOIs() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationPermission()
            return
        }

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    findNearbyPOIs(userLatLng, "hospital")
                    findNearbyPOIs(userLatLng, "hotel")
                    findNearbyPOIs(userLatLng, "turismo")
                    findNearbyPOIs(userLatLng, "restaurante")
                    findNearbyPOIs(userLatLng, "parqueadero")
                } else {
                    Log.e("MainActivity", "La ubicación es nula.")
                }
            }
    }

    // Búsqueda Cercana (Nearby Search)
    private fun findNearbyPOIs(userLocation: LatLng, poiType: String) {
        lifecycleScope.launch {
            try {
                val locationStr = "${userLocation.latitude},${userLocation.longitude}"
                val radius = getSearchRadiusInMeters()
                val apiKey = "AIzaSyDvxVyPnj5NPDNb0jh92v0q4-ccJwuKJ48"

                val response = ApiClient.placesService.getNearbyPlaces(
                    location = locationStr,
                    radius = radius,
                    type = poiType,
                    apiKey = apiKey
                )

                if (response.isSuccessful && response.body() != null) {
                    val places = response.body()!!.results
                    addPoisToMap(places)
                } else {
                    Log.e("MainActivity", "Error en Nearby Search: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Excepción en Nearby Search: ${e.message}")
            }
        }
    }

    private fun addPoisToMap(places: List<PlaceResult>) {
        mMap.clear()

        places.forEach { place ->
            val poiLatLng = LatLng(place.geometry.location.lat, place.geometry.location.lng)

            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(poiLatLng)
                    .title(place.name)
            )
            marker?.tag = place.place_id
        }
    }
}