package com.cryptopulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.ui.components.CryptoButton
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.StatusGreen
import com.cryptopulse.ui.theme.StatusRed
import com.cryptopulse.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPriceAlertScreen(
    asset: CoinEntity,
    onBackClick: () -> Unit,
    onAlertCreated: (PriceAlertEntity) -> Unit
) {
    var targetPrice by remember { mutableStateOf(asset.currentPrice.toString()) }
    var isAbove by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Set Alert for ${asset.name}", style = Typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .padding(bottom = 16.dp)
            ) {
                CryptoButton(
                    text = "Create Alert",
                    onClick = {
                        onAlertCreated(
                            PriceAlertEntity(
                                coinId = asset.id,
                                coinName = asset.name,
                                targetPrice = targetPrice.toDoubleOrNull() ?: asset.currentPrice,
                                isAbove = isAbove
                            )
                        )
                        onBackClick()
                    }
                )
            }
        }
    ) { innerPadding ->
        CenteredAdaptiveColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Context Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(40.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CurrencyBitcoin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(asset.name, style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(asset.symbol, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$${asset.currentPrice}", style = NumericDataStyle, color = MaterialTheme.colorScheme.onSurface)
                        Text("${if (asset.priceChangePercentage24h >= 0) "+" else ""}${asset.priceChangePercentage24h}%", style = Typography.labelMedium, color = if (asset.priceChangePercentage24h >= 0) StatusGreen else StatusRed)
                    }
                }
            }

            // Condition
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Condition", style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConditionButton(
                        label = "Price Rises Above",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        selected = isAbove,
                        onClick = { isAbove = true },
                        modifier = Modifier.weight(1f),
                    )
                    ConditionButton(
                        label = "Price Drops Below",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        selected = !isAbove,
                        onClick = { isAbove = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Target Price
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Target Price", style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { targetPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = Typography.displayLarge.copy(fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurface),
                    prefix = { Text("$", style = Typography.displayLarge.copy(fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun ConditionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = Typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddPriceAlertScreenPreview() {
    CryptoPulseTheme {
        AddPriceAlertScreen(
            asset = CoinEntity("bitcoin", "BTC", "Bitcoin", "", 64000.0, 2.5),
            onBackClick = {},
            onAlertCreated = {}
        )
    }
}
