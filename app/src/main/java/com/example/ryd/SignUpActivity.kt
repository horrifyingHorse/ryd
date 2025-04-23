package com.example.ryd

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etFullName: EditText
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        val tvTitle = findViewById<TextView>(R.id.tvAppTitle)
        val paint = tvTitle.paint
        val width = paint.measureText(tvTitle.text.toString())

        val textShader = LinearGradient(
            0f, 0f, width, tvTitle.textSize,
            intArrayOf(
                Color.parseColor("#7270B3"),
                Color.parseColor("#E8E7FF")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        tvTitle.paint.shader = textShader

        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        etFullName = findViewById<EditText>(R.id.etName)
        etEmail = findViewById<EditText>(R.id.etEmail)
        etPassword = findViewById<EditText>(R.id.etPassword)
        etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val cbTnC = findViewById<CheckBox>(R.id.cbAgreeToTerms)
        val btSignUp = findViewById<Button>(R.id.btnSignUp)

        btSignUp.setOnClickListener {
            lifecycleScope.launch {
                try {
                    signUpUser()
                } catch (e: Exception) {
                    Toast.makeText(this@SignUpActivity, "Something went wrong: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateSignupFields(): Boolean {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val agreed = findViewById<CheckBox>(R.id.cbAgreeToTerms).isChecked

        var isValid = true

        if (fullName.isEmpty()) {
            etFullName.error = "Full name required"
            isValid = false
        }

        // Institute email validation
        if (!validateInstituteEmail(email)) {
            isValid = false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            if (!password.matches(Regex(".*[A-Z].*"))) {
                etPassword.error = "Password must contain an uppercase letter"
                isValid = false
            }
            if (!password.matches(Regex(".*\\d.*"))) {
                etPassword.error = "Password must contain a digit"
                isValid = false
            }
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (!agreed) {
            findViewById<CheckBox>(R.id.cbAgreeToTerms).error = "You must agree to the terms"
            isValid = false
        }

        return isValid
    }

    private fun validateInstituteEmail(email: String): Boolean {
        // Check for institute domain
        if (!email.endsWith("@iiitn.ac.in")) {
            etEmail.error = "Email must use the @iiitn.ac.in domain"
            return false
        }

        // Extract the part before @
        val emailPrefix = email.substringBefore('@')

        // Check format: btyybbbnnn
        val instituteEmailRegex = "^bt[0-9]{2}(cse|csh|ece)[0-9]{3}$".toRegex()

        if (!instituteEmailRegex.matches(emailPrefix)) {
            etEmail.error = "Invalid format. Must be btyybbbnnn@iiitn.ac.in (e.g., bt23cse028@iiitn.ac.in)"
            return false
        }

        return true
    }

    private fun extractStudentInfo(email: String): Map<String, Any> {
        val emailPrefix = email.substringBefore('@')
        val yearCode = emailPrefix.substring(2, 4).toInt()
        val branchCode = emailPrefix.substring(4, 7)
        val rollNumber = emailPrefix.substring(7, 10).toInt()

        // Get current year and month
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based

        // Calculate student's academic year
        val admissionYear = 2000 + yearCode
        val currentAcademicYear = if (currentMonth >= 8) {
            currentYear - admissionYear + 1
        } else {
            currentYear - admissionYear
        }

        // Map branch code to full name
        val branch = when (branchCode) {
            "cse" -> "Computer Science and Engineering"
            "csh" -> "Computer Science (Gaming Technology)"
            "ece" -> "Electronics and Communications Engineering"
            else -> "Unknown Branch"
        }

        return mapOf(
            "admissionYear" to admissionYear,
            "branch" to branch,
            "branchCode" to branchCode,
            "academicYear" to currentAcademicYear,
            "rollNumber" to rollNumber
        )
    }

    private fun signUpUser() {
        val email = etEmail.text.toString()
        val pass = etPassword.text.toString()
        val fullName = etFullName.text.toString()

        if (!validateSignupFields()) {
            return
        }

        val studentInfo = extractStudentInfo(email)

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                val user = auth.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                    // Store additional user data in Firestore
                    val userData = hashMapOf(
                        "name" to fullName,
                        "email" to email,
                        "admissionYear" to studentInfo["admissionYear"],
                        "branch" to studentInfo["branch"],
                        "branchCode" to studentInfo["branchCode"],
                        "academicYear" to studentInfo["academicYear"],
                        "rollNumber" to studentInfo["rollNumber"],
                        "createdAt" to System.currentTimeMillis()
                    )

                    val db = FirebaseFirestore.getInstance()
                    user.uid.let { uid ->
                        db.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Successfully Signed Up", Toast.LENGTH_SHORT)
                                    .show()

                                // Navigate to home activity
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to save user data: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            } else {
                Toast.makeText(this, "Sign Up Failed: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}