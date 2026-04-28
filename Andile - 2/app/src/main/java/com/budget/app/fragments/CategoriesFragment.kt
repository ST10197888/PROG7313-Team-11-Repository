package com.budget.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.models.TransactionType
import com.budget.app.utils.AppData

class CategoriesFragment : Fragment(), MainActivity.OnBackPressedListener {

    private lateinit var scrollView: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_categories, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view.findViewById(R.id.scrollViewCategories)

        setupSection(
            view.findViewById(R.id.btnHeaderExpense),
            view.findViewById(R.id.layoutExpenseList),
            view.findViewById(R.id.ivArrowExpense),
            TransactionType.EXPENSE
        )

        setupSection(
            view.findViewById(R.id.btnHeaderIncome),
            view.findViewById(R.id.layoutIncomeList),
            view.findViewById(R.id.ivArrowIncome),
            TransactionType.INCOME
        )

        setupSection(
            view.findViewById(R.id.btnHeaderSavings),
            view.findViewById(R.id.layoutSavingsList),
            view.findViewById(R.id.ivArrowSavings),
            TransactionType.SAVINGS
        )
    }

    override fun onBackPressed(): Boolean {
        if (::scrollView.isInitialized && scrollView.scrollY > 0) {
            scrollView.smoothScrollTo(0, 0)
            return true
        }
        return false
    }

    private fun setupSection(header: View, listContainer: LinearLayout, arrow: ImageView, type: TransactionType) {
        populateList(listContainer, type)

        header.setOnClickListener {
            if (listContainer.visibility == View.VISIBLE) {
                listContainer.visibility = View.GONE
                arrow.animate().rotation(0f).start()
            } else {
                listContainer.visibility = View.VISIBLE
                arrow.animate().rotation(180f).start()
            }
        }
    }

    private fun populateList(listContainer: LinearLayout, type: TransactionType) {
        listContainer.removeAllViews()

        val allCategories = AppData.getCategoriesForType(type)

        allCategories.forEach { category ->
            val isCustom = AppData.isCustomCategory(type, category)
            addCategoryRow(listContainer, type, category, isCustom)
        }

        // "Add Category" button at the bottom of each section
        val btnAdd = TextView(requireContext())
        btnAdd.text = "+ Add Category"
        btnAdd.textSize = 15f
        btnAdd.setPadding(48, 32, 32, 32)
        btnAdd.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        btnAdd.setTypeface(btnAdd.typeface, android.graphics.Typeface.BOLD)
        btnAdd.setBackgroundResource(android.R.drawable.list_selector_background)
        btnAdd.isClickable = true
        btnAdd.isFocusable = true
        btnAdd.setOnClickListener { showAddCategoryDialog(listContainer, type) }
        listContainer.addView(btnAdd)
    }

    private fun addCategoryRow(listContainer: LinearLayout, type: TransactionType, category: String, isCustom: Boolean) {
        val row = LinearLayout(requireContext())
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.setBackgroundResource(android.R.drawable.list_selector_background)
        row.isClickable = true
        row.isFocusable = true
        row.setOnClickListener {
            val fragment = AddTransactionFragment.newInstance(type, category)
            (activity as? MainActivity)?.loadFragment(fragment)
        }

        val tv = TextView(requireContext())
        tv.text = "• $category"
        tv.textSize = 16f
        tv.setPadding(48, 32, 16, 32)
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        val tvParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tv.layoutParams = tvParams
        row.addView(tv)

        // Delete button — only for custom categories
        if (isCustom) {
            val btnDelete = TextView(requireContext())
            btnDelete.text = "✕"
            btnDelete.textSize = 16f
            btnDelete.setPadding(16, 32, 32, 32)
            btnDelete.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
            btnDelete.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove Category")
                    .setMessage("Remove \"$category\"? Existing transactions using it won't be affected.")
                    .setPositiveButton("Remove") { _, _ ->
                        AppData.removeCustomCategory(requireContext(), type, category)
                        val parent = btnDelete.parent?.parent as? LinearLayout ?: return@setPositiveButton
                        populateList(parent, type)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            row.addView(btnDelete)
        }

        listContainer.addView(row)

        // Divider
        val divider = View(requireContext())
        divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also {
            it.setMargins(48, 0, 0, 0)
        }
        divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_grey))
        listContainer.addView(divider)
    }

    private fun showAddCategoryDialog(listContainer: LinearLayout, type: TransactionType) {
        val input = EditText(requireContext())
        input.hint = "Category name"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        input.setPadding(48, 32, 48, 32)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("New ${type.name.lowercase().replaceFirstChar { it.uppercase() }} Category")
            .setView(input)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                when {
                    name.isEmpty() -> input.error = "Enter a category name"
                    else -> {
                        val added = AppData.addCustomCategory(requireContext(), type, name)
                        if (added) {
                            populateList(listContainer, type)
                            dialog.dismiss()
                        } else {
                            input.error = "\"$name\" already exists"
                        }
                    }
                }
            }
        }

        dialog.show()
    }
}