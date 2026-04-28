package com.budget.app.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.adapters.TransactionAdapter
import com.budget.app.models.Transaction
import com.budget.app.utils.AppData
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment(), MainActivity.OnBackPressedListener {

    private lateinit var adapter: TransactionAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var rv: RecyclerView
    private lateinit var tvActiveFilter: TextView
    private lateinit var btnFilter: ImageButton

    private lateinit var overlayContainer: View    // Overlay Views
    private lateinit var progressBarCard: CardView
    private lateinit var tvProgressBar: TextView
    private lateinit var btnCancelDelete: Button

    private var deletionTimer: CountDownTimer? = null
    private var transactionToDelete: Transaction? = null

    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_transactions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvTransactions)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvActiveFilter = view.findViewById(R.id.tvActiveFilter)
        btnFilter = view.findViewById(R.id.btnFilter)

        overlayContainer = view.findViewById(R.id.overlayContainer)
        progressBarCard = view.findViewById(R.id.progressBarCard)
        tvProgressBar = view.findViewById(R.id.tvProgressBar)
        btnCancelDelete = view.findViewById(R.id.btnCancelDelete)

        adapter = TransactionAdapter(
            initialData = AppData.getAllTransactions(),
            onDelete = { transaction ->
                startDeletionProcess(transaction)
            },
            onDetails = { transaction ->
                (activity as? MainActivity)?.loadFragment(TransactionDetailsFragment.newInstance(transaction.id))
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnCancelDelete.setOnClickListener {
            cancelDeletion()
        }

        btnFilter.setOnClickListener {
            showDatePicker()
        }

        refreshList()
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select Date Range")
        builder.setTheme(R.style.AppDatePicker)

        if (startDate != null && endDate != null) {
            builder.setSelection(Pair(startDate!!.time, endDate!!.time))
        }

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            startDate = Date(selection.first)
            endDate = Date(selection.second)
            refreshList()
        }
        picker.addOnNegativeButtonClickListener {
            startDate = null
            endDate = null
            refreshList()
        }
        picker.show(childFragmentManager, "date_picker")
    }

    override fun onBackPressed(): Boolean {
        if (::rv.isInitialized && rv.computeVerticalScrollOffset() > 0) {
            rv.smoothScrollToPosition(0)
            return true
        }
        return false
    }

    private fun startDeletionProcess(transaction: Transaction) {
        transactionToDelete = transaction
        showOverlay()

        var elapsed = 0L
        val totalDuration = 1500L
        val tickInterval = 50L

        deletionTimer = object : CountDownTimer(totalDuration, tickInterval) {
            override fun onTick(millisUntilFinished: Long) {
                elapsed += tickInterval
                val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                updateProgressBarText(progress)
            }

            override fun onFinish() {
                updateProgressBarText(1f)
                completeDeletion()
            }
        }.start()
    }

    private fun updateProgressBarText(progress: Float) {
        val length = 20
        val filledCount = (progress * length).toInt()
        val emptyCount = length - filledCount
        val percentage = (progress * 100).toInt()

        val bar = buildString {
            append("[")
            repeat(filledCount) { append("█") }
            repeat(emptyCount) { append("░") }
            append("] $percentage%")
        }
        tvProgressBar.text = bar
    }

    private fun showOverlay() {
        overlayContainer.visibility = View.VISIBLE
        progressBarCard.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        overlayContainer.visibility = View.GONE
        progressBarCard.visibility = View.GONE
    }

    private fun cancelDeletion() {
        deletionTimer?.cancel()
        hideOverlay()
        transactionToDelete = null
        Toast.makeText(requireContext(), "Deletion cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun completeDeletion() {
        transactionToDelete?.let {
            AppData.removeTransaction(requireContext(), it.id)
            refreshList()
            Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
        }
        hideOverlay()
        transactionToDelete = null
    }

    private fun refreshList() {
        val list = AppData.getFilteredTransactions(startDate, endDate)
        adapter.updateData(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        if (startDate != null && endDate != null) {
            val df = SimpleDateFormat("dd MMM", Locale.getDefault())
            tvActiveFilter.text = "Filtering: ${df.format(startDate!!)} - ${df.format(endDate!!)}"
            tvActiveFilter.visibility = View.VISIBLE
        } else {
            tvActiveFilter.text = "Filtering: All Time"
            // Keep it visible to show current state
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deletionTimer?.cancel()
    }
}