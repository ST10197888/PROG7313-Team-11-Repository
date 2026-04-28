package com.budget.app.models

import java.util.Date

enum class TransactionType { INCOME, EXPENSE, SAVINGS }

data class Transaction(
    val id: Int,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: Date = Date(),
    val notes: String = "",
    val attachmentUri: String? = null,
    val attachmentName: String? = null
)

data class BudgetGoal(
    val category: String,
    var limitAmount: Double,          // maximum monthly goal
    var minAmount: Double = 0.0,      // minimum monthly goal (Kennedy added)
    var spentAmount: Double = 0.0
) {
    val remainingAmount: Double get() = limitAmount - spentAmount
    val percentageUsed: Int get() = if (limitAmount > 0) ((spentAmount / limitAmount) * 100).toInt().coerceAtMost(100) else 0
    val isBelowMinimum: Boolean get() = minAmount > 0 && spentAmount < minAmount
    val isAboveMaximum: Boolean get() = spentAmount > limitAmount
}

data class FinancialGoal(
    val id: Int,
    val name: String,
    val targetAmount: Double,
    var currentAmount: Double = 0.0,
    val deadline: Date
) {
    val progress: Int get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt().coerceAtMost(100) else 0
}

data class Debt(
    val id: Int,
    val name: String,
    val amount: Double,
    val interestRate: Double,
    val minPayment: Double,
    var remainingAmount: Double
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    var isUnlocked: Boolean = false
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val password: String
)