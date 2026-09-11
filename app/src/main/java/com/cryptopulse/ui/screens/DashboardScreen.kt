package com.cryptopulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptopulse.ui.components.AssetRow
import com.cryptopulse.ui.components.GlassCard
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import com.cryptopulse.ui.viewmodels.DashboardUiState

@Composable
fun DashboardScreen(
    viewModel: CryptoViewModel,
    onAssetClick: (String, Boolean) -> Unit,
    onAnalyticsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAddAssetClick: () -> Unit,
    onSyncApiClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    
    DashboardContent(
        uiState = uiState,
        syncMessage = syncMessage,
        onAssetClick = onAssetClick,
        onAnalyticsClick = onAnalyticsClick,
        onNotificationsClick = onNotificationsClick,
        onAddAssetClick = onAddAssetClick,
        onSyncApiClick = onSyncApiClick,
        onAlertsClick = onAlertsClick,
        onHistoryClick = onHistoryClick,
        onSearchClick = onSearchClick,
        onDeleteAsset = { viewModel.deletePortfolioItem(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    syncMessage: String,
    onAssetClick: (String, Boolean) -> Unit,
    onAnalyticsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAddAssetClick: () -> Unit,
    onSyncApiClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "CryptoPulse",
                        style = Typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        CenteredAdaptiveColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                when (uiState) {
                    is DashboardUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(syncMessage, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    is DashboardUiState.Error -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is DashboardUiState.Empty, is DashboardUiState.Success -> {
                        val portfolio = if (uiState is DashboardUiState.Success) uiState.portfolio else emptyList()
                        val totalBalance = if (uiState is DashboardUiState.Success) uiState.totalBalance else 0.0
                        val performance = if (uiState is DashboardUiState.Success) uiState.performance24h else 0.0

                        val formattedBalance = if (totalBalance > 0 && totalBalance < 0.01) {
                            String.format(java.util.Locale.US, "$%.4f", totalBalance)
                        } else {
                            java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).format(totalBalance)
                        }
                        
                        val performanceText = "${if (performance >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", performance)}%"
                        
                        PortfolioCard(
                            totalBalance = formattedBalance,
                            percentageChange = performanceText,
                            sparklineData = portfolio.find { it.coin.symbol == "BTC" }?.coin?.sparklineData ?: emptyList(),
                            onClick = onAnalyticsClick
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        QuickActionsGrid(
                            onAddAssetClick = onAddAssetClick,
                            onSyncApiClick = onSyncApiClick,
                            onAlertsClick = onAlertsClick,
                            onHistoryClick = onHistoryClick
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Your Portfolio",
                            style = Typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (portfolio.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No assets tracked yet.",
                                            style = Typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Tap '+' to add manual entries or sync with an exchange.",
                                            style = Typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 32.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = portfolio,
                                    key = { it.coin.id }
                                ) { item ->
                                    val dismissState = rememberSwipeToDismissBoxState()

                                    LaunchedEffect(dismissState.currentValue) {
                                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                            onDeleteAsset(item.coin.id)
                                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                        }
                                    }

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                MaterialTheme.colorScheme.errorContainer
                                            } else Color.Transparent
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(color)
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        },
                                        enableDismissFromStartToEnd = false
                                    ) {
                                        AssetRow(
                                            name = item.coin.name,
                                            symbol = item.coin.symbol,
                                            price = item.coin.currentPrice,
                                            change = item.coin.priceChangePercentage24h,
                                            imageUrl = item.coin.imageUrl,
                                            sources = item.sources,
                                            amount = item.totalAmount,
                                            holdingValue = item.totalValue,
                                            onClick = { onAssetClick(item.coin.id, true) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onAddAssetClick: () -> Unit,
    onSyncApiClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton(Icons.Default.Add, "Add Asset", onAddAssetClick)
        QuickActionButton(Icons.Default.Sync, "Sync API", onSyncApiClick)
        QuickActionButton(Icons.Default.Notifications, "Alerts", onAlertsClick)
        QuickActionButton(Icons.Default.BarChart, "History", onHistoryClick)
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            text = label,
            style = Typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun PortfolioCard(
    totalBalance: String,
    percentageChange: String,
    sparklineData: List<Double>,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = "Total Balance",
            style = Typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = totalBalance,
            style = Typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 32.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(percentageChange, style = NumericDataStyle, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            
            com.cryptopulse.ui.components.SparklineChart(
                data = sparklineData,
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    CryptoPulseTheme {
        DashboardContent(
            uiState = DashboardUiState.Success(
                portfolio = emptyList(),
                totalBalance = 0.0,
                performance24h = 0.0
            ),
            syncMessage = "Ready",
            onAssetClick = { _, _ -> },
            onAnalyticsClick = {},
            onNotificationsClick = {},
            onAddAssetClick = {},
            onSyncApiClick = {},
            onAlertsClick = {},
            onHistoryClick = {},
            onSearchClick = {},
            onDeleteAsset = {}
        )
    }
}
