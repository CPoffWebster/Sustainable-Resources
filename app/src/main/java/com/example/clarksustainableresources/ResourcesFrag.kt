package com.example.clarksustainableresources

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_resources.*

/*
    There is a lot of code here, but most of it is quite easy to understand.
    In order to avoid making a recycler of categories with a recycler for each
    category (recycler of recyclers) each category is statically made and dealt with individually.
    This means each category has all the code needed for that category so there are a few
    segments with a lot of ifs or where loops.
 */

var categoryOpen = "Other" // which category is selected and (open) for user to see

class ResourcesFrag : Fragment() {

    /**
    Recycler Variables
     */
    lateinit var viewAdapter: RecyclerViewAdapter
    lateinit var viewManagerWater: RecyclerView.LayoutManager
    lateinit var viewManagerRecycle: RecyclerView.LayoutManager
    lateinit var viewManagerCompost: RecyclerView.LayoutManager
    lateinit var viewManagerTransportation: RecyclerView.LayoutManager
    lateinit var viewManagerNeutralBathroom: RecyclerView.LayoutManager
    lateinit var viewManagerGreen: RecyclerView.LayoutManager
    lateinit var viewManagerOther: RecyclerView.LayoutManager

    /**
     * ViewModel and Animations
     */
    lateinit var resourceViewModel: ResourceViewModel
    lateinit var rotateClock: AnimatorSet
    lateinit var rotateCounterClock: AnimatorSet

    /**
     * Global Variables
     */
    var fullResourceList = ArrayList<SustainableResource>()  // full list of resource items
    lateinit var recyclerUsed: RecyclerView     // which recycler (category) is being used
    var waterOpen = false   // if the category list is being shown
    var recycleOpen = false
    var compostOpen = false
    var transportationOpen = false
    var neutralBathroomOpen = false
    var greenOpen = false
    var otherOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        resourceViewModel = activity?.run {
            ViewModelProviders.of(this).get(ResourceViewModel::class.java)
        } ?: throw Exception("activity invalid")

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        /**
         * Initialize all recyclers as invisible
         */
        water_resources_recycler.visibility = View.GONE
        recycle_resources_recycler.visibility = View.GONE
        compost_resources_recycler.visibility = View.GONE
        transportation_resources_recycler.visibility = View.GONE
        neutral_bathroom_resources_recycler.visibility = View.GONE
        green_resources_recycler.visibility = View.GONE
        other_resources_recycler.visibility = View.GONE

        /**
         * Animation for category plus buttons
         */
        rotateClock = AnimatorInflater.loadAnimator(
            context, R.animator.rotate_clockwise
        ) as AnimatorSet

        rotateCounterClock = AnimatorInflater.loadAnimator(
            context, R.animator.rotate_counterclockwise
        ) as AnimatorSet


        /**
         * Show items in specific category
         */
        water_btn.setOnClickListener {
            if (!waterOpen) {
                categoryOpen = "Water Bottle Refill"
                manageCategoryWork()
            } else {
                waterOpen = false
                hideButton(water_btn)
                water_resources_recycler.visibility = View.GONE
            }
        }
        recycle_btn.setOnClickListener {
            if (!recycleOpen) {
                categoryOpen = "Recycling Bin"
                manageCategoryWork()
            } else {
                recycleOpen = false
                hideButton(recycle_btn)
                recycle_resources_recycler.visibility = View.GONE
            }
        }
        compost_btn.setOnClickListener {
            if (!compostOpen) {
                categoryOpen = "Compost Bin"
                manageCategoryWork()
            } else {
                compostOpen = false
                hideButton(compost_btn)
                compost_resources_recycler.visibility = View.GONE
            }
        }
        transportation_btn.setOnClickListener {
            if (!transportationOpen) {
                categoryOpen = "Transportation"
                manageCategoryWork()
            } else {
                transportationOpen = false
                hideButton(transportation_btn)
                transportation_resources_recycler.visibility = View.GONE
            }
        }
        neutral_bathroom_btn.setOnClickListener {
            if (!neutralBathroomOpen) {
                categoryOpen = "Gender Neutral Bathroom"
                manageCategoryWork()
            } else {
                neutralBathroomOpen = false
                hideButton(neutral_bathroom_btn)
                neutral_bathroom_resources_recycler.visibility = View.GONE
            }
        }
        green_btn.setOnClickListener {
            if (!greenOpen) {
                categoryOpen = "Greenspace"
                manageCategoryWork()
            } else {
                greenOpen = false
                hideButton(green_btn)
                green_resources_recycler.visibility = View.GONE
            }
        }
        other_btn.setOnClickListener {
            if (!otherOpen) {
                categoryOpen = "Other"
                manageCategoryWork()
            } else {
                otherOpen = false
                hideButton(other_btn)
                other_resources_recycler.visibility = View.GONE
            }
        }

        /**
         * Click listeners --> when you click category, redirect to,
         * and show clicked category on the map
         */
        water_category.setOnClickListener {
            categoryItemSelected("Water Bottle Refill")
        }
        recycle_category.setOnClickListener {
            categoryItemSelected("Recycling Bin")
        }
        compost_category.setOnClickListener {
            categoryItemSelected("Compost Bin")
        }
        transportation_category.setOnClickListener {
            categoryItemSelected("Transportation")
        }
        neutral_category.setOnClickListener {
            categoryItemSelected("Gender Neutral Bathroom")
        }
        green_category.setOnClickListener {
            categoryItemSelected("Greenspace")
        }
        other_category.setOnClickListener {
            categoryItemSelected("Other")
        }

        /**
         * Initialize ViewManagers for each category recycler
         */
        viewManagerWater = LinearLayoutManager(context)
        viewManagerRecycle = LinearLayoutManager(context)
        viewManagerCompost = LinearLayoutManager(context)
        viewManagerTransportation = LinearLayoutManager(context)
        viewManagerNeutralBathroom = LinearLayoutManager(context)
        viewManagerGreen = LinearLayoutManager(context)
        viewManagerOther = LinearLayoutManager(context)
        viewAdapter = RecyclerViewAdapter(ArrayList()) {
            recyclerViewItemSelected(it)
        }
    }


    /**
     * Redirect to map with selected category item
     */
    fun categoryItemSelected(categorySelected: String) {
        val bundle = Bundle()
        bundle.putString("categorySelected", categorySelected)
        findNavController().navigate(R.id.action_resourcesFrag_to_mapsFrag, bundle)
    }

    /**
     * RecyclerView of categories and individual resources
     */
    class RecyclerViewAdapter(
        var resourceList: ArrayList<SustainableResource>,
        val clickListener: (SustainableResource) -> Unit
    ) : RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.resources_recycler, parent, false)
            return RecyclerViewHolder(v)
        }

        override fun getItemCount(): Int {
            return resourceList.size
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            holder.bind(resourceList[position], clickListener)
        }

        class RecyclerViewHolder(val viewItem: View) : RecyclerView.ViewHolder(viewItem) {
            fun bind(
                resourceItem: SustainableResource,
                clickListener: (SustainableResource) -> Unit
            ) {
                val drawable: Int = when (resourceItem.category) {
                    "Water Bottle Refill" -> R.drawable.water_symbol
                    "Recycling Bin" -> R.drawable.recycler_symbol
                    "Compost Bin" -> R.drawable.compost_symbol
                    "Transportation" -> R.drawable.transportation_symbol
                    "Gender Neutral Bathroom" -> R.drawable.gendern_bathroom_symbol
                    "Greenspace" -> R.drawable.green_symbol
                    "Other" -> R.drawable.other_symbol
                    else -> R.drawable.clark_sustainability_icon
                }
                viewItem.findViewById<ImageView>(R.id.category_symbol_recycler)
                    .setImageResource(drawable)
                viewItem.findViewById<TextView>(R.id.title_recycler).text = resourceItem.name
                viewItem.findViewById<TextView>(R.id.location_recycler).text = resourceItem.location
                viewItem.setOnClickListener { clickListener(resourceItem) }
            }
        }
    }

    fun showButton(button: ImageButton) {
        rotateClock.apply {
            setTarget(button)
            start()
        }
    }

    fun hideButton(button: ImageButton) {
        rotateCounterClock.apply {
            setTarget(button)
            start()
        }
    }

    /**
     * Open category chosen with the given list, close
     * any other category that may be open
     */
    fun manageCategoryWork() {
        resourceViewModel.resourcesList.observe(this, Observer {
            fullResourceList = it
        })


        /**
         * find whichever category is open: Set it to be
         * open, swivel the button, show the RecyclerView, and
         * schedule the animation
         */
        when (categoryOpen) {
            "Water Bottle Refill" -> {
                waterOpen = true
                showButton(water_btn)
                water_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = water_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerWater
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            "Recycling Bin" -> {
                recycleOpen = true
                showButton(recycle_btn)
                recycle_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = recycle_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerRecycle
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            "Compost Bin" -> {
                compostOpen = true
                showButton(compost_btn)
                compost_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = compost_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerCompost
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            "Transportation" -> {
                transportationOpen = true
                showButton(transportation_btn)
                transportation_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = transportation_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerTransportation
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            "Gender Neutral Bathroom" -> {
                neutralBathroomOpen = true
                showButton(neutral_bathroom_btn)
                neutral_bathroom_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = neutral_bathroom_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerNeutralBathroom
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            "Greenspace" -> {
                greenOpen = true
                showButton(green_btn)
                green_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = green_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerGreen
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            "Other" -> {
                otherOpen = true
                showButton(other_btn)
                other_resources_recycler.visibility = View.VISIBLE
                recyclerUsed = other_resources_recycler
                recyclerUsed.apply {
                    this.layoutManager = viewManagerOther
                    this.adapter = viewAdapter
                    this.scheduleLayoutAnimation()
                }
            }
            else -> println("do nothing")
        }

        /**
         * Set the recycler list to be only of the category chosen
         */
        val sortedResourceList = ArrayList<SustainableResource>()
        for (item in fullResourceList) {
            if (item.category == categoryOpen && item.approved) {
                sortedResourceList.add(item)
            }
        }
        viewAdapter.resourceList = sortedResourceList
        viewAdapter.notifyDataSetChanged()
        viewAdapter = RecyclerViewAdapter(sortedResourceList) {
            recyclerViewItemSelected(it)
        }

        /**
         * If another category is open, close it and hide the recycler
         */
        if (waterOpen && categoryOpen != "Water Bottle Refill") {
            hideButton(water_btn)
            water_resources_recycler.visibility = View.GONE
            waterOpen = false
        }
        if (recycleOpen && categoryOpen != "Recycling Bin") {
            hideButton(recycle_btn)
            recycle_resources_recycler.visibility = View.GONE
            recycleOpen = false
        }
        if (compostOpen && categoryOpen != "Compost Bin") {
            hideButton(compost_btn)
            compost_resources_recycler.visibility = View.GONE
            compostOpen = false
        }
        if (transportationOpen && categoryOpen != "Transportation") {
            hideButton(transportation_btn)
            transportation_resources_recycler.visibility = View.GONE
            transportationOpen = false
        }
        if (neutralBathroomOpen && categoryOpen != "Gender Neutral Bathroom") {
            hideButton(neutral_bathroom_btn)
            neutral_bathroom_resources_recycler.visibility = View.GONE
            neutralBathroomOpen = false
        }
        if (greenOpen && categoryOpen != "Greenspace") {
            hideButton(green_btn)
            green_resources_recycler.visibility = View.GONE
            greenOpen = false
        }
        if (otherOpen && categoryOpen != "Other") {
            hideButton(other_btn)
            other_resources_recycler.visibility = View.GONE
            otherOpen = false
        }
    }

    /**
    Redirect to detail page if a category ITEM is chosen
     */
    fun recyclerViewItemSelected(resourceItem: SustainableResource) {
        val bundle = Bundle()
        bundle.putString("resID", resourceItem.id)
        bundle.putString("resName", resourceItem.name)
        bundle.putString("resDescription", resourceItem.description)
        bundle.putString("resCategory", resourceItem.category)
        bundle.putString("resLocation", resourceItem.location)
        bundle.putBoolean("approvedResource", resourceItem.approved)
        findNavController().navigate(R.id.action_resourcesFrag_to_detailFrag, bundle)
    }
}
