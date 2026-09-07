package com.example.smartqueue

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class StaffRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        private val VALID_QUEUE_IDS = setOf(
            "RESTAURANT",
            "BEAUTY_PARLOR",
            "DENTAL_CLINIC",
            "SUPERMARKET",
            "AIRPORT"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val name = findViewById<EditText>(R.id.etStaffRegisterName)
        val email = findViewById<EditText>(R.id.etStaffRegisterEmail)
        val code = findViewById<EditText>(R.id.etStaffInviteCode)
        val password = findViewById<EditText>(R.id.etStaffRegisterPassword)
        val confirm = findViewById<EditText>(R.id.etStaffRegisterConfirmPassword)
        val register = findViewById<Button>(R.id.btnStaffRegister)
        val back = findViewById<TextView>(R.id.tvBackToStaffLogin)

        register.setOnClickListener {
            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim().lowercase()
            val codeText = code.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmText = confirm.text.toString()

            if (nameText.isEmpty()) {
                name.error = "Enter your name"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (!codeText.matches(Regex("\\d{6}"))) {
                code.error = "Enter the 6-digit invitation code"
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                password.error = "Password must contain at least 6 characters"
                return@setOnClickListener
            }

            if (passwordText != confirmText) {
                confirm.error = "Passwords do not match"
                return@setOnClickListener
            }

            register.isEnabled = false
            register.text = "Checking invitation..."
            findInviteAndCreateAccount(nameText, emailText, codeText, passwordText, register)
        }

        back.setOnClickListener { finish() }
    }

    private fun findInviteAndCreateAccount(
        name: String,
        email: String,
        code: String,
        password: String,
        register: Button
    ) {
        db.collection("staffInvites").document(code).get()
            .addOnSuccessListener { inviteDoc ->
                if (!inviteDoc.exists()) {
                    resetRegister(register, "Invitation code not found.")
                    return@addOnSuccessListener
                }

                val invitedEmail = inviteDoc.getString("email")?.trim()?.lowercase()
                val status = inviteDoc.getString("status")
                val enabled = inviteDoc.getBoolean("enabled") ?: false
                val queueId = inviteDoc.getString("queueId")

                if (queueId.isNullOrBlank() || queueId !in VALID_QUEUE_IDS) {
                    resetRegister(register, "This invitation has an invalid section assignment.")
                    return@addOnSuccessListener
                }

                if (invitedEmail != email || status != "INVITED" || !enabled) {
                    resetRegister(register, "This invitation is invalid, already used, or disabled.")
                    return@addOnSuccessListener
                }

                register.text = "Creating account..."

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (!task.isSuccessful) {
                            resetRegister(register, "Registration failed: ${task.exception?.message}")
                            return@addOnCompleteListener
                        }

                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            resetRegister(register, "Unable to create staff account.")
                            return@addOnCompleteListener
                        }

                        val staffData = hashMapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "role" to "staff",
                            "queueId" to queueId,
                            "enabled" to true,
                            "status" to "ACTIVE",
                            "inviteCode" to code,
                            "createdBy" to (inviteDoc.getString("createdBy") ?: ""),
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("staff").document(uid).set(staffData)
                            .addOnSuccessListener {
                                inviteDoc.reference.update(
                                    mapOf(
                                        "status" to "CLAIMED",
                                        "claimedBy" to uid,
                                        "claimedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                    .addOnSuccessListener {
                                        auth.signOut()
                                        Toast.makeText(
                                            this,
                                            "Staff account created. You can now use Staff Login.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    }
                                    .addOnFailureListener { error ->
                                        // Leave the account/profile intact so the admin can
                                        // see the problem rather than silently losing access.
                                        resetRegister(
                                            register,
                                            "Account created, but invitation could not be finalized: ${error.message}"
                                        )
                                    }
                            }
                            .addOnFailureListener { error ->
                                auth.currentUser?.delete()
                                resetRegister(
                                    register,
                                    "Could not save staff profile: ${error.message}"
                                )
                            }
                    }
            }
            .addOnFailureListener { error ->
                resetRegister(register, "Could not verify invitation: ${error.message}")
            }
    }

    private fun resetRegister(button: Button, message: String) {
        auth.signOut()
        button.isEnabled = true
        button.text = "Create Staff Account"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
