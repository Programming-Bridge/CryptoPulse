package com.cryptopulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.Error
import com.cryptopulse.ui.theme.LightPrimary
import com.cryptopulse.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", style = Typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Today", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            items(todayNotifications) { notification ->
                NotificationItem(notification)
            }
            
            item {
                Text("Earlier", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            items(earlierNotifications) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

@Composable
fun NotificationItem(notification: CryptoNotification) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = notification.color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(notification.icon, contentDescription = null, tint = notification.color, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(notification.title, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(notification.time, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(notification.message, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class CryptoNotification(
    val title: String,
    val message: String,
    val time: String,
    val icon: ImageVector,
    val color: Color
)

val todayNotifications = listOf(
    CryptoNotification("Price Alert: BTC", "Bitcoin reached your target of $65,000.", "2m ago", Icons.Default.Notifications, LightPrimary),
    CryptoNotification("Portfolio Update", "Your portfolio is up 5.2% today!", "1h ago", Icons.AutoMirrored.Filled.TrendingUp, LightPrimary)
)

val earlierNotifications = listOf(
    CryptoNotification("Security Alert", "New login detected from a new device.", "Yesterday", Icons.Default.Security, Error),
    CryptoNotification("App Update", "Version 1.1.0 is now available.", "Oct 24", Icons.Default.Update, Color(0xFFFFBA79))
)

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    CryptoPulseTheme {
        NotificationsScreen(onBackClick = {})
    }
}
