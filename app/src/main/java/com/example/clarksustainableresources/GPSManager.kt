package com.example.clarksustainableresources

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

/*
    The GPS manager is used to register the location one a user takes a picture to add a resource
 */
class GPSManager(
    internal var main: MainActivity
) : LocationCallback() {

    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    var currentLocation: Location? = null
    var locationRequest: LocationRequest = LocationRequest.create()

    override fun onLocationResult(p0: LocationResult?) {
        currentLocation = p0?.lastLocation
        currentLocation?.run {
            main.updateCurrentLocation(this)
        }
    }

    fun register() {
        fusedLocationProviderClient?.lastLocation?.addOnSuccessListener {
            currentLocation = it
            main.updateCurrentLocation(currentLocation)
        }

        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, this, Looper.myLooper()
        )
    }

    fun unregister(){
        fusedLocationProviderClient!!.removeLocationUpdates(this)
    }

    init {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000L
        locationRequest.fastestInterval = 1000L
        locationRequest.smallestDisplacement = 5f

        var builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(main)
        settingsClient.checkLocationSettings(locationSettingRequest)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(main)

    }
}
