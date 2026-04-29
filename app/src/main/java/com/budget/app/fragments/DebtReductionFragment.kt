package com.budget.app.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.adapters.DebtAdapter
import com.budget.app.database.AppDatabase
import com.budget.app.database.DebtEntity
import com.budget.app.models.Debt
import com.budget.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.map
import kotlin.isInitialized
import kotlin.ranges.coerceAtLeast
import kotlin.text.isNotEmpty
import kotlin.text.toDoubleOrNull
import kotlin.text.trim

class DebtReductionFragment : Fragment(), MainActivity.OnBackPressedListener {

    private lateinit var adapter: DebtAdapter
    private lateinit var rv: RecyclerView

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private val currentUserId by lazy { SessionManager.getUserId(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_debts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (currentUserId == -1) return

        val etName = view.findViewById<EditText>(R.id.etDebtName)
        val etAmount = view.findViewById<EditText>(R.id.etDebtAmount)
        val etRate = view.findViewById<EditText>(R.id.etDebtRate)
        val etMinPay = view.findViewById<EditText>(R.id.etDebtMinPay)
        val btnAdd = view.findViewById<Button>(R.id.btnAddDebt)
        rv = view.findViewById(R.id.rvDebts)

        adapter = DebtAdapter(emptyList()) { debt ->
            showPaymentDialog(debt)
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        refreshList()

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRate.text.toString().toDoubleOrNull() ?: 0.0
            val minPay = etMinPay.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty() && amount > 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val newDebt = DebtEntity(
                        userId = currentUserId,
                        name = name,
                        amount = amount,
                        interestRate = rate,
                        minPayment = minPay,
                        remainingAmount = amount // Initially, remaining equals total
                    )
                    db.debtDao().insertOrUpdate(newDebt)

                    withContext(Dispatchers.Main) {
                        etName.text.clear()
                        etAmount.text.clear()
                        etRate.text.clear()
                        etMinPay.text.clear()
                        refreshList()
                    }
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (::rv.isInitialized && rv.computeVerticalScrollOffset() > 0) {
            rv.smoothScrollToPosition(0)
            return true
        }
        return false
    }

    private fun showPaymentDialog(debt: Debt) {
        val input = EditText(requireContext())
        input.hint = "Payment Amount"
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(requireContext())
            .setTitle("Record Payment")
            .setView(input)
            .setPositiveButton("Pay") { _, _ ->
                val payment = input.text.toString().toDoubleOrNull() ?: 0.0
                if (payment > 0) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        // Calculate new remaining amount
                        val newRemaining = (debt.remainingAmount - payment).coerceAtLeast(0.0)
                        db.debtDao().updateRemaining(debt.id, newRemaining)

                        withContext(Dispatchers.Main) {
                            refreshList()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshList() {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = db.debtDao().getAllForUser(currentUserId)
            // Using your toModel() extension/function
            val debts: List<Debt> = data.map { it.toModel() }

            withContext(Dispatchers.Main) {
                adapter.updateData(debts)
            }
        }
    }
}