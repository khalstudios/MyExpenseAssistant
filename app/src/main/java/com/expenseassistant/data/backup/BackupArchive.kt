package com.expenseassistant.data.backup

import androidx.room.withTransaction
import com.expenseassistant.data.local.AppDatabase
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.MerchantRule
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.data.prefs.CategoryIconStore
import com.expenseassistant.data.prefs.UserPreferences
import com.expenseassistant.data.prefs.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupArchive(
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val categoryIcons: CategoryIconStore,
) {
    suspend fun export(): String = JSONObject().apply {
        put("formatVersion", FORMAT_VERSION)
        put("transactions", JSONArray(database.transactionDao().allOnce().map(::transactionToJson)))
        put("merchantRules", JSONArray(database.merchantRuleDao().all().map(::ruleToJson)))
        put("budgets", JSONArray(database.budgetDao().allOnce().map(::budgetToJson)))
        put("profile", profileToJson(preferences.load()))
        put("categoryIcons", JSONObject(categoryIcons.overrides.value))
    }.toString(2)

    suspend fun restore(contents: String) {
        val archive = JSONObject(contents)
        require(archive.optInt("formatVersion", -1) == FORMAT_VERSION) { "Unsupported backup format" }

        val transactions = archive.requiredArray("transactions").map(::transactionFromJson)
        val rules = archive.requiredArray("merchantRules").map(::ruleFromJson)
        val budgets = archive.requiredArray("budgets").map(::budgetFromJson)
        val profile = profileFromJson(archive.getJSONObject("profile"))
        val icons = archive.getJSONObject("categoryIcons").keys().asSequence()
            .associateWith { key -> archive.getJSONObject("categoryIcons").getString(key) }

        database.withTransaction {
            database.transactionDao().deleteAll()
            database.merchantRuleDao().deleteAll()
            database.budgetDao().deleteAll()
            database.transactionDao().insertAll(transactions)
            database.merchantRuleDao().upsertAll(rules)
            database.budgetDao().upsertAll(budgets)
        }
        preferences.save(profile)
        categoryIcons.replaceAll(icons)
    }

    private fun transactionToJson(transaction: TransactionEntity) = JSONObject().apply {
        put("id", transaction.id)
        put("amountMinor", transaction.amountMinor)
        put("currency", transaction.currency)
        put("direction", transaction.direction.name)
        put("merchantRaw", transaction.merchantRaw)
        put("merchant", transaction.merchant)
        put("category", transaction.category.name)
        put("categoryConfidence", transaction.categoryConfidence.toDouble())
        put("sourcePackage", transaction.sourcePackage)
        put("sourceApp", transaction.sourceApp)
        put("captureSource", transaction.captureSource.name)
        put("rawText", transaction.rawText)
        put("referenceId", transaction.referenceId)
        put("occurredAt", transaction.occurredAt)
        put("createdAt", transaction.createdAt)
        put("description", transaction.description)
        put("tags", JSONArray(transaction.tags))
        put("paymentMode", transaction.paymentMode.name)
        put("userCorrected", transaction.userCorrected)
        put("dedupeKey", transaction.dedupeKey)
        put("customCategoryName", transaction.customCategoryName)
        put("customCategoryColor", transaction.customCategoryColor)
        put("customCategoryIcon", transaction.customCategoryIcon)
    }

    private fun transactionFromJson(json: JSONObject) = TransactionEntity(
        id = json.getLong("id"), amountMinor = json.getLong("amountMinor"), currency = json.getString("currency"),
        direction = enumValueOf(json.getString("direction")), merchantRaw = json.nullableString("merchantRaw"),
        merchant = json.getString("merchant"), category = enumValueOf(json.getString("category")),
        categoryConfidence = json.getDouble("categoryConfidence").toFloat(), sourcePackage = json.nullableString("sourcePackage"),
        sourceApp = json.getString("sourceApp"), captureSource = enumValueOf(json.getString("captureSource")),
        rawText = json.getString("rawText"), referenceId = json.nullableString("referenceId"),
        occurredAt = json.getLong("occurredAt"), createdAt = json.getLong("createdAt"),
        description = json.nullableString("description"), tags = json.requiredArray("tags").strings(),
        paymentMode = enumValueOf(json.getString("paymentMode")), userCorrected = json.getBoolean("userCorrected"),
        dedupeKey = json.getString("dedupeKey"), customCategoryName = json.nullableString("customCategoryName"),
        customCategoryColor = json.nullableString("customCategoryColor"), customCategoryIcon = json.nullableString("customCategoryIcon"),
    )

    private fun ruleToJson(rule: MerchantRule) = JSONObject().apply {
        put("merchantKey", rule.merchantKey); put("category", rule.category.name); put("displayName", rule.displayName)
        put("hitCount", rule.hitCount); put("updatedAt", rule.updatedAt)
    }

    private fun ruleFromJson(json: JSONObject) = MerchantRule(
        merchantKey = json.getString("merchantKey"), category = enumValueOf(json.getString("category")),
        displayName = json.nullableString("displayName"), hitCount = json.getInt("hitCount"), updatedAt = json.getLong("updatedAt"),
    )

    private fun budgetToJson(budget: BudgetEntity) = JSONObject().apply {
        put("categoryKey", budget.categoryKey); put("limitMinor", budget.limitMinor); put("updatedAt", budget.updatedAt)
    }

    private fun budgetFromJson(json: JSONObject) = BudgetEntity(
        categoryKey = json.getString("categoryKey"), limitMinor = json.getLong("limitMinor"), updatedAt = json.getLong("updatedAt"),
    )

    private fun profileToJson(profile: UserProfile) = JSONObject().apply {
        put("name", profile.name); put("email", profile.email); put("monthlyIncomeMinor", profile.monthlyIncomeMinor)
    }

    private fun profileFromJson(json: JSONObject) = UserProfile(
        name = json.getString("name"), email = json.getString("email"), monthlyIncomeMinor = json.getLong("monthlyIncomeMinor"),
    )

    private fun JSONObject.requiredArray(key: String): JSONArray = getJSONArray(key)

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONArray.strings(): List<String> = List(length()) { index -> getString(index) }

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)

    companion object {
        private const val FORMAT_VERSION = 1
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)

        fun fileName(prefix: String = "expense-assistant-backup"): String =
            "$prefix-${DATE_FORMAT.format(Date())}.json"
    }
}