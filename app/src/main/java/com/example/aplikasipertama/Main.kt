package com.example.aplikasipertama

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewWaterIntake: TextView
    private lateinit var textViewWaterRequirement: TextView
    private lateinit var textViewUsername: TextView
    private lateinit var addWaterButton: ImageButton
    private lateinit var buttonSettings: Button
    private lateinit var buttonHistory: Button
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var adapter: WaterIntakeAdapter
    private var historyList = mutableListOf<WaterIntakeEntry>()

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    private var waterIntake: Int = 0
    private var dailyWaterRequirement = 2000
    private var weight = 60.0f
    private var unit = "ml"
    private var username = "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        if (username == null) {
            // User belum login, arahkan ke AuthActivity
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // User sudah login, tampilkan halaman utama
            Log.d("MainActivity", "User sudah login: $username")
            // Tambahkan logika untuk menampilkan halaman utama aplikasi
        }

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "Guest")
        val weight = sharedPreferences.getFloat("weight", 0f)

        Log.d("MainActivity", "Username: $username, Berat Badan: $weight")


        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d("AuthCheck", "User tidak ditemukan, kembali ke Auth Page")
        } else {
            Log.d("AuthCheck", "User masih login: ${user.uid}")
        }

        db = FirebaseFirestore.getInstance()

        initUI()
        loadUserPreferences()
        fetchUserData()
        loadWaterIntakeHistory() // Tambahkan di sini
    }

    private fun initUI() {
        progressBar = findViewById(R.id.progressBar)
        textViewWaterIntake = findViewById(R.id.textView)
        textViewWaterRequirement = findViewById(R.id.textViewWaterRequirement)
        textViewUsername = findViewById(R.id.textViewUsername)
        addWaterButton = findViewById(R.id.imageButton)
        buttonSettings = findViewById(R.id.button2)
        buttonHistory = findViewById(R.id.button3)
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)

        adapter = WaterIntakeAdapter(this, historyList, ::editEntry, ::deleteEntry)
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        recyclerViewHistory.adapter = adapter

        addWaterButton.setOnClickListener { showWaterAmountDialog() }
        buttonSettings.setOnClickListener {
            startActivity(Intent(this, Settings1Activity::class.java)) // Ubah menjadi startActivity()
        }
        buttonHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun loadUserPreferences() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        weight = sharedPreferences.getFloat("weight", 60.0f)
        username = sharedPreferences.getString("username", "User") ?: "User"
        unit = sharedPreferences.getString("waterUnit", "ml") ?: "ml"
        updateWaterTarget()
        textViewUsername.text = username
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    weight = document.getDouble("weight")?.toFloat() ?: 60.0f
                    username = document.getString("username") ?: "User"
                    waterIntake =
                        document.getLong("waterIntake")?.toInt() ?: 0 // Tambahkan baris ini
                    saveUserDataToPreferences(weight, username)
                    updateWaterTarget()
                    textViewUsername.text = username
                    updateWaterIntakeUI() // Tambahkan baris ini
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal mengambil data pengguna: ${e.message}")
            }
    }

    private fun saveUserDataToPreferences(weight: Float, username: String) {
        sharedPreferences.edit()
            .putFloat("weight", weight)
            .putString("username", username)
            .apply()
    }

    private fun updateWaterTarget() {
        dailyWaterRequirement = (weight * 35).toInt()
        textViewWaterRequirement.text = "$dailyWaterRequirement $unit"
        updateProgressBar()
    }

    private fun updateProgressBar() {
        progressBar.max = dailyWaterRequirement
        progressBar.progress = waterIntake
    }

    private fun showWaterAmountDialog() {
        val options = arrayOf("100 ml", "250 ml", "535 ml", "600 ml")
        val values = intArrayOf(100, 250, 535, 600)

        AlertDialog.Builder(this)
            .setTitle("Pilih jumlah air yang diminum")
            .setItems(options) { _, which -> addWater(values[which]) }
            .show()
    }

    private fun editEntry(position: Int) {
        Log.i("MainActivity", "editEntry called with position: $position")
        val options = arrayOf("100 ml", "250 ml", "535 ml", "600 ml")
        val values = intArrayOf(100, 250, 535, 600)

        AlertDialog.Builder(this)
            .setTitle("Edit jumlah air")
            .setItems(options) { _, which ->
                val updatedAmount = values[which]
                val entry = historyList[position]

                val userId = auth.currentUser?.uid ?: return@setItems

                val waterEntryRef = db.collection("users")
                    .document(userId)
                    .collection("waterIntake")
                    .document(entry.timestamp.toString())

                waterEntryRef.update("amount", updatedAmount)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Data konsumsi air diperbarui")

                        // Update data di RecyclerView
                        historyList[position] = entry.copy(amount = updatedAmount)
                        adapter.notifyItemChanged(position)

                        // Update konsumsi air secara lokal & progress bar
                        updateWaterIntakeFromHistory()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Gagal update data: ${e.message}")
                    }
            }
            .show()
    }

    private fun deleteEntry(position: Int) {
        val userId = auth.currentUser?.uid ?: return
        val entry = historyList[position]

        db.collection("users").document(userId)
            .collection("waterIntake").document(entry.timestamp.toString())
            .delete()
            .addOnSuccessListener {
                // Hapus dari RecyclerView
                historyList.removeAt(position)
                adapter.notifyItemRemoved(position)

                // Update konsumsi air & progress bar
                updateWaterIntakeFromHistory()

                Log.d("Firestore", "Data konsumsi air dihapus")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal menghapus data: ${e.message}")
            }
    }

    private fun updateWaterIntakeFromHistory() {
        waterIntake = historyList.sumOf { it.amount }
        updateWaterIntakeUI()
        saveTotalWaterIntakeToDB()
    }

    private fun saveTotalWaterIntakeToDB() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("waterIntake", waterIntake)
            .addOnSuccessListener {
                Log.d("Firestore", "Total water intake disimpan: $waterIntake")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal menyimpan total water intake: ${e.message}")
            }
    }

    private fun addWater(amount: Int) {
        waterIntake += amount
        updateWaterIntakeUI()
        updateProgressBar()
        val timestampRaw = System.currentTimeMillis()
        val timestampString = timestampRaw.toString()
        val timestamp = Timestamp(timestampRaw / 1000, ((timestampRaw % 1000) * 1000000).toInt())
//        Log.d("Kontol", timestamp)
        val newEntry = WaterIntakeEntry(amount, timestamp, timestampString)

//        historyList.add(0, newEntry)
        adapter.notifyItemInserted(0)
        recyclerViewHistory.scrollToPosition(0)
        saveWaterIntakeToDB(amount, timestampString)
        saveTotalWaterIntakeToDB()
    }

    private fun updateWaterIntakeUI() {
        val target = if (dailyWaterRequirement > 0) dailyWaterRequirement else 1
        val progress = (waterIntake * 100) / target
        textViewWaterIntake.text = "$waterIntake / $dailyWaterRequirement $unit"
        progressBar.progress = progress.coerceIn(0, 100)
        Log.d("MainActivity", "Water Intake: $waterIntake, Target: $dailyWaterRequirement")
    }

    private fun loadWaterIntakeHistory() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("waterIntake")
            .orderBy("timestamp") // assuming this is Firestore Timestamp
            .get()
            .addOnSuccessListener { result ->
                historyList.clear()
                waterIntake = 0
                for (document in result) {
                    val amount = document.getLong("amount")?.toInt() ?: 0
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.time.toString()
//                    historyList.add(WaterIntakeEntry(amount, timestamp))
                    waterIntake += amount
                }
                adapter.notifyDataSetChanged()
                updateWaterIntakeUI()
                saveTotalWaterIntakeToDB()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal mengambil riwayat konsumsi air: ${e.message}")
            }
    }

    private fun saveWaterIntakeToDB(amount: Int, timestamp: String) {
        val userId = auth.currentUser?.uid ?: return
        val entry = hashMapOf(
            "amount" to amount,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("waterIntake").document(timestamp)
            .set(entry, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Data konsumsi air disimpan.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal menyimpan data: ${e.message}")
            }
    }
}
