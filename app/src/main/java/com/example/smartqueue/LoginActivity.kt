package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val email =
            findViewById<EditText>(R.id.etEmail)

        val password =
            findViewById<EditText>(R.id.etPassword)

        val loginButton =
            findViewById<Button>(R.id.btnLogin)

        val registerText =
            findViewById<TextView>(R.id.tvRegister)

        // =====================================================
        // TEMPORARY TEST LOGIN
        // Replace these with your test account credentials.
        // REMOVE THESE BEFORE SHARING/UPLOADING THE CODE.
        // =====================================================

        email.setText("testuser@gmail.com")
        password.setText("test1234")

        // =====================================================

        loginButton.setOnClickListener {

            val emailText =
                email.text.toString().trim()

            val passwordText =
                password.text.toString().trim()

            if (emailText.isEmpty()) {

                email.error =
                    "Enter your email"

                return@setOnClickListener
            }

            if (passwordText.isEmpty()) {

                password.error =
                    "Enter your password"

                return@setOnClickListener
            }

            loginButton.isEnabled = false
            loginButton.text = "Logging in..."

            auth.signInWithEmailAndPassword(
                emailText,
                passwordText
            ).addOnCompleteListener(this) { task ->

                loginButton.isEnabled = true
                loginButton.text = "LOGIN"

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent =
                        Intent(
                            this,
                            HomeActivity::class.java
                        )

                    startActivity(intent)
                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Login failed: " +
                                "${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        registerText.setOnClickListener {

            val intent =
                Intent(
                    this,
                    RegisterActivity::class.java
                )

            startActivity(intent)
        }
    }
}