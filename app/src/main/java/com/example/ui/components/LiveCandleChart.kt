package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Candle
import com.example.domain.LockedSignal
import com.example.domain.SignalDirection
import com.example.ui.theme.AmberQuant
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun LiveCandleChartCard(
    candles: List<Candle>,
    lockedSignals: List<LockedSignal>,
    pairDecimals: Int,
    formatPrice: (Double, Int) -> String,
    patternLabel: String,
    rsi14: Double,
    modifier: Modifier = Modifier
) {
    val visibleCandles = candles.takeLast(16)
    val latestPrice = visibleCandles.lastOrNull()?.close ?: 0.0
    val signalByEpoch = lockedSignals.associateBy { it.candleEpochMinute }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SlateCard.copy(alpha = 0.88f))
            .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "2-CANDLE LIVE MATRIX (1M OTC)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = patternLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "RSI ${"%.1f".format(rsi14)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (rsi14 > 70 || rsi14 < 30) AmberQuant else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatPrice(latestPrice, pairDecimals),
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
        ) {
            if (visibleCandles.isEmpty()) {
                Text(
                    text = "Syncing live OTC candles…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = visibleCandles.size
                    val minLow = visibleCandles.minOf { it.low }
                    val maxHigh = visibleCandles.maxOf { it.high }
                    val pad = (maxHigh - minLow) * 0.18
                    val lowBound = minLow - pad
                    val highBound = maxHigh + pad
                    val priceRange = (highBound - lowBound).coerceAtLeast(1e-9)

                    fun yFor(price: Double): Float {
                        val norm = ((price - lowBound) / priceRange).toFloat().coerceIn(0f, 1f)
                        return size.height - norm * size.height
                    }

                    // Horizontal subtle grid lines
                    for (i in 1..3) {
                        val y = size.height * (i / 4f)
                        drawLine(
                            color = HairlineBorder.copy(alpha = 0.55f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    val slotWidth = size.width / count.toFloat()
                    val bodyWidth = (slotWidth * 0.58f).coerceAtLeast(6f)

                    // Highlight latest 2-Candle Advanced Tech window
                    if (count >= 2) {
                        val boxStartX = (count - 2) * slotWidth
                        drawRoundRect(
                            color = ElectricCyan.copy(alpha = 0.08f),
                            topLeft = Offset(boxStartX, 2f),
                            size = Size(slotWidth * 2f, size.height - 4f),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawRoundRect(
                            color = ElectricCyan.copy(alpha = 0.35f),
                            topLeft = Offset(boxStartX, 2f),
                            size = Size(slotWidth * 2f, size.height - 4f),
                            cornerRadius = CornerRadius(10f, 10f),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // EMA-9 overlay curve
                    val emaPath = Path()
                    val k = 2.0 / 10.0
                    var ema = visibleCandles.first().close
                    visibleCandles.forEachIndexed { index, candle ->
                        ema = candle.close * k + ema * (1.0 - k)
                        val cx = index * slotWidth + slotWidth / 2f
                        val cy = yFor(ema)
                        if (index == 0) emaPath.moveTo(cx, cy) else emaPath.lineTo(cx, cy)
                    }
                    drawPath(
                        path = emaPath,
                        color = AmberQuant.copy(alpha = 0.75f),
                        style = Stroke(width = 2.2f)
                    )

                    // Draw Candles + Immutable No-Repaint Lock markers
                    visibleCandles.forEachIndexed { index, candle ->
                        val cx = index * slotWidth + slotWidth / 2f
                        val color = if (candle.isBullish) BullishEmerald else BearishCrimson

                        val highY = yFor(candle.high)
                        val lowY = yFor(candle.low)
                        val openY = yFor(candle.open)
                        val closeY = yFor(candle.close)

                        // Wick
                        drawLine(
                            color = color,
                            start = Offset(cx, highY),
                            end = Offset(cx, lowY),
                            strokeWidth = 2.2f
                        )

                        // Body
                        val topY = min(openY, closeY)
                        val heightPx = max(abs(closeY - openY), 3f)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(cx - bodyWidth / 2f, topY),
                            size = Size(bodyWidth, heightPx),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        // Historical / Active No-Repaint Signal Marker
                        val locked = signalByEpoch[candle.epochMinute]
                        if (locked != null && locked.direction != SignalDirection.STANDBY) {
                            val markerColor = if (locked.direction == SignalDirection.BUY) BullishEmerald else BearishCrimson
                            val markerY = if (locked.direction == SignalDirection.BUY) {
                                (lowY + 10f).coerceAtMost(size.height - 6f)
                            } else {
                                (highY - 10f).coerceAtLeast(6f)
                            }
                            drawCircle(
                                color = markerColor,
                                radius = 5f,
                                center = Offset(cx, markerY)
                            )
                        }
                    }

                    // Live price dashed horizontal line
                    val liveY = yFor(latestPrice)
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.85f),
                        start = Offset(0f, liveY),
                        end = Offset(size.width, liveY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }
            }
        }
    }
}
