package com.khaltech.expenseassistant.data.model

enum class Category(val displayName: String) {
    FOOD_AND_DRINK("Food & Drink"),
    GROCERIES("Groceries"),
    TRANSPORT("Transport"),
    FUEL("Fuel"),
    SHOPPING("Shopping"),
    BILLS_AND_UTILITIES("Bills & Utilities"),
    RENT("Rent"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    EDUCATION("Education"),
    TRAVEL("Travel"),
    INVESTMENTS("Investments"),
    TRANSFER("People"),
    FRIENDS_AND_FAMILY("Friends/Family"),
    EMI("EMIs"),
    TAXES("Taxes"),
    INSURANCE("Insurance"),
    GIFTS_AND_DONATION("Gifts/Donation"),
    HOUSE_EXPENSE("House Expense"),
    VEHICLE_EXPENSE("Vehicle Expense"),
    PERSONAL_CARE("Personal Care"),
    HOBBIES("Hobbies"),
    INCOME("Income"),
    OTHER("Unknown");

    companion object {
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
