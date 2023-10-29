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
    lateinit var placesClient: PlacesClient
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

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCZR0gVZBwoIod0xP9P_0TWI4PUb4Wfr9A")
        }

        placesClient = Places.createClient(this)

        val query = intent.getStringExtra("query") ?: return  // Adjusted position
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                SearchNearbyActivity.LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        } else {
            Log.d("PlacesAPI", "Query: $query")
        }
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
        var lat = 34.16547
        var long = -119.045097

        val channelislands = LatLng(lat, long)
        //val channelislands = LatLng(34.165470, -119.045097)
        val target = LatLng(34.216969, -119.072578)
        val wholefoods = LatLng(34.239929, -119.178871)
        val costco = LatLng(34.225231, -119.148193)
        val vons = LatLng(34.224941, -119.037064)
        //mMap.addMarker(MarkerOptions().position(target).title("target"))
        //mMap.addMarker(MarkerOptions().position(wholefoods).title("whole foods"))
        //mMap.addMarker(MarkerOptions().position(costco).title("costco"))
        //mMap.addMarker(MarkerOptions().position(vons).title("vons"))


        //mMap.addMarker(MarkerOptions().position(channelislands).title("target"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(channelislands, 12.0f))



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
                val radius = 10000 // Define the radius in which you want to search places
                mMap.addMarker(MarkerOptions().position(curLocation).title("currentLocation"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curLocation, 12.0f))
                val apiKey =
                    "AIzaSyCZR0gVZBwoIod0xP9P_0TWI4PUb4Wfr9A" // Replace with your actual API key
                val urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=$latitude,$longitude" +
                        "&radius=$radius" +
                        "&type=grocery_or_supermarket" +
                        "&keyword=Vons|Costco|Walmart|Ralphs|Trader Joe's|Albertsons|Grocery Outlet" +
                        "&key=$apiKey"
                //CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(urlString)
                    val connection: HttpURLConnection =
                        url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    val inputStream: InputStream = BufferedInputStream(connection.inputStream)
                    val response =
                        inputStream.bufferedReader().use { it.readText() }  // defaults to UTF-8
                    connection.disconnect()
                    Log.d("PlacesAPI", "Response: $response")

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
                } catch (e: Exception) {
                    Log.e("PlacesAPI", "Error: $e")
                }
            }
        }
    }
}