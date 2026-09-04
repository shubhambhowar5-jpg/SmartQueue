package com.example.smartqueue

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyQueueActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var ticketListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_queue)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Connect UI
        val tvRestaurantName =
            findViewById<TextView>(R.id.tvRestaurantName)

        val tvYourToken =
            findViewById<TextView>(R.id.tvYourToken)

        val tvNowServing =
            findViewById<TextView>(R.id.tvNowServing)

        val tvPeopleAhead =
            findViewById<TextView>(R.id.tvPeopleAhead)

        val tvEstimatedWait =
            findViewById<TextView>(R.id.tvEstimatedWait)

        val tvQueueStatus =
            findViewById<TextView>(R.id.tvQueueStatus)

        val tvMessage =
            findViewById<TextView>(R.id.tvMessage)

        val btnCancelQueue =
            findViewById<Button>(R.id.btnCancelQueue)

        // Display Restaurant
        tvRestaurantName.text = "Restaurant Queue"

        // Check logged-in user
        val currentUser = auth.currentUser

        if (currentUser == null) {

            tvMessage.text =
                "You are not logged in."

            btnCancelQueue.isEnabled = false

            return
        }

        val userId = currentUser.uid

        /*
         * Listen to the user's latest Restaurant ticket.
         *
         * We only look for:
         * service = Restaurant
         * userId = current user
         */
        ticketListener = db.collection("tickets")
            .whereEqualTo("userId", userId)
            .whereEqualTo("service", "Restaurant")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    tvMessage.text =
                        "Could not load queue.\n${error.message}"

                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {

                    tvYourToken.text = "--"
                    tvNowServing.text = "--"
                    tvPeopleAhead.text = "0"
                    tvEstimatedWait.text =
                        "Estimated wait: 0 minutes"

                    tvQueueStatus.text = "No Active Queue"

                    tvMessage.text =
                        "You are not currently in a Restaurant queue."

                    btnCancelQueue.isEnabled = false

                    return@addSnapshotListener
                }

                // Find the latest non-cancelled ticket
                val activeTickets = snapshot.documents
                    .filter { document ->

                        val status =
                            document.getString("status")

                        status != "CANCELLED" &&
                                status != "COMPLETED"
                    }
                    .sortedByDescending { document ->

                        document.getLong("joinedAt") ?: 0L
                    }

                if (activeTickets.isEmpty()) {

                    tvYourToken.text = "--"
                    tvNowServing.text = "--"
                    tvPeopleAhead.text = "0"
                    tvEstimatedWait.text =
                        "Estimated wait: 0 minutes"

                    tvQueueStatus.text = "No Active Queue"

                    tvMessage.text =
                        "You are not currently in a Restaurant queue."

                    btnCancelQueue.isEnabled = false

                    return@addSnapshotListener
                }

                val ticket = activeTickets.first()

                // Get ticket values
                val tokenNumber =
                    ticket.getString("tokenNumber") ?: "R-00"

                val status =
                    ticket.getString("status") ?: "WAITING"

                val tokenValue =
                    ticket.getLong("tokenNumberValue") ?: 0L

                // Display customer's token
                tvYourToken.text = tokenNumber

                // Get Restaurant queue document
                val queueRef = db.collection("queues")
                    .document("RESTAURANT")

                queueRef.get()
                    .addOnSuccessListener { queueDocument ->

                        val currentServing =
                            queueDocument.getLong("currentlyServing")
                                ?: 0L

                        val currentServingToken =
                            if (currentServing > 0) {
                                "R-" +
                                        String.format(
                                            "%02d",
                                            currentServing
                                        )
                            } else {
                                "R-00"
                            }

                        tvNowServing.text =
                            currentServingToken

                        // Calculate people ahead
                        val peopleAhead =
                            if (tokenValue > currentServing) {
                                tokenValue - currentServing - 1
                            } else {
                                0
                            }

                        tvPeopleAhead.text =
                            peopleAhead.toString()

                        // Restaurant estimated service time
                        val estimatedMinutes =
                            peopleAhead * 5

                        tvEstimatedWait.text =
                            "Estimated wait: " +
                                    "$estimatedMinutes minutes"
                    }

                // Update status text
                tvQueueStatus.text =
                    when (status) {

                        "WAITING" ->
                            "Waiting in Queue"

                        "CALLED" ->
                            "Your Token Has Been Called!"

                        "SERVING" ->
                            "You Are Being Served"

                        else ->
                            status
                    }

                // Status colors
                when (status) {

                    "CALLED",
                    "SERVING" -> {
                        tvQueueStatus.setTextColor(
                            getColor(R.color.queue_serving)
                        )
                    }

                    "WAITING" -> {
                        tvQueueStatus.setTextColor(
                            getColor(R.color.queue_waiting)
                        )
                    }

                    else -> {
                        tvQueueStatus.setTextColor(
                            getColor(R.color.text_secondary)
                        )
                    }
                }

                tvMessage.text =
                    "Queue information updates automatically."

                btnCancelQueue.isEnabled = true

                // Cancel queue
                btnCancelQueue.setOnClickListener {

                    cancelTicket(
                        ticket.id,
                        tvMessage,
                        btnCancelQueue
                    )
                }
            }
    }

    private fun cancelTicket(
        ticketId: String,
        messageText: TextView,
        button: Button
    ) {

        button.isEnabled = false

        db.collection("tickets")
            .document(ticketId)
            .update("status", "CANCELLED")
            .addOnSuccessListener {

                messageText.text =
                    "Restaurant queue cancelled successfully."

                Toast.makeText(
                    this,
                    "Queue cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                button.isEnabled = false
            }
            .addOnFailureListener { error ->

                messageText.text =
                    "Could not cancel queue.\n${error.message}"

                button.isEnabled = true
            }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop Firestore listener
        ticketListener?.remove()
    }
}