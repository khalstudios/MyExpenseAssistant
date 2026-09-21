package com.khaltech.expenseassistant.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.TransactionEntity

/** User-chosen icon overrides, keyed by Category enum name; provided once near the app root. */
val LocalCategoryIconOverrides = compositionLocalOf<Map<String, String>> { emptyMap() }

/** Colours the user chose for built-in categories, keyed by enum name, as "#RRGGBB". */
val LocalCategoryColorOverrides = compositionLocalOf<Map<String, String>> { emptyMap() }

/** Null for anything that is not a colour, so a bad stored value falls back rather than crashing. */
fun colorFromHex(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

/**
 * Extended categories this user's data already mentions, provided once near the app root.
 *
 * A category is offered to whoever is already filing money under it, entitlement or not. Someone
 * who has been using Travel since before it moved behind Pro would otherwise open the picker to
 * find the category on their own transactions missing from the list they can choose — the app
 * disowning its own history. Empty is the safe default: it offers nothing extra, rather than
 * quietly offering everything if a provider is ever missed.
 */
val LocalCategoriesInUse = compositionLocalOf<Set<Category>> { emptySet() }

/**
 * Changing or removing a category the user made, provided once near the app root.
 *
 * The picker appears on six screens and none of them owns this: a custom category belongs to the
 * whole history rather than to the transaction that happens to be open. Null by default, which
 * hides the affordance rather than offering a delete that would do nothing.
 */
@Immutable
data class CustomCategoryActions(
    /** Transactions currently filed under the name, for the confirmation before it goes. */
    val countTransactions: suspend (String) -> Int,
    val delete: (String) -> Unit,
    /** Renames and restyles the category named first, on every transaction filed under it. */
    val update: (name: String, newName: String, colorHex: String, iconKey: String) -> Unit,
)

val LocalCustomCategoryActions = compositionLocalOf<CustomCategoryActions?> { null }

/**
 * How many transactions each category holds, so the picker can lead with the ones this user
 * actually files under. Empty on a fresh install, which leaves the picker in its default order.
 */
val LocalCategoryUsage = compositionLocalOf<Map<Category, Int>> { emptyMap() }

/**
 * How many categories of their own this user has made, across all their history. Read from the
 * root rather than from the picker's own list, because not every screen that opens the picker
 * passes that list, and the free limit has to count the same way everywhere.
 */
val LocalCustomCategoryCount = compositionLocalOf { 0 }

val Category.icon: ImageVector
    get() = when (this) {
        Category.FOOD_AND_DRINK -> Icons.Filled.Restaurant
        Category.GROCERIES -> Icons.Filled.LocalGroceryStore
        Category.TRANSPORT -> Icons.Filled.DirectionsBus
        Category.FUEL -> Icons.Filled.LocalGasStation
        Category.SHOPPING -> Icons.Filled.ShoppingBag
        Category.BILLS_AND_UTILITIES -> Icons.AutoMirrored.Filled.ReceiptLong
        Category.RENT -> Icons.Filled.Home
        Category.ENTERTAINMENT -> Icons.Filled.Movie
        Category.HEALTH -> Icons.Filled.MedicalServices
        Category.EDUCATION -> Icons.Filled.School
        Category.TRAVEL -> Icons.Filled.Flight
        Category.INVESTMENTS -> Icons.AutoMirrored.Filled.TrendingUp
        Category.TRANSFER -> Icons.Filled.People
        Category.FRIENDS -> Icons.Filled.Groups
        Category.FAMILY -> Icons.Filled.FamilyRestroom
        Category.GIFTS -> Icons.Filled.CardGiftcard
        Category.DONATIONS -> Icons.Filled.VolunteerActivism
        Category.EMI -> Icons.Filled.AccountBalance
        Category.TAXES -> Icons.Filled.RequestQuote
        Category.INSURANCE -> Icons.Filled.Shield
        Category.HOUSE_EXPENSE -> Icons.Filled.Roofing
        Category.VEHICLE_EXPENSE -> Icons.Filled.CarRepair
        Category.PERSONAL_CARE -> Icons.Filled.Spa
        Category.HOBBIES -> Icons.Filled.Palette
        Category.INCOME -> Icons.Filled.Payments
        Category.OTHER -> Icons.AutoMirrored.Filled.HelpOutline
    }

/** The icon actually shown for this category, honouring any user override. */
@Composable
fun Category.resolvedIcon(): ImageVector {
    val overrides = LocalCategoryIconOverrides.current
    return overrides[name]?.let { CategoryIconCatalog.iconFor(it) } ?: icon
}

/** The colour a category ships with, before anything the user chose. */
val Category.defaultColor: Color
    get() = when (this) {
        Category.FOOD_AND_DRINK -> Color(0xFFE67E22)
        Category.GROCERIES -> Color(0xFF43A047)
        Category.TRANSPORT -> Color(0xFF1E88E5)
        Category.FUEL -> Color(0xFF00897B)
        Category.SHOPPING -> Color(0xFF5C6BC0)
        Category.BILLS_AND_UTILITIES -> Color(0xFFEF5350)
        Category.RENT -> Color(0xFF8E24AA)
        Category.ENTERTAINMENT -> Color(0xFFD81B60)
        Category.HEALTH -> Color(0xFFE53935)
        Category.EDUCATION -> Color(0xFF7E57C2)
        Category.TRAVEL -> Color(0xFF00ACC1)
        Category.INVESTMENTS -> Color(0xFF7CB342)
        Category.TRANSFER -> Color(0xFF3949AB)
        Category.FRIENDS -> Color(0xFF29B6F6)
        Category.FAMILY -> Color(0xFFF06292)
        Category.GIFTS -> Color(0xFFFF8A65)
        Category.DONATIONS -> Color(0xFF9575CD)
        Category.EMI -> Color(0xFF6D4C41)
        Category.TAXES -> Color(0xFF546E7A)
        Category.INSURANCE -> Color(0xFF00838F)
        Category.HOUSE_EXPENSE -> Color(0xFFF9A825)
        Category.VEHICLE_EXPENSE -> Color(0xFF9E9D24)
        Category.PERSONAL_CARE -> Color(0xFF26A69A)
        Category.HOBBIES -> Color(0xFFAB47BC)
        Category.INCOME -> Color(0xFF2E7D32)
        Category.OTHER -> Color(0xFF78909C)
    }

/**
 * The colour actually shown for this category, honouring any the user chose, so the pie chart,
 * legend and list icons always agree. Composable because the override is read from the root.
 */
val Category.color: Color
    @Composable get() = colorFromHex(LocalCategoryColorOverrides.current[name]) ?: defaultColor

/** Custom categories (user-created) override the built-in name/colour but keep a shared icon. */
val TransactionEntity.displayCategoryName: String
    get() = customCategoryName ?: category.displayName

val TransactionEntity.displayCategoryColor: Color
    @Composable get() = colorFromHex(customCategoryColor) ?: category.color

val TransactionEntity.displayCategoryIcon: ImageVector
    @Composable get() = when {
        customCategoryIcon != null -> CategoryIconCatalog.iconFor(customCategoryIcon)
        customCategoryName != null -> Icons.AutoMirrored.Filled.Label
        else -> category.resolvedIcon()
    }
