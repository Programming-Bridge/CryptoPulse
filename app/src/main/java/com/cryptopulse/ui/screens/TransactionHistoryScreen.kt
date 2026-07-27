package com.cryptopulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.data.models.TransactionEntity
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.StatusGreen
import com.cryptopulse.ui.theme.StatusRed
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.viewmodels.CryptoViewModel

@Composable
fun TransactionHistoryScreen(
    viewModel: CryptoViewModel,
    onBackClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    TransactionHistoryContent(transactions = transactions, onBackClick = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryContent(
    transactions: List<TransactionEntity>,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
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
    ) { innerPadding ->
        CenteredAdaptiveColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions) { tx ->
                    TransactionItem(tx)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: TransactionEntity) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isIn = tx.type == "In"
            val icon = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
            val iconColor = if (isIn) StatusGreen else StatusRed
            
            Surface(
                color = iconColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.coinName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${if (isIn) "Deposit" else "Withdrawal"} • ${tx.date}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tx.source != "Manual") {
                        Spacer(modifier = Modifier.width(4.dp))
                        com.cryptopulse.ui.components.SourceIndicator(tx.source)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (isIn) "+" else "-"}${tx.amount} ${tx.coinSymbol}", style = NumericDataStyle, color = MaterialTheme.colorScheme.onSurface)
                Text("$${String.format(java.util.Locale.US, "%.2f", tx.value)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionHistoryScreenPreview() {
    CryptoPulseTheme {
        TransactionHistoryContent(
            transactions = listOf(
                TransactionEntity(id = 1, coinId = "bitcoin", coinName = "Bitcoin", coinSymbol = "BTC", date = "Oct 24", amount = 0.05, value = 3200.0, type = "In"),
                TransactionEntity(id = 2, coinId = "ethereum", coinName = "Ethereum", coinSymbol = "ETH", date = "Oct 23", amount = 1.2, value = 4000.0, type = "Out")
            ),
            onBackClick = {}
        )
    }
}
