package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_staff_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val email = findViewById<EditText>(R.id.etStaffEmail)
        val password = findViewById<EditText>(R.id.etStaffPassword)
        val loginButton = findViewById<Button>(R.id.btnStaffLogin)
        val backButton = findViewById<TextView>(R.id.tvBackToCustomerLogin)

        loginButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty()) {
                email.error = "Enter staff email"
                return@setOnClickListener
            }

            if (passwordText.isEmpty()) {
                password.error = "Enter password"
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            loginButton.text = "Checking..."

            auth.signInWithEmailAndPassword(
                emailText,
                passwordText
            ).addOnCompleteListener(this) { task ->

                if (!task.isSuccessful) {
                    loginButton.isEnabled = true
                    loginButton.text = "Staff Login"

                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid

                if (uid == null) {
                    auth.signOut()
                    loginButton.isEnabled = true
                    loginButton.text = "Staff Login"
                    Toast.makeText(
                        this,
                        "Unable to verify staff account.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                db.collection("staff")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        val isStaff =
                            document.exists() &&
                                    (document.getBoolean("enabled") ?: true) &&
                                    document.getString("role")?.lowercase() == "staff"

                        if (isStaff) {
                            Toast.makeText(
                                this,
                                "Staff login successful!",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(
                                this,
                                RestaurantStaffActivity::class.java
                            )

                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                            startActivity(intent)
                            finish()
                        } else {
                            auth.signOut()
                            loginButton.isEnabled = true
                            loginButton.text = "Staff Login"

                            Toast.makeText(
                                this,
                                "This account is not authorized for staff access.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .addOnFailureListener { error ->
                        auth.signOut()
                        loginButton.isEnabled = true
                        loginButton.text = "Staff Login"

                        Toast.makeText(
                            this,
                            "Could not verify staff access: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
