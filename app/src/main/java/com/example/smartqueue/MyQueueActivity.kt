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
    private var queueListener: ListenerRegistration? = null

    private var myTokenValue = 0L
    private var currentServingValue = 0L
    private var serviceTime = 5L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_queue)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

        tvRestaurantName.text = "Restaurant Queue"

        val currentUser = auth.currentUser

        if (currentUser == null) {

            showNoQueue(
                tvYourToken,
                tvNowServing,
                tvPeopleAhead,
                tvEstimatedWait,
                tvQueueStatus,
                tvMessage,
                btnCancelQueue
            )

            return
        }

        val userId = currentUser.uid

        /*
         * Listen to the user's Restaurant ticket.
         */
        ticketListener =
            db.collection("tickets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("service", "Restaurant")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        tvMessage.text =
                            "Could not load queue.\n${error.message}"

                        return@addSnapshotListener
                    }

                    if (snapshot == null ||
                        snapshot.isEmpty
                    ) {

                        showNoQueue(
                            tvYourToken,
                            tvNowServing,
                            tvPeopleAhead,
                            tvEstimatedWait,
                            tvQueueStatus,
                            tvMessage,
                            btnCancelQueue
                        )

                        return@addSnapshotListener
                    }

                    /*
                     * Keep only active tickets.
                     */
                    val activeTickets =
                        snapshot.documents
                            .filter { document ->

                                val status =
                                    document.getString("status")
                                        ?: ""

                                status == "WAITING" ||
                                        status == "CALLED" ||
                                        status == "SERVING"
                            }
                            .sortedByDescending { document ->

                                document.getLong("joinedAt")
                                    ?: 0L
                            }

                    if (activeTickets.isEmpty()) {

                        showNoQueue(
                            tvYourToken,
                            tvNowServing,
                            tvPeopleAhead,
                            tvEstimatedWait,
                            tvQueueStatus,
                            tvMessage,
                            btnCancelQueue
                        )

                        return@addSnapshotListener
                    }

                    val ticket =
                        activeTickets.first()

                    val tokenNumber =
                        ticket.getString("tokenNumber")
                            ?: "R00"

                    myTokenValue =
                        ticket.getLong("tokenNumberValue")
                            ?: 0L

                    val status =
                        ticket.getString("status")
                            ?: "WAITING"

                    /*
                     * Show user's token.
                     */
                    tvYourToken.text =
                        tokenNumber

                    /*
                     * Show current status.
                     */
                    when (status) {

                        "WAITING" -> {

                            tvQueueStatus.text =
                                "Waiting in Queue"

                            tvQueueStatus.setTextColor(
                                getColor(
                                    R.color.queue_waiting
                                )
                            )

                            tvMessage.text =
                                "Please wait for your token to be called."

                            btnCancelQueue.isEnabled =
                                true
                        }

                        "CALLED" -> {

                            tvQueueStatus.text =
                                "Your Token Has Been Called!"

                            tvQueueStatus.setTextColor(
                                getColor(
                                    R.color.queue_serving
                                )
                            )

                            tvMessage.text =
                                "Your token has been called. Please proceed to the restaurant counter."

                            btnCancelQueue.isEnabled =
                                false
                        }

                        "SERVING" -> {

                            tvQueueStatus.text =
                                "You Are Being Served"

                            tvQueueStatus.setTextColor(
                                getColor(
                                    R.color.queue_serving
                                )
                            )

                            tvMessage.text =
                                "Your restaurant service is currently in progress."

                            btnCancelQueue.isEnabled =
                                false
                        }
                    }

                    /*
                     * Cancel button.
                     */
                    btnCancelQueue.setOnClickListener {

                        if (status == "WAITING") {

                            cancelTicket(
                                ticket.id,
                                tvMessage,
                                btnCancelQueue
                            )
                        }
                    }

                    /*
                     * Calculate current queue position.
                     */
                    updateQueuePosition(
                        tvPeopleAhead,
                        tvEstimatedWait
                    )
                }

        /*
         * Listen to Restaurant queue in real time.
         */
        queueListener =
            db.collection("queues")
                .document("RESTAURANT")
                .addSnapshotListener { document, error ->

                    if (error != null) {
                        return@addSnapshotListener
                    }

                    if (document == null ||
                        !document.exists()
                    ) {

                        currentServingValue = 0L
                        serviceTime = 5L

                        tvNowServing.text = "R00"

                        updateQueuePosition(
                            tvPeopleAhead,
                            tvEstimatedWait
                        )

                        return@addSnapshotListener
                    }

                    currentServingValue =
                        document.getLong(
                            "currentlyServing"
                        ) ?: 0L

                    serviceTime =
                        document.getLong(
                            "estimatedServiceTime"
                        ) ?: 5L

                    /*
                     * Display current serving token.
                     */
                    tvNowServing.text =
                        if (currentServingValue > 0) {

                            "R" +
                                    String.format(
                                        "%02d",
                                        currentServingValue
                                    )

                        } else {

                            "R00"
                        }

                    updateQueuePosition(
                        tvPeopleAhead,
                        tvEstimatedWait
                    )
                }
    }

    private fun updateQueuePosition(
        tvPeopleAhead: TextView,
        tvEstimatedWait: TextView
    ) {

        if (myTokenValue <= 0L) {

            tvPeopleAhead.text = "0"

            tvEstimatedWait.text =
                "Estimated wait: 0 minutes"

            return
        }

        /*
         * If our token is already serving,
         * nobody is ahead.
         */
        if (myTokenValue <= currentServingValue) {

            tvPeopleAhead.text = "0"

            tvEstimatedWait.text =
                "Estimated wait: 0 minutes"

            return
        }

        db.collection("tickets")
            .whereEqualTo(
                "queueId",
                "RESTAURANT"
            )
            .whereEqualTo(
                "status",
                "WAITING"
            )
            .get()
            .addOnSuccessListener { tickets ->

                var peopleAhead = 0

                for (document in tickets.documents) {

                    val otherToken =
                        document.getLong(
                            "tokenNumberValue"
                        ) ?: 0L

                    /*
                     * Only count real waiting customers
                     * between current serving and our token.
                     */
                    if (
                        otherToken > currentServingValue &&
                        otherToken < myTokenValue
                    ) {

                        peopleAhead++
                    }
                }

                tvPeopleAhead.text =
                    peopleAhead.toString()

                val estimatedWait =
                    peopleAhead * serviceTime

                tvEstimatedWait.text =
                    "Estimated wait: " +
                            "$estimatedWait minutes"
            }
            .addOnFailureListener {

                tvPeopleAhead.text = "0"

                tvEstimatedWait.text =
                    "Estimated wait unavailable"
            }
    }

    private fun showNoQueue(
        tvYourToken: TextView,
        tvNowServing: TextView,
        tvPeopleAhead: TextView,
        tvEstimatedWait: TextView,
        tvQueueStatus: TextView,
        tvMessage: TextView,
        btnCancelQueue: Button
    ) {

        myTokenValue = 0L

        tvYourToken.text = "--"

        tvNowServing.text = "R00"

        tvPeopleAhead.text = "0"

        tvEstimatedWait.text =
            "Estimated wait: 0 minutes"

        tvQueueStatus.text =
            "No Active Queue"

        tvQueueStatus.setTextColor(
            getColor(
                R.color.text_secondary
            )
        )

        tvMessage.text =
            "You are not currently in the Restaurant queue."

        btnCancelQueue.isEnabled = false
        btnCancelQueue.setOnClickListener(null)
    }

    private fun cancelTicket(
        ticketId: String,
        messageText: TextView,
        button: Button
    ) {

        button.isEnabled = false

        db.collection("tickets")
            .document(ticketId)
            .update(
                "status",
                "CANCELLED"
            )
            .addOnSuccessListener {

                messageText.text =
                    "Restaurant queue cancelled successfully."

                Toast.makeText(
                    this,
                    "Queue cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->

                messageText.text =
                    "Could not cancel queue.\n${error.message}"

                button.isEnabled = true
            }
    }

    override fun onDestroy() {

        ticketListener?.remove()
        queueListener?.remove()

        super.onDestroy()
    }
}