package com.budget.app.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.models.TransactionType
import com.budget.app.utils.AppData
import com.budget.app.utils.CurrencyFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ReportFragment : Fragment(), MainActivity.OnBackPressedListener {

    private var selectedMonth = 0
    private var selectedYear  = 0
    private var startDate: Date? = null
    private var endDate: Date? = null

    private lateinit var tvMonthLabel    : TextView
    private lateinit var tvRangeSubtitle : TextView
    private lateinit var layoutGraphs     : LinearLayout
    private lateinit var layoutBreakdown : LinearLayout
    private lateinit var tvReportIncome  : TextView
    private lateinit var tvReportExpenses: TextView
    private lateinit var tvReportSavings : TextView
    private lateinit var tvSavingsRate   : TextView
    private lateinit var pbSavingsRate   : ProgressBar
    private lateinit var layoutBarChart  : LinearLayout
    private lateinit var scrollView      : ScrollView

    // Budget Summary views
    private lateinit var tvTotalBudgeted : TextView
    private lateinit var tvTotalSpent    : TextView
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var pbBudgetUtilization: ProgressBar
    private lateinit var tvBudgetPercent : TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_reports, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView       = view.findViewById(R.id.scrollViewReports)
        tvReportIncome   = view.findViewById(R.id.tvReportIncome)
        tvReportExpenses = view.findViewById(R.id.tvReportExpenses)
        tvReportSavings  = view.findViewById(R.id.tvReportSavings)
        tvSavingsRate    = view.findViewById(R.id.tvSavingsRate)
        pbSavingsRate    = view.findViewById(R.id.pbSavingsRate)
        layoutBreakdown  = view.findViewById(R.id.layoutCategoryBreakdown)
        tvMonthLabel     = view.findViewById(R.id.tvMonthLabel)
        tvRangeSubtitle  = view.findViewById(R.id.tvRangeSubtitle)
        layoutGraphs     = view.findViewById(R.id.layoutSkylineGraphs)
        layoutBarChart   = view.findViewById(R.id.layoutBarChart)

        // Initialize Budget Summary views
        tvTotalBudgeted     = view.findViewById(R.id.tvTotalBudgeted)
        tvTotalSpent        = view.findViewById(R.id.tvTotalSpent)
        tvBudgetRemaining   = view.findViewById(R.id.tvBudgetRemaining)
        pbBudgetUtilization = view.findViewById(R.id.pbBudgetUtilization)
        tvBudgetPercent     = view.findViewById(R.id.tvBudgetPercent)

        val btnPrev = view.findViewById<TextView>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<TextView>(R.id.btnNextMonth)
        val btnRange = view.findViewById<ImageButton>(R.id.btnRangeFilter)

        val now = Calendar.getInstance()
        selectedMonth = now.get(Calendar.MONTH)
        selectedYear  = now.get(Calendar.YEAR)

        btnPrev.setOnClickListener {
            startDate = null
            endDate = null
            if (selectedMonth == 0) { selectedMonth = 11; selectedYear-- }
            else selectedMonth--
            refresh()
        }

        btnNext.setOnClickListener {
            startDate = null
            endDate = null
            if (selectedMonth == 11) { selectedMonth = 0; selectedYear++ }
            else selectedMonth++
            refresh()
        }

        btnRange.setOnClickListener {
            showDatePicker()
        }

        refresh()
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select Custom Period")
        builder.setTheme(R.style.AppDatePicker)

        if (startDate != null && endDate != null) {
            builder.setSelection(Pair(startDate!!.time, endDate!!.time))
        }

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            startDate = Date(selection.first)
            endDate = Date(selection.second)
            refresh()
        }
        picker.show(childFragmentManager, "report_date_picker")
    }

    override fun onBackPressed(): Boolean {
        if (::scrollView.isInitialized && scrollView.scrollY > 0) {
            scrollView.smoothScrollTo(0, 0)
            return true
        }
        return false
    }

    private fun refresh() {
        val transactions: List<com.budget.app.models.Transaction>

        if (startDate != null && endDate != null) {
            val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvMonthLabel.text = "${df.format(startDate!!)} - ${df.format(endDate!!)}"
            tvRangeSubtitle.text = "Custom Range"
            transactions = AppData.getFilteredTransactions(startDate, endDate)
        } else {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, selectedMonth)
            cal.set(Calendar.YEAR, selectedYear)
            tvMonthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            tvRangeSubtitle.text = "Monthly View"
            transactions = AppData.getTransactionsForMonth(selectedMonth, selectedYear)
        }

        val income   = transactions.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings  = transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
        val rate     = if (income > 0) ((savings / income) * 100).toInt().coerceIn(0, 100) else 0

        tvReportIncome.text   = CurrencyFormatter.format(income)
        tvReportExpenses.text = CurrencyFormatter.format(expenses)
        tvReportSavings.text  = CurrencyFormatter.format(savings)
        tvSavingsRate.text    = "Savings Rate: $rate%"
        pbSavingsRate.progress = rate

        setupWeeklyTracker(layoutBarChart)
        if (startDate == null) {
            buildVisualWeeklyGraphs(selectedMonth, selectedYear)
        } else {
            layoutGraphs.removeAllViews() // Skip skyline for custom ranges for now to keep it simple
        }
        setupDetailedBreakdown(transactions)
        updateBudgetSummary(transactions)
    }

    private fun updateBudgetSummary(periodTransactions: List<com.budget.app.models.Transaction>) {
        val goals = AppData.getBudgetGoals()
        val totalBudgeted = goals.sumOf { it.limitAmount }

        val spentMap = periodTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }

        val totalSpent = goals.sumOf { spentMap[it.category] ?: 0.0 }
        val remaining = (totalBudgeted - totalSpent).coerceAtLeast(0.0)
        val percent = if (totalBudgeted > 0) ((totalSpent / totalBudgeted) * 100).toInt() else 0

        tvTotalBudgeted.text = CurrencyFormatter.format(totalBudgeted)
        tvTotalSpent.text = CurrencyFormatter.format(totalSpent)
        tvBudgetRemaining.text = CurrencyFormatter.format(remaining)
        tvBudgetPercent.text = "Utilization: $percent%"
        pbBudgetUtilization.progress = percent.coerceAtMost(100)
    }

    private fun setupWeeklyTracker(container: LinearLayout) {
        container.removeAllViews()

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        cal.add(Calendar.MONTH, -1)
        val lastMonth = cal.get(Calendar.MONTH)
        val lastYear = cal.get(Calendar.YEAR)

        val lmWeekly = calculateWeeklyData(AppData.getTransactionsForMonth(lastMonth, lastYear))
        val cmWeekly = calculateWeeklyData(AppData.getTransactionsForMonth(currentMonth, currentYear))

        val maxVal = (lmWeekly + cmWeekly).maxOfOrNull { it.max() }?.coerceAtLeast(1.0) ?: 1.0

        lmWeekly.forEachIndexed { i, data -> addWeeklyGroup(container, data, "LM W${i+1}", maxVal) }
        cmWeekly.forEachIndexed { i, data -> addWeeklyGroup(container, data, "CM W${i+1}", maxVal) }
    }

    private data class WeeklyData(val income: Double, val expense: Double, val savings: Double) {
        fun max() = maxOf(income, maxOf(expense, savings))
    }

    private fun calculateWeeklyData(transactions: List<com.budget.app.models.Transaction>): List<WeeklyData> {
        val result = List(4) { mutableMapOf<TransactionType, Double>() }
        val cal = Calendar.getInstance()
        transactions.forEach {
            cal.time = it.date
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val weekIndex = ((day - 1) / 7).coerceAtMost(3)
            val current = result[weekIndex].getOrDefault(it.type, 0.0)
            result[weekIndex][it.type] = current + it.amount
        }
        return result.map { map ->
            WeeklyData(
                map[TransactionType.INCOME] ?: 0.0,
                map[TransactionType.EXPENSE] ?: 0.0,
                map[TransactionType.SAVINGS] ?: 0.0
            )
        }
    }

    private fun addWeeklyGroup(container: LinearLayout, data: WeeklyData, label: String, maxVal: Double) {
        val groupLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val barsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(2, 0, 2, 0)
        }

        fun createBar(valAmount: Double, colorRes: Int): View {
            val barHeight = (valAmount / maxVal * 120).toInt().coerceAtLeast(0)
            return View(requireContext()).apply {
                val barHeightPx = (barHeight * resources.displayMetrics.density).toInt().coerceAtLeast(2)
                layoutParams = LinearLayout.LayoutParams(0, barHeightPx, 1f).apply { setMargins(1, 0, 1, 0) }
                setBackgroundColor(requireContext().getColor(colorRes))
            }
        }

        barsLayout.addView(createBar(data.income, R.color.income_green))
        barsLayout.addView(createBar(data.expense, R.color.expense_red))
        barsLayout.addView(createBar(data.savings, R.color.colorPrimary))

        val labelTv = TextView(requireContext()).apply {
            text = label
            textSize = 6f
            gravity = android.view.Gravity.CENTER
        }

        groupLayout.addView(barsLayout)
        groupLayout.addView(labelTv)
        container.addView(groupLayout)
    }

    private fun buildVisualWeeklyGraphs(month: Int, year: Int) {
        layoutGraphs.removeAllViews()

        val transactions = AppData.getTransactionsForMonth(month, year)

        fun getDailyMap(type: TransactionType): Map<Int, Double> {
            val cal = Calendar.getInstance()
            return transactions.filter { it.type == type }
                .groupBy {
                    cal.time = it.date
                    cal.get(Calendar.DAY_OF_MONTH)
                }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }

        addDailySkylineChart("Daily Income Skyline", getDailyMap(TransactionType.INCOME), R.color.income_green, month, year)
        addDailySkylineChart("Daily Expense Skyline", getDailyMap(TransactionType.EXPENSE), R.color.expense_red, month, year)
        addDailySkylineChart("Daily Savings Skyline", getDailyMap(TransactionType.SAVINGS), R.color.colorPrimary, month, year)
    }

    private fun addDailySkylineChart(title: String, data: Map<Int, Double>, colorRes: Int, month: Int, year: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 24.dpToPx())
            }
        }

        val titleTv = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setPadding(0, 0, 0, 8.dpToPx())
        }
        container.addView(titleTv)

        val hsv = android.widget.HorizontalScrollView(requireContext())
        val chartLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 120.dpToPx())
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
        }

        val maxVal = data.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

        for (day in 1..daysInMonth) {
            val amount = data[day] ?: 0.0
            val barHeight = (amount / maxVal * 100).toInt()

            val bar = View(requireContext()).apply {
                val heightPx = (barHeight * resources.displayMetrics.density).toInt().coerceAtLeast(2)
                layoutParams = LinearLayout.LayoutParams(12.dpToPx(), heightPx).apply {
                    setMargins(2.dpToPx(), 0, 2.dpToPx(), 0)
                }
                setBackgroundColor(requireContext().getColor(colorRes))
            }
            chartLayout.addView(bar)
        }

        hsv.addView(chartLayout)
        container.addView(hsv)
        layoutGraphs.addView(container)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupDetailedBreakdown(transactions: List<com.budget.app.models.Transaction>) {
        layoutBreakdown.removeAllViews()

        // 1. Budget Goal Status
        addSectionHeader("Budget Goal Status (Period)")
        val budgetGoals = AppData.getBudgetGoals()
        if (budgetGoals.isEmpty()) {
            addEmptyMessage("No budget goals set.")
        } else {
            val spentMap = transactions.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { t -> t.amount } }

            budgetGoals.forEach { goal ->
                val spent = spentMap[goal.category] ?: 0.0
                addBudgetRow(goal.category, spent, goal.limitAmount, R.color.expense_red)
            }
        }

        // 2. Income Breakdown
        addSectionHeader("Income Breakdown")
        addTypeBreakdown(TransactionType.INCOME, transactions, R.color.income_green)

        // 3. Expense Breakdown
        addSectionHeader("Expense Breakdown")
        addTypeBreakdown(TransactionType.EXPENSE, transactions, R.color.expense_red)

        // 4. Savings Breakdown
        addSectionHeader("Savings Breakdown")
        addTypeBreakdown(TransactionType.SAVINGS, transactions, R.color.colorPrimary)

        // 5. Debt Breakdown
        addSectionHeader("Debt Breakdown")
        addDebtBreakdown(transactions)
    }

    private fun addSectionHeader(title: String) {
        val tv = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(requireContext().getColor(R.color.text_primary))
            setPadding(0, 24.dpToPx(), 0, 8.dpToPx())
        }
        layoutBreakdown.addView(tv)
    }

    private fun addEmptyMessage(message: String) {
        val tv = TextView(requireContext()).apply {
            text = message
            setTextColor(Color.GRAY)
            setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
        }
        layoutBreakdown.addView(tv)
    }

    private fun addBudgetRow(category: String, spent: Double, limit: Double, colorRes: Int) {
        val row = layoutInflater.inflate(R.layout.item_category_bar, layoutBreakdown, false)
        row.findViewById<TextView>(R.id.tvBarCategory).text = category

        val percent = if (limit > 0) ((spent / limit) * 100).toInt() else 0
        row.findViewById<TextView>(R.id.tvBarPercent).text = "$percent%"

        row.findViewById<TextView>(R.id.tvBarAmount).text =
            "${CurrencyFormatter.format(spent)} / ${CurrencyFormatter.format(limit)}"

        val bar = row.findViewById<ProgressBar>(R.id.pbCategory)
        bar.progress = percent.coerceAtMost(100)
        bar.progressTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(colorRes))

        row.isClickable = true
        row.isFocusable = true
        row.setBackgroundResource(android.R.drawable.list_selector_background)
        row.setOnClickListener {
            val fragment = AddTransactionFragment.newInstance(TransactionType.EXPENSE, category)
            (activity as? MainActivity)?.loadFragment(fragment)
        }

        layoutBreakdown.addView(row)
    }

    private fun addTypeBreakdown(type: TransactionType, transactions: List<com.budget.app.models.Transaction>, colorRes: Int) {
        val filtered = transactions.filter { it.type == type }
        val byCategory = filtered.groupBy { it.category }

        if (byCategory.isEmpty()) {
            addEmptyMessage("No $type transactions.")
            return
        }

        byCategory.forEach { (category, txs) ->
            val total = txs.sumOf { it.amount }

            val row = layoutInflater.inflate(R.layout.item_category_bar, layoutBreakdown, false)
            row.findViewById<TextView>(R.id.tvBarCategory).text = category
            row.findViewById<TextView>(R.id.tvBarPercent).text = ""
            row.findViewById<TextView>(R.id.tvBarAmount).text = CurrencyFormatter.format(total)

            val bar = row.findViewById<ProgressBar>(R.id.pbCategory)
            bar.progress = 100
            bar.progressTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(colorRes))

            row.isClickable = true
            row.isFocusable = true
            row.setBackgroundResource(android.R.drawable.list_selector_background)
            row.setOnClickListener {
                val fragment = AddTransactionFragment.newInstance(type, category)
                (activity as? MainActivity)?.loadFragment(fragment)
            }

            layoutBreakdown.addView(row)

            txs.forEach { tx ->
                val txTv = TextView(requireContext()).apply {
                    text = "  • ${tx.title}: ${CurrencyFormatter.format(tx.amount)}"
                    textSize = 12f
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                    setPadding(32.dpToPx(), 2.dpToPx(), 0, 2.dpToPx())
                }
                layoutBreakdown.addView(txTv)
            }
        }
    }

    private fun addDebtBreakdown(transactions: List<com.budget.app.models.Transaction>) {
        val debts = AppData.getDebts()
        val debtTxs = transactions.filter { it.category == "Debt Payment" }

        if (debts.isEmpty() && debtTxs.isEmpty()) {
            addEmptyMessage("No debt data.")
            return
        }

        debts.forEach { debt ->
            val row = layoutInflater.inflate(R.layout.item_category_bar, layoutBreakdown, false)
            row.findViewById<TextView>(R.id.tvBarCategory).text = debt.name

            val totalPaid = debt.amount - debt.remainingAmount
            val percent = if (debt.amount > 0) ((totalPaid / debt.amount) * 100).toInt() else 0

            row.findViewById<TextView>(R.id.tvBarPercent).text = "$percent%"
            row.findViewById<TextView>(R.id.tvBarAmount).text = "${CurrencyFormatter.format(debt.remainingAmount)} remaining"

            val bar = row.findViewById<ProgressBar>(R.id.pbCategory)
            bar.progress = percent
            bar.progressTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.expense_red))

            row.isClickable = true
            row.isFocusable = true
            row.setBackgroundResource(android.R.drawable.list_selector_background)
            row.setOnClickListener {
                val fragment = AddTransactionFragment.newInstance(TransactionType.EXPENSE, "Debt Payment")
                (activity as? MainActivity)?.loadFragment(fragment)
            }

            layoutBreakdown.addView(row)
        }

        if (debtTxs.isNotEmpty()) {
            val header = TextView(requireContext()).apply {
                text = "Period Debt Payments:"
                textSize = 12f
                setTypeface(null, Typeface.ITALIC)
                setPadding(16.dpToPx(), 8.dpToPx(), 0, 4.dpToPx())
            }
            layoutBreakdown.addView(header)

            debtTxs.forEach { tx ->
                val txTv = TextView(requireContext()).apply {
                    text = "  • ${tx.title}: ${CurrencyFormatter.format(tx.amount)}"
                    textSize = 12f
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                    setPadding(16.dpToPx(), 2.dpToPx(), 0, 2.dpToPx())
                }
                layoutBreakdown.addView(txTv)
            }
        }
    }
}