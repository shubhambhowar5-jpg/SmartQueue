package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        val restaurant =
            findViewById<Button>(R.id.btnRestaurant)

        val beauty =
            findViewById<Button>(R.id.btnBeauty)

        val dental =
            findViewById<Button>(R.id.btnDental)

        val supermarket =
            findViewById<Button>(R.id.btnSupermarket)

        val airport =
            findViewById<Button>(R.id.btnAirport)

        val myQueue =
            findViewById<Button>(R.id.btnMyQueue)

        val logout =
            findViewById<Button>(R.id.btnLogout)

        restaurant.setOnClickListener {

            val intent =
                Intent(this, QueueActivity::class.java)

            intent.putExtra(
                "SERVICE_NAME",
                "Restaurant"
            )

            startActivity(intent)
        }

        /*
         * The other four sections belong
         * to your group members.
         *
         * We are not changing their modules here.
         */

        beauty.setOnClickListener {
            // Other team member's module
        }

        dental.setOnClickListener {
            // Other team member's module
        }

        supermarket.setOnClickListener {
            // Other team member's module
        }

        airport.setOnClickListener {
            // Other team member's module
        }

        // Restaurant user's My Queue
        myQueue.setOnClickListener {

            val intent =
                Intent(this, MyQueueActivity::class.java)

            startActivity(intent)
        }

        // Logout
        logout.setOnClickListener {

            auth.signOut()

            val intent =
                Intent(this, LoginActivity::class.java)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }
    }
}