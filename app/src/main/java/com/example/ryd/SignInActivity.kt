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
            finish() // ends activity
        }
    }

    private fun login() {
        val email = etEmail.text.toString()
        val pass = etPassword.text.toString()
        // calling signInWithEmailAndPassword(email, pass)
        // function using Firebase auth object
        // On successful response Display a Toast
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                // Hide progress
                // progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    Toast.makeText(this, "Sign in successful",
                        Toast.LENGTH_SHORT).show()

                    // Navigate to home activity
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish() // Close this activity so user can't go back
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }

//        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
//            if (it.isSuccessful) {
//                Toast.makeText(this, "Successfully LoggedIn", Toast.LENGTH_SHORT).show()
//                val intent = Intent(this, HomeActivity::class.java)
//                startActivity(intent)
//                finish()
//            } else {
//                Toast.makeText(this, "Log In failed ", Toast.LENGTH_SHORT).show()
//            }
//        }
    }
}