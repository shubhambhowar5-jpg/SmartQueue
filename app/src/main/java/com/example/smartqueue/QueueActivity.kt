package com.example.smartqueue

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class QueueActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_queue)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get selected service
        val serviceName =
            intent.getStringExtra("SERVICE_NAME") ?: "Service"

        // Find views
        val tvServiceName =
            findViewById<TextView>(R.id.tvServiceName)

        val tvCurrentToken =
            findViewById<TextView>(R.id.tvCurrentToken)

        val tvPeopleWaiting =
            findViewById<TextView>(R.id.tvPeopleWaiting)

        val tvEstimatedWait =
            findViewById<TextView>(R.id.tvEstimatedWait)

        val tvResult =
            findViewById<TextView>(R.id.tvResult)

        val btnJoinQueue =
            findViewById<Button>(R.id.btnJoinQueue)

        // Display service name
        tvServiceName.text = serviceName

        // Temporary values
        tvCurrentToken.text = "00"
        tvPeopleWaiting.text = "0"
        tvEstimatedWait.text = "Estimated wait: 0 minutes"

        // Join queue
        btnJoinQueue.setOnClickListener {

            btnJoinQueue.isEnabled = false
            btnJoinQueue.text = "Joining..."

            tvResult.text = "Generating your token..."

            joinQueue(
                serviceName,
                tvResult,
                btnJoinQueue
            )
        }
    }

    private fun joinQueue(
        serviceName: String,
        resultText: TextView,
        button: Button
    ) {

        // Check logged-in user
        val currentUser = auth.currentUser

        if (currentUser == null) {

            resultText.text =
                "You are not logged in.\nPlease login again."

            button.isEnabled = true
            button.text = "Join Queue"

            return
        }

        val userId = currentUser.uid

        // Get queue document ID
        val queueId = getQueueId(serviceName)

        // Get token prefix
        val prefix = getTokenPrefix(serviceName)

        val queueRef =
            db.collection("queues").document(queueId)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(queueRef)

            val currentNumber =
                if (snapshot.exists()) {
                    snapshot.getLong("currentNumber") ?: 0L
                } else {
                    0L
                }

            val nextNumber = currentNumber + 1

            // Create or update queue counter
            transaction.set(
                queueRef,
                hashMapOf(
                    "service" to serviceName,
                    "currentNumber" to nextNumber
                )
            )

            nextNumber

        }.addOnSuccessListener { number ->

            val tokenNumber =
                "$prefix${String.format("%02d", number)}"

            val ticket = hashMapOf(
                "userId" to userId,
                "service" to serviceName,
                "queueId" to queueId,
                "tokenNumber" to tokenNumber,
                "tokenNumberValue" to number,
                "status" to "WAITING",
                "joinedAt" to System.currentTimeMillis()
            )

            db.collection("tickets")
                .add(ticket)
                .addOnSuccessListener {

                    resultText.text =
                        "Queue joined successfully!\n\n" +
                                "Your Token\n" +
                                "$tokenNumber"

                    button.text = "Queue Joined"
                    button.isEnabled = false

                    // Update queue information
                    updateQueueInformation(
                        serviceName,
                        number,
                        tvCurrentTokenId = R.id.tvCurrentToken,
                        tvPeopleWaitingId = R.id.tvPeopleWaiting,
                        tvEstimatedWaitId = R.id.tvEstimatedWait
                    )
                }
                .addOnFailureListener { error ->

                    resultText.text =
                        "Ticket could not be saved.\n\n" +
                                "Error: ${error.message}"

                    button.isEnabled = true
                    button.text = "Join Queue"
                }

        }.addOnFailureListener { error ->

            resultText.text =
                "Could not generate token.\n\n" +
                        "Error: ${error.message}"

            button.isEnabled = true
            button.text = "Join Queue"
        }
    }

    private fun updateQueueInformation(
        serviceName: String,
        currentNumber: Long,
        tvCurrentTokenId: Int,
        tvPeopleWaitingId: Int,
        tvEstimatedWaitId: Int
    ) {

        val tvCurrentToken =
            findViewById<TextView>(tvCurrentTokenId)

        val tvPeopleWaiting =
            findViewById<TextView>(tvPeopleWaitingId)

        val tvEstimatedWait =
            findViewById<TextView>(tvEstimatedWaitId)

        val prefix = getTokenPrefix(serviceName)

        tvCurrentToken.text =
            "$prefix${String.format("%02d", currentNumber)}"

        tvPeopleWaiting.text = "0"

        tvEstimatedWait.text =
            "Estimated wait: 0 minutes"
    }

    private fun getQueueId(serviceName: String): String {

        return when (serviceName) {

            "Restaurant" -> "RESTAURANT"

            "Beauty Parlor" -> "BEAUTY_PARLOR"

            "Dental Clinic" -> "DENTAL_CLINIC"

            "Supermarket" -> "SUPERMARKET"

            "Airport" -> "AIRPORT"

            else -> "GENERAL"
        }
    }

    private fun getTokenPrefix(serviceName: String): String {

        return when (serviceName) {

            "Restaurant" -> "R"

            "Beauty Parlor" -> "B"

            "Dental Clinic" -> "D"

            "Supermarket" -> "S"

            "Airport" -> "A"

            else -> "Q"
        }
    }
}