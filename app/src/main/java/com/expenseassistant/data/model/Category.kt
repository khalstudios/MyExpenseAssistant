package com.expenseassistant.data.model

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
    MAINTENANCE("Maintenance"),
    PERSONAL_CARE("Personal Care"),
    HOBBIES("Hobbies"),
    INCOME("Income"),
    OTHER("Unknown");

    companion object {
        fun fromName(value: String?): Category =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
