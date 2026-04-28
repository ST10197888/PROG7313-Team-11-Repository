package com.budget.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.adapters.BudgetGoalAdapter
import com.budget.app.models.TransactionType
import com.budget.app.utils.AppData
import com.budget.app.utils.CurrencyFormatter
import com.google.android.material.textfield.TextInputLayout

class BudgetGoalsFragment : Fragment(), MainActivity.OnBackPressedListener {

    private lateinit var adapter: BudgetGoalAdapter
    private lateinit var tvLeftToAllocate: TextView
    private lateinit var rv: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_budget_goals, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinner          = view.findViewById<Spinner>(R.id.spinnerBudgetCategory)
        val tilCustom        = view.findViewById<TextInputLayout>(R.id.tilCustomBudget)
        val etCustom         = view.findViewById<EditText>(R.id.etCustomBudget)
        val etMin            = view.findViewById<EditText>(R.id.etBudgetMin)
        val etLimit          = view.findViewById<EditText>(R.id.etBudgetLimit)
        val btnAdd           = view.findViewById<Button>(R.id.btnAddBudget)
        rv                   = view.findViewById(R.id.rvBudgetGoals)
        tvLeftToAllocate     = view.findViewById(R.id.tvLeftToAllocate)

        // Category spinner
        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, AppData.getCategoriesForType(TransactionType.EXPENSE))
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = catAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                tilCustom.visibility = if (selected == "Other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // RecyclerView
        adapter = BudgetGoalAdapter(AppData.getBudgetGoals().toMutableList()) { goal ->
            AppData.removeBudgetGoal(requireContext(), goal.category)
            refreshUI()
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnAdd.setOnClickListener {
            val minStr = etMin.text.toString().trim()
            val limitStr = etLimit.text.toString().trim()
            val selectedCategory = spinner.selectedItem.toString()

            val category = if (selectedCategory == "Other") {
                etCustom.text.toString().trim()
            } else {
                selectedCategory
            }

            val minVal = minStr.toDoubleOrNull() ?: 0.0

            when {
                category.isEmpty() -> {
                    if (selectedCategory == "Other") etCustom.error = "Enter custom budget name"
                    else Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                }
                limitStr.isEmpty() -> etLimit.error = "Enter a limit amount"
                limitStr.toDoubleOrNull() == null || limitStr.toDouble() <= 0 ->
                    etLimit.error = "Enter a valid amount"
                minVal > (limitStr.toDoubleOrNull() ?: 0.0) ->
                    etMin.error = "Min cannot be greater than Max"
                else -> {
                    AppData.addOrUpdateBudgetGoal(requireContext(), category, limitStr.toDouble(), minVal)
                    etLimit.text.clear()
                    etMin.text.clear()
                    etCustom.text.clear()
                    Toast.makeText(requireContext(), "Budget goal saved!", Toast.LENGTH_SHORT).show()
                    refreshUI()
                }
            }
        }

        refreshUI()
    }

    override fun onBackPressed(): Boolean {
        if (::rv.isInitialized && rv.computeVerticalScrollOffset() > 0) {
            rv.smoothScrollToPosition(0)
            return true
        }
        return false
    }

    private fun refreshUI() {
        adapter.updateData(AppData.getBudgetGoals())
        tvLeftToAllocate.text = CurrencyFormatter.format(AppData.getUnallocatedIncome())
    }
}