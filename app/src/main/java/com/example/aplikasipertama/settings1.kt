package com.example.aplikasipertama

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Settings1Activity : AppCompatActivity() {
    private lateinit var etWeight: EditText
    private lateinit var switchDarkMode: Switch
    private lateinit var switchReminder: Switch
    private lateinit var spinnerUnit: Spinner
    private lateinit var btnSaveSettings: Button
    private lateinit var tvUsername: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings1)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        etWeight = findViewById(R.id.etWeight)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchReminder = findViewById(R.id.switchReminder)
        spinnerUnit = findViewById(R.id.spinnerUnit)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        tvUsername = findViewById(R.id.tvUsername1)

        loadSettings()
        btnSaveSettings.setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d("SettingsActivity", "User UID: ${user.uid}") // Tambahkan log

        val username = sharedPreferences.getString("username", "User")
        tvUsername.text = "Hello, $username"

        val userRef = db.collection("user").document(user.uid)
            .collection("settings").document("general")

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                etWeight.setText(document.getDouble("weight")?.toInt().toString())
                switchDarkMode.isChecked = document.getBoolean("darkMode") ?: false
                switchReminder.isChecked = document.getBoolean("reminder") ?: true

                val unit = document.getString("waterUnit") ?: "ml"
                val unitIndex = resources.getStringArray(R.array.water_units).indexOf(unit)
                if (unitIndex >= 0) {
                    spinnerUnit.setSelection(unitIndex)
                }

                // Terapkan mode gelap secara langsung
                if (switchDarkMode.isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }

    private fun saveSettings() {
        val userId = auth.currentUser?.uid ?: return
        val weight = etWeight.text.toString().toFloatOrNull() ?: 60.0f
        val isDarkMode = switchDarkMode.isChecked
        val isReminderEnabled = switchReminder.isChecked
        val selectedUnit = spinnerUnit.selectedItem.toString()

        val userData = hashMapOf(
            "weight" to weight,
            "darkMode" to isDarkMode,
            "reminder" to isReminderEnabled,
            "waterUnit" to selectedUnit
        )

        db.collection("user").document(userId)
            .collection("settings").document("general")
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()

                // Simpan ke SharedPreferences agar data bisa dipakai di MainActivity
                val editor = sharedPreferences.edit()
                editor.putFloat("weight", weight)
                editor.putBoolean("darkMode", isDarkMode)
                editor.putBoolean("reminder", isReminderEnabled)
                editor.putString("waterUnit", selectedUnit)
                editor.apply()

                // Terapkan perubahan mode gelap
                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                // Kembali ke halaman utama setelah menyimpan
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}