package com.example.ryd

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
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
import kotlinx.coroutines.launch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
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
        val etFullName = findViewById<EditText>(R.id.etName)
        etEmail = findViewById<EditText>(R.id.etEmail)
        val etUserName = findViewById<EditText>(R.id.etUsername)
        etPassword = findViewById<EditText>(R.id.etPassword)
        etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val cbTnC = findViewById<CheckBox>(R.id.cbAgreeToTerms)
        val btSignUp = findViewById<Button>(R.id.btnSignUp)

        btSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val emailVal = etEmail.text.toString().trim()
            val username = etUserName.text.toString().trim()
            val passwordVal = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            val agreed = cbTnC.isChecked

            // Email regex that accepts things like aight@gmai.com
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$")

            var isValid = true

            if (fullName.isEmpty()) {
                etFullName.error = "Full name required"
                isValid = false
            }

            if (username.isEmpty()) {
                etUserName.error = "Username required"
                isValid = false
            } else if (username.contains("\\s".toRegex())) {
                etUserName.error = "Username cannot contain spaces"
                isValid = false
            }

            if (!emailVal.matches(emailRegex)) {
                etEmail.error = "Enter a valid email"
                isValid = false
            }

            if (passwordVal.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                isValid = false
            } else {
                if (!passwordVal.matches(Regex(".*[A-Z].*"))) {
                    etPassword.error = "Password must contain an uppercase letter"
                    isValid = false
                }
                if (!passwordVal.matches(Regex(".*\\d.*"))) {
                    etPassword.error = "Password must contain a digit"
                    isValid = false
                }
            }

            if (passwordVal != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                isValid = false
            }

            if (!agreed) {
                cbTnC.error = "You must agree to the terms"
                isValid = false
            } else {
                cbTnC.error = null // clear any previous error
            }

            if (isValid) {
                Toast.makeText(this, "All inputs valid, proceeding…", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    try{
                        signUpUser()
                    } catch (e: Exception) {
                        Toast.makeText(this@SignUpActivity, "Something went wrong: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        tvSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signUpUser() {
        val email = etEmail.text.toString()
        val pass = etPassword.text.toString()

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Toast.makeText(this, "Successfully Singed Up", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Singed Up Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}