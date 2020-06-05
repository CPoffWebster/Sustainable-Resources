package com.example.clarksustainableresources

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_detail.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class DetailFrag : Fragment(), ResourceViewModel.DataChangedListener {

    // Variables
    lateinit var resourceViewModel: ResourceViewModel
    lateinit var viewAdapter: RecyclerViewAdapter
    lateinit var viewManager: RecyclerView.LayoutManager
    var resID: String? = ""     // resource ID for resource shown on this screen

    /**
        Update the comments recycler to have the new comment added
        First find the comment list for that resource
        Then split the comments, separated by |||| and then updating
        If nothing to update, don't update recycler
     */
    override fun updateRecycler(updatedList: ArrayList<SustainableResource>?) {
        if (updatedList != null) {
            val commentList = ArrayList<CommentItem>()
            val iterableList: ArrayList<SustainableResource> = updatedList
            for (item in iterableList) {
                if (item.id == resID) {
                    for (comment in item.comments) {
                        if (comment != null) {
                            // split the comment item that is set as a string
                            val newComment = CommentItem()
                            val separated = comment.split("||||").toTypedArray()
                            newComment.date = "Posted on: " + separated[0]
                            newComment.comment = separated[1]
                            commentList.add(0, newComment)
                        }
                    }
                    viewAdapter.commentList = commentList
                    viewAdapter.notifyDataSetChanged()
                    if (comments_recycler != null) {
                        comments_recycler.smoothScrollToPosition(Int.MAX_VALUE)
                    }
                    break
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        resourceViewModel = activity?.run {
            ViewModelProviders.of(this).get(ResourceViewModel::class.java)
        } ?: throw Exception("activity invalid")

        resourceViewModel.listener = this

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        resID = arguments?.getString("resID")
        val resName = arguments?.getString("resName")
        val resDescription = arguments?.getString("resDescription")
        val resCategory = arguments?.getString("resCategory")
        val resLocation = arguments?.getString("resLocation")

        /**
         * Set resource text views
         */
        resource_name.text = "Name: $resName"
        resource_location.text = "Location: $resLocation"
        resource_category.text = "Category: $resCategory"
        resource_details.text = resDescription

        if (resource_details.text.toString() == "") {
            resource_details.visibility = View.GONE
        }

        /**
         * Retrieve image from Firebase Storage
         */
        val storageRef = FirebaseStorage.getInstance().reference
        var islandRef = storageRef.child("$resID")
        val ONE_MEGABYTE: Long = 1024 * 1024
        islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
            println("Image retrieval success")
            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
            val image = getView()!!.findViewById(R.id.resource_picture) as ImageView

            image.setImageBitmap(
                Bitmap.createScaledBitmap(
                    bmp, image.width,
                    image.height, false
                )
            )
            // could allow horizontal pictures in the future
            // resource_picture.width = image.width
        }.addOnFailureListener {
            // println("Image retrieval failed")
        }

        viewManager = LinearLayoutManager(context)
        viewAdapter = RecyclerViewAdapter(ArrayList())
        comments_recycler?.apply {
            this.layoutManager = viewManager
            this.adapter = viewAdapter
        }

        /**
         * Comments, show in RecyclerView and add new comments
         */
        val commentList = ArrayList<CommentItem>()
        resourceViewModel.resourcesList.observe(this, Observer {
            for (item in it) {
                if (item.id == resID) {
                    for (comment in item.comments) {
                        if (comment != null) {
                            // split the comment item that is set as a string
                            val newComment = CommentItem()
                            val separated = comment.split("||||").toTypedArray()
                            newComment.date = "Posted on: " + separated[0]
                            newComment.comment = separated[1]
                            commentList.add(0, newComment)
                        }
                    }
                    viewAdapter.commentList = commentList
                    viewAdapter.notifyDataSetChanged()
                    break
                }
            }
        })

        /**
         * Get User Information to update UserInfo on firebase when comments added
         */
        var userItem = UserInformation()
        resourceViewModel.userList.observe(this, Observer {
            for (item in it) {
                if (item.authID == FirebaseAuth.getInstance().currentUser!!.uid) {
                    userItem = item
                    break
                }
            }
        })

        /**
         * Only show the submit button after text has been added to the comment section
         */
        add_comment_btn.visibility = View.GONE
        add_coment_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
                add_comment_btn.visibility = View.GONE
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (add_coment_text.text.toString() == "") {
                    add_comment_btn.visibility = View.GONE
                } else {
                    add_comment_btn.visibility = View.VISIBLE
                }
            }
        })

        /**
         * set comment item as a string of format "date||||comment"
         * At the time of creation this is the simplest method for storing a consistently
         * changing array list of strings in Firebase
         */
        add_comment_btn.setOnClickListener {
            if (userItem.reportUserAddComment) {
                Toast.makeText(
                    context,
                    "You have been flagged and are no longer allowed to add comments. For" +
                            "more information please email jisler@clarku.edu",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val updatedCommentList = ArrayList<CommentItem>()
                var updatedResourceItem = SustainableResource()
                resourceViewModel.resourcesList.observe(this, Observer {
                    for (item in it) {
                        if (item.id == resID) {
                            updatedResourceItem = item
                            for (comment in item.comments) {
                                if (comment != null) {
                                    // split the comment item that is set as a string
                                    val newComment = CommentItem()
                                    val separated = comment.split("||||").toTypedArray()
                                    newComment.date = "Posted on: " + separated[0]
                                    newComment.comment = separated[1]
                                    updatedCommentList.add(0, newComment)
                                }
                            }
                            viewAdapter.commentList = updatedCommentList
                            viewAdapter.notifyDataSetChanged()
                            break
                        }
                    }
                })
                var updatedUserItem = UserInformation()
                resourceViewModel.userList.observe(this, Observer {
                    for (item in it) {
                        if (item.authID == FirebaseAuth.getInstance().currentUser!!.uid) {
                            updatedUserItem = item
                            break
                        }
                    }
                })

                val date = SimpleDateFormat("MM-dd-yyyy").format(Calendar.getInstance().time)
                updatedResourceItem.comments.add(date + "||||" + add_coment_text.text.toString() + "||||" + updatedUserItem.authID)
                resourceViewModel.updateComments(resCategory!!, resID!!, updatedResourceItem.comments)
                viewAdapter.notifyDataSetChanged()

                updatedUserItem.comments.add(date + "||||" + add_coment_text.text.toString() + "||||" + updatedResourceItem.id)
                resourceViewModel.updateUserComments(updatedUserItem)
                add_coment_text.text = null
            }
        }
    }

    /**
     * RecyclerView of comments
     */
    class RecyclerViewAdapter(
        var commentList: ArrayList<CommentItem>
    ) : RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.comments_recycler, parent, false)
            return RecyclerViewHolder(v)
        }

        override fun getItemCount(): Int {
            return commentList.size
        }

        override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
            holder.bind(commentList[position])
        }

        class RecyclerViewHolder(val viewItem: View) : RecyclerView.ViewHolder(viewItem) {
            fun bind(commentItem: CommentItem) {
                viewItem.findViewById<TextView>(R.id.recycler_comment_date).text = commentItem.date
                viewItem.findViewById<TextView>(R.id.recycler_comment_text).text =
                    commentItem.comment
            }
        }
    }
}
