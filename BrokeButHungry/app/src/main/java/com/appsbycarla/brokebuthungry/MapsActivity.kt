package com.appsbycarla.brokebuthungry

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.appsbycarla.brokebuthungry.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val channelislands = LatLng(34.165470, -119.045097)
        val target = LatLng(34.216969, -119.072578)
        val wholefoods = LatLng(34.239929, -119.178871)
        val costco = LatLng(34.225231, -119.148193)
        val vons = LatLng(34.224941, -119.037064)
        mMap.addMarker(MarkerOptions().position(target).title("target"))
        mMap.addMarker(MarkerOptions().position(wholefoods).title("whole foods"))
        mMap.addMarker(MarkerOptions().position(costco).title("costco"))
        mMap.addMarker(MarkerOptions().position(vons).title("vons"))
        mMap.addMarker(MarkerOptions().position(target).title("target"))



        mMap.moveCamera(CameraUpdateFactory.newLatLng(channelislands))
    }
}