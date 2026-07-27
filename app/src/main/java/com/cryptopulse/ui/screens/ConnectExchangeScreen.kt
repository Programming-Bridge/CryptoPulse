package com.cryptopulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.data.remote.ExchangeField
import com.cryptopulse.data.remote.ExchangeProvider
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.StatusGreen
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.theme.Error
import com.cryptopulse.ui.viewmodels.ConnectionEvent
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectExchangeScreen(
    viewModel: CryptoViewModel,
    onBackClick: () -> Unit
) {
    val connectedExchanges by viewModel.connectedExchanges.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var selectedProvider by remember { mutableStateOf<ExchangeProvider?>(null) }

    LaunchedEffect(Unit) {
        viewModel.connectionEvents.collect { event ->
            if (event is ConnectionEvent.Success) {
                selectedProvider = null
            }
        }
    }

    if (selectedProvider != null) {
        DynamicConnectDialog(
            provider = selectedProvider!!,
            isSyncing = isSyncing,
            onDismiss = { if (!isSyncing) selectedProvider = null },
            onConnect = { credentials ->
                viewModel.connectExchange(selectedProvider!!, credentials)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Connect Exchange", style = Typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        "Link your favorite exchanges and wallets to track everything in one place.",
                        style = Typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Text("Supported Exchanges", style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                
                items(viewModel.availableProviders) { provider ->
                    val isConnected = connectedExchanges.contains(provider.name)
                    ExchangeItem(
                        exchange = ExchangeInfo(provider.name, isConnected),
                        onConnectClick = { selectedProvider = provider },
                        onDisconnectClick = { viewModel.disconnectExchange(provider.name) }
                    )
                }

                if (viewModel.availableProviders.isEmpty()) {
                    item {
                        Text("No providers available yet.", style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (isSyncing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(syncMessage, color = Color.White, style = Typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicConnectDialog(
    provider: ExchangeProvider,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onConnect: (Map<ExchangeField, String>) -> Unit
) {
    val credentials = remember { mutableStateMapOf<ExchangeField, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect ${provider.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter your read-only credentials to import your portfolio data securely.", style = Typography.bodyMedium)
                provider.requiredFields.forEach { field ->
                    OutlinedTextField(
                        value = credentials[field] ?: "",
                        onValueChange = { credentials[field] = it },
                        label = { Text(field.label) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing
                    )
                }
            }
        },
        confirmButton = {
            val isComplete = provider.requiredFields.all { (credentials[it] ?: "").isNotEmpty() }
            Button(
                onClick = { onConnect(credentials.toMap()) },
                enabled = isComplete && !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExchangeItem(
    exchange: ExchangeInfo,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (exchange.isConnected) Icons.Default.Link else Icons.Default.Add, contentDescription = null, tint = if (exchange.isConnected) StatusGreen else MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exchange.name, style = Typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(if (exchange.isConnected) "Connected" else "Not Connected", style = Typography.labelMedium, color = if (exchange.isConnected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (exchange.isConnected) {
                IconButton(onClick = onDisconnectClick) {
                    Icon(Icons.Default.LinkOff, contentDescription = "Disconnect", tint = Error)
                }
            } else {
                TextButton(onClick = onConnectClick) {
                    Text("Connect", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

data class ExchangeInfo(val name: String, val isConnected: Boolean)

@Preview(showBackground = true)
@Composable
fun ConnectExchangeScreenPreview() {
    CryptoPulseTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview requires a real ViewModel context")
        }
    }
}
