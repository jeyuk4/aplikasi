//WaterIntakeEntry.kt
package com.example.aplikasipertama

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Model data untuk menyimpan riwayat minum
data class WaterIntakeEntry(val amount: Int, val timestamp: String)

class WaterIntakeAdapter(
    private val context: Context,
    private var historyList: MutableList<WaterIntakeEntry>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<WaterIntakeAdapter.WaterViewHolder>() {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    class WaterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewAmount: TextView = view.findViewById(R.id.textViewAmount)
        val buttonEdit: Button = view.findViewById(R.id.buttonEdit)
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_water_entry, parent, false)
        return WaterViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaterViewHolder, position: Int) {
        val entry = historyList[position]

        // Ambil satuan dari SharedPreferences
        val unit = sharedPreferences.getString("waterUnit", "ml") ?: "ml"
        val waterAmount = if (unit == "oz") convertMlToOz(entry.amount) else entry.amount
        val displayUnit = if (unit == "oz") "oz" else "ml"

        holder.textViewAmount.text = "Minum ${waterAmount.toInt()} $displayUnit"

        holder.buttonEdit.setOnClickListener { editEntry(position) }
        holder.buttonDelete.setOnClickListener { deleteEntry(position) }
    }

    override fun getItemCount(): Int = historyList.size

    // Fungsi konversi dari ml ke oz
    private fun convertMlToOz(ml: Int): Double {
        return ml / 29.5735 // 1 oz = 29.5735 ml
    }

    // Fungsi untuk mengedit entri minum
    private fun editEntry(position: Int) {
        val options = arrayOf("100 ml", "250 ml", "535 ml", "600 ml")
        val values = intArrayOf(100, 250, 535, 600)

        AlertDialog.Builder(context)
            .setTitle("Edit jumlah air")
            .setItems(options) { _, which ->
                val updatedAmount = values[which]
                val entry = historyList[position]
                val user = auth.currentUser

                if (user == null) {
                    Log.e("Firestore", "User belum login!")
                    return@setItems
                }

                val userId = user.uid
                val waterEntryRef = db.collection("user")
                    .document(userId)
                    .collection("timestamp")
                    .document(entry.timestamp)

                waterEntryRef.update("amount", updatedAmount)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Data konsumsi air berhasil diperbarui")
                        historyList[position] = historyList[position].copy(amount = updatedAmount)
                        notifyItemChanged(position)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Gagal update data: ${e.message}")
                    }
            }
            .show()
    }

    // Fungsi untuk menghapus entri minum
    private fun deleteEntry(position: Int) {
        val entry = historyList[position]
        val user = auth.currentUser

        if (user == null) {
            Log.e("Firestore", "User belum login!")
            return
        }

        val userId = user.uid
        val waterEntryRef = db.collection("user")
            .document(userId)
            .collection("timestamp")
            .document(entry.timestamp)

        AlertDialog.Builder(context)
            .setTitle("Hapus Entri")
            .setMessage("Apakah Anda yakin ingin menghapus entri ini?")
            .setPositiveButton("Ya") { _, _ ->
                waterEntryRef.delete()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Data konsumsi air berhasil dihapus")
                        historyList.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Gagal menghapus data: ${e.message}")
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
