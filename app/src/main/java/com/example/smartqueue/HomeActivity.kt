package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        val welcome =
            findViewById<TextView>(R.id.tvWelcome)

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

        welcome.text = "Welcome!"

        restaurant.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    QueueActivity::class.java
                )
            )
        }

        /*
         * These four sections are reserved for
         * the other team members.
         */
        beauty.setOnClickListener {

        }

        dental.setOnClickListener {

        }

        supermarket.setOnClickListener {

        }

        airport.setOnClickListener {

        }

        myQueue.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MyQueueActivity::class.java
                )
            )
        }


        logout.setOnClickListener {

            auth.signOut()

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }
    }
}