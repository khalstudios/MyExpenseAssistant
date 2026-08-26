package com.expenseassistant.data.prefs

import android.content.Context

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val monthlyIncomeMinor: Long = 0,
)

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("user-profile", Context.MODE_PRIVATE)

    fun load(): UserProfile = UserProfile(
        name = prefs.getString(KEY_NAME, "").orEmpty(),
        email = prefs.getString(KEY_EMAIL, "").orEmpty(),
        monthlyIncomeMinor = prefs.getLong(KEY_INCOME, 0),
    )

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putLong(KEY_INCOME, profile.monthlyIncomeMinor)
            .apply()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_INCOME = "monthly_income_minor"
    }
}
