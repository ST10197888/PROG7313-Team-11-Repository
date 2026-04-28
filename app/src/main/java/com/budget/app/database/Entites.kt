package com.budget.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.budget.app.models.*
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val password: String
) {
    fun toModel() = User(id, name, email, password)
    companion object {
        fun fromModel(u: User) = UserEntity(u.id, u.name, u.email, u.password)
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String,           // TransactionType.name()
    val category: String,
    val date: Date,
    val notes: String = "",
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val userId: Int             // foreign key — which user owns this
) {
    fun toModel() = Transaction(id, title, amount, TransactionType.valueOf(type), category, date, notes, attachmentUri, attachmentName)
    companion object {
        fun fromModel(t: Transaction, userId: Int) =
            TransactionEntity(t.id, t.title, t.amount, t.type.name, t.category, t.date, t.notes, t.attachmentUri, t.attachmentName, userId)
    }
}

// BudgetGoalEntity — Andile fills in table logic, Kennedy defines the entity
@Entity(tableName = "budget_goals", primaryKeys = ["category", "userId"])
data class BudgetGoalEntity(
    val category: String,
    val limitAmount: Double,
    val minAmount: Double = 0.0,   // Kennedy added min field
    val spentAmount: Double = 0.0,
    val userId: Int
) {
    fun toModel() = BudgetGoal(category, limitAmount, minAmount, spentAmount)
    companion object {
        fun fromModel(g: BudgetGoal, userId: Int) =
            BudgetGoalEntity(g.category, g.limitAmount, g.minAmount, g.spentAmount, userId)
    }
}

// FinancialGoalEntity — Nairon fills in goal logic, Kennedy defines the entity
@Entity(tableName = "financial_goals")
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Date,
    val userId: Int
) {
    fun toModel() = FinancialGoal(id, name, targetAmount, currentAmount, deadline)
    companion object {
        fun fromModel(g: FinancialGoal, userId: Int) =
            FinancialGoalEntity(g.id, g.name, g.targetAmount, g.currentAmount, g.deadline, userId)
    }
}

// DebtEntity: Nairon fills in debt logic, Kennedy defines the entity
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val interestRate: Double,
    val minPayment: Double,
    val remainingAmount: Double,
    val userId: Int
) {
    fun toModel() = Debt(id, name, amount, interestRate, minPayment, remainingAmount)
    companion object {
        fun fromModel(d: Debt, userId: Int) =
            DebtEntity(d.id, d.name, d.amount, d.interestRate, d.minPayment, d.remainingAmount, userId)
    }
}