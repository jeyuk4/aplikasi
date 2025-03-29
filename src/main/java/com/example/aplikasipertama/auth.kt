package com.example.aplikasipertama

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usernameInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        usernameInput = findViewById(R.id.editTextUsername)
        loginButton = findViewById(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isNotEmpty()) {
                checkOrRegisterUser(username)
            } else {
                Toast.makeText(this, "Masukkan username!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkOrRegisterUser(username: String) {
        val userRef = db.collection("user").document(username)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    saveUserSession(username)
                    goToWeighActivity(username)
                } else {
                    val userData = hashMapOf(
                        "username" to username,
                        "weight" to "" // Default, nanti diperbarui di WeightActivity
                    )
                    userRef.set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            saveUserSession(username)
                            goToWeighActivity(username)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengakses database: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserSession(username: String) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("username", username)  // Simpan username
        editor.apply()  // Simpan perubahan secara asinkron
         }

    private fun goToWeighActivity(username: String) {
        Log.d("AuthActivity", "Mengirim username: $username ke WeightActivity") // Debug log

        val intent = Intent(this, WeightActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
    }
}