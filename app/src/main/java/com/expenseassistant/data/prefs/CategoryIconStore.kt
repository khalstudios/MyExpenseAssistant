package com.expenseassistant.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists per-category icon overrides chosen by the user, keyed by Category enum name. */
class CategoryIconStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("category-icons", Context.MODE_PRIVATE)

    private val _overrides = MutableStateFlow(loadAll())
    val overrides: StateFlow<Map<String, String>> = _overrides.asStateFlow()

    fun setIcon(categoryKey: String, iconKey: String) {
        prefs.edit().putString(categoryKey, iconKey).apply()
        _overrides.value = _overrides.value + (categoryKey to iconKey)
    }

    fun clearIcon(categoryKey: String) {
        prefs.edit().remove(categoryKey).apply()
        _overrides.value = _overrides.value - categoryKey
    }

    private fun loadAll(): Map<String, String> =
        prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()
}
