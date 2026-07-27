package com.cryptopulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseCurrencyScreen(onBackClick: () -> Unit) {
    var selectedCurrency by remember { mutableStateOf("USD") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Base Currency", style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
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
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currencies) { currency ->
                val isSelected = currency.code == selectedCurrency
                Surface(
                    onClick = { selectedCurrency = currency.code },
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currency.symbol, style = Typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currency.name, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(currency.code, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

data class CurrencyInfo(val name: String, val code: String, val symbol: String)

val currencies = listOf(
    CurrencyInfo("US Dollar", "USD", "$"),
    CurrencyInfo("Euro", "EUR", "€"),
    CurrencyInfo("British Pound", "GBP", "£"),
    CurrencyInfo("Japanese Yen", "JPY", "¥"),
    CurrencyInfo("Bitcoin", "BTC", "₿")
)

@Preview(showBackground = true)
@Composable
fun BaseCurrencyScreenPreview() {
    CryptoPulseTheme {
        BaseCurrencyScreen(onBackClick = {})
    }
}
