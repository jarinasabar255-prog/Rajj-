package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LockedSignal
import com.example.domain.OtcMarketCatalog
import com.example.domain.SignalDirection
import com.example.domain.SignalOutcome
import com.example.ui.LedgerFilter
import com.example.ui.LedgerStats
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberQuant
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LedgerScreen(
    signals: List<LockedSignal>,
    stats: LedgerStats,
    activeFilter: LedgerFilter,
    formatPrice: (Double, Int) -> String,
    onSelectFilter: (LedgerFilter) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Reset No-Repaint Audit Log?") },
            text = { Text("This clears your locally stored 1-minute signal ledger from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) {
                    Text("CLEAR", color = BearishCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL", color = ElectricCyan)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NO-REPAINT LEDGER",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Immutable 1M signal locks • Auto-settled on candle close",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                if (stats.totalSignals > 0) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_ledger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear signal ledger",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // KPI Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    label = "WIN RATE",
                    value = "${stats.winRatePercent}%",
                    accent = BullishEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "ITM / OTM",
                    value = "${stats.wins}/${stats.losses}",
                    accent = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "STREAK",
                    value = "${stats.currentStreak}W",
                    accent = AmberQuant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                val filters = listOf(
                    LedgerFilter.ALL to "ALL",
                    LedgerFilter.BUY to "BUY",
                    LedgerFilter.SELL to "SELL",
                    LedgerFilter.ITM_WIN to "ITM WIN",
                    LedgerFilter.OTM_LOSS to "OTM LOSS"
                )
                items(filters) { (filter, label) ->
                    val selected = filter == activeFilter
                    Surface(
                        color = if (selected) ElectricCyan.copy(alpha = 0.16f) else SlateCard,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) ElectricCyan else HairlineBorder
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .testTag("ledger_filter_${filter.name}")
                            .clickable { onSelectFilter(filter) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) ElectricCyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (signals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "No signals locked yet",
                            tint = ElectricCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Locked 1M Signals Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the Center Orb on Signal Orb tab to run Gemini 3.8 Live 2-Candle analysis and create an immutable No-Repaint lock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(signals, key = { it.id }) { signal ->
                        LockedSignalCard(
                            signal = signal,
                            formatPrice = formatPrice
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LockedSignalCard(
    signal: LockedSignal,
    formatPrice: (Double, Int) -> String
) {
    val pair = OtcMarketCatalog.findBySymbol(signal.pairSymbol)
    val isBuy = signal.direction == SignalDirection.BUY
    val dirColor = if (isBuy) BullishEmerald else BearishCrimson

    val (outcomeLabel, outcomeColor, outcomeBg) = when (signal.outcome) {
        SignalOutcome.ITM_WIN -> Triple("ITM WIN", BullishEmerald, BullishContainer)
        SignalOutcome.OTM_LOSS -> Triple("OTM LOSS", BearishCrimson, BearishContainer)
        SignalOutcome.DOJI_TIE -> Triple("DOJI TIE", AmberQuant, AmberContainer)
        SignalOutcome.PENDING -> Triple("LIVE 1M", ElectricCyan, ElectricCyan.copy(alpha = 0.14f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("signal_card_${signal.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isBuy) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = signal.direction.name,
                    tint = dirColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = signal.pairDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = dirColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isBuy) "BUY" else "SELL",
                        style = MaterialTheme.typography.labelSmall,
                        color = dirColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                color = outcomeBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = outcomeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = outcomeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val entryText = formatPrice(signal.entryPrice, pair.decimals)
            val closeText = signal.closePrice?.let { formatPrice(it, pair.decimals) } ?: "Open…"
            Text(
                text = "Candle ${signal.candleTimeLabel} • $entryText → $closeText",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = "${signal.confidence}% CONF",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = signal.patternName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = signal.aiRationale,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked no-repaint",
                tint = BullishEmerald,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "NO-REPAINT SEAL • ${signal.modelTag}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
