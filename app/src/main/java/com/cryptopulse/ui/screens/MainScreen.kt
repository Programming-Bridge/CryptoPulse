package com.cryptopulse.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cryptopulse.navigation.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.tasks.await
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.StatusRed
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import kotlinx.coroutines.launch

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Default.Dashboard),
    BottomNavItem(Screen.Markets, "Markets", Icons.Default.Search),
    BottomNavItem(Screen.ConnectExchange, "Connect", Icons.Default.Link),
    BottomNavItem(Screen.Profile, "Profile", Icons.Default.Person)
)

@Composable
fun MainScreen(
    rootNavController: NavHostController, 
    viewModel: CryptoViewModel,
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets // Rhythm sync
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            bottomNavController.navigate(item.screen.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Key fix: Stop top-level padding
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()) // Sync with bottom nav only
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onAssetClick = { assetId, isEdit ->
                        rootNavController.navigate(Screen.ManualEntry.createRoute(assetId, isEdit))
                    },
                    onAnalyticsClick = {
                        rootNavController.navigate(Screen.Analytics.route)
                    },
                    onNotificationsClick = {
                        rootNavController.navigate(Screen.Notifications.route)
                    },
                    onAddAssetClick = {
                        bottomNavController.navigate(Screen.Markets.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSyncApiClick = {
                        bottomNavController.navigate(Screen.ConnectExchange.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAlertsClick = {
                        rootNavController.navigate(Screen.PriceAlerts.route)
                    },
                    onHistoryClick = {
                        rootNavController.navigate(Screen.TransactionHistory.route)
                    },
                    onSearchClick = {
                        bottomNavController.navigate(Screen.Markets.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Markets.route) {
                MarketsScreen(
                    viewModel = viewModel,
                    onAssetClick = { assetId ->
                        rootNavController.navigate(Screen.CoinDetails.createRoute(assetId))
                    },
                    onNotificationsClick = {
                        rootNavController.navigate(Screen.Notifications.route)
                    }
                )
            }
            composable(Screen.ConnectExchange.route) {
                ConnectExchangeScreen(
                    viewModel = viewModel,
                    onBackClick = { 
                        bottomNavController.navigate(Screen.Dashboard.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigate = { route: String -> rootNavController.navigate(route) },
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    val scope = rememberCoroutineScope()
    
    var isEditing by remember { mutableStateOf(value = false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUpdating by remember { mutableStateOf(value = false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri },
    )

    val displayName = user?.displayName ?: ""
    val email = user?.email ?: ""
    val photoUrl = user?.photoUrl
    
    val initials = remember(displayName, email) {
        if (displayName.isNotEmpty()) {
            displayName.splitToSequence(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
        } else if (email.isNotEmpty()) {
            email.take(1).uppercase()
        } else {
            "?"
        }
    }

    if (isEditing) {
        EditProfileDialog(
            currentName = displayName,
            currentPhotoUri = selectedImageUri ?: photoUrl,
            onDismiss = { isEditing = false; selectedImageUri = null },
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSave = { newName ->
                scope.launch {
                    isUpdating = true
                    try {
                        val profileUpdates = userProfileChangeRequest {
                            this.displayName = newName
                            selectedImageUri?.let { this.photoUri = it }
                        }
                        user?.updateProfile(profileUpdates)?.await()
                        user = auth.currentUser // Refresh user state
                        isEditing = false
                        selectedImageUri = null
                    } catch (e: Exception) {
                        // Handle error (e.g., show snackbar)
                    } finally {
                        isUpdating = false
                    }
                }
            },
            isUpdating = isUpdating
        )
    }

    CenteredAdaptiveColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Fixed: Ensures content starts below punch-hole
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.clickable { isEditing = true },
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initials, style = Typography.displayLarge, color = MaterialTheme.colorScheme.onSurface, fontSize = 40.sp)
                    }
                }
            }
            
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.background)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        Text(
            text = displayName.ifEmpty { "Crypto Enthusiast" },
            style = Typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = email,
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 32.dp) // Added padding to ensure logout is visible
        ) {
            ProfileItem(Icons.Default.NotificationsActive, "Price Alerts") {
                onNavigate(Screen.PriceAlerts.route)
            }
            ProfileItem(Icons.AutoMirrored.Filled.ListAlt, "Transaction History") {
                onNavigate(Screen.TransactionHistory.route)
            }
            ProfileItem(Icons.Default.Security, "Security & 2FA") { 
                onNavigate(Screen.Security.route)
            }
            ProfileItem(Icons.Default.Link, "Connect Exchange") { 
                onNavigate(Screen.ConnectExchange.route)
            }
            ProfileItem(Icons.AutoMirrored.Filled.Help, "Help Center") { 
                onNavigate(Screen.Help.route)
            }
            ProfileItem(Icons.Default.Lock, "Privacy") { }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.1f), contentColor = StatusRed),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.2f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StatusRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", style = Typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhotoUri: Any?,
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onSave: (String) -> Unit,
    isUpdating: Boolean
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { if (!isUpdating) onPickImage() },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        AsyncImage(
                            model = currentPhotoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = !isUpdating && name.isNotEmpty()
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProfileItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CryptoPulseTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = item.screen == Screen.Dashboard,
                            onClick = { },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Content Area")
            }
        }
    }
}
