package com.example.clarksustainableresources

/**
 * SustainableResource class
 */
class SustainableResource {
    var id = ""
    var name = "NAME_HERE"
    var category = "CATEGORY_HERE"
    var location = ""
    var lat = 0.0
    var long = 0.0
    var description = ""
    var approved = false
    var userAdded = ""
    var userAddedID = ""
    var comments = ArrayList<String>() // to make it easier to store on firebase it is a string
}