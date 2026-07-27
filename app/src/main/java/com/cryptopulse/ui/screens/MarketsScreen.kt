package com.cryptopulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.components.AssetRow
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.theme.CryptoPulseTheme

import com.cryptopulse.ui.viewmodels.CryptoViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MarketsScreen(
    viewModel: CryptoViewModel,
    onAssetClick: (String) -> Unit,
    onNotificationsClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allCoins by viewModel.coins.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var searchResults by remember { mutableStateOf(emptyList<com.cryptopulse.data.models.CoinEntity>()) }

    LaunchedEffect(query) {
        if (query.length > 1) {
            delay(300.milliseconds)
            searchResults = viewModel.searchCoins(query)
        } else {
            searchResults = emptyList()
        }
    }

    MarketsContent(
        query = query,
        onQueryChange = { query = it },
        searchResults = if (query.isEmpty()) allCoins else searchResults,
        isRefreshing = isSyncing,
        onRefresh = { viewModel.refreshCoins(manual = true) },
        onAssetClick = onAssetClick,
        onNotificationsClick = onNotificationsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsContent(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<com.cryptopulse.data.models.CoinEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAssetClick: (String) -> Unit,
    onNotificationsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Markets",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding)
        ) {
            CenteredAdaptiveColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    placeholder = { Text("Search coins, tokens...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                Text(
                    text = if (query.isEmpty()) "Market Overview" else "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    items(searchResults) { asset ->
                        AssetRow(
                            name = asset.name,
                            symbol = asset.symbol.uppercase(),
                            price = asset.currentPrice,
                            change = asset.priceChangePercentage24h,
                            imageUrl = asset.imageUrl,
                            onClick = { onAssetClick(asset.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketsScreenPreview() {
    CryptoPulseTheme {
        MarketsContent(
            query = "bit",
            onQueryChange = {},
            searchResults = listOf(
                com.cryptopulse.data.models.CoinEntity("bitcoin", "BTC", "Bitcoin", "", 64000.0, 2.5)
            ),
            isRefreshing = false,
            onRefresh = {},
            onAssetClick = {},
            onNotificationsClick = {}
        )
    }
}
