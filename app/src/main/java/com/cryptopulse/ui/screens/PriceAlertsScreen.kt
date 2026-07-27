package com.cryptopulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun PriceAlertsScreen(
    viewModel: CryptoViewModel,
    onBackClick: () -> Unit,
    onAddAlertClick: () -> Unit,
) {
    val alerts by viewModel.priceAlerts.collectAsState()
    PriceAlertsContent(
        alerts = alerts,
        onBackClick = onBackClick,
        onStatusToggle = { alertId, isActive -> viewModel.toggleAlertStatus(alertId, isActive) },
        onAddAlertClick = onAddAlertClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsContent(
    alerts: List<PriceAlertEntity>,
    onBackClick: () -> Unit,
    onStatusToggle: (Int, Boolean) -> Unit,
    onAddAlertClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Price Alerts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlertClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        CenteredAdaptiveColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Your Active Alerts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                items(alerts) { alert ->
                    AlertItem(
                        alert = alert,
                        onStatusToggle = { onStatusToggle(alert.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItem(
    alert: PriceAlertEntity,
    onStatusToggle: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.coinName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Target: $${alert.targetPrice}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = alert.isActive,
                onCheckedChange = onStatusToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PriceAlertsScreenPreview() {
    CryptoPulseTheme {
        PriceAlertsContent(
            alerts = listOf(
                PriceAlertEntity(id = 1, coinId = "bitcoin", coinName = "Bitcoin", targetPrice = 70000.0, isAbove = true),
                PriceAlertEntity(id = 2, coinId = "ethereum", coinName = "Ethereum", targetPrice = 3000.0, isAbove = false)
            ),
            onBackClick = {},
            onStatusToggle = { _, _ -> },
            onAddAlertClick = {},
        )
    }
}
