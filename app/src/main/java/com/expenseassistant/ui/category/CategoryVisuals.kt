package com.expenseassistant.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.expenseassistant.data.model.Category

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
        Category.INCOME -> Icons.Filled.Payments
        Category.OTHER -> Icons.AutoMirrored.Filled.HelpOutline
    }

/** Stable per-category colour so the pie chart, legend and list icons always agree. */
val Category.color: Color
    get() = when (this) {
        Category.FOOD_AND_DRINK -> Color(0xFFEF6C00)
        Category.GROCERIES -> Color(0xFF2E7D32)
        Category.TRANSPORT -> Color(0xFF1565C0)
        Category.FUEL -> Color(0xFF6D4C41)
        Category.SHOPPING -> Color(0xFFAD1457)
        Category.BILLS_AND_UTILITIES -> Color(0xFF00838F)
        Category.RENT -> Color(0xFF4527A0)
        Category.ENTERTAINMENT -> Color(0xFFD81B60)
        Category.HEALTH -> Color(0xFFC62828)
        Category.EDUCATION -> Color(0xFF0277BD)
        Category.TRAVEL -> Color(0xFF00695C)
        Category.INVESTMENTS -> Color(0xFF558B2F)
        Category.TRANSFER -> Color(0xFF5D4037)
        Category.INCOME -> Color(0xFF2E7D32)
        Category.OTHER -> Color(0xFF616161)
    }
