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
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.viewmodels.CryptoViewModel

@Composable
fun SplashScreen(
    viewModel: CryptoViewModel,
    onTransition: () -> Unit
) {
    val isSyncComplete by viewModel.isInitialSyncComplete.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()

    val currentProgress = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Continuous 0% -> 90% fluid progress sweep on splash enter
    LaunchedEffect(Unit) {
        currentProgress.animateTo(
            targetValue = 0.90f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
    }

    // Smooth final 10% sweep to 100% upon sync completion
    LaunchedEffect(loadingProgress, isSyncComplete) {
        if (loadingProgress >= 1f && isSyncComplete) {
            currentProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            onTransition()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .padding(top = 48.dp)
        ) {
            // Branding Section - Clean Pulsing Bitcoin Logo
            Icon(
                imageVector = Icons.Default.CurrencyBitcoin,
                contentDescription = "Logo",
                modifier = Modifier
                    .size(130.dp)
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
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Real-Time Multi-Exchange Tracking",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Bottom Progress Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 48.dp, end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { currentProgress.value },
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    CryptoPulseTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview requires ViewModel context")
        }
    }
}
