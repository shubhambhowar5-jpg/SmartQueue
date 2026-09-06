package com.example.smartqueue

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class AdminStaffActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var staffContainer: LinearLayout
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_staff)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()

        staffContainer = findViewById(R.id.staffContainer)
        message = findViewById(R.id.tvAdminMessage)

        findViewById<Button>(R.id.btnAddStaff).setOnClickListener { showCreateStaffDialog() }
        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        verifyAdminAndLoad()
    }

    private fun verifyAdminAndLoad() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
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
        functions.getHttpsCallable("listStaff").call()
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val staff = data?.get("staff") as? List<*> ?: emptyList<Any>()

                staffContainer.removeAllViews()
                if (staff.isEmpty()) {
                    message.text = "No staff accounts yet. Add the first staff member."
                    return@addOnSuccessListener
                }

                message.text = "${staff.size} staff account(s)"
                staff.sortedBy { ((it as? Map<*, *>)?.get("email") as? String ?: "") }
                    .forEach { item ->
                        val staffMap = item as? Map<*, *> ?: return@forEach
                        addStaffCard(
                            uid = staffMap["uid"] as? String ?: return@forEach,
                            name = staffMap["name"] as? String ?: "Staff",
                            email = staffMap["email"] as? String ?: "",
                            enabled = staffMap["enabled"] as? Boolean ?: true
                        )
                    }
            }
            .addOnFailureListener { error ->
                message.text = "Could not load staff"
                Toast.makeText(this, error.message ?: "Unable to load staff.", Toast.LENGTH_LONG).show()
            }
    }

    private fun addStaffCard(uid: String, name: String, email: String, enabled: Boolean) {
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
        val status = TextView(this).apply {
            text = if (enabled) "Active" else "Disabled"
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
        card.addView(status)
        card.addView(action, ViewGroup.LayoutParams.MATCH_PARENT, dp(52))

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, dp(10), 0, 0)
        staffContainer.addView(card, params)
    }

    private fun setStaffEnabled(uid: String, enabled: Boolean) {
        functions.getHttpsCallable("setStaffEnabled")
            .call(mapOf("uid" to uid, "enabled" to enabled))
            .addOnSuccessListener {
                Toast.makeText(this, if (enabled) "Staff enabled." else "Staff disabled.", Toast.LENGTH_SHORT).show()
                loadStaff()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, error.message ?: "Could not update staff.", Toast.LENGTH_LONG).show()
                loadStaff()
            }
    }

    private fun showCreateStaffDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }

        val name = EditText(this).apply { hint = "Staff name" }
        val email = EditText(this).apply {
            hint = "Staff email"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val password = EditText(this).apply {
            hint = "Temporary password (6+ characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(name, ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        layout.addView(email, ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        layout.addView(password, ViewGroup.LayoutParams.MATCH_PARENT, dp(56))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Staff Account")
            .setMessage("The staff member will use this email and password in Staff Login.")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nameText = name.text.toString().trim()
                val emailText = email.text.toString().trim()
                val passwordText = password.text.toString()

                if (nameText.isEmpty() || emailText.isEmpty() || passwordText.length < 6) {
                    Toast.makeText(this, "Enter name, valid email and a password of at least 6 characters.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                createStaff(nameText, emailText, passwordText, dialog)
            }
        }

        dialog.show()
    }

    private fun createStaff(name: String, email: String, password: String, dialog: AlertDialog) {
        functions.getHttpsCallable("createStaff")
            .call(mapOf("name" to name, "email" to email, "password" to password))
            .addOnSuccessListener {
                dialog.dismiss()
                Toast.makeText(this, "Staff account created successfully.", Toast.LENGTH_LONG).show()
                loadStaff()
            }
            .addOnFailureListener { error ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                Toast.makeText(this, error.message ?: "Could not create staff account.", Toast.LENGTH_LONG).show()
            }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
