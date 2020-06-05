package com.example.clarksustainableresources

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.opencensus.resource.Resource
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    var signOutCode: Int? = 0
    lateinit var resourceViewModel: ResourceViewModel

    /**
     * Configure google sign in to request user data, create GoogleSignInClient, get FirebaseAuth
     * object
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // ResourceViewModel
        resourceViewModel = ViewModelProviders.of(this).get(ResourceViewModel::class.java)


        // retrieve bundle from main activity
        val extras = intent.extras
        signOutCode = extras?.getInt("signed out")

        // Button listeners
        findViewById<SignInButton>(R.id.signInButton).setOnClickListener(this)

        // configure google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()


        // get google sign in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // get instance of firebase auth object
        auth = FirebaseAuth.getInstance()

        // if user signed out
        if(signOutCode == 1){
            signOut()
        }
    }

    /**
     * ActivityResult for when user signs in
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignIn(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                // ...
            }
        }
    }

    /**
     * Authenticate user with Firebase - get ID token from google, exchange for Firebase
     * credential, and use credential to authenticate
     */
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Snackbar.make(main_layout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    /**
     * Update UI depending on user login status.
     * If user is logged in, start MainActivity
     * else, show Google sign-in button
     */
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // clear UI
            signInButton.visibility = View.GONE
            status.visibility = View.GONE
            findViewById<TextView>(R.id.titleText).visibility = View.GONE

            // add user to firebase
            addUserInfo()

            // start application
            val startIntent = Intent(this, MainActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(startIntent)
        } else {
            val status = findViewById<TextView>(R.id.status)
            status.setText(R.string.signed_out)

            signInButton.visibility = View.VISIBLE
        }
    }

    /**
     * Creates a new user and adds it into the system unless the
     * user exists in the system already
     */
    private fun addUserInfo() {
        var newUser = UserInformation()
        newUser.authEmail = FirebaseAuth.getInstance().currentUser!!.email!!
        newUser.authID = FirebaseAuth.getInstance().currentUser!!.uid
        resourceViewModel.uploadUserData(newUser)
    }

    /**
     * Signin user using Google acct
     */
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * Signout user
     */
    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun revokeAccess() {
        // Firebase sign out
        auth.signOut()

        // Google revoke access
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    /**
     * check if user is signed in already
     */
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    /**
     * User click
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.signInButton -> signIn()
        }
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}