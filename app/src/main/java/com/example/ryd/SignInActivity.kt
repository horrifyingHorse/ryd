package com.example.ryd

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val tvTitle = findViewById<TextView>(R.id.tvAppTitle)
        val paint = tvTitle.paint
        val width = paint.measureText(tvTitle.text.toString())

        val textShader = LinearGradient(
            0f, 0f, width, tvTitle.textSize,
            intArrayOf(
                Color.parseColor("#7270B3"), // start color
                Color.parseColor("#E8E7FF")  // end color
            ),
            null,
            Shader.TileMode.CLAMP
        )

        tvTitle.paint.shader = textShader

        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val btLogIn = findViewById<Button>(R.id.btnLogin)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        auth = FirebaseAuth.getInstance()

        btLogIn.setOnClickListener {
            login()
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun login() {
        val email = etEmail.text.toString()
        val pass = etPassword.text.toString()
        val cbHuman = findViewById<CheckBox>(R.id.cbHuman)

        if (!cbHuman.isChecked) {
            Toast.makeText(this, "Please verify that you are not a robot", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Input validation
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                // progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                            etEmail.error = "Email not registered"
                            etEmail.requestFocus()
                        }

                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            etPassword.error = "Incorrect password"
                            etPassword.requestFocus()
                        }

                        else -> {
                            Toast.makeText(
                                this,
                                "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }
}