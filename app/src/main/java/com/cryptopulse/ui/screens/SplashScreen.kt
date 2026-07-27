package com.cryptopulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: com.cryptopulse.ui.viewmodels.CryptoViewModel,
    onTransition: () -> Unit
) {
    val isSyncComplete by viewModel.isInitialSyncComplete.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = loadingProgress,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "SplashProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        // Wait for data to be ready (ViewModel handles progress)
        val startTime = System.currentTimeMillis()
        val timeout = 5000L // 5 seconds maximum
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (loadingProgress >= 1f && isSyncComplete) break
            delay(100)
        }
        
        onTransition()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Fixed: Ensures logo starts below punch-hole/status bar
                .padding(24.dp)
                .padding(top = 24.dp) // Adjusted for natural top rhythm
        ) {
            // Branding Section
            Icon(
                imageVector = Icons.Default.CurrencyBitcoin,
                contentDescription = "Logo",
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "CryptoPulse",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Tracking the Heartbeat of the Market",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Bottom Line Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 48.dp, end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round,
                trackColor = Color.Transparent
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = syncMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    // Note: In a real app, you'd pass a mock or use LocalInspectionMode
    CryptoPulseTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview requires ViewModel context")
        }
    }
}
