package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        val nameView = findViewById<TextView>(R.id.tvProfileName)
        val emailView = findViewById<TextView>(R.id.tvProfileEmail)
        val logoutButton = findViewById<Button>(R.id.btnProfileLogout)

        nameView.text = user.displayName?.takeIf { it.isNotBlank() } ?: "SmartQueue User"
        emailView.text = user.email ?: "No email available"

        logoutButton.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
