package com.company.sns_crawlling

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.properties.Delegates


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var longitude = 0.0
    private var latitude = 0.0

    private lateinit var mMap : GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        longitude = intent.getDoubleExtra("Longitude", 0.0)
        latitude = intent.getDoubleExtra("Latitude", 0.0)

        val mapFragment : SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this@MapActivity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0

        val pos = LatLng(latitude, longitude)
        val point : String = "(" + latitude + ", " + longitude + ")"

        val options = MarkerOptions()
        options.position(pos)
            .title("현재 위치")
            .snippet(point)
        mMap.addMarker(options)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 10F))
    }
}
