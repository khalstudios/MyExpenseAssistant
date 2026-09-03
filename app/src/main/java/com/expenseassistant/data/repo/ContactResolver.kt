package com.expenseassistant.data.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class ContactResolver(private val context: Context) {

    fun hasAccess(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun resolve(merchant: String?): String? {
        if (!hasAccess()) return null

        return merchant?.trim()?.takeIf { it.isNotEmpty() }?.let(::lookupSimilarName)
    }

    private fun lookupSimilarName(merchant: String): String? {
        val uri = ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon()
            .appendPath(merchant)
            .build()
        val matches = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
                }
            }
        }.orEmpty().distinct()

        val ranked = matches.map { name -> name to similarityScore(merchant, name) }
            .filter { (_, score) -> score >= MINIMUM_SIMILARITY }
            .sortedByDescending { (_, score) -> score }
        val best = ranked.firstOrNull() ?: return null
        val runnerUp = ranked.getOrNull(1)
        return best.first.takeIf { runnerUp == null || best.second - runnerUp.second >= MINIMUM_LEAD }
    }

    private fun similarityScore(merchant: String, contact: String): Float {
        val merchantTokens = tokens(merchant)
        val contactTokens = tokens(contact)
        if (merchantTokens.isEmpty() || contactTokens.isEmpty()) return 0f
        if (merchantTokens == contactTokens) return 1f

        val shared = merchantTokens.intersect(contactTokens.toSet()).size
        val coverage = shared.toFloat() / merchantTokens.size
        val precision = shared.toFloat() / contactTokens.size
        return (coverage * 0.7f) + (precision * 0.3f)
    }

    private fun tokens(value: String): List<String> = value.lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 2 }
        .filter { it !in IGNORED_TOKENS }

    private companion object {
        const val MINIMUM_SIMILARITY = 0.8f
        const val MINIMUM_LEAD = 0.15f
        val IGNORED_TOKENS = setOf("upi", "pay", "payments", "payment", "bank", "india")
    }
}
