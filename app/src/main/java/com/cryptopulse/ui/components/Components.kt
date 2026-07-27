package com.cryptopulse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cryptopulse.ui.theme.NumericDataStyle
import com.cryptopulse.ui.theme.StatusGreen
import com.cryptopulse.ui.theme.StatusRed
import com.cryptopulse.ui.theme.Typography
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale

fun formatPrice(price: Double): String {
    return if (price <= 0.0) "$0.00"
    else if (price < 0.0000000001) String.format(Locale.US, "$%.12f", price)
    else if (price < 0.000001) String.format(Locale.US, "$%.10f", price)
    else if (price < 0.0001) String.format(Locale.US, "$%.8f", price)
    else if (price < 1.0) String.format(Locale.US, "$%.4f", price)
    else String.format(Locale.US, "$%,.2f", price)
}

fun formatAmount(amount: Double): String {
    return if (amount < 0.000001) String.format(Locale.US, "%.10f", amount)
    else if (amount < 0.001) String.format(Locale.US, "%.6f", amount)
    else String.format(Locale.US, "%,.4f", amount)
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(Size.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startInX by transition.animateFloat(
        initialValue = -2 * size.width,
        targetValue = 2 * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.6f),
                Color.LightGray.copy(alpha = 0.2f),
                Color.LightGray.copy(alpha = 0.6f),
            ),
            start = Offset(startInX, 0f),
            end = Offset(startInX + size.width, size.height)
        )
    ).onGloballyPositioned {
        size = it.size.toSize()
    }
}

@Composable
fun CenteredAdaptiveColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

@Composable
fun DetailedChart(
    data: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    
    val prices = data.map { it.second }
    var scrubPoint by remember { mutableStateOf<Int?>(null) }
    
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500),
        label = "ChartAnimation",
    )

    Column(modifier = modifier) {
        if (scrubPoint != null && scrubPoint!! < data.size) {
            val point = data[scrubPoint!!]
            val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(java.util.Date(point.first))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(date, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$${point.second}", style = NumericDataStyle, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectDragGestures(
                        onDragEnd = { scrubPoint = null },
                        onDragCancel = { scrubPoint = null },
                        onDrag = { change, _ ->
                            val x = change.position.x
                            val index = (x / size.width * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
                            scrubPoint = index
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val min = prices.minOrNull() ?: 0.0
                val max = prices.maxOrNull() ?: 1.0
                val range = (max - min).coerceAtLeast(1.0)
                
                val path = Path()
                val width = size.width
                val height = size.height
                val stepX = width / (data.size - 1)
                
                prices.forEachIndexed { index, price ->
                    val x = index * stepX
                    val y = height - ((price - min) / range * height).toFloat()
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    alpha = animationProgress
                )
                
                // Draw scrubber line and point
                scrubPoint?.let { index ->
                    val x = index * stepX
                    val price = prices[index]
                    val y = height - ((price - min) / range * height).toFloat()
                    
                    drawLine(
                        color = color.copy(alpha = 0.4f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    drawCircle(
                        color = color,
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                if (scrubPoint == null) {
                    val lastX = width
                    val lastY = height - ((prices.last() - min) / range * height).toFloat()
                    
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = (8.dp * animationProgress).toPx(),
                        center = Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = color,
                        radius = 4.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}

@Composable
fun SparklineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier) {
        val min = data.minOrNull() ?: 0.0
        val max = data.maxOrNull() ?: 1.0
        val range = (max - min).coerceAtLeast(1.0)
        val path = Path()
        val stepX = size.width / (data.size - 1)
        
        data.forEachIndexed { index, price ->
            val x = index * stepX
            val y = size.height - ((price - min) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun TrendTag(
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = percentage >= 0
    val color = if (isPositive) StatusGreen else StatusRed
    val icon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = CircleShape,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${if (isPositive) "+" else ""}$percentage%",
                style = NumericDataStyle,
                color = color,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CryptoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = Typography.titleMedium
        )
    }
}

@Composable
fun AssetRow(
    name: String,
    symbol: String,
    price: Double,
    change: Double,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sources: List<String> = emptyList(),
    amount: Double = 0.0,
    holdingValue: Double = 0.0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(imageUrl)
                .crossfade(enable = true)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = Typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (amount > 0) "${formatAmount(amount)} $symbol" else symbol,
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    sources.distinct().forEach { source ->
                        SourceIndicator(source)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = formatPrice(holdingValue),
                style = NumericDataStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = formatPrice(price),
                style = Typography.labelSmall.copy(
                    fontSize = if (price < 0.0001) 10.sp else 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${if (change >= 0) "+" else ""}${String.format(Locale.US, "%.2f", change)}%",
                style = Typography.labelSmall,
                color = if (change >= 0) StatusGreen else StatusRed
            )
        }
    }
}

@Composable
fun SourceIndicator(source: String) {
    val color = when (source) {
        "Manual" -> MaterialTheme.colorScheme.secondary
        "Binance" -> Color(0xFFF3BA2F)
        "Coinbase" -> Color(0xFF0052FF)
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = source.take(1).uppercase(),
            style = Typography.labelSmall,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
