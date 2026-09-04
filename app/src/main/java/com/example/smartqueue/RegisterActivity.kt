package com.example.smartqueue

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val name = findViewById<EditText>(R.id.etName)
        val email = findViewById<EditText>(R.id.etEmail)
        val phone = findViewById<EditText>(R.id.etPhone)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        registerButton.setOnClickListener {

            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim()
            val phoneText = phone.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            if (nameText.isEmpty()) {
                name.error = "Enter your name"
                return@setOnClickListener
            }

            if (emailText.isEmpty()) {
                email.error = "Enter your email"
                return@setOnClickListener
            }

            if (phoneText.isEmpty()) {
                phone.error = "Enter your phone number"
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                password.error = "Password must contain at least 6 characters"
                return@setOnClickListener
            }

            if (passwordText != confirmPasswordText) {
                confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(
                emailText,
                passwordText
            ).addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid

                    val user = hashMapOf(
                        "userId" to userId,
                        "name" to nameText,
                        "email" to emailText,
                        "phone" to phoneText,
                        "role" to "customer"
                    )

                    if (userId != null) {

                        db.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "Registration successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()
                            }
                            .addOnFailureListener {

                                Toast.makeText(
                                    this,
                                    "Account created, but profile could not be saved.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}