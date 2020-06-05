package com.example.clarksustainableresources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlin.collections.ArrayList

class ResourceViewModel(application: Application) : AndroidViewModel(application) {

    // Variables
    var popupClosed = true // initial popup
    var selectedCategoryItems =
        MutableLiveData<ArrayList<Int>>() // list of categories selected in maps frag
    var uniqueIDCount = MutableLiveData<Long>() // the unique IDs for each Sustainable Resource
    var resourcesList = MutableLiveData<ArrayList<SustainableResource>>() // list of all resources
    var userList = MutableLiveData<ArrayList<UserInformation>>() // list of all current users
    private var database = MutableLiveData<DatabaseReference>()

    var categoryList = arrayOf(
        "Water Bottle Refill", "Recycling Bin", "Compost Bin",
        "Transportation", "Gender Neutral Bathroom", "Greenspace", "Other"
    )

    init {
        selectedCategoryItems.value = ArrayList()
        uniqueIDCount.value = 0.toLong()
        resourcesList.value = ArrayList()
        userList.value = ArrayList()
        database.value = FirebaseDatabase.getInstance().reference

        database.value?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                println("no internet connection!")
            }

            // gets the items in each category
            override fun onDataChange(p0: DataSnapshot) {
                resourcesList.value!!.clear()
                userList.value!!.clear()

                for (category in categoryList) {
                    p0.child("resourcesList").child(category).children.forEach {
                        it.getValue(SustainableResource::class.java)?.let {
                            resourcesList.value?.add(it)
                        }
                    }
                }
                p0.child("userInfoList").children.forEach {
                    it.getValue(UserInformation::class.java)?.let {
                        userList.value?.add(it)
                    }
                }
                p0.child("UniqueID").getValue(Long::class.java)?.let {
                    uniqueIDCount.value = it
                }
                listener?.updateRecycler(resourcesList.value)
            }
        })
    }

    var listener: DataChangedListener? = null

    interface DataChangedListener {
        fun updateRecycler(updatedList: ArrayList<SustainableResource>?)
    }

    fun updateNextID(idCount: Int) {
        database.value?.child("UniqueID")
            ?.setValue(idCount.toLong())
    }

    /**
     * Uploads a new resource to the Firebase database
     */
    fun uploadData(uploadResource: SustainableResource) {
        viewModelScope.launch {
            async {
                uploadNewResource(uploadResource)
            }.await() //this is done after upload (done asyncronously)
            updateNextID(uploadResource.id.toInt() + 1)
        }
    }

    /**
     * Helper function
     */
    suspend fun uploadNewResource(resource: SustainableResource) = withContext(Dispatchers.IO) {
        delay(3000)
        resourcesList.value?.add(resource)
        database.value?.child("unapprovedResources")?.child(resource.category)?.child(resource.id)
            ?.setValue(resource)
        println("Uploaded Resource")
    }

    /**
     * Uploads a new user to the firebase database
     */
    fun uploadUserData(uploadUser: UserInformation) {
        viewModelScope.launch {
            async {
                uploadNewUser(uploadUser)
            }.await() //this is done after upload (done asyncronously)
        }
    }

    /**
     * Helper function
     */
    suspend fun uploadNewUser(uploadUser: UserInformation) = withContext(Dispatchers.IO) {
        delay(3000)
        var userExists = false
        for (user in userList.value!!.iterator()) {
            if (user.authID == uploadUser.authID) {
                userExists = true
                break
            }
        }
        if (!userExists) {
            userList.value?.add(uploadUser)
            database.value?.child("userInfoList")?.child(uploadUser.authID)?.setValue(uploadUser)
            println("Uploaded User")
        }
    }

    /**
     * Update a users saved comments
     */
    fun updateUserComments(userInfo: UserInformation) {
        database.value?.child("userInfoList")
            ?.child(userInfo.authID)
            ?.child("comments")
            ?.setValue(userInfo.comments)
    }


    /**
     * Update comments for a resource
     */
    fun updateComments(category: String, ID: String, comments: ArrayList<String>) {
        database.value?.child("resourcesList")
            ?.child(category)
            ?.child(ID)
            ?.child("comments")
            ?.setValue(comments)
    }


    /**
     * Save categories selected in the map frag
     */
    fun updateCategoriesSelected(update: ArrayList<Int>) {
        selectedCategoryItems.value = update
    }
}