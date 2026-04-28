package com.budget.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit // Necessary import for KTX extension

object SessionManager {
    private const val PREF_NAME = "BudgetAppSession"
    private const val KEY_USER_ID = "current_user_id"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setUserId(context: Context, userId: Int) {
        // The edit { } block automatically handles .edit() and .apply()
        getPreferences(context).edit {
            putInt(KEY_USER_ID, userId)
        }
    }

    fun getUserId(context: Context): Int {
        return getPreferences(context).getInt(KEY_USER_ID, -1)
    }
}