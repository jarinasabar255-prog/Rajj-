package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.domain.BotSettings
import com.example.ui.theme.BearishCrimson
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
fun EngineSettingsScreen(
    settings: BotSettings,
    isTestingConnection: Boolean,
    diagnosticMessage: String?,
    onMinConfidenceChange: (Int) -> Unit,
    onAutoLockChange: (Boolean) -> Unit,
    onVoiceCalloutChange: (Boolean) -> Unit,
    onTestConnectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "2-CANDLE BOT ENGINE",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure No-Repaint lock rules & Gemini 3.8 Live telemetry",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            // Confidence Threshold Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SlateCard)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minimum Lock Confidence",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${settings.minConfidenceThreshold}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Signals below this Gemini 3.8 conviction score stay on STANDBY instead of locking into the No-Repaint ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Slider(
                    value = settings.minConfidenceThreshold.toFloat(),
                    onValueChange = { onMinConfidenceChange(it.toInt()) },
                    valueRange = 65f..95f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = SlateElevated
                    ),
                    modifier = Modifier.testTag("confidence_slider")
                )
            }

            // Switches Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SlateCard)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingToggleRow(
                    title = "Auto-Scan on 1M Candle Open",
                    subtitle = "Automatically trigger Gemini 3.8 2-Candle scan at the start of every 1-minute candle.",
                    checked = settings.autoLockAtCandleOpen,
                    onCheckedChange = onAutoLockChange,
                    testTag = "auto_scan_switch"
                )

                SettingToggleRow(
                    title = "Live Voice Callouts (TTS)",
                    subtitle = "Speak BUY / SELL signal direction & confidence hands-free when a No-Repaint lock triggers.",
                    checked = settings.voiceCalloutEnabled,
                    onCheckedChange = onVoiceCalloutChange,
                    testTag = "voice_callout_switch"
                )
            }

            // Gemini 3.8 & App Check Diagnostic Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SlateCard)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEMINI 3.8 & APP CHECK STATUS",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = BullishEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (BuildConfig.DEBUG) {
                            "Model: gemini-3.8-flash • DebugAppCheckProviderFactory"
                        } else {
                            "Model: gemini-3.8-flash • RecaptchaAppCheckProviderFactory"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onTestConnectionClick,
                    enabled = !isTestingConnection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateElevated,
                        contentColor = ElectricCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_gemini_button")
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TESTING GEMINI 3.8…", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST GEMINI 3.8 CONNECTION", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (diagnosticMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isError = diagnosticMessage.startsWith("Error:")
                    Text(
                        text = diagnosticMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) BearishCrimson else BullishEmerald
                    )
                }
            }

            Text(
                text = "App Version ${BuildConfig.VERSION_NAME} • No-Repaint Lock uses unique Room constraints per pair & epoch minute.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = ElectricCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SlateElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
