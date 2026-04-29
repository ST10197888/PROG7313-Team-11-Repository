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
import com.budget.app.adapters.FinancialGoalAdapter
import com.budget.app.database.AppDatabase
import com.budget.app.database.FinancialGoalEntity
import com.budget.app.models.FinancialGoal
import com.budget.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.collections.map
import kotlin.isInitialized
import kotlin.text.isNotEmpty
import kotlin.text.toDoubleOrNull
import kotlin.text.trim

class FinancialGoalsFragment : Fragment(), MainActivity.OnBackPressedListener {

    private lateinit var adapter: FinancialGoalAdapter
    private lateinit var rv: RecyclerView

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private val currentUserId by lazy { SessionManager.getUserId(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_goals, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (currentUserId == -1) return

        val etName = view.findViewById<EditText>(R.id.etGoalName)
        val etTarget = view.findViewById<EditText>(R.id.etGoalTarget)
        val btnAdd = view.findViewById<Button>(R.id.btnAddGoal)
        rv = view.findViewById(R.id.rvGoals)

        adapter = FinancialGoalAdapter(emptyList()) { goal ->
            showAddProgressDialog(goal.id)
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        refreshList()

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val targetStr = etTarget.text.toString().trim()

            if (name.isNotEmpty() && targetStr.isNotEmpty()) {
                val target = targetStr.toDoubleOrNull() ?: 0.0
                if (target > 0) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val newGoal = FinancialGoalEntity(
                            userId = currentUserId,
                            name = name,
                            targetAmount = target,
                            currentAmount = 0.0,
                            deadline = Date() // Added required deadline
                        )
                        db.financialGoalDao().insertOrUpdate(newGoal)

                        withContext(Dispatchers.Main) {
                            etName.text.clear()
                            etTarget.text.clear()
                            refreshList()
                        }
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

    private fun showAddProgressDialog(goalId: Int) {
        val input = EditText(requireContext())
        input.hint = "Amount to add"
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(requireContext())
            .setTitle("Add Progress")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.financialGoalDao().addProgress(goalId, amount)
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
            val data = db.financialGoalDao().getAllForUser(currentUserId)

            // Map Entity to Domain Model using your defined .toModel() function
            val goals: List<FinancialGoal> = data.map { it.toModel() }

            withContext(Dispatchers.Main) {
                adapter.updateData(goals)
            }
        }
    }
}