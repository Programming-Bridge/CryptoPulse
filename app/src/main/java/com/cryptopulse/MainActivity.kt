package com.cryptopulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.cryptopulse.data.local.prefs.SecurePrefsManager
import com.cryptopulse.navigation.NavGraph
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val app = application as CryptoApp
        val securePrefs = SecurePrefsManager(applicationContext)
        val viewModel: CryptoViewModel by viewModels {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CryptoViewModel(app.repository, securePrefs, app.providers) as T
                }
            }
        }

        // Keep the splash screen on screen until the ViewModel is ready
        splashScreen.setKeepOnScreenCondition {
            // In a real app, you might check if the first frame is ready
            // For now, we allow the Compose UI to take over immediately
            false
        }
        
        val startDestination = "splash"
        
        enableEdgeToEdge()
        setContent {
            CryptoPulseTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController, 
                    viewModel = viewModel,
                    startDestination = startDestination,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("splash") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
