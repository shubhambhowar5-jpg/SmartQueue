package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminStaffActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var staffContainer: LinearLayout
    private lateinit var message: TextView

    companion object {
        private val QUEUES = linkedMapOf(
            "Restaurant" to "RESTAURANT",
            "Beauty Parlor" to "BEAUTY_PARLOR",
            "Dental Clinic" to "DENTAL_CLINIC",
            "Supermarket" to "SUPERMARKET",
            "Airport" to "AIRPORT"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_staff)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        staffContainer = findViewById(R.id.staffContainer)
        message = findViewById(R.id.tvAdminMessage)

        findViewById<Button>(R.id.btnAddStaff).setOnClickListener {
            showCreateStaffInviteDialog()
        }

        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        verifyAdminAndLoad()
    }

    private fun verifyAdminAndLoad() {
        val uid = auth.currentUser?.uid ?: run {
            goToLogin()
            return
        }

        db.collection("admins").document(uid).get()
            .addOnSuccessListener { document ->
                val allowed = document.exists() &&
                        (document.getBoolean("enabled") ?: true) &&
                        document.getString("role")?.lowercase() == "admin"

                if (!allowed) {
                    auth.signOut()
                    goToLogin()
                } else {
                    loadStaff()
                }
            }
            .addOnFailureListener {
                auth.signOut()
                goToLogin()
            }
    }

    private fun loadStaff() {
        message.text = "Loading staff..."

        db.collection("staff")
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                staffContainer.removeAllViews()

                if (snapshot.isEmpty) {
                    message.text = "No staff accounts yet. Add a staff invitation."
                    loadPendingInvitations()
                    return@addOnSuccessListener
                }

                message.text = "${snapshot.size()} staff account(s)"
                snapshot.documents.forEach { doc ->
                    addStaffCard(
                        uid = doc.id,
                        name = doc.getString("name") ?: "Staff",
                        email = doc.getString("email") ?: "",
                        queueId = doc.getString("queueId") ?: "",
                        enabled = doc.getBoolean("enabled") ?: true,
                        status = doc.getString("status") ?: "ACTIVE"
                    )
                }
                loadPendingInvitations()
            }
            .addOnFailureListener { error ->
                message.text = "Could not load staff"
                Toast.makeText(this, error.message ?: "Unable to load staff.", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadPendingInvitations() {
        db.collection("staffInvites")
            .whereEqualTo("status", "INVITED")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    addInvitationCard(
                        name = doc.getString("name") ?: "Staff",
                        email = doc.getString("email") ?: "",
                        code = doc.id,
                        queueId = doc.getString("queueId") ?: ""
                    )
                }
            }
    }

    private fun addStaffCard(
        uid: String,
        name: String,
        email: String,
        queueId: String,
        enabled: Boolean,
        status: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundResource(R.drawable.bg_purple_card)
            elevation = dp(4).toFloat()
        }

        val title = TextView(this).apply {
            text = name
            setTextColor(getColor(R.color.text_primary))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val emailView = TextView(this).apply {
            text = email
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
        }

        val queueView = TextView(this).apply {
            text = "Section: ${displayQueueName(queueId)}"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            setPadding(0, dp(6), 0, 0)
        }

        val statusText = if (enabled) "Active" else "Disabled"
        val statusView = TextView(this).apply {
            text = "$statusText • $status"
            setTextColor(getColor(if (enabled) R.color.queue_serving else R.color.queue_waiting))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        }

        val action = Button(this).apply {
            text = if (enabled) "Disable Staff" else "Enable Staff"
            setAllCaps(false)
            setTextColor(getColor(if (enabled) R.color.text_primary else R.color.white))
            background = getDrawable(if (enabled) R.drawable.bg_secondary_button else R.drawable.bg_purple_button)
        }

        action.setOnClickListener {
            action.isEnabled = false
            action.text = "Updating..."
            setStaffEnabled(uid, !enabled)
        }

        card.addView(title)
        card.addView(emailView)
        card.addView(queueView)
        card.addView(statusView)
        card.addView(action, ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
        addCardToContainer(card)
    }

    private fun addInvitationCard(name: String, email: String, code: String, queueId: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundResource(R.drawable.bg_purple_card)
            elevation = dp(4).toFloat()
        }

        val title = TextView(this).apply {
            text = name
            setTextColor(getColor(R.color.text_primary))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val details = TextView(this).apply {
            text = "Invitation pending\n$email\nSection: ${displayQueueName(queueId)}\nCode: $code"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            setPadding(0, dp(6), 0, 0)
        }

        card.addView(title)
        card.addView(details)
        addCardToContainer(card)
    }

    private fun addCardToContainer(card: LinearLayout) {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, dp(10), 0, 0)
        staffContainer.addView(card, params)
    }

    private fun setStaffEnabled(uid: String, enabled: Boolean) {
        db.collection("staff").document(uid)
            .update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, if (enabled) "Staff enabled." else "Staff disabled.", Toast.LENGTH_SHORT).show()
                loadStaff()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, error.message ?: "Could not update staff.", Toast.LENGTH_LONG).show()
                loadStaff()
            }
    }

    private fun showCreateStaffInviteDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }

        val name = EditText(this).apply {
            hint = "Staff name"
        }

        val email = EditText(this).apply {
            hint = "Staff email"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val queueSpinner = Spinner(this)
        val queueNames = QUEUES.keys.toList()
        queueSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            queueNames
        )

        layout.addView(name, ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        layout.addView(email, ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        layout.addView(queueSpinner, ViewGroup.LayoutParams.MATCH_PARENT, dp(56))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Staff Invitation")
            .setMessage("Choose the section this staff member will manage. They will register using the invitation code.")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create Invitation", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nameText = name.text.toString().trim()
                val emailText = email.text.toString().trim().lowercase()
                val selectedName = queueNames[queueSpinner.selectedItemPosition]
                val queueId = QUEUES[selectedName]

                if (nameText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                    Toast.makeText(this, "Enter a name and valid email.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (queueId.isNullOrBlank() || !QUEUES.values.contains(queueId)) {
                    Toast.makeText(this, "Select a valid section.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                createStaffInvitation(nameText, emailText, queueId, dialog)
            }
        }

        dialog.show()
    }

    private fun createStaffInvitation(name: String, email: String, queueId: String, dialog: AlertDialog) {
        val adminUid = auth.currentUser?.uid ?: run {
            dialog.dismiss()
            goToLogin()
            return
        }

        if (!QUEUES.values.contains(queueId)) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            Toast.makeText(this, "Invalid section.", Toast.LENGTH_LONG).show()
            return
        }

        createUniqueInviteCode { inviteCode ->
            val invite = hashMapOf(
                "name" to name,
                "email" to email,
                "role" to "staff",
                "queueId" to queueId,
                "enabled" to true,
                "status" to "INVITED",
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("staffInvites").document(inviteCode).set(invite)
                .addOnSuccessListener {
                    dialog.dismiss()
                    showInviteCode(name, email, queueId, inviteCode)
                    loadStaff()
                }
                .addOnFailureListener { error ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    Toast.makeText(this, error.message ?: "Could not create invitation.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun createUniqueInviteCode(onReady: (String) -> Unit) {
        val code = (100000..999999).random().toString()

        db.collection("staffInvites").document(code).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onReady(code)
                } else {
                    createUniqueInviteCode(onReady)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not create invitation code. Check your connection.", Toast.LENGTH_LONG).show()
            }
    }

    private fun showInviteCode(name: String, email: String, queueId: String, code: String) {
        AlertDialog.Builder(this)
            .setTitle("Staff Invitation Created")
            .setMessage(
                "Give this information to $name:\n\n" +
                        "Email: $email\n" +
                        "Section: ${displayQueueName(queueId)}\n" +
                        "Invitation code: $code\n\n" +
                        "They must use Staff Registration in SmartQueue to create their password."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun displayQueueName(queueId: String): String =
        QUEUES.entries.firstOrNull { it.value == queueId }?.key ?: queueId

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
