package com.expenseassistant.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.TransactionEntity

/** User-chosen icon overrides, keyed by Category enum name; provided once near the app root. */
val LocalCategoryIconOverrides = compositionLocalOf<Map<String, String>> { emptyMap() }

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
        Category.FRIENDS_AND_FAMILY -> Icons.Filled.Groups
        Category.EMI -> Icons.Filled.AccountBalance
        Category.TAXES -> Icons.Filled.RequestQuote
        Category.INSURANCE -> Icons.Filled.Shield
        Category.GIFTS_AND_DONATION -> Icons.Filled.CardGiftcard
        Category.MAINTENANCE -> Icons.Filled.Handyman
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

/** Stable per-category colour so the pie chart, legend and list icons always agree. */
val Category.color: Color
    get() = when (this) {
        Category.FOOD_AND_DRINK -> Color(0xFFFF9800)
        Category.GROCERIES -> Color(0xFF66BB6A)
        Category.TRANSPORT -> Color(0xFF29B6F6)
        Category.FUEL -> Color(0xFF26A69A)
        Category.SHOPPING -> Color(0xFF42A5F5)
        Category.BILLS_AND_UTILITIES -> Color(0xFFEF5350)
        Category.RENT -> Color(0xFF7E57C2)
        Category.ENTERTAINMENT -> Color(0xFFEC407A)
        Category.HEALTH -> Color(0xFFE53935)
        Category.EDUCATION -> Color(0xFFAB47BC)
        Category.TRAVEL -> Color(0xFF29B6F6)
        Category.INVESTMENTS -> Color(0xFF9CCC65)
        Category.TRANSFER -> Color(0xFFEC407A)
        Category.FRIENDS_AND_FAMILY -> Color(0xFFF06292)
        Category.EMI -> Color(0xFFEF5350)
        Category.TAXES -> Color(0xFF78909C)
        Category.INSURANCE -> Color(0xFF5C6BC0)
        Category.GIFTS_AND_DONATION -> Color(0xFFF48FB1)
        Category.MAINTENANCE -> Color(0xFFFFA726)
        Category.PERSONAL_CARE -> Color(0xFF4DD0E1)
        Category.HOBBIES -> Color(0xFFBA68C8)
        Category.INCOME -> Color(0xFF66BB6A)
        Category.OTHER -> Color(0xFF90A4AE)
    }

/** Custom categories (user-created) override the built-in name/colour but keep a shared icon. */
val TransactionEntity.displayCategoryName: String
    get() = customCategoryName ?: category.displayName

val TransactionEntity.displayCategoryColor: Color
    get() = customCategoryColor?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    } ?: category.color

val TransactionEntity.displayCategoryIcon: ImageVector
    @Composable get() = when {
        customCategoryIcon != null -> CategoryIconCatalog.iconFor(customCategoryIcon)
        customCategoryName != null -> Icons.Filled.Label
        else -> category.resolvedIcon()
    }
