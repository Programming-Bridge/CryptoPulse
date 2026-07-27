package com.cryptopulse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.cryptopulse.ui.screens.*
import com.cryptopulse.ui.viewmodels.CryptoViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun NavGraph(
    navController: NavHostController, 
    viewModel: CryptoViewModel,
    startDestination: String = Screen.Splash.route,
    onLogout: () -> Unit = {},
) {
    val coins by viewModel.coins.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = viewModel,
                onTransition = { 
                    val nextScreen = if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                        Screen.Main.route
                    } else {
                        Screen.Welcome.route
                    }
                    navController.navigate(nextScreen) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStartedClick = { navController.navigate(Screen.Auth.route) }
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = { 
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                rootNavController = navController, 
                viewModel = viewModel,
                onLogout = onLogout
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
        composable(Screen.ManualEntry.route) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId")
            val isEdit = backStackEntry.arguments?.getString("isEdit")?.toBoolean() ?: false
            val asset = coins.find { it.id == assetId }
            if (asset != null) {
                ManualEntryScreen(
                    asset = asset,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    isEdit = isEdit
                )
            }
        }
        composable(Screen.Receive.route) {
            ReceiveScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.PriceAlerts.route) {
            PriceAlertsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddAlertClick = { 
                    // Since PriceAlerts is top-level, we pop back to Main
                    // In a real app, we'd pass a "select tab" signal
                    navController.popBackStack() 
                },
            )
        }
        composable(Screen.AddPriceAlert.route) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId")
            val asset = coins.find { it.id == assetId }
            if (asset != null) {
                AddPriceAlertScreen(
                    asset = asset,
                    onBackClick = { navController.popBackStack() },
                    onAlertCreated = { viewModel.addPriceAlert(it) }
                )
            }
        }
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.ConnectExchange.route) {
            ConnectExchangeScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.BaseCurrency.route) {
            BaseCurrencyScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.PrivacyData.route) {
            PrivacyDataScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Security.route) {
            Security2FAScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Help.route) {
            HelpCenterScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.CoinDetails.route) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId")
            val asset = coins.find { it.id == assetId }
            
            if (asset != null) {
                CoinDetailsScreen(
                    asset = asset,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAddTransactionClick = { navController.navigate(Screen.ManualEntry.createRoute(asset.id, isEdit = false)) },
                    onAlertClick = { navController.navigate(Screen.AddPriceAlert.createRoute(asset.id)) },
                )
            }
        }
    }
}
