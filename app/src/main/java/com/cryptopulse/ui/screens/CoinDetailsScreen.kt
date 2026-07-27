package com.cryptopulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.components.CryptoButton
import com.cryptopulse.ui.components.DetailedChart
import com.cryptopulse.ui.components.TrendTag
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.StatusGreen
import com.cryptopulse.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailsScreen(
    asset: CoinEntity,
    viewModel: com.cryptopulse.ui.viewmodels.CryptoViewModel,
    onBackClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onAlertClick: () -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("24H") }
    
    val chartData by viewModel.chartData.collectAsState()
    val isChartLoading by viewModel.isChartLoading.collectAsState()

    LaunchedEffect(selectedTimeframe, asset.id) {
        val days = when (selectedTimeframe) {
            "1H" -> "1"
            "24H" -> "1"
            "1W" -> "7"
            "1M" -> "30"
            "3M" -> "90"
            "6M" -> "180"
            "1Y" -> "365"
            "ALL" -> "max"
            else -> "1"
        }
        viewModel.fetchMarketChart(asset.id, days)
        
        // Start live polling for low timeframes (1H, 24H)
        if (selectedTimeframe == "1H" || selectedTimeframe == "24H") {
            viewModel.startPricePolling(asset.id, days)
        } else {
            viewModel.stopPricePolling()
        }
    }

    // Stop polling when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPricePolling()
        }
    }

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(asset.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // No actions needed for read-only tracking focus
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onAlertClick,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alerts", color = MaterialTheme.colorScheme.primary)
                }
                CryptoButton(
                    text = "Add Record",
                    onClick = onAddTransactionClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { innerPadding ->
        CenteredAdaptiveColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isLive = selectedTimeframe == "1H" || selectedTimeframe == "24H"
                            Box(modifier = Modifier.size(8.dp).background(if (isLive) StatusGreen else Color.Gray, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isLive) "LIVE PRICE" else "CLOSE PRICE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = com.cryptopulse.ui.components.formatPrice(asset.currentPrice),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 44.sp
                        )
                        TrendTag(percentage = asset.priceChangePercentage24h, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                
                item {
                    TimeframeSelector(selectedTimeframe) { selectedTimeframe = it }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChartLoading && chartData.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        } else {
                            DetailedChart(
                                data = chartData.ifEmpty { asset.sparklineData.mapIndexed { index, d -> index.toLong() to d } },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 16.dp),
                                color = if (asset.priceChangePercentage24h >= 0) StatusGreen else StatusRed
                            )
                        }
                    }
                }

                item {
                    val unifiedPortfolio by viewModel.unifiedPortfolio.collectAsState()
                    val myItem = unifiedPortfolio.find { it.coin.id == asset.id }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Your Holdings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            if (myItem != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Position Active",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${myItem?.totalAmount ?: 0.0} ${asset.symbol}", style = NumericDataStyle, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("≈ $${String.format(java.util.Locale.US, "%.2f", (myItem?.totalAmount ?: 0.0) * asset.currentPrice)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (myItem == null) {
                                        Text("No balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                
                                if (myItem != null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Avg. Buy Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$${String.format(java.util.Locale.US, "%.2f", myItem.avgBuyPrice)}", style = NumericDataStyle, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            val isProfit = myItem.unrealizedPL >= 0
                                            val color = if (isProfit) StatusGreen else StatusRed
                                            Text("Profit/Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${if (isProfit) "+" else ""}${String.format(java.util.Locale.US, "%.1f%%", myItem.plPercentage)}",
                                                style = NumericDataStyle,
                                                color = color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Market Stats", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        MarketStatRow("Market Cap", formatLargeNumber(asset.marketCap))
                        MarketStatRow("24h Volume", formatLargeNumber(asset.totalVolume))
                        MarketStatRow("24h High", "$${asset.high24h}")
                        MarketStatRow("24h Low", "$${asset.low24h}")
                        MarketStatRow("Circulating Supply", "${formatLargeNumber(asset.circulatingSupply, isSupply = true)} ${asset.symbol}")
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun TimeframeSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("1H", "24H", "1W", "1M", "3M", "6M", "1Y", "ALL")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(60.dp)
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MarketStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = NumericDataStyle, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatLargeNumber(number: Double, isSupply: Boolean = false): String {
    if (number == 0.0) return "---"
    val suffix = arrayOf("", "K", "M", "B", "T")
    var value = number
    var index = 0
    while (value >= 1000 && index < suffix.size - 1) {
        value /= 1000
        index++
    }
    val prefix = if (isSupply) "" else "$"
    return String.format(java.util.Locale.US, "%s%.2f%s", prefix, value, suffix[index])
}

@Preview(showBackground = true)
@Composable
fun CoinDetailsScreenPreview() {
    // Preview might not render chart correctly without a real ViewModel
    CryptoPulseTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Text("Preview requires a real ViewModel", modifier = Modifier.align(Alignment.Center))
        }
    }
}
