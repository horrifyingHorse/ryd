package com.example.ryd

import android.app.Activity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.UUID

class ProfileActivity : AppCompatActivity() {
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private lateinit var toolbar: Toolbar
    private lateinit var ivProfilePic: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etYear: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnSignOut: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        ivProfilePic = findViewById(R.id.ivProfilePic)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etDepartment = findViewById(R.id.etDepartment)
        etYear = findViewById(R.id.etYear)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnSignOut = findViewById(R.id.btnSignOut)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"

        // Make email non-editable
        etEmail.isEnabled = false

        // Load user profile
        loadUserProfile()

        // Setup click listeners
        btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        btnSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        btnSignOut.setOnClickListener {
            signOut()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            etEmail.setText(currentUser.email)
            etName.setText(currentUser.displayName)

            // Load profile pic if available
            if (currentUser.photoUrl != null) {
                Picasso.get()
                    .load(currentUser.photoUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(ivProfilePic)
            }

            // Load additional user data from Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        etDepartment.setText(documentSnapshot.getString("department"))
                        etYear.setText(documentSnapshot.getString("year"))
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            ivProfilePic.setImageURI(selectedImageUri)
        }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return
        val name = etName.text.toString().trim()
        val department = etDepartment.text.toString().trim()
        val year = etYear.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Name is required"
            return
        }

        // Upload image if new one is selected
        if (selectedImageUri != null) {
            val imageRef = storage.reference.child("profile_images/${UUID.randomUUID()}")

            imageRef.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        updateProfile(currentUser.uid, name, downloadUri, department, year)
                    } else {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // No new image, just update profile data
            updateProfile(currentUser.uid, name, null, department, year)
        }
    }

    private fun updateProfile(userId: String, name: String, photoUri: Uri?, department: String, year: String) {
        // Update in Firebase Auth
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)

        if (photoUri != null) {
            profileUpdates.setPhotoUri(photoUri)
        }

        auth.currentUser?.updateProfile(profileUpdates.build())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Now update in Firestore
                    val userData = hashMapOf(
                        "name" to name,
                        "department" to department,
                        "year" to year
                    )

                    if (photoUri != null) {
                        userData["photoUrl"] = photoUri.toString()
                    }

                    firestore.collection("users")
                        .document(userId)
                        .update(userData as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}