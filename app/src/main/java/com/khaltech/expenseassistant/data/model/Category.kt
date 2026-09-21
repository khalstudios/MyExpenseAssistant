package com.khaltech.expenseassistant.data.model

/**
 * Where a transaction is filed.
 *
 * [isExtended] marks the narrower categories the default picker does not offer. They are still real
 * categories in every other respect — their own icon and colour, counted in analytics, kept by
 * anyone already using them — so nothing about capture is weakened by the distinction.
 *
 * **An extended category must be one the categoriser never assigns on its own.** Free users are not
 * stopped from being filed under a category, only from choosing it; so the moment a category can be
 * assigned automatically it will appear in its own right on someone's transactions, unlock itself
 * for them, and make the paywall's claim about it false. Travel, House Expense and Vehicle Expense
 * are in the default set for exactly this reason: `MerchantKeywords` matches IRCTC, plumbers and
 * garages, so they were never really withheld. Before adding keyword rules for a category here,
 * move it out of the extended set — or the rule will quietly give it away.
 *
 * Deliberately not deleted. Retiring a category would mean rewriting every transaction, rule and
 * budget that referenced it, and telling a user their own filing was wrong. Anyone already using
 * one keeps it, and Pro offers the rest.
 */
enum class Category(val displayName: String, val isExtended: Boolean = false) {
    FOOD_AND_DRINK("Food & Drink"),
    GROCERIES("Groceries"),
    TRANSPORT("Transport"),
    FUEL("Fuel"),
    VEHICLE_EXPENSE("Vehicle Expense"),
    SHOPPING("Shopping"),
    BILLS_AND_UTILITIES("Bills & Utilities"),
    RENT("Rent"),
    HOUSE_EXPENSE("House Expense"),
    ENTERTAINMENT("Entertainment"),
    TRAVEL("Travel"),
    HEALTH("Health"),
    EDUCATION("Education"),
    INVESTMENTS("Investments"),
    TRANSFER("People"),
    EMI("EMIs"),
    TAXES("Taxes"),
    INCOME("Income"),
    OTHER("Unknown"),

    // Offered with Pro, or to anyone whose data already uses them. None of these is reachable by
    // the categoriser: no keyword rule names one, and the heuristics only ever reach Income,
    // People and Unknown. Picking them by hand is the only way in, which is what makes them Pro.
    FRIENDS_AND_FAMILY("Friends/Family", isExtended = true),
    PERSONAL_CARE("Personal Care", isExtended = true),
    HOBBIES("Hobbies", isExtended = true),
    INSURANCE("Insurance", isExtended = true),
    GIFTS_AND_DONATION("Gifts/Donation", isExtended = true);

    companion object {
        /** Offered to everyone, in the order the picker lays them out. */
        val Default: List<Category> = entries.filter { !it.isExtended }

        /** The narrower set, offered with Pro. */
        val Extended: List<Category> = entries.filter { it.isExtended }

        /**
         * Names earlier versions stored, mapped to the category that replaced them. Maintenance was
         * split into House and Vehicle Expense; with no way to tell which it was, it becomes House.
         */
        private val LEGACY_NAMES = mapOf(
            "MAINTENANCE" to HOUSE_EXPENSE,
            "HOUSE_MAINTENANCE" to HOUSE_EXPENSE,
            "VEHICLE_MAINTENANCE" to VEHICLE_EXPENSE,
        )

        fun fromName(value: String?): Category =
            entries.firstOrNull { it.name == value } ?: LEGACY_NAMES[value] ?: OTHER

        /** Rewrites a stored category key (budgets, icon overrides) from a retired name to its successor. */
        fun currentKey(key: String): String = LEGACY_NAMES[key]?.name ?: key
    }
}
