package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AiEngineState
import com.example.domain.BotSettings
import com.example.domain.LockedSignal
import com.example.domain.OtcMarketCatalog
import com.example.domain.OtcPair
import com.example.ui.TerminalUiState
import com.example.ui.components.FinorixCenterOrb
import com.example.ui.components.LiveCandleChartCard
import com.example.ui.theme.AmberQuant
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OrbTerminalScreen(
    uiState: TerminalUiState,
    settings: BotSettings,
    pairSignals: List<LockedSignal>,
    formatPrice: (Double, Int) -> String,
    onSelectPair: (OtcPair) -> Unit,
    onScanOrbClick: () -> Unit,
    onAskGeminiLive: (String) -> Unit,
    onAnalyzeChartBitmap: (Bitmap) -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promptInput by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeScaledBitmap(context, uri)
            if (bitmap != null) {
                onAnalyzeChartBitmap(bitmap)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        // Ambient generated backdrop artwork
        Image(
            painter = painterResource(id = R.drawable.img_orb_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.24f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianVoid.copy(alpha = 0.72f),
                            ObsidianVoid.copy(alpha = 0.88f),
                            ObsidianVoid
                        )
                    )
                )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 760.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                TerminalHeader(
                    secondsRemaining = uiState.secondsRemaining,
                    voiceEnabled = settings.voiceCalloutEnabled,
                    onToggleVoice = { onToggleVoice(!settings.voiceCalloutEnabled) },
                    modifier = Modifier.widthIn(max = 960.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quotex OTC Pair Selector Carousel
                OtcPairSelectorRow(
                    pairs = OtcMarketCatalog.pairs,
                    selectedPair = uiState.activePair,
                    onSelectPair = onSelectPair,
                    modifier = Modifier.widthIn(max = 960.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                val latestPrice = uiState.candles.lastOrNull()?.close ?: uiState.activePair.basePrice
                val formattedEntry = formatPrice(
                    uiState.activeLockedSignal?.entryPrice ?: latestPrice,
                    uiState.activePair.decimals
                )

                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 960.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(0.45f)
                        ) {
                            FinorixCenterOrb(
                                lockedSignal = uiState.activeLockedSignal,
                                currentSetup = uiState.currentSetup,
                                aiState = uiState.aiState,
                                secondsRemaining = uiState.secondsRemaining,
                                formattedEntryPrice = formattedEntry,
                                orbSize = 260.dp,
                                onOrbClick = onScanOrbClick
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionButtonRow(
                                isScanning = uiState.aiState is AiEngineState.Analyzing,
                                hasLockedSignal = uiState.activeLockedSignal != null,
                                onScanClick = onScanOrbClick,
                                onPickChartClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }

                        Column(
                            modifier = Modifier.weight(0.55f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LiveCandleChartCard(
                                candles = uiState.candles,
                                lockedSignals = pairSignals,
                                pairDecimals = uiState.activePair.decimals,
                                formatPrice = formatPrice,
                                patternLabel = uiState.currentSetup?.patternName ?: "Scanning 2-Candle Structure",
                                rsi14 = uiState.currentSetup?.rsi14 ?: 50.0
                            )
                            GeminiLiveCard(
                                aiState = uiState.aiState,
                                setupWickNote = uiState.currentSetup?.wickPressureLabel ?: "",
                                promptInput = promptInput,
                                onPromptChange = { promptInput = it },
                                onSendPrompt = {
                                    val q = promptInput
                                    promptInput = ""
                                    onAskGeminiLive(q)
                                },
                                onRetryScan = onScanOrbClick
                            )
                        }
                    }
                } else {
                    // Mobile vertical layout centered around the Orb
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                    ) {
                        FinorixCenterOrb(
                            lockedSignal = uiState.activeLockedSignal,
                            currentSetup = uiState.currentSetup,
                            aiState = uiState.aiState,
                            secondsRemaining = uiState.secondsRemaining,
                            formattedEntryPrice = formattedEntry,
                            orbSize = 236.dp,
                            onOrbClick = onScanOrbClick
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ActionButtonRow(
                            isScanning = uiState.aiState is AiEngineState.Analyzing,
                            hasLockedSignal = uiState.activeLockedSignal != null,
                            onScanClick = onScanOrbClick,
                            onPickChartClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LiveCandleChartCard(
                            candles = uiState.candles,
                            lockedSignals = pairSignals,
                            pairDecimals = uiState.activePair.decimals,
                            formatPrice = formatPrice,
                            patternLabel = uiState.currentSetup?.patternName ?: "Scanning 2-Candle Structure",
                            rsi14 = uiState.currentSetup?.rsi14 ?: 50.0
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GeminiLiveCard(
                            aiState = uiState.aiState,
                            setupWickNote = uiState.currentSetup?.wickPressureLabel ?: "",
                            promptInput = promptInput,
                            onPromptChange = { promptInput = it },
                            onSendPrompt = {
                                val q = promptInput
                                promptInput = ""
                                onAskGeminiLive(q)
                            },
                            onRetryScan = onScanOrbClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.disclaimer_text),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TerminalHeader(
    secondsRemaining: Int,
    voiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FINORIX PRO",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = BullishContainer,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BullishEmerald.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "No repaint guarantee",
                            tint = BullishEmerald,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NO-REPAINT",
                            style = MaterialTheme.typography.labelSmall,
                            color = BullishEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = stringResource(id = R.string.brand_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = SlateElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "1-minute candle timer",
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "00:${String.format("%02d", secondsRemaining)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (secondsRemaining <= 5) AmberQuant else ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_voice_button")
            ) {
                Icon(
                    imageVector = if (voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (voiceEnabled) "Mute voice callouts" else "Enable voice callouts",
                    tint = if (voiceEnabled) ElectricCyan else TextMuted
                )
            }
        }
    }
}

@Composable
private fun OtcPairSelectorRow(
    pairs: List<OtcPair>,
    selectedPair: OtcPair,
    onSelectPair: (OtcPair) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(pairs, key = { it.symbol }) { pair ->
            val isSelected = pair.symbol == selectedPair.symbol
            Surface(
                color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else SlateCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) ElectricCyan else HairlineBorder
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("pair_chip_${pair.symbol}")
                    .clickable { onSelectPair(pair) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = pair.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) ElectricCyan else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${pair.payoutPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullishEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonRow(
    isScanning: Boolean,
    hasLockedSignal: Boolean,
    onScanClick: () -> Unit,
    onPickChartClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onScanClick,
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = ObsidianVoid,
                disabledContainerColor = SlateElevated,
                disabledContentColor = TextSecondary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("scan_lock_button")
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCANNING…",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasLockedSignal) "VIEW LOCK" else "SCAN 1M SIGNAL",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        OutlinedButton(
            onClick = onPickChartClick,
            enabled = !isScanning,
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("pick_chart_button")
        ) {
            Icon(
                imageVector = Icons.Filled.AddPhotoAlternate,
                contentDescription = stringResource(id = R.string.upload_chart_desc),
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CHART IMG",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GeminiLiveCard(
    aiState: AiEngineState,
    setupWickNote: String,
    promptInput: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onRetryScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SlateCard.copy(alpha = 0.92f))
            .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Gemini 3.8 Live",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GEMINI 3.8 LIVE ENGINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "2-CANDLE TECH",
                style = MaterialTheme.typography.labelSmall,
                color = AmberQuant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (aiState) {
            is AiEngineState.Idle -> {
                Text(
                    text = if (setupWickNote.isNotBlank()) {
                        "Live Pre-Scan: $setupWickNote. Tap the Center Orb to lock a 1-minute BUY/SELL callout with Gemini 3.8."
                    } else {
                        "Tap the Center Orb to run a real-time 2-Candle Quotex OTC signal lock with Gemini 3.8."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            is AiEngineState.Analyzing -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AmberQuant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Streaming 2-Candle OHLC & wick rejection telemetry to Gemini 3.8…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberQuant
                    )
                }
            }
            is AiEngineState.Streaming -> {
                Text(
                    text = aiState.partialText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            is AiEngineState.Ready -> {
                Text(
                    text = aiState.summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            is AiEngineState.Error -> {
                Surface(
                    color = BearishContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BearishCrimson.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "AI Error",
                            tint = BearishCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BearishCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RETRY",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRetryScan() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Ask Gemini 3.8 Live input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.ask_gemini_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ElectricCyan
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendPrompt() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ask_gemini_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendPrompt,
                enabled = promptInput.isNotBlank() && aiState !is AiEngineState.Analyzing,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (promptInput.isNotBlank()) ElectricCyan else SlateElevated)
                    .testTag("ask_gemini_send")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send to Gemini 3.8 Live",
                    tint = if (promptInput.isNotBlank()) ObsidianVoid else TextMuted
                )
            }
        }
    }
}

private fun decodeScaledBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val maxDim = 1024
        val scale = minOf(
            maxDim.toFloat() / raw.width.coerceAtLeast(1),
            maxDim.toFloat() / raw.height.coerceAtLeast(1),
            1f
        )
        if (scale < 1f) {
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            raw
        }
    }.getOrNull()
}
