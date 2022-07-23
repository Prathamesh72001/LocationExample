package com.example.locationexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMapClickListener,GoogleMap.OnMapLongClickListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //hooks
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationText = findViewById(R.id.Location)
        currLocBtn = findViewById(R.id.currentLocBtn)

        //Methods
        checkMyPermission()
        initMaps()

        //Listeners
        currLocBtn!!.setOnClickListener(View.OnClickListener { getCurrentLocation() })


    }

    private fun initMaps() {
        if (isPermissionGranted) {
            if (isGPSEnabled()) {
                supportMapFragment =
                    supportFragmentManager.findFragmentById(R.id.frag) as SupportMapFragment?
                supportMapFragment!!.getMapAsync { googleMap: GoogleMap ->
                    this.onMapReady(
                        googleMap
                    )
                }
            } else {
                Toast.makeText(this, "Unable to detect current location", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            PermissionDenied()
        }
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (providerEnable) {
            return true
        } else {
            AlertDialog.Builder(this).setTitle("GPS Permission")
                .setMessage("GPS is required for this work. Please enable GPS").setPositiveButton(
                    "Yes"
                ) { dialog: DialogInterface?, which: Int ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent, GPS_REQUEST_CODE)
                }.setCancelable(false)
                .show()
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationProviderClient!!.lastLocation.addOnCompleteListener { task: Task<Location?> ->
            if (task.isSuccessful && task.result != null) {
                val location = task.result
                try {
                    gotoLocation(location!!.latitude, location.longitude)
                    try {
                        val geocoder =
                            Geocoder(this, Locale.getDefault())
                        val addressList =
                            geocoder.getFromLocation(
                                location.latitude, location.longitude, 1
                            )
                        locationText!!.text = addressList[0].getAddressLine(0)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } catch (e: NullPointerException) {
                    Toast.makeText(this, "Unable to detect location", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Unable to detect current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gotoLocation(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        if(marker1!=null){
            marker1!!.remove()
        }
        marker1=mgoogleMap!!.addMarker(MarkerOptions().position(latLng).title("Current Location"))
        mgoogleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        mgoogleMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    private fun checkMyPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                    isPermissionGranted = true
                }

                override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {

                    Toast.makeText(applicationContext, "Permission Denied : This Application Does Not Have Permission To Access Location", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissionRequest: PermissionRequest,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.cancelPermissionRequest()
                }
            }).check()
    }

    private fun PermissionDenied() {
        AlertDialog.Builder(this).setTitle("Access Location Permission")
            .setIcon(R.drawable.ic_baseline_error_24)
            .setMessage("Location access is required for this work. Please allow this application to access this device's location").setPositiveButton(
                "Yes"
            ) { dialog: DialogInterface?, which: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, "")
                intent.data = uri
                startActivity(intent)
            }.setCancelable(false)
            .show()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mgoogleMap = googleMap
        mgoogleMap!!.setOnMapClickListener(this)
        mgoogleMap!!.setOnMapLongClickListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (providerEnable) {
                Toast.makeText(this, "GPS is enable", Toast.LENGTH_SHORT).show()
                initMaps()
            } else {
                Toast.makeText(this, "GPS is not enable", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object{
        var isPermissionGranted = false
        var locationText: TextView? = null
        var fusedLocationProviderClient: FusedLocationProviderClient? = null
        var mgoogleMap: GoogleMap? = null
        var currLocBtn:RelativeLayout? = null
        var supportMapFragment: SupportMapFragment? = null
        private val GPS_REQUEST_CODE = 9001
        var marker1:Marker?=null

    }

    override fun onMapClick(p0: LatLng) {
        Log.d("tag", p0.toString())
        if(marker1!=null){
            marker1!!.remove()
        }
        marker1 = mgoogleMap!!.addMarker(
            MarkerOptions().position(p0).title("Selected Location")
        )
        mgoogleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(p0, 18f))
        mgoogleMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL

        try {
            val geocoder =
                Geocoder(this@MainActivity, Locale.getDefault())
            val addressList =
                geocoder.getFromLocation(
                    p0.latitude, p0.longitude, 1
                )
            locationText!!.text = addressList[0].getAddressLine(0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onMapLongClick(p0: LatLng) {

    }
}