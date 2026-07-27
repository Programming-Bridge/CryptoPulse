package com.cryptopulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Privacy & Data", style = Typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Manage your privacy settings and how your data is used.",
                    style = Typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            items(privacyOptions) { option ->
                ProfileItem(icon = option.icon, label = option.label) { }
            }
        }
    }
}

data class PrivacyOption(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

val privacyOptions = listOf(
    PrivacyOption(Icons.Default.Lock, "Data Encryption"),
    PrivacyOption(Icons.Default.Visibility, "Public Profile"),
    PrivacyOption(Icons.Default.Visibility, "Marketing Preferences"),
    PrivacyOption(Icons.Default.Lock, "Delete Account")
)

@Preview(showBackground = true)
@Composable
fun PrivacyDataScreenPreview() {
    CryptoPulseTheme {
        PrivacyDataScreen(onBackClick = {})
    }
}
