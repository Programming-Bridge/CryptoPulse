package com.cryptopulse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.theme.BitcoinColor
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.EthereumColor
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.SolanaColor
import com.cryptopulse.ui.theme.StatusGreen
import com.cryptopulse.ui.theme.StatusRed
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.viewmodels.AllocationInfo
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: CryptoViewModel,
    onBackClick: () -> Unit
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalPL by viewModel.totalProfitLoss.collectAsState()
    val totalPLPercentage by viewModel.totalProfitLossPercentage.collectAsState()
    val allocation by viewModel.portfolioAllocation.collectAsState()
    val portfolio by viewModel.unifiedPortfolio.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                    PortfolioSummaryCard(totalBalance, totalPL, totalPLPercentage)
                }
                
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AssetAllocationCard(allocation, modifier = Modifier.weight(1.2f))
                        MonthlyPerformanceCard(modifier = Modifier.weight(0.8f))
                    }
                }

                item {
                    ProfitLossBreakdown(portfolio)
                }
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(totalBalance: Double, totalPL: Double, totalPLPercentage: Double) {
    val formattedBalance = NumberFormat.getCurrencyInstance(Locale.US).format(totalBalance)
    val isProfit = totalPL >= 0
    val color = if (isProfit) StatusGreen else StatusRed
    val sign = if (isProfit) "+" else ""

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Portfolio Value", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formattedBalance, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp)
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$sign${String.format(Locale.US, "%.1f%%", totalPLPercentage)}",
                            style = NumericDataStyle,
                            fontSize = 12.sp,
                            color = color
                        )
                    }
                }
            }
            Text(
                text = "Total Profit: $sign${NumberFormat.getCurrencyInstance(Locale.US).format(totalPL)}",
                style = Typography.labelMedium,
                color = color,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AssetAllocationCard(allocation: List<AllocationInfo>, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Asset Allocation", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            if (allocation.isEmpty()) {
                Box(modifier = Modifier.height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No assets in portfolio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                DonutChart(allocation)
                Spacer(modifier = Modifier.height(16.dp))
                AllocationLegend(allocation)
            }
        }
    }
}

@Composable
fun DonutChart(allocation: List<AllocationInfo>) {
    val colors = listOf(BitcoinColor, EthereumColor, SolanaColor, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 30f
        var currentStartAngle = 0f
        allocation.forEachIndexed { index, info ->
            val sweepAngle = info.percentage * 360f
            drawArc(
                color = colors.getOrElse(index) { Color.Gray },
                startAngle = currentStartAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            currentStartAngle += sweepAngle
        }
    }
}

@Composable
fun AllocationLegend(allocation: List<AllocationInfo>) {
    val colors = listOf(BitcoinColor, EthereumColor, SolanaColor, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allocation.take(4).forEachIndexed { index, info ->
            LegendItem(
                colors.getOrElse(index) { Color.Gray },
                info.symbol,
                String.format(Locale.US, "%.1f%%", info.percentage * 100)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, percentage: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(percentage, style = NumericDataStyle, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ProfitLossBreakdown(portfolio: List<com.cryptopulse.ui.viewmodels.PortfolioItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Asset Performance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        portfolio.forEach { item ->
            PerformanceRow(item)
        }
    }
}

@Composable
fun PerformanceRow(item: com.cryptopulse.ui.viewmodels.PortfolioItem) {
    val isProfit = item.unrealizedPL >= 0
    val color = if (isProfit) StatusGreen else StatusRed
    val sign = if (isProfit) "+" else ""

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.coin.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Avg: $${String.format(Locale.US, "%.2f", item.avgBuyPrice)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${NumberFormat.getCurrencyInstance(Locale.US).format(item.unrealizedPL)}",
                    style = NumericDataStyle,
                    color = color
                )
                Text(
                    text = "$sign${String.format(Locale.US, "%.1f%%", item.plPercentage)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

@Composable
fun MonthlyPerformanceCard(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Performance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            PerformanceBarChart()
        }
    }
}

@Composable
fun PerformanceBarChart() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val data = listOf(0.4f, 0.2f, 0.6f, 0.8f, 0.1f, 0.9f)
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val barWidth = 15f
        val spacing = (size.width - (data.size * barWidth)) / (data.size - 1)
        data.forEachIndexed { index, value ->
            val x = index * (barWidth + spacing)
            val barHeight = value * size.height
            drawRect(
                color = if (index % 2 == 0) primaryColor else errorColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    CryptoPulseTheme {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            PortfolioSummaryCard(4250.0, 500.0, 12.5)
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AssetAllocationCard(
                    listOf(AllocationInfo("BTC", 0.6f, 2500.0)),
                    modifier = Modifier.weight(1f)
                )
                MonthlyPerformanceCard(modifier = Modifier.weight(1f))
            }
        }
    }
}
