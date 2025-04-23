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
import kotlin.text.get
import kotlin.toString

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
    private lateinit var etPhone: EditText
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
        etPhone = findViewById(R.id.etPhone)
        etDepartment = findViewById(R.id.etDepartment)
        etYear = findViewById(R.id.etYear)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnSignOut = findViewById(R.id.btnSignOut)

        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"

        etDepartment.isEnabled = false
        etYear.isEnabled = false

        // Make email non-editable
        etEmail.isEnabled = false

        // Load user profile
        loadUserProfile()

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

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
                        etDepartment.setText(documentSnapshot.getString("branch"))
                        etYear.setText(documentSnapshot.getLong("academicYear")?.toString() ?: "")
                        // Add phone number retrieval
                        etPhone.setText(documentSnapshot.getString("phone") ?: "")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
//            firestore.collection("users")
//                .document(currentUser.uid)
//                .get()
//                .addOnSuccessListener { documentSnapshot ->
//                    if (documentSnapshot.exists()) {
//                        etDepartment.setText(documentSnapshot.getString("department"))
//                        etYear.setText(documentSnapshot.getString("year"))
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(this, "Error loading profile data: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
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
        val phone = etPhone.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Name is required"
            return
        }

        // Validate phone number (basic validation)
        if (phone.isNotEmpty() && !isValidPhoneNumber(phone)) {
            etPhone.error = "Please enter a valid phone number"
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
                        updateProfile(currentUser.uid, name, downloadUri, phone)
                    } else {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // No new image, just update profile data
            updateProfile(currentUser.uid, name, null, phone)
        }
    }

    private fun updateProfile(userId: String, name: String, photoUri: Uri?, phone: String) {
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
                        "phone" to phone
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

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic validation - adjust as needed for your requirements
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", null) // Set to null to override default behavior
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the click listener to prevent automatic dismissal on validation failure
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (currentPassword.isEmpty()) {
                etCurrentPassword.error = "Please enter current password"
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                etNewPassword.error = "Please enter new password"
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (confirmPassword != newPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Re-authenticate before changing password
            val user = auth.currentUser
            if (user?.email != null) {
                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { passwordTask ->
                                    if (passwordTask.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Failed to update password: ${passwordTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Current password is incorrect",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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