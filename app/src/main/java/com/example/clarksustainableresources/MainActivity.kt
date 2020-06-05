package com.example.clarksustainableresources

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*
import android.view.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(),
    AddResourceFrag.OnFragmentInteractionListener {

    var latLngCurrent = LatLng(0.0, 0.0)
    lateinit var gpsManager: GPSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        val navController = findNavController(R.id.nav_host_frag)
        toolbar.setupWithNavController(navController)

        /**
         * GPS Permissions
         */
        gpsManager = GPSManager(this)

        /**
         * Give permission to access/take photos
         */
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ),
                100
            )
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar, menu)
        return true
    }

    /**
     * Toolbar Items
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sustainable_map -> findNavController(R.id.nav_host_frag)
                .navigate(R.id.action_global_mapsFrag)
            R.id.action_sustainable_resources -> findNavController(R.id.nav_host_frag)
                .navigate(R.id.action_global_resourcesFrag)
            R.id.action_add_resource -> findNavController(R.id.nav_host_frag)
                .navigate(R.id.action_global_addResourceFrag)
            R.id.action_help -> findNavController(R.id.nav_host_frag)
                .navigate(R.id.action_global_helpFrag)
            R.id.action_sign_out -> {
                val alertDialog: AlertDialog? = let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(R.string.ok) { dialog, id ->
                            signOutUser()
                        }
                        setNegativeButton(R.string.cancel) { dialog, id ->
                            // User cancelled the dialog
                        }
                    }
                    builder.setMessage(R.string.sign_out_message)
                        .setTitle(R.string.sign_out_message_title)
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
        }
        return true
    }

    /**
     * Sign out user and redirect to login page
     */
    fun signOutUser() {
        FirebaseAuth.getInstance().signOut()
        val loginIntent = Intent(this, LoginActivity::class.java)
        val bundle = Bundle()
        bundle.putInt("signed out", 1)
        loginIntent.putExtras(bundle)
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.startActivity(loginIntent)
    }

    /**
     * Return current location of user
     */
    override fun setLatLong() {
        latLngCurrent = LatLng(0.0, 0.0)
        if (!checkPermission()) {
            println("no permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        } else {
            println("permission")
            gpsManager.register()
        }
    }

    override fun returnLatLong(): LatLng {
        println("called: $latLngCurrent")
        return latLngCurrent
    }

    /**
     * Update the current location
     */
    fun updateCurrentLocation(location: Location?) {
        if (location != null) {
            latLngCurrent = LatLng(location.latitude, location.longitude)
        }
    }

    /**
     * Check to see if user has access to location services
     */
    fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}