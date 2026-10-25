package com.khaltech.expenseassistant.data.model

/**
 * Where a transaction is filed.
 *
 * [isExtended] marks the narrower categories the default picker does not offer. They are still real
 * categories in every other respect — their own icon and colour, counted in analytics, kept by
 * anyone already using them — so nothing about capture is weakened by the distinction.
 *
 * **The categoriser must never put an extended category on a free user's payment.** Free users are
 * not stopped from being filed under a category, only from choosing it; so a category assigned
 * automatically would appear on their transactions, unlock itself for them, and make the paywall's
 * claim about it false. Most extended categories have no keyword rules at all. House Expense and
 * Vehicle Expense do — plumbers, garages — and those rules only apply with Pro: without it the
 * payment goes to the free stand-in in `MerchantKeywords.freeFallback`, which makes precise
 * automatic filing itself part of what Pro sells. Any new keyword rule for an extended category
 * needs a fallback there too; `MerchantKeywordsTest` fails the build otherwise.
 *
 * Moving a category behind Pro never takes it from anyone already using it; Pro offers the rest.
 * Removing one outright is a bigger step: a database migration has to rewrite every transaction,
 * rule, plan and budget that names it, and its old name has to stay in `LEGACY_NAMES` so backups
 * and anything else carrying it still read back as its successor.
 */
enum class Category(val displayName: String, val isExtended: Boolean = false) {
    FOOD_AND_DRINK("Food & Drink"),
    GROCERIES("Groceries"),
    TRANSPORT("Transport"),
    FUEL("Fuel"),
    VEHICLE_EXPENSE("Vehicle Expense", isExtended = true),
    SHOPPING("Shopping"),
    BILLS_AND_UTILITIES("Bills & Utilities"),
    RENT("Rent"),
    HOUSE_EXPENSE("House Expense", isExtended = true),
    ENTERTAINMENT("Entertainment"),
    TRAVEL("Travel"),
    HEALTH("Health"),
    EDUCATION("Education"),
    INVESTMENTS("Investments"),
    TRANSFER("People"),
    EMI("EMIs"),
    // Extended, though it sits here to keep its place in older layouts: nothing assigns it
    // automatically, and it is rarely part of everyday spending, so it is honestly withheld.
    TAXES("Taxes", isExtended = true),
    INCOME("Income"),
    OTHER("Unknown"),

    // Offered with Pro, or to anyone whose data already uses them. None of these is reachable by
    // the categoriser: no keyword rule names one, and the heuristics only ever reach Income,
    // People and Unknown. Picking them by hand is the only way in, which is what makes them Pro.
    FRIENDS("Friends / Relatives", isExtended = true),
    FAMILY("Family", isExtended = true),
    PERSONAL_CARE("Personal Care", isExtended = true),
    HOBBIES("Hobbies", isExtended = true),
    INSURANCE("Insurance", isExtended = true),
    GIFTS("Gifts", isExtended = true),
    DONATIONS("Donations", isExtended = true);

    companion object {
        /** Offered to everyone, in the order the picker lays them out. */
        val Default: List<Category> = entries.filter { !it.isExtended }

        /** The narrower set, offered with Pro. */
        val Extended: List<Category> = entries.filter { it.isExtended }

        /**
         * What a user can file under: the default set, the Pro set if they have it, and whatever
         * extended category their data already uses or [current] is set to, so nothing already on
         * their records is ever missing from the list.
         */
        fun offered(isPro: Boolean, inUse: Set<Category>, current: Category? = null): List<Category> =
            Default + Extended.filter { isPro || it in inUse || it == current }

        /**
         * Names earlier versions stored, mapped to the category that replaced them. Each split moves
         * everything to one half, since nothing records which half an old entry was: Maintenance to
         * House Expense, Friends/Family to Friends / Relatives, Gifts/Donation to Gifts. The database
         * rewrites these on upgrade (see AppDatabase); this catches everything else that still
         * carries an old name, such as an older backup or a stored icon choice.
         */
        private val LEGACY_NAMES = mapOf(
            "MAINTENANCE" to HOUSE_EXPENSE,
            "HOUSE_MAINTENANCE" to HOUSE_EXPENSE,
            "VEHICLE_MAINTENANCE" to VEHICLE_EXPENSE,
            "FRIENDS_AND_FAMILY" to FRIENDS,
            "GIFTS_AND_DONATION" to GIFTS,
        )

        fun fromName(value: String?): Category =
            entries.firstOrNull { it.name == value } ?: LEGACY_NAMES[value] ?: OTHER

        /** Rewrites a stored category key (budgets, icon overrides) from a retired name to its successor. */
        fun currentKey(key: String): String = LEGACY_NAMES[key]?.name ?: key

        /**
         * Icon or colour choices keyed by category, with retired keys moved to their successors. A
         * choice made for the successor itself wins, since it was chosen for that category directly.
         */
        fun currentKeys(choices: Map<String, String>): Map<String, String> {
            val result = choices.filterKeys { it !in LEGACY_NAMES }.toMutableMap()
            choices.forEach { (key, value) ->
                LEGACY_NAMES[key]?.let { successor -> result.putIfAbsent(successor.name, value) }
            }
            return result
        }
    }
}
