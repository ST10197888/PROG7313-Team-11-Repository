package com.budget.app.database

import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    fun login(email: String, password: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAll(): List<UserEntity>
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tx: TransactionEntity): Long

    @Delete
    fun delete(tx: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllForUser(userId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun getForUserInRange(userId: Int, from: Long, to: Long): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    fun deleteById(id: Int)
}

@Dao
interface BudgetGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(goal: BudgetGoalEntity)

    @Query("SELECT * FROM budget_goals WHERE userId = :userId")
    fun getAllForUser(userId: Int): List<BudgetGoalEntity>

    @Query("DELETE FROM budget_goals WHERE category = :category AND userId = :userId")
    fun deleteByCategory(category: String, userId: Int)
}

@Dao
interface FinancialGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(goal: FinancialGoalEntity): Long

    @Query("SELECT * FROM financial_goals WHERE userId = :userId")
    fun getAllForUser(userId: Int): List<FinancialGoalEntity>

    @Query("UPDATE financial_goals SET currentAmount = currentAmount + :amount WHERE id = :id")
    fun addProgress(id: Int, amount: Double)
}

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(debt: DebtEntity): Long

    @Query("SELECT * FROM debts WHERE userId = :userId")
    fun getAllForUser(userId: Int): List<DebtEntity>

    @Query("UPDATE debts SET remainingAmount = :remaining WHERE id = :id")
    fun updateRemaining(id: Int, remaining: Double)
}