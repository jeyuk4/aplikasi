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

class WeightActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var weightInput: EditText
    private lateinit var submitButton: Button
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight)

        // Inisialisasi Firestore & SharedPreferences
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        weightInput = findViewById(R.id.editTextNumber)
        submitButton = findViewById(R.id.weightbuttonnext1)

        // Ambil username dari intent atau SharedPreferences
        username = intent.getStringExtra("username") ?: sharedPreferences.getString("username", null)

        if (username == null) {
            Log.e("WeightActivity", "Error: Username tidak ditemukan di Intent maupun SharedPreferences!")
            Toast.makeText(this, "Error: Username tidak ditemukan!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        Log.d("WeightActivity", "Username diterima: $username")

        // Load berat badan dari SharedPreferences jika sudah ada
        val savedWeight = sharedPreferences.getFloat("weight", -1f)
        if (savedWeight > 0) {
            weightInput.setText(savedWeight.toInt().toString())
        }

        // Simpan berat badan ke Firestore & SharedPreferences saat tombol ditekan
        submitButton.setOnClickListener {
            val weight = weightInput.text.toString().toFloatOrNull()
            if (weight == null || weight <= 0) {
                Toast.makeText(this, "Masukkan berat badan yang valid!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            saveUserWeight(username!!, weight)
        }
    }

    private fun loadUserSession(): Pair<String?, String?> {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val username = sharedPreferences.getString("username", null) // Ambil username
        val weight = sharedPreferences.getString("weight", null) // Ambil berat badan

        return Pair(username, weight)
    }

    private fun saveUserWeight(username: String, weight: Float) {
        val usersRef = db.collection("users")

        // Cari dokumen yang username-nya sama
        usersRef.whereEqualTo("username", username).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Ambil dokumen pertama yang cocok
                    val userRef = usersRef.document(document.id)

                    userRef.set(mapOf("weight" to weight), SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("WeightActivity", "Berat badan diperbarui: $weight")

                            // Simpan ke SharedPreferences
                            sharedPreferences.edit()
                                .putString("username", username)
                                .putFloat("weight", weight)
                                .apply()

                            // Pindah ke MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("WeightActivity", "Gagal menyimpan berat badan", e)
                            Toast.makeText(this, "Gagal menyimpan berat badan!", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Log.e("WeightActivity", "User dengan username '$username' tidak ditemukan.")
                    Toast.makeText(this, "User tidak ditemukan!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("WeightActivity", "Gagal mengambil data user", e)
                Toast.makeText(this, "Gagal mengambil data user!", Toast.LENGTH_LONG).show()
            }
    }}
