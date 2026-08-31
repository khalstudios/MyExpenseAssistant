package com.expenseassistant.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.expenseassistant.data.model.Category

/** The full set of icons a user can pick from for any category, built-in or custom. */
object CategoryIconCatalog {

    val options: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Filled.Restaurant,
        "cafe" to Icons.Filled.LocalCafe,
        "groceries" to Icons.Filled.LocalGroceryStore,
        "transport" to Icons.Filled.DirectionsBus,
        "car" to Icons.Filled.DirectionsCar,
        "fuel" to Icons.Filled.LocalGasStation,
        "shopping" to Icons.Filled.ShoppingBag,
        "bills" to Icons.AutoMirrored.Filled.ReceiptLong,
        "home" to Icons.Filled.Home,
        "repair" to Icons.Filled.HomeRepairService,
        "handyman" to Icons.Filled.Handyman,
        "spa" to Icons.Filled.Spa,
        "palette" to Icons.Filled.Palette,
        "movie" to Icons.Filled.Movie,
        "games" to Icons.Filled.SportsEsports,
        "health" to Icons.Filled.MedicalServices,
        "hospital" to Icons.Filled.LocalHospital,
        "fitness" to Icons.Filled.FitnessCenter,
        "school" to Icons.Filled.School,
        "flight" to Icons.Filled.Flight,
        "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
        "savings" to Icons.Filled.Savings,
        "wallet" to Icons.Filled.Wallet,
        "payments" to Icons.Filled.Payments,
        "people" to Icons.Filled.People,
        "groups" to Icons.Filled.Groups,
        "child" to Icons.Filled.ChildCare,
        "pets" to Icons.Filled.Pets,
        "celebration" to Icons.Filled.Celebration,
        "gift" to Icons.Filled.CardGiftcard,
        "bank" to Icons.Filled.AccountBalance,
        "shield" to Icons.Filled.Shield,
        "tax" to Icons.Filled.RequestQuote,
        "build" to Icons.Filled.Build,
        "percent" to Icons.Filled.Percent,
        "cash" to Icons.Filled.Money,
        "label" to Icons.Filled.Label,
        "help" to Icons.AutoMirrored.Filled.HelpOutline,
    )

    fun iconFor(key: String?): ImageVector = options.firstOrNull { it.first == key }?.second ?: Icons.Filled.Label

    /** The catalog key matching a built-in category's default icon, so the edit dialog can preselect it. */
    fun defaultKeyFor(category: Category): String = when (category) {
        Category.FOOD_AND_DRINK -> "restaurant"
        Category.GROCERIES -> "groceries"
        Category.TRANSPORT -> "transport"
        Category.FUEL -> "fuel"
        Category.SHOPPING -> "shopping"
        Category.BILLS_AND_UTILITIES -> "bills"
        Category.RENT -> "home"
        Category.ENTERTAINMENT -> "movie"
        Category.HEALTH -> "health"
        Category.EDUCATION -> "school"
        Category.TRAVEL -> "flight"
        Category.INVESTMENTS -> "trending_up"
        Category.TRANSFER -> "people"
        Category.FRIENDS_AND_FAMILY -> "groups"
        Category.EMI -> "bank"
        Category.TAXES -> "tax"
        Category.INSURANCE -> "shield"
        Category.GIFTS_AND_DONATION -> "gift"
        Category.MAINTENANCE -> "handyman"
        Category.PERSONAL_CARE -> "spa"
        Category.HOBBIES -> "palette"
        Category.INCOME -> "payments"
        Category.OTHER -> "help"
    }
}
