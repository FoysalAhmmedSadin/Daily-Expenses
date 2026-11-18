package com.example.core.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.model.AccountType

object CategoryIcons {

    val allKeys: List<String> = listOf(
        "restaurant",
        "shopping_cart",
        "directions_car",
        "home",
        "bolt",
        "local_hospital",
        "shopping_bag",
        "movie",
        "school",
        "smartphone",
        "flight",
        "payments",
        "laptop",
        "store",
        "trending_up",
        "gift",
        "work",
        "savings",
        "wallet",
        "bank",
        "credit_card",
        "category"
    )

    val paletteColors: List<Long> = listOf(
        0xFF34C759,
        0xFF0A84FF,
        0xFFFF9F0A,
        0xFFFF3B30,
        0xFFAF52DE,
        0xFF5AC8FA,
        0xFFFF2D55,
        0xFF30B0C7,
        0xFF5856D6,
        0xFF00C7BE
    )

    fun resolve(iconKey: String?): ImageVector = when (iconKey) {
        "restaurant" -> Icons.Rounded.Restaurant
        "shopping_cart" -> Icons.Rounded.ShoppingCart
        "directions_car" -> Icons.Rounded.DirectionsCar
        "home" -> Icons.Rounded.Home
        "bolt" -> Icons.Rounded.Bolt
        "local_hospital" -> Icons.Rounded.LocalHospital
        "shopping_bag" -> Icons.Rounded.ShoppingBag
        "movie" -> Icons.Rounded.Movie
        "school" -> Icons.Rounded.School
        "smartphone" -> Icons.Rounded.PhoneAndroid
        "flight" -> Icons.Rounded.Flight
        "payments" -> Icons.Rounded.Payments
        "laptop" -> Icons.Rounded.LaptopMac
        "store" -> Icons.Rounded.Storefront
        "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
        "gift" -> Icons.Rounded.CardGiftcard
        "work" -> Icons.Rounded.Work
        "savings" -> Icons.Rounded.AccountBalance
        "wallet" -> Icons.Rounded.AccountBalanceWallet
        "bank" -> Icons.Rounded.AccountBalance
        "credit_card" -> Icons.Rounded.CreditCard
        "swap" -> Icons.Rounded.SwapHoriz
        else -> Icons.Rounded.Category
    }

    fun forAccountType(type: AccountType): ImageVector = when (type) {
        AccountType.CASH -> Icons.Rounded.AccountBalanceWallet
        AccountType.BANK -> Icons.Rounded.AccountBalance
        AccountType.SAVINGS -> Icons.Rounded.AccountBalance
        AccountType.CREDIT_CARD -> Icons.Rounded.CreditCard
        AccountType.MOBILE_BANKING -> Icons.Rounded.PhoneAndroid
        AccountType.CUSTOM -> Icons.Rounded.Payments
    }

    fun defaultKeyForAccountType(type: AccountType): String = when (type) {
        AccountType.CASH -> "wallet"
        AccountType.BANK -> "bank"
        AccountType.SAVINGS -> "savings"
        AccountType.CREDIT_CARD -> "credit_card"
        AccountType.MOBILE_BANKING -> "smartphone"
        AccountType.CUSTOM -> "payments"
    }
}
