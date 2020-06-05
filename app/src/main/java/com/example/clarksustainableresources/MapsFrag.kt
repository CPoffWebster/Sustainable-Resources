package com.example.clarksustainableresources

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.Marker
import androidx.navigation.fragment.findNavController

class MapsFrag : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnInfoWindowClickListener {

    /**
     * Variables
     */
    lateinit var resourceViewModel: ResourceViewModel
    private lateinit var mMap: GoogleMap
    private var mapView: MapView? = null

    var fullResourceList = ArrayList<SustainableResource>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        resourceViewModel = activity?.run {
            ViewModelProviders.of(this).get(ResourceViewModel::class.java)
        } ?: throw Exception("activity invalid")

        // initialize GoogleMaps using MapAsync --> initializes map systems and view
        val v = inflater.inflate(R.layout.fragment_maps, container, false)

        mapView = v?.findViewById(R.id.mapView) as MapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this) //this is important
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        /**
         * Create dialog box
         */
        if (arguments?.getString("popup") == null && resourceViewModel.popupClosed) {
            val alertDialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it, R.style.DialogTheme)
                builder.apply {
                    setPositiveButton(R.string.ok) { dialog, id ->
                        showResourceDialogBox()
                    }
                }
                // Set other dialog properties
                builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title)

                // Create the AlertDialog
                builder.create()
            }
            alertDialog?.show()
            resourceViewModel.popupClosed = false
        } else {
            showResourceDialogBox()
        }

        /**
         * Toast alert for when user creates a new resource
         */
        val styleFilter = arguments?.getString("resourceAdded")
        if (styleFilter == "true") {
            Toast.makeText(
                context,
                "New Resource Added! It will be reviewed and then added to the list. " +
                        "Thank you for you contribution!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Allows selection of categories to show on map
     */
    fun showResourceDialogBox() {
        if (arguments?.getString("categorySelected") == null) {
            val resourceCategories = arrayOf(
                "Water Bottle Refill",
                "Recycling Bins",
                "Compost Bins",
                "Transportation",
                "Gender Neutral Bathrooms",
                "Greenspaces",
                "Other"
            )
            var selectedCategories = ArrayList<Int>()
            var oldSelectedCategories = ArrayList<Int>()
            val checkedItem = BooleanArray(resourceCategories.size)
            resourceViewModel.selectedCategoryItems.observe(this, Observer {
                selectedCategories = it
                oldSelectedCategories = it
                for (index in resourceCategories.indices) {
                    if (selectedCategories.contains(index)) {
                        checkedItem.set(index, true)
                    } else {
                        checkedItem.set(index, false)
                    }
                }
            })

            val builder = AlertDialog.Builder(context, R.style.DialogTheme)
            builder.setTitle("Choose which categories you want to see on the map:")
                .setMultiChoiceItems(resourceCategories, checkedItem) { dialog, which, isChecked ->
                    if (isChecked) {
                        selectedCategories.add(which)
                    } else if (selectedCategories.contains(which)) {
                        selectedCategories.remove(Integer.valueOf(which))
                    }
                }
                .setPositiveButton("Confirm") { dialog, id ->
                    callShowCategoriesOnMap(selectedCategories)
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    callShowCategoriesOnMap(oldSelectedCategories)
                }
            val dialog = builder.create()
            dialog.show()

        }
    }

    /**
     * Show old or new selected categories on the map
     */
    fun callShowCategoriesOnMap(showCategories: ArrayList<Int>) {
        var combinedString = ""
        for (category in showCategories) {
            // call function to add items of that category to the map
            var intToCategory = ""
            when (category) {
                0 -> intToCategory = "Water Bottle Refill"
                1 -> intToCategory = "Recycling Bin"
                2 -> intToCategory = "Compost Bin"
                3 -> intToCategory = "Transportation"
                4 -> intToCategory = "Gender Neutral Bathroom"
                5 -> intToCategory = "Greenspace"
                6 -> intToCategory = "Other"
            }
            if (category != showCategories[showCategories.size - 1]) {
                combinedString += "$intToCategory||||"
            } else {
                combinedString += intToCategory
                showCategoryOnMap(combinedString)
            }
            resourceViewModel.updateCategoriesSelected(showCategories)
        }
    }

    /**
     * Show all items of a category on the map
     */
    fun showCategoryOnMap(categorySelected: String) {
        val bundle = Bundle()
        bundle.putString("categorySelected", categorySelected)
        bundle.putString("popup", "popupClosed")
        findNavController().navigate(R.id.action_mapsFrag_self, bundle)
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        if (marker.snippet == null) {
            mMap.moveCamera(CameraUpdateFactory.zoomIn());
            return true
        }

        Toast.makeText(
            context,
            "Click on the resource window to see more information!",
            Toast.LENGTH_LONG
        ).show()

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur.
        return false
    }

    override fun onInfoWindowClick(marker: Marker) {
        val resourceTag = marker.tag as String
        var resource = SustainableResource()
        resourceViewModel.resourcesList.observe(this, Observer {
            for (item in it) {
                if (item.id == resourceTag) {
                    resource = item
                }
            }
        })

        val bundle = Bundle()
        bundle.putString("resID", resource.id)
        bundle.putString("resName", resource.name)
        bundle.putString("resDescription", resource.description)
        bundle.putString("resCategory", resource.category)
        bundle.putString("resLocation", resource.location)
        bundle.putBoolean("approvedResource", resource.approved)
        findNavController().navigate(R.id.action_mapsFrag_to_detailFrag, bundle)
    }

    /**
     * Show the items of the clicked category on the map
     */
    fun showCategoryItems(categorySelected: String) {
        mMap.isMyLocationEnabled = true
        val drawable = when (categorySelected) {
            "Water Bottle Refill" -> R.drawable.water_symbol
            "Recycling Bin" -> R.drawable.recycler_symbol
            "Compost Bin" -> R.drawable.compost_symbol
            "Transportation" -> R.drawable.transportation_symbol
            "Gender Neutral Bathroom" -> R.drawable.gendern_bathroom_symbol
            "Greenspace" -> R.drawable.green_symbol
            "Other" -> R.drawable.other_symbol
            else -> R.drawable.other_symbol
        }
        val b = BitmapFactory.decodeResource(resources, drawable)
        val smallMarker = Bitmap.createScaledBitmap(b, 150, 150, false)
        val smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker)

        val markerList = ArrayList<Marker>()
        resourceViewModel.resourcesList.observe(this, Observer {
            for (item in it) {
                if (item.approved && item.category == categorySelected) { // only for approved items
                    val pp = LatLng(item.lat, item.long)
                    var newMarker: Marker
                    newMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(pp)
                            .title(item.name)
                            .snippet("Location: " + item.location)
                    )
                    newMarker.tag = item.id
                    newMarker.setIcon(smallMarkerIcon)

                    markerList.add(newMarker)
                    fullResourceList.add(item)
                    mMap.setOnMarkerClickListener(this)
                    mMap.setOnInfoWindowClickListener(this)
                }
            }
        })
    }

    /**
     * Create the MapView
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(42.2507, -71.8228), 17f))

        // Show item of a category on the map
        mMap.clear()
        val categorySelected = arguments?.getString("categorySelected")
        if (categorySelected != null) {
            if (categorySelected.contains("||||")) {
                val separated = categorySelected.split("||||").toTypedArray()
                for (category in separated) {
                    showCategoryItems(category)
                }
            } else {
                showCategoryItems(categorySelected)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

}
