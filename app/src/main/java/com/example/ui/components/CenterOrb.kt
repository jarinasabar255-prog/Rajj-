package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AiEngineState
import com.example.domain.LockedSignal
import com.example.domain.SignalDirection
import com.example.domain.TwoCandleSetup
import com.example.ui.theme.AmberQuant
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FinorixCenterOrb(
    lockedSignal: LockedSignal?,
    currentSetup: TwoCandleSetup?,
    aiState: AiEngineState,
    secondsRemaining: Int,
    formattedEntryPrice: String,
    orbSize: Dp = 248.dp,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning = aiState is AiEngineState.Analyzing
    val activeDirection = lockedSignal?.direction ?: currentSetup?.suggestedDirection ?: SignalDirection.STANDBY

    val orbPrimaryColor by animateColorAsState(
        targetValue = when {
            isScanning -> AmberQuant
            lockedSignal?.direction == SignalDirection.BUY -> BullishEmerald
            lockedSignal?.direction == SignalDirection.SELL -> BearishCrimson
            activeDirection == SignalDirection.BUY -> BullishEmerald.copy(alpha = 0.85f)
            activeDirection == SignalDirection.SELL -> BearishCrimson.copy(alpha = 0.85f)
            else -> ElectricCyan
        },
        animationSpec = tween(durationMillis = 450),
        label = "orbPrimaryColor"
    )

    val countdownSweep by animateFloatAsState(
        targetValue = (secondsRemaining / 60f) * 360f,
        animationSpec = spring(),
        label = "countdownSweep"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isScanning) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isScanning) 4500 else 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isScanning) 3200 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val accessibilityText = when {
        isScanning -> "Scanning 2-candle setup with Gemini 3.8 Live"
        lockedSignal != null -> "Locked ${lockedSignal.direction.name} signal at ${lockedSignal.confidence} percent confidence"
        else -> "Tap Center Orb to run Gemini 3.8 1-minute signal scan"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(orbSize)
            .clip(CircleShape)
            .testTag("center_orb_button")
            .semantics { contentDescription = accessibilityText }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = orbPrimaryColor),
                role = Role.Button,
                onClick = onOrbClick
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // 1. Ambient outer radial aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orbPrimaryColor.copy(alpha = 0.30f * pulseScale),
                        orbPrimaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. 60-second precision tick dial around perimeter
            val tickOuter = maxRadius * 0.94f
            val tickInnerShort = maxRadius * 0.89f
            val tickInnerMajor = maxRadius * 0.86f
            for (i in 0 until 60) {
                val angleRad = (i * 6.0 - 90.0) * (PI / 180.0)
                val isMajor = i % 5 == 0
                val innerR = if (isMajor) tickInnerMajor else tickInnerShort
                val start = Offset(
                    center.x + (cos(angleRad) * innerR).toFloat(),
                    center.y + (sin(angleRad) * innerR).toFloat()
                )
                val end = Offset(
                    center.x + (cos(angleRad) * tickOuter).toFloat(),
                    center.y + (sin(angleRad) * tickOuter).toFloat()
                )
                drawLine(
                    color = if (i <= secondsRemaining) orbPrimaryColor.copy(alpha = 0.85f)
                    else Color.White.copy(alpha = 0.14f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 3f else 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // 3. 1-Minute Countdown Sweep Arc
            val arcRadius = maxRadius * 0.82f
            drawArc(
                color = orbPrimaryColor.copy(alpha = 0.22f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            drawArc(
                color = orbPrimaryColor,
                startAngle = -90f,
                sweepAngle = countdownSweep,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // 4. Outer dashed orbital ring
            rotate(degrees = outerRotation, pivot = center) {
                val r = maxRadius * 0.74f
                drawCircle(
                    color = orbPrimaryColor.copy(alpha = 0.50f),
                    radius = r,
                    center = center,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                    )
                )
            }

            // 5. Inner counter-rotating orbital ring
            rotate(degrees = innerRotation, pivot = center) {
                val r = maxRadius * 0.66f
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.35f),
                    radius = r,
                    center = center,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 14f), 0f)
                    )
                )
            }

            // 6. Core Orb Sphere
            val coreRadius = maxRadius * 0.60f * (0.97f + (pulseScale - 1f) * 0.25f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orbPrimaryColor.copy(alpha = 0.26f),
                        SlateCard.copy(alpha = 0.96f),
                        ObsidianVoid.copy(alpha = 0.98f)
                    ),
                    center = Offset(center.x, center.y - coreRadius * 0.22f),
                    radius = coreRadius * 1.15f
                ),
                radius = coreRadius,
                center = center
            )

            // Core glass rim
            drawCircle(
                color = orbPrimaryColor.copy(alpha = 0.85f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Center Orb Content Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            if (lockedSignal != null) {
                Surface(
                    color = orbPrimaryColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "No repaint locked",
                            tint = orbPrimaryColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NO-REPAINT",
                            style = MaterialTheme.typography.labelSmall,
                            color = orbPrimaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Icon(
                    imageVector = if (lockedSignal.direction == SignalDirection.BUY) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = lockedSignal.direction.name,
                    tint = orbPrimaryColor,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = if (lockedSignal.direction == SignalDirection.BUY) "BUY • CALL" else "SELL • PUT",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                    color = orbPrimaryColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${lockedSignal.confidence}% • @ $formattedEntryPrice",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1M EXPIRY • ${String.format("%02d", secondsRemaining)}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            } else if (isScanning) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Gemini 3.8 Live analyzing",
                    tint = AmberQuant,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GEMINI 3.8",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberQuant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "2-CANDLE SCAN…",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "00:${String.format("%02d", secondsRemaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            } else {
                Icon(
                    imageVector = when (activeDirection) {
                        SignalDirection.BUY -> Icons.AutoMirrored.Filled.TrendingUp
                        SignalDirection.SELL -> Icons.AutoMirrored.Filled.TrendingDown
                        SignalDirection.STANDBY -> Icons.Filled.AutoAwesome
                    },
                    contentDescription = "Orb Scanner Ready",
                    tint = orbPrimaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (activeDirection) {
                        SignalDirection.BUY -> "BIAS: BUY"
                        SignalDirection.SELL -> "BIAS: SELL"
                        SignalDirection.STANDBY -> "FINORIX ORB"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = orbPrimaryColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentSetup?.quantConfidence ?: 0}% 2C SCORE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "TAP TO LOCK 1M",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
