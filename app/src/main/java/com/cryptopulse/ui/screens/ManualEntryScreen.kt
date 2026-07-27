package com.cryptopulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.ui.components.CenteredAdaptiveColumn
import com.cryptopulse.ui.components.CryptoButton
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.Typography
import com.cryptopulse.ui.viewmodels.CryptoViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManualEntryScreen(
    asset: CoinEntity,
    viewModel: CryptoViewModel,
    onBackClick: () -> Unit,
    isEdit: Boolean = false
) {
    val unifiedPortfolio by viewModel.unifiedPortfolio.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val existingItem = unifiedPortfolio.find { it.coin.id == asset.id }
    
    val initialSource = if (isEdit) existingItem?.sources?.firstOrNull() ?: "Manual" else "Manual"
    val initialAmount = if (isEdit) {
        holdings.find { it.coinId == asset.id && it.source == initialSource }?.amount?.toString() ?: ""
    } else ""
    val initialDate = if (isEdit) {
        viewModel.getLatestTransactionDate(asset.id, initialSource) ?: SimpleDateFormat("MMM dd", Locale.US).format(Date())
    } else {
        SimpleDateFormat("MMM dd", Locale.US).format(Date())
    }

    ManualEntryContent(
        asset = asset,
        availableSources = viewModel.availableSources,
        onBackClick = onBackClick,
        initialAmount = initialAmount,
        initialSource = initialSource,
        initialDate = initialDate,
        isEdit = isEdit,
        onConfirmTransaction = { amount, price, type, source, date ->
            viewModel.addManualTransaction(asset, amount, price, type, source, date, isUpdate = isEdit)
            onBackClick()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryContent(
    asset: CoinEntity,
    availableSources: List<String>,
    onBackClick: () -> Unit,
    onConfirmTransaction: (Double, Double, String, String, String) -> Unit,
    initialAmount: String = "",
    initialSource: String = "Manual",
    initialDate: String = "",
    isEdit: Boolean = false
) {
    var amountText by remember(initialAmount) { mutableStateOf(initialAmount) }
    var priceText by remember { mutableStateOf(asset.currentPrice.toString()) }
    var selectedType by remember { mutableStateOf("In") } // "In" or "Out"
    var selectedSource by remember(initialSource) { mutableStateOf(initialSource) }
    var dateText by remember(initialDate) { mutableStateOf(initialDate) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateText = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US).format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEdit) "Update Transaction" else "Add Transaction", style = Typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
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
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                CryptoButton(
                    text = if (isEdit) "Update Transaction" else "Log Transaction",
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val price = priceText.toDoubleOrNull() ?: asset.currentPrice
                        if (amount > 0) {
                            onConfirmTransaction(amount, price, selectedType, selectedSource, dateText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amountText.isNotEmpty() && (amountText.toDoubleOrNull() ?: 0.0) > 0
                )
            }
        }
    ) { innerPadding ->
        CenteredAdaptiveColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Asset Info
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(asset.name, style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(asset.symbol, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("$${asset.currentPrice}", style = NumericDataStyle, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Type Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Transaction Type", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TransactionTypeButton(
                            label = "Deposit (In)",
                            isSelected = selectedType == "In",
                            onClick = { selectedType = "In" },
                            modifier = Modifier.weight(1f)
                        )
                        TransactionTypeButton(
                            label = "Sell (Out)",
                            isSelected = selectedType == "Out",
                            onClick = { selectedType = "Out" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Source Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Source", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSources.forEach { source ->
                            FilterChip(
                                selected = selectedSource == source,
                                onClick = { selectedSource = source },
                                label = { Text(source) },
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                }

                // Date & Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { },
                            label = { Text("Date") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Invisible click layer
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || (it.toDoubleOrNull() != null)) amountText = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        suffix = { Text(asset.symbol, color = MaterialTheme.colorScheme.primary) }
                    )
                }

                // Price Input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) priceText = it },
                    label = { Text("Purchase Price ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    prefix = { Text("$", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                if (amountText.isNotEmpty()) {
                    val price = priceText.toDoubleOrNull() ?: asset.currentPrice
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val value = amount * price
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Est. Value", style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format(java.util.Locale.US, "%.2f", value)}", style = NumericDataStyle.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = Typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManualEntryScreenPreview() {
    CryptoPulseTheme {
        ManualEntryContent(
            asset = CoinEntity(
                id = "bitcoin",
                symbol = "BTC",
                name = "Bitcoin",
                imageUrl = "",
                currentPrice = 64230.50,
                priceChangePercentage24h = 2.4
            ),
            availableSources = listOf("Manual", "Binance", "Coinbase"),
            onBackClick = {},
            onConfirmTransaction = { _, _, _, _, _ -> }
        )
    }
}
