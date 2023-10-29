package com.appsbycarla.brokebuthungry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.appsbycarla.brokebuthungry.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL



class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mMap: GoogleMap
    lateinit var binding: ActivityMapsBinding
    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_nearby)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    /**
     * Fetches and displays nearby grocery or supermarket locations based on the user's current location.
     * Uses Google Places API to retrieve the nearby places information and google map to show them
     * Author: Carla Hernandez and James Cowman
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.moveCamera(CameraUpdateFactory.zoomBy(17f))
        val response = intent.getStringExtra("JSON")
        val jsonObject = JSONObject(response)
        val results = jsonObject.getJSONArray("results")
        for (i in 0 until results.length()) {
            val place = results.getJSONObject(i)
            val name = place.getString("name")
            val geo = place.getJSONObject("geometry")
            val loc = geo.getJSONObject("location")
            val lat = loc.getString("lat").toDouble()
            val lng = loc.getString("lng").toDouble()
            val location = LatLng(lat, lng)
            mMap.addMarker(MarkerOptions().position(location).title(name))
        }

        //val channelislands = LatLng(34.165470, -119.045097)
        val target = LatLng(34.216969, -119.072578)
        //mMap.addMarker(MarkerOptions().position(target).title("target"))



        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                val curLocation = LatLng(latitude, longitude)
                mMap.addMarker(MarkerOptions().position(curLocation).title("currentLocation"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation, 12.0f))
            }
        }
    }
}