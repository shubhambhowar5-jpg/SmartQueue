package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RestaurantStaffActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private var queueListener: ListenerRegistration? = null
    private var currentTicketId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restaurant_staff)

        db = FirebaseFirestore.getInstance()

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            redirectToLogin()
            return
        }

        db.collection("staff")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val isStaff =
                    document.exists() &&
                            (document.getBoolean("enabled") ?: true) &&
                            document.getString("role")?.lowercase() == "staff"

                if (!isStaff) {
                    auth.signOut()
                    redirectToLogin()
                    return@addOnSuccessListener
                }

                startStaffQueueListener()
            }
            .addOnFailureListener {
                auth.signOut()
                redirectToLogin()
            }
    }

    private fun startStaffQueueListener() {

        val tvNowServing =
            findViewById<TextView>(R.id.tvNowServing)

        val tvPeopleWaiting =
            findViewById<TextView>(R.id.tvPeopleWaiting)

        val tvCurrentCustomer =
            findViewById<TextView>(R.id.tvCurrentCustomer)

        val tvQueueStatus =
            findViewById<TextView>(R.id.tvQueueStatus)

        val tvStaffMessage =
            findViewById<TextView>(R.id.tvStaffMessage)

        val btnCallNext =
            findViewById<Button>(R.id.btnCallNext)

        val btnComplete =
            findViewById<Button>(R.id.btnComplete)

        val btnSkip =
            findViewById<Button>(R.id.btnSkip)

        // Listen to the Restaurant queue
        queueListener =
            db.collection("queues")
                .document("RESTAURANT")
                .addSnapshotListener { document, error ->

                    if (error != null) {

                        tvStaffMessage.text =
                            "Queue error:\n${error.message}"

                        return@addSnapshotListener
                    }

                    if (document == null ||
                        !document.exists()
                    ) {

                        tvNowServing.text = "R00"
                        tvPeopleWaiting.text = "0"
                        tvCurrentCustomer.text =
                            "No customer is currently being served."

                        tvQueueStatus.text =
                            "Queue Status: ACTIVE"

                        currentTicketId = null

                        btnComplete.isEnabled = false
                        btnSkip.isEnabled = false

                        return@addSnapshotListener
                    }

                    val currentlyServing =
                        document.getLong(
                            "currentlyServing"
                        ) ?: 0L

                    val queueStatus =
                        document.getString("status")
                            ?: "ACTIVE"

                    tvNowServing.text =
                        if (currentlyServing > 0) {

                            "R" + String.format(
                                "%02d",
                                currentlyServing
                            )

                        } else {

                            "R00"
                        }

                    tvQueueStatus.text =
                        "Queue Status: $queueStatus"

                    loadWaitingCustomers(
                        tvPeopleWaiting
                    )

                    if (currentlyServing > 0) {

                        loadCurrentCustomer(
                            currentlyServing,
                            tvCurrentCustomer,
                            btnComplete,
                            btnSkip
                        )

                    } else {

                        currentTicketId = null

                        tvCurrentCustomer.text =
                            "No customer is currently being served."

                        btnComplete.isEnabled = false
                        btnSkip.isEnabled = false
                    }
                }

        // Call Next Customer
        btnCallNext.setOnClickListener {

            callNextCustomer(
                tvStaffMessage,
                btnCallNext,
                btnComplete,
                btnSkip
            )
        }

        // Complete Customer
        btnComplete.setOnClickListener {

            completeCustomer(
                tvStaffMessage,
                btnComplete,
                btnSkip
            )
        }

        // Skip Customer
        btnSkip.setOnClickListener {

            skipCustomer(
                tvStaffMessage,
                btnComplete,
                btnSkip
            )
        }
    }

    private fun redirectToLogin() {

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

    private fun loadWaitingCustomers(
        waitingView: TextView
    ) {

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
            .addOnSuccessListener { documents ->

                waitingView.text =
                    documents.size().toString()
            }
            .addOnFailureListener {

                waitingView.text = "0"
            }
    }

    private fun loadCurrentCustomer(
        tokenValue: Long,
        customerView: TextView,
        completeButton: Button,
        skipButton: Button
    ) {

        db.collection("tickets")
            .whereEqualTo(
                "queueId",
                "RESTAURANT"
            )
            .whereEqualTo(
                "tokenNumberValue",
                tokenValue
            )
            .whereEqualTo(
                "status",
                "SERVING"
            )
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {

                    currentTicketId = null

                    customerView.text =
                        "R" +
                                String.format(
                                    "%02d",
                                    tokenValue
                                ) +
                                " is currently being served."

                    completeButton.isEnabled = false
                    skipButton.isEnabled = false

                    return@addOnSuccessListener
                }

                val ticket =
                    documents.documents.first()

                currentTicketId =
                    ticket.id

                customerView.text =
                    "Currently serving: " +
                            (ticket.getString(
                                "tokenNumber"
                            ) ?: "R00")

                completeButton.isEnabled = true
                skipButton.isEnabled = true
            }
            .addOnFailureListener {

                currentTicketId = null

                customerView.text =
                    "Unable to load current customer."

                completeButton.isEnabled = false
                skipButton.isEnabled = false
            }
    }

    private fun callNextCustomer(
        messageView: TextView,
        callNextButton: Button,
        completeButton: Button,
        skipButton: Button
    ) {

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
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {

                    messageView.text =
                        "No waiting customers."

                    return@addOnSuccessListener
                }

                /*
                 * Select the smallest waiting token.
                 * This also handles skipped/cancelled tokens.
                 */
                val nextTicket =
                    documents.documents.minByOrNull { document ->

                        document.getLong(
                            "tokenNumberValue"
                        ) ?: Long.MAX_VALUE
                    }

                if (nextTicket == null) {

                    messageView.text =
                        "Could not find next customer."

                    return@addOnSuccessListener
                }

                val nextNumber =
                    nextTicket.getLong(
                        "tokenNumberValue"
                    ) ?: 0L

                val token =
                    nextTicket.getString(
                        "tokenNumber"
                    ) ?: "R00"

                val queueReference =
                    db.collection("queues")
                        .document("RESTAURANT")

                db.runTransaction { transaction ->

                    transaction.update(
                        queueReference,
                        "currentlyServing",
                        nextNumber
                    )

                    transaction.update(
                        nextTicket.reference,
                        "status",
                        "SERVING"
                    )

                    transaction.update(
                        nextTicket.reference,
                        "calledAt",
                        System.currentTimeMillis()
                    )

                    null

                }.addOnSuccessListener {

                    currentTicketId =
                        nextTicket.id

                    messageView.text =
                        "$token is now being served."

                    callNextButton.isEnabled = true
                    completeButton.isEnabled = true
                    skipButton.isEnabled = true

                }.addOnFailureListener { error ->

                    messageView.text =
                        "Could not call customer.\n" +
                                error.message
                }
            }
            .addOnFailureListener { error ->

                messageView.text =
                    "Could not load customers.\n" +
                            error.message
            }
    }

    private fun completeCustomer(
        messageView: TextView,
        completeButton: Button,
        skipButton: Button
    ) {

        val ticketId =
            currentTicketId

        if (ticketId == null) {

            messageView.text =
                "No customer is currently selected."

            return
        }

        completeButton.isEnabled = false
        skipButton.isEnabled = false

        db.collection("tickets")
            .document(ticketId)
            .update(
                "status",
                "COMPLETED"
            )
            .addOnSuccessListener {

                messageView.text =
                    "Customer completed successfully."

                currentTicketId = null
            }
            .addOnFailureListener { error ->

                messageView.text =
                    "Could not complete customer.\n" +
                            error.message

                completeButton.isEnabled = true
                skipButton.isEnabled = true
            }
    }

    private fun skipCustomer(
        messageView: TextView,
        completeButton: Button,
        skipButton: Button
    ) {

        val ticketId =
            currentTicketId

        if (ticketId == null) {

            messageView.text =
                "No customer is currently selected."

            return
        }

        completeButton.isEnabled = false
        skipButton.isEnabled = false

        db.collection("tickets")
            .document(ticketId)
            .update(
                "status",
                "SKIPPED"
            )
            .addOnSuccessListener {

                messageView.text =
                    "Customer skipped."

                currentTicketId = null
            }
            .addOnFailureListener { error ->

                messageView.text =
                    "Could not skip customer.\n" +
                            error.message

                completeButton.isEnabled = true
                skipButton.isEnabled = true
            }
    }

    override fun onDestroy() {

        queueListener?.remove()

        super.onDestroy()
    }
}