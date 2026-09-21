package com.khaltech.expenseassistant.data.backup

import androidx.room.withTransaction
import com.khaltech.expenseassistant.data.local.AppDatabase
import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.BudgetEntity
import com.khaltech.expenseassistant.data.model.BudgetPeriod
import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.MerchantRule
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.data.model.RecurringDismissal
import com.khaltech.expenseassistant.data.model.RecurringPlanEntity
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.model.TransactionType
import com.khaltech.expenseassistant.data.prefs.CategoryColorStore
import com.khaltech.expenseassistant.data.prefs.CategoryIconStore
import com.khaltech.expenseassistant.data.prefs.UserPreferences
import com.khaltech.expenseassistant.data.prefs.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupArchive(
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val categoryIcons: CategoryIconStore,
    private val categoryColors: CategoryColorStore,
) {
    suspend fun export(): String = JSONObject().apply {
        put("formatVersion", FORMAT_VERSION)
        put("transactions", JSONArray(database.transactionDao().allOnce().map(::transactionToJson)))
        put("merchantRules", JSONArray(database.merchantRuleDao().all().map(::ruleToJson)))
        put("budgets", JSONArray(database.budgetDao().allOnce().map(::budgetToJson)))
        put("recurringPlans", JSONArray(database.recurringPlanDao().allOnce().map(::recurringPlanToJson)))
        put("recurringDismissals", JSONArray(database.recurringDismissalDao().allOnce().map(::dismissalToJson)))
        put("profile", profileToJson(preferences.load()))
        put("categoryIcons", JSONObject(categoryIcons.overrides.value))
        put("categoryColors", JSONObject(categoryColors.overrides.value))
    }.toString(2)

    suspend fun restore(contents: String) {
        val archive = JSONObject(contents)
        require(archive.optInt("formatVersion", -1) == FORMAT_VERSION) { "Unsupported backup format" }

        val transactions = archive.requiredArray("transactions").map(::transactionFromJson)
        val rules = archive.requiredArray("merchantRules").map(::ruleFromJson)
        // Two retired categories can collapse onto one key, and the key plus its period is the
        // budgets' primary key. Where they do, the limit set most recently is the one kept.
        val budgets = archive.requiredArray("budgets").map(::budgetFromJson)
            .sortedByDescending { it.updatedAt }
            .distinctBy { it.categoryKey to it.period }
        val recurringPlans = archive.optionalArray("recurringPlans")?.map(::recurringPlanFromJson).orEmpty()
        val dismissals = archive.optionalArray("recurringDismissals")?.map(::dismissalFromJson).orEmpty()
        val profile = profileFromJson(archive.getJSONObject("profile"))
        val icons = Category.currentKeys(
            archive.getJSONObject("categoryIcons").keys().asSequence()
                .associateWith { key -> archive.getJSONObject("categoryIcons").getString(key) },
        )
        // Absent from backups made before colours could be changed, which restore as the defaults.
        val colors = Category.currentKeys(
            archive.optJSONObject("categoryColors")?.let { json ->
                json.keys().asSequence().associateWith { key -> json.getString(key) }
            }.orEmpty(),
        )

        database.withTransaction {
            database.transactionDao().deleteAll()
            database.merchantRuleDao().deleteAll()
            database.budgetDao().deleteAll()
            database.recurringPlanDao().deleteAll()
            database.recurringDismissalDao().deleteAll()
            database.transactionDao().insertAll(transactions)
            database.merchantRuleDao().upsertAll(rules)
            database.budgetDao().upsertAll(budgets)
            database.recurringPlanDao().upsertAll(recurringPlans)
            database.recurringDismissalDao().upsertAll(dismissals)
        }
        preferences.save(profile)
        categoryIcons.replaceAll(icons)
        categoryColors.replaceAll(colors)
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
        put("bankName", transaction.bankName)
        put("accountType", transaction.accountType?.name)
        put("accountLast4", transaction.accountLast4)
        put("transactionType", transaction.transactionType?.name)
        put("availableBalanceMinor", transaction.availableBalanceMinor)
    }

    private fun transactionFromJson(json: JSONObject) = TransactionEntity(
        id = json.getLong("id"), amountMinor = json.getLong("amountMinor"), currency = json.getString("currency"),
        direction = enumValueOf(json.getString("direction")), merchantRaw = json.nullableString("merchantRaw"),
        merchant = json.getString("merchant"), category = Category.fromName(json.getString("category")),
        categoryConfidence = json.getDouble("categoryConfidence").toFloat(), sourcePackage = json.nullableString("sourcePackage"),
        sourceApp = json.getString("sourceApp"), captureSource = enumValueOf(json.getString("captureSource")),
        rawText = json.getString("rawText"), referenceId = json.nullableString("referenceId"),
        occurredAt = json.getLong("occurredAt"), createdAt = json.getLong("createdAt"),
        description = json.nullableString("description"), tags = json.requiredArray("tags").strings(),
        paymentMode = enumValueOf(json.getString("paymentMode")), userCorrected = json.getBoolean("userCorrected"),
        dedupeKey = json.getString("dedupeKey"), customCategoryName = json.nullableString("customCategoryName"),
        customCategoryColor = json.nullableString("customCategoryColor"), customCategoryIcon = json.nullableString("customCategoryIcon"),
        // Backups from before bank parsing leave these keys out, which reads back as null.
        bankName = json.nullableString("bankName"), accountType = AccountType.fromName(json.nullableString("accountType")),
        accountLast4 = json.nullableString("accountLast4"),
        transactionType = TransactionType.fromName(json.nullableString("transactionType")),
        availableBalanceMinor = if (json.isNull("availableBalanceMinor")) null else json.getLong("availableBalanceMinor"),
    )

    private fun ruleToJson(rule: MerchantRule) = JSONObject().apply {
        put("merchantKey", rule.merchantKey); put("category", rule.category.name); put("displayName", rule.displayName)
        put("tags", JSONArray(rule.tags)); put("note", rule.note)
        put("hitCount", rule.hitCount); put("updatedAt", rule.updatedAt)
    }

    private fun ruleFromJson(json: JSONObject) = MerchantRule(
        merchantKey = json.getString("merchantKey"), category = Category.fromName(json.getString("category")),
        displayName = json.nullableString("displayName"), tags = json.optionalArray("tags")?.strings().orEmpty(),
        note = json.nullableString("note"), hitCount = json.getInt("hitCount"), updatedAt = json.getLong("updatedAt"),
    )

    private fun budgetToJson(budget: BudgetEntity) = JSONObject().apply {
        put("categoryKey", budget.categoryKey); put("period", budget.period.name)
        put("limitMinor", budget.limitMinor); put("updatedAt", budget.updatedAt)
    }

    /**
     * Archives written before budgets had periods carry no "period" field and hold the day's cap
     * under a reserved key; both are folded in the same way the database migration does, so an old
     * backup restores with its limits intact.
     */
    private fun budgetFromJson(json: JSONObject): BudgetEntity {
        val storedKey = json.getString("categoryKey")
        val legacyDaily = storedKey == BudgetEntity.LEGACY_DAILY
        return BudgetEntity(
            categoryKey = if (legacyDaily) BudgetEntity.OVERALL else Category.currentKey(storedKey),
            period = when {
                legacyDaily -> BudgetPeriod.DAILY
                else -> BudgetPeriod.fromName(json.nullableString("period"))
            },
            limitMinor = json.getLong("limitMinor"),
            updatedAt = json.getLong("updatedAt"),
        )
    }

    private fun recurringPlanToJson(plan: RecurringPlanEntity) = JSONObject().apply {
        put("id", plan.id); put("merchant", plan.merchant); put("category", plan.category.name)
        put("amountMinor", plan.amountMinor); put("cadence", plan.cadence)
        put("nextDueAt", plan.nextDueAt); put("createdAt", plan.createdAt)
    }

    private fun recurringPlanFromJson(json: JSONObject) = RecurringPlanEntity(
        id = json.getLong("id"), merchant = json.getString("merchant"),
        category = Category.fromName(json.getString("category")),
        amountMinor = json.getLong("amountMinor"), cadence = json.getString("cadence"),
        nextDueAt = json.getLong("nextDueAt"), createdAt = json.getLong("createdAt"),
    )

    private fun dismissalToJson(dismissal: RecurringDismissal) = JSONObject().apply {
        put("merchantKey", dismissal.merchantKey); put("dismissedAt", dismissal.dismissedAt)
    }

    private fun dismissalFromJson(json: JSONObject) = RecurringDismissal(
        merchantKey = json.getString("merchantKey"), dismissedAt = json.getLong("dismissedAt"),
    )

    private fun profileToJson(profile: UserProfile) = JSONObject().apply {
        put("name", profile.name); put("email", profile.email); put("monthlyIncomeMinor", profile.monthlyIncomeMinor)
    }

    private fun profileFromJson(json: JSONObject) = UserProfile(
        name = json.getString("name"), email = json.getString("email"), monthlyIncomeMinor = json.getLong("monthlyIncomeMinor"),
    )

    private fun JSONObject.requiredArray(key: String): JSONArray = getJSONArray(key)

    /**
     * Backups written before merchant rules learned tags, or before manual recurring payments
     * existed, simply leave the key out.
     */
    private fun JSONObject.optionalArray(key: String): JSONArray? = optJSONArray(key)

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONArray.strings(): List<String> = List(length()) { index -> getString(index) }

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)

    companion object {
        private const val FORMAT_VERSION = 1
        /** Locale.US so the file name is the same shape on every device, whatever the locale. */
        private val DATE_FORMAT = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        private val TIME_FORMAT = SimpleDateFormat("HHmm", Locale.US)

        /**
         * Date first, then the app name, then the time: `18-09-2026-kahan-gaya-paisa-backup-1430.json`.
         * The time keeps a second backup on the same day from colliding with the first.
         */
        fun fileName(suffix: String = "kahan-gaya-paisa-backup"): String {
            val now = Date()
            return "${DATE_FORMAT.format(now)}-$suffix-${TIME_FORMAT.format(now)}.json"
        }
    }
}