// SearchNearbyActivity.kt

package com.appsbycarla.brokebuthungry

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
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


class SearchNearbyActivity : AppCompatActivity() {

    lateinit var placesClient: PlacesClient
    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_nearby)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCZR0gVZBwoIod0xP9P_0TWI4PUb4Wfr9A")
        }

        placesClient = Places.createClient(this)

        val query = intent.getStringExtra("query") ?: return  // Adjusted position

        val backButton: Button = findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }

        // Check if the app has location permission
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        } else {
            Log.d("PlacesAPI", "Query: $query")
//            findNearbyPlaces(query)
            findNearbyPlaces()
        }
    }

    private fun findNearbyPlaces() {
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
                val radius = 10000 // Define the radius in which you want to search places

                val apiKey = "AIzaSyCZR0gVZBwoIod0xP9P_0TWI4PUb4Wfr9A" // Replace with your actual API key
                val urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=$latitude,$longitude" +
                        "&radius=$radius" +
                        "&type=grocery_or_supermarket" +
                        "&keyword=Vons|Costco|Walmart|Ralphs|Trader Joe's|Albertsons|Grocery Outlet" +
                        "&key=$apiKey"

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val url = URL(urlString)
                        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        val inputStream: InputStream = BufferedInputStream(connection.inputStream)
                        val response = inputStream.bufferedReader().use { it.readText() }  // defaults to UTF-8
                        connection.disconnect()

                        Log.d("PlacesAPI", "Response: $response")

                        val jsonObject = JSONObject(response)
                        val results = jsonObject.getJSONArray("results")
                        val stringBuilder = StringBuilder()

                        for (i in 0 until results.length()) {
                            val place = results.getJSONObject(i)
                            val name = place.getString("name")
                            val vicinity = place.getString("vicinity")
                            stringBuilder.append("$name\n$vicinity\n")

                        }

                        withContext(Dispatchers.Main) {
                            val resultsTextView: TextView = findViewById(R.id.nearbyGroceryView)
                            resultsTextView.text = stringBuilder.toString()
                        }
                    } catch (e: Exception) {
                        Log.e("PlacesAPI", "Error: $e")
                    }
                }
            } ?: run {
                Log.e("LocationError", "Location is null")
            }
        }.addOnFailureListener { exception ->
            Log.e("LocationError", "Error getting location: $exception")
        }
    }


//    private fun findNearbyPlaces(query: String) {
//        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//            location?.let {
//                val userLatitude = location.latitude
//                val userLongitude = location.longitude
//                val lowerLeftLatitude = userLatitude - 0.01 // Example offset
//                val lowerLeftLongitude = userLongitude - 0.01 // Example offset
//                val upperRightLatitude = userLatitude + 0.01 // Example offset
//                val upperRightLongitude = userLongitude + 0.01 // Example offset
//
//                Log.d("Location", "Latitude: $userLatitude, Longitude: $userLongitude")
//
//                val bounds = RectangularBounds.newInstance(
//                    LatLng(lowerLeftLatitude, lowerLeftLongitude),
//                    LatLng(upperRightLatitude, upperRightLongitude)
//                )
//
//                val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES)
//
//                val placeTypes: MutableList<String> = mutableListOf(
//                    Place.Type.GROCERY_OR_SUPERMARKET.name,
//                    Place.Type.SUPERMARKET.name
//                )
//
//                val request: FindAutocompletePredictionsRequest =
//                    FindAutocompletePredictionsRequest.builder()
//                        .setLocationBias(bounds)
//                        //.setTypesFilter(placeTypes)
//                        .setQuery(query)
//                        .build()
//
//                Log.d("PlacesAPI", "Request: $request")
//
//                if (ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    ActivityCompat.requestPermissions(
//                        this,
//                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                        LOCATION_PERMISSION_REQUEST_CODE
//                    )
//                    return@addOnSuccessListener
//                }
//
//                placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
//                    val resultsTextView: TextView = findViewById(R.id.nearbyGroceryView)
//                    val stringBuilder = StringBuilder()
//                    for (prediction in response.autocompletePredictions) {
//                        placesClient.fetchPlace(FetchPlaceRequest.newInstance(prediction.placeId, placeFields))
//                            .addOnSuccessListener { fetchPlaceResponse ->
//                                val place = fetchPlaceResponse.place
//                                val types = place.types
//                                if (types != null && types.contains(Place.Type.GROCERY_OR_SUPERMARKET)) {
//                                    stringBuilder.append("${place.name}, ${place.address}\n")
//                                    resultsTextView.text = stringBuilder.toString()
//                                }
//                                Log.d("PlacesAPI", "Place Types: $types")
//                            }.addOnFailureListener { exception ->
//                                if (exception is ApiException) {
//                                    Log.e("PlacesAPI", "Place not found: ${exception.statusCode}")
//                                }
//                            }
//                    }
//                }.addOnFailureListener { exception ->
//                    Log.e("PlacesAPI", "Error finding places: $exception")
//                }
//            } ?: run {
//                Log.e("LocationError", "Location is null")
//                // Handle the case where location is null
//            }
//        }.addOnFailureListener { exception ->
//            Log.e("LocationError", "Error getting location: $exception")
//        }
//    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val query = intent.getStringExtra("query") ?: return // Adjusted position
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with your functionality
                findNearbyPlaces()
            } else {
                // Permission denied
            }
        }
    }

    // This function will contain your logic to find places
    private fun findPlaces() {
        // Your existing logic to find places
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
