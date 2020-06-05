package com.example.clarksustainableresources


import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_add_resource.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class AddResourceFrag : Fragment() {

    /**
     * Variables
     */
    lateinit var photoViewModel: PhotoViewModel
    lateinit var resourceViewModel: ResourceViewModel
    lateinit var appExecutors: AppExecutors
    var cred = Credentials()
    var newResource = SustainableResource()
    var pictureTaken = false
    var cameraImage: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var userAdding = UserInformation()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        photoViewModel = activity?.run {
            ViewModelProviders.of(this).get(PhotoViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        resourceViewModel = activity?.run {
            ViewModelProviders.of(this).get(ResourceViewModel::class.java)
        } ?: throw Exception("activity invalid")

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_resource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resourceViewModel.userList.observe(this, Observer {
            for (item in it) {
                if (item.authID == FirebaseAuth.getInstance().currentUser!!.uid) {
                    userAdding = item
                    break
                }
            }
            // If userAdding.reportUser than they have been reported and are not allowed to add resources
            if (userAdding.reportUserAddResource) {
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it, R.style.DialogTheme)
                    builder.setMessage(R.string.reported_user).setTitle("Flagged User")
                    builder.apply {
                        setPositiveButton(R.string.ok) { dialog, id ->
                            findNavController().navigate(R.id.action_addResourceFrag_to_mapsFrag)
                        }
                    }
                    builder.create()
                }
                alertDialog?.show()
            } else {
                Toast.makeText(
                    context,
                    "Note: taking the photo also sets the location. Please take photos Vertically!",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        /**
         * Take a photo
         */
        photo_taken_image.setOnClickListener {
            pictureTaken = true
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 1)
            listener.setLatLong()

            resourceViewModel.uniqueIDCount.observe(this, Observer {
                newResource.id = it.toString()
            })
        }

        /**
         * Select the category for item
         */
        category_spinner.setOnClickListener {
            val resourceCategories = arrayOf(
                "Water Bottle Refill",
                "Recycling Bin",
                "Compost Bin",
                "Transportation",
                "Gender Neutral Bathroom",
                "Greenspace",
                "Other"
            )
            val builder = AlertDialog.Builder(context, R.style.DialogTheme)
            builder.setTitle("Select a Category")
                .setItems(
                    resourceCategories
                ) { dialog, which ->
                    category_spinner.text = resourceCategories[which]
                    category_spinner.setTextColor(Color.parseColor("#000000"))
                }
            val dialog = builder.create()
            dialog.show()
        }

        /**
         * Adds new resource to Firebase
         */
        add_resource_btn.setOnClickListener {
            val latLng = listener.returnLatLong()
            newResource.name = name_text.text.toString()
            newResource.lat = latLng.latitude
            newResource.long = latLng.longitude
            newResource.location = location_text.text.toString()
            newResource.category = category_spinner.text.toString()
            newResource.description = information_text.text.toString()

            /**
             * Check if user filled out necessary information. Save the image, upload
             * data and image to Firebase, send email notification. Else, notify
             * user they must fill out required info
             */
            if (pictureTaken && newResource.name != "" && newResource.location != "" &&
                newResource.category != "Select Category"
            ) {
                newResource.approved = false
                newResource.userAdded = FirebaseAuth.getInstance().currentUser!!.email!! // set from current GoogleUser
                newResource.userAddedID = FirebaseAuth.getInstance().currentUser!!.uid
                photoViewModel.saveBitmap(cameraImage)
                resourceViewModel.uploadData(newResource)
                submit(cameraImage)
                sendEmail(
                    newResource.name,
                    newResource.location,
                    newResource.description,
                    newResource.category
                )

                // return to MapsFrag
                val bundle = Bundle()
                bundle.putString("resourceAdded", "true")
                findNavController().navigate(R.id.action_addResourceFrag_to_mapsFrag, bundle)
            } else {
                Toast.makeText(
                    context,
                    "You haven't filled out all the information!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * ActivityResult for camera
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1) {
            cameraImage = data?.extras?.get("data") as Bitmap
            photo_taken_image.setImageBitmap(cameraImage)
        }
    }

    /**
     * submit photo to Firebase storage
     */
    fun submit(image: Bitmap) {
        var stream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        var b = stream.toByteArray()
        var storageRef =
            FirebaseStorage.getInstance().reference.child(newResource.id)
        var uploadTask = storageRef.putBytes(b)
        uploadTask.addOnSuccessListener {
            Toast.makeText(activity, "uploaded", Toast.LENGTH_SHORT)
        }.addOnFailureListener {
            Toast.makeText(activity, "failure", Toast.LENGTH_SHORT)
        }

    }

    /**
     * Send email when resource is added
     */
    fun sendEmail(name: String, loc: String, desc: String, category: String) {
        appExecutors.diskIO().execute {
            val props = System.getProperties()
            props.put("mail.smtp.host", "smtp.gmail.com")
            props.put("mail.smtp.socketFactory.port", "465")
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.port", "465")

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                    return javax.mail.PasswordAuthentication(cred.email, cred.password)
                }
            })

            try {
                val mm = MimeMessage(session)
                val emailID = cred.email
                // sending email to itself
                mm.setFrom(InternetAddress(emailID, "Sustainable Clark"))
                mm.addRecipient(Message.RecipientType.TO, InternetAddress(emailID))
                mm.subject = "New Resource Added!"
                var message =
                    "A new resource was just added. Here is the information below:\n\n " +
                            "Name: $name\n" +
                            "Location: $loc\n" +
                            "Additional Information: $desc\n" +
                            "Category: $category\n\n" +
                            "To automatically approve or reject this resource, either change it's approved " +
                            "parameter to 'True', or delete the resource from the database"
                mm.setText(message)
                Transport.send(mm)
                appExecutors.mainThread().execute {
                    //Something that should be executed on main thread.
                }
            } catch (e: MessagingException) {
                e.printStackTrace()
            }
        }
    }


    /**
     * Interface used to retrieve latitude and longitude
     */
    lateinit var listener: OnFragmentInteractionListener

    interface OnFragmentInteractionListener {
        fun setLatLong()
        fun returnLatLong(): LatLng
    }

    override fun onAttach(context: Context) {
        appExecutors = AppExecutors()
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        }
    }

}
