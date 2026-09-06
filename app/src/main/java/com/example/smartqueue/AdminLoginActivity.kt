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

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val email = findViewById<EditText>(R.id.etAdminEmail)
        val password = findViewById<EditText>(R.id.etAdminPassword)
        val loginButton = findViewById<Button>(R.id.btnAdminLogin)
        val backButton = findViewById<TextView>(R.id.tvBackToStaffLogin)

        loginButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()

            if (emailText.isEmpty()) {
                email.error = "Enter admin email"
                return@setOnClickListener
            }
            if (passwordText.isEmpty()) {
                password.error = "Enter password"
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            loginButton.text = "Checking..."

            auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this) { task ->
                    if (!task.isSuccessful) {
                        loginButton.isEnabled = true
                        loginButton.text = "Admin Login"
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        reject(loginButton, "Unable to verify admin account.")
                        return@addOnCompleteListener
                    }

                    db.collection("admins").document(uid).get()
                        .addOnSuccessListener { document ->
                            val isAdmin = document.exists() &&
                                    (document.getBoolean("enabled") ?: true) &&
                                    document.getString("role")?.lowercase() == "admin"

                            if (!isAdmin) {
                                reject(loginButton, "This account is not authorized for admin access.")
                                return@addOnSuccessListener
                            }

                            startActivity(Intent(this, AdminStaffActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { error ->
                            reject(loginButton, "Could not verify admin access: ${error.message}")
                        }
                }
        }

        backButton.setOnClickListener { finish() }
    }

    private fun reject(button: Button, message: String) {
        auth.signOut()
        button.isEnabled = true
        button.text = "Admin Login"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
