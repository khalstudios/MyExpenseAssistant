package com.khaltech.expenseassistant.data.prefs

import android.content.Context
import com.khaltech.expenseassistant.data.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists per-category colour overrides chosen by the user, keyed by Category enum name, as
 * "#RRGGBB". The counterpart of [CategoryIconStore]; categories the user made carry their colour on
 * their transactions instead, so they never appear here.
 */
class CategoryColorStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("category-colors", Context.MODE_PRIVATE)

    private val _overrides = MutableStateFlow(loadAll())
    val overrides: StateFlow<Map<String, String>> = _overrides.asStateFlow()

    fun setColor(categoryKey: String, colorHex: String) {
        prefs.edit().putString(categoryKey, colorHex).apply()
        _overrides.value = _overrides.value + (categoryKey to colorHex)
    }

    fun clearColor(categoryKey: String) {
        prefs.edit().remove(categoryKey).apply()
        _overrides.value = _overrides.value - categoryKey
    }

    fun replaceAll(overrides: Map<String, String>) {
        prefs.edit().clear().apply {
            overrides.forEach { (categoryKey, colorHex) -> putString(categoryKey, colorHex) }
        }.apply()
        _overrides.value = overrides
    }

    /**
     * Choices saved under a category that has since been retired move to its successor the first
     * time they are read, and are written back that way so the move happens only once.
     */
    private fun loadAll(): Map<String, String> {
        val stored = prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()
        val current = Category.currentKeys(stored)
        if (current != stored) {
            prefs.edit().clear().apply { current.forEach { (key, value) -> putString(key, value) } }.apply()
        }
        return current
    }
}
