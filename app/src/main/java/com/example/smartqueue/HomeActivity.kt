package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        supportActionBar?.title = "SmartQueue"

        navigationView.setNavigationItemSelectedListener(this)

        findViewById<ImageButton>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val welcome = findViewById<TextView>(R.id.tvWelcome)
        val restaurant = findViewById<Button>(R.id.btnRestaurant)
        val beauty = findViewById<Button>(R.id.btnBeauty)
        val dental = findViewById<Button>(R.id.btnDental)
        val supermarket = findViewById<Button>(R.id.btnSupermarket)
        val airport = findViewById<Button>(R.id.btnAirport)
        val myQueue = findViewById<Button>(R.id.btnMyQueue)
        val logout = findViewById<Button>(R.id.btnLogout)

        welcome.text = "Welcome!"

        restaurant.setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }

        // Reserved for the other team members.
        beauty.setOnClickListener { }
        dental.setOnClickListener { }
        supermarket.setOnClickListener { }
        airport.setOnClickListener { }

        myQueue.setOnClickListener {
            startActivity(Intent(this, MyQueueActivity::class.java))
        }

        logout.setOnClickListener {
            logoutUser()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        drawerLayout.openDrawer(GravityCompat.START)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            R.id.nav_my_queue -> {
                startActivity(Intent(this, MyQueueActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            R.id.nav_settings -> {
                Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            R.id.nav_help -> {
                Toast.makeText(this, "Help & Support coming soon", Toast.LENGTH_SHORT).show()
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            R.id.nav_logout -> {
                drawerLayout.closeDrawer(GravityCompat.START)
                logoutUser()
            }
        }

        return true
    }

    private fun logoutUser() {
        auth.signOut()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}
