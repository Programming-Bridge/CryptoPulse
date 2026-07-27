package com.cryptopulse.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Auth : Screen("auth")
    object Main : Screen("main")
    
    // Bottom tabs
    object Dashboard : Screen("dashboard")
    object Markets : Screen("markets")
    object Profile : Screen("profile")
    object Analytics : Screen("analytics")
    object ManualEntry : Screen("manual_entry/{assetId}/{isEdit}") {
        fun createRoute(assetId: String, isEdit: Boolean = false) = "manual_entry/$assetId/$isEdit"
    }
    object Receive : Screen("receive")
    object PriceAlerts : Screen("price_alerts")
    object AddPriceAlert : Screen("add_price_alert/{assetId}") {
        fun createRoute(assetId: String) = "add_price_alert/$assetId"
    }
    object Notifications : Screen("notifications")
    object TransactionHistory : Screen("transaction_history")
    object Security : Screen("security")
    object Help : Screen("help")
    object EditProfile : Screen("edit_profile")
    object ConnectExchange : Screen("connect_exchange")
    object BaseCurrency : Screen("base_currency")
    object PrivacyData : Screen("privacy_data")
    object CoinDetails : Screen("coin_details/{assetId}") {
        fun createRoute(assetId: String) = "coin_details/$assetId"
    }
}
