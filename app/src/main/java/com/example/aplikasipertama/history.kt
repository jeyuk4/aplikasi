package com.example.aplikasipertama

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikasipertama.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var barChart: BarChart
    private lateinit var btnDaily: Button
    private lateinit var btnWeekly: Button
    private lateinit var btnMonthly: Button
    private lateinit var tvWeeklyAvg: TextView
    private lateinit var tvMonthlyAvg: TextView
    private lateinit var tvCompletion: TextView
    private lateinit var tvUsername: TextView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Inisialisasi Firebase dan SharedPreferences
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Inisialisasi UI
        barChart = findViewById(R.id.barChart)
        btnDaily = findViewById(R.id.btnDaily)
        btnWeekly = findViewById(R.id.btnWeekly)
        btnMonthly = findViewById(R.id.btnMonthly)
        tvWeeklyAvg = findViewById(R.id.tvWeeklyAvg)
        tvMonthlyAvg = findViewById(R.id.tvMonthlyAvg)
        tvCompletion = findViewById(R.id.tvCompletion)
        tvUsername = findViewById(R.id.tvUsername)

        // Ambil username
        getUsername()

        // Muat data awal (daily)
        loadChartData("daily")

        // Set listener tombol
        btnDaily.setOnClickListener { loadChartData("daily") }
        btnWeekly.setOnClickListener { loadChartData("weekly") }
        btnMonthly.setOnClickListener { loadChartData("monthly") }
    }

    private fun getUsername() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username") ?: "Unknown"
                    tvUsername.text = "Username: $username"
                    saveUsernameLocally(username)
                }
                .addOnFailureListener {
                    tvUsername.text = "Username: Guest"
                }
        } else {
            tvUsername.text = "Username: Guest"
        }
    }

    private fun saveUsernameLocally(username: String) {
        with(sharedPreferences.edit()) {
            putString("username", username)
            apply()
        }
    }

    private fun loadChartData(type: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val entries = mutableListOf<BarEntry>()
        val data = mutableListOf<Int>()

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        when (type) {
            "daily" -> calendar.add(Calendar.DAY_OF_MONTH, -6)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, -4)
            "monthly" -> calendar.add(Calendar.MONTH, -3)
        }
        val startDate = dateFormat.format(calendar.time)

        db.collection("users").document(userId).collection("waterIntake")
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                var index = 0
                for (doc in documents) {
                    val amount = doc.getLong("amount")?.toInt() ?: 0
                    entries.add(BarEntry(index.toFloat(), amount.toFloat()))
                    index++
                }

                val dataSet = BarDataSet(entries, "Water Consumption")
                val barData = BarData(dataSet)
                barChart.data = barData
                barChart.setFitBars(true)

                val description = Description()
                description.text = when (type) {
                    "daily" -> "Daily Consumption"
                    "weekly" -> "Weekly Consumption"
                    else -> "Monthly Consumption"
                }
                barChart.description = description
                barChart.notifyDataSetChanged()
                barChart.invalidate()

                updateStatistics(entries.map { it.y.toInt() })
            }
    }

    private fun updateStatistics(data: List<Int>) {
        val weeklyAvg = if (data.isNotEmpty()) data.sum() / 7.0 else 0.0
        val monthlyAvg = if (data.isNotEmpty()) data.sum() / 30.0 else 0.0
        val completion = if (weeklyAvg > 0) (weeklyAvg / 2000) * 100 else 0.0

        tvWeeklyAvg.text = "Weekly average: ${weeklyAvg.toInt()} ml/day"
        tvMonthlyAvg.text = "Monthly average: ${monthlyAvg.toInt()} ml/day"
        tvCompletion.text = "Completion: ${completion.toInt()}%"
    }
}
