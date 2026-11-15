package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.SignalDirection
import com.example.domain.TwoCandleSetup
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "GeminiLiveSignal"
private const val MODEL_NAME = "gemini-3.8-flash"

data class GeminiSignalVerdict(
    val direction: SignalDirection,
    val confidence: Int,
    val patternTitle: String,
    val rationale: String,
    val riskTip: String
)

class GeminiLiveSignalService {

    private val signalSchema = Schema.obj(
        properties = mapOf(
            "direction" to Schema.enumeration(
                values = listOf("BUY", "SELL", "STANDBY"),
                description = "1-minute OTC trade callout: BUY (CALL), SELL (PUT), or STANDBY if setup is weak."
            ),
            "confidence" to Schema.integer(
                description = "Signal conviction score from 55 to 96 based on 2-candle confluence."
            ),
            "patternTitle" to Schema.string(
                description = "Concise 2-Candle Advanced Technology pattern name."
            ),
            "rationale" to Schema.string(
                description = "Sharp 2-sentence live execution rationale referencing candle bodies, wicks, EMA-9/21, and RSI-14."
            ),
            "riskTip" to Schema.string(
                description = "Short 1-sentence OTC risk & no-repaint execution note."
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.25f
                responseMimeType = "application/json"
                responseSchema = signalSchema
            },
            systemInstruction = content {
                text(
                    "You are Finorix Pro Gemini 3.8 Live Quant Analyst specializing in 1-minute Quotex OTC " +
                        "2-Candle Advanced Technology price action. Analyze the 2-candle setup objectively, " +
                        "lock BUY, SELL, or STANDBY, and never promise guaranteed profits."
                )
            }
        )
    }

    private val streamModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.35f
            },
            systemInstruction = content {
                text(
                    "You are Finorix Pro Gemini 3.8 Live Desk Assistant inside an Android 1-minute OTC " +
                        "trading terminal. Respond concisely in crisp Hinglish/English trader terminology, " +
                        "referencing the active 2-candle structure, wick rejection, RSI-14, and no-repaint risk rules."
                )
            }
        )
    }

    suspend fun evaluateTwoCandleSignal(
        setup: TwoCandleSetup,
        minConfidenceThreshold: Int
    ): Result<GeminiSignalVerdict> {
        return try {
            val dec = setup.pair.decimals
            val prompt = buildString {
                appendLine("Analyze this live 1-minute Quotex OTC 2-Candle telemetry:")
                appendLine("Pair: ${setup.pair.displayName} (Payout ${setup.pair.payoutPercent}%)")
                appendLine(
                    "Candle-1 [Setup]: O=${fmt(setup.setupCandle.open, dec)}, H=${fmt(setup.setupCandle.high, dec)}, " +
                        "L=${fmt(setup.setupCandle.low, dec)}, C=${fmt(setup.setupCandle.close, dec)}, " +
                        "Bullish=${setup.setupCandle.isBullish}, BodyRatio=${"%.2f".format(setup.setupCandle.bodyRatio)}"
                )
                appendLine(
                    "Candle-2 [Trigger]: O=${fmt(setup.triggerCandle.open, dec)}, H=${fmt(setup.triggerCandle.high, dec)}, " +
                        "L=${fmt(setup.triggerCandle.low, dec)}, C=${fmt(setup.triggerCandle.close, dec)}, " +
                        "Bullish=${setup.triggerCandle.isBullish}, BodyRatio=${"%.2f".format(setup.triggerCandle.bodyRatio)}, " +
                        "UpperWick=${"%.2f".format(setup.triggerCandle.upperWickRatio)}, LowerWick=${"%.2f".format(setup.triggerCandle.lowerWickRatio)}"
                )
                appendLine("Pre-Scan Pattern: ${setup.patternName} | Bias: ${setup.suggestedDirection.name} (${setup.quantConfidence}%)")
                appendLine(
                    "Indicators: EMA9=${fmt(setup.ema9, dec)}, EMA21=${fmt(setup.ema21, dec)}, " +
                        "RSI14=${"%.1f".format(setup.rsi14)}, MomentumBps=${"%.2f".format(setup.momentumDeltaBps)}"
                )
                appendLine("Wick Note: ${setup.wickPressureLabel}")
                appendLine("Rule: Recommend BUY or SELL only if confidence >= $minConfidenceThreshold, else STANDBY.")
            }

            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini 3.8 returned an empty signal payload."))

            val json = JSONObject(rawJson)
            val direction = runCatching {
                SignalDirection.valueOf(json.getString("direction").uppercase(Locale.US))
            }.getOrDefault(SignalDirection.STANDBY)
            val confidence = json.optInt("confidence", setup.quantConfidence).coerceIn(50, 98)
            val patternTitle = json.optString("patternTitle", setup.patternName).ifBlank { setup.patternName }
            val rationale = json.optString("rationale", setup.wickPressureLabel)
            val riskTip = json.optString("riskTip", "1M No-Repaint lock active; manage stake size.")

            Result.success(
                GeminiSignalVerdict(
                    direction = direction,
                    confidence = confidence,
                    patternTitle = patternTitle,
                    rationale = rationale,
                    riskTip = riskTip
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini 3.8 signal scan timed out", e)
            Result.failure(Exception("Gemini 3.8 scan timed out after 60s. Check network and retry.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 3.8 signal evaluation failed", e)
            Result.failure(e)
        }
    }

    fun streamLiveCommentary(
        setup: TwoCandleSetup,
        userQuestion: String
    ): Flow<String> = flow {
        val dec = setup.pair.decimals
        val contextPrompt = buildString {
            appendLine("Active Pair: ${setup.pair.displayName} | 1M OTC")
            appendLine(
                "2-Candle Setup: ${setup.patternName} | Bias=${setup.suggestedDirection} (${setup.quantConfidence}%) | " +
                    "RSI14=${"%.1f".format(setup.rsi14)} | EMA9=${fmt(setup.ema9, dec)} vs EMA21=${fmt(setup.ema21, dec)}"
            )
            appendLine("Wick Pressure: ${setup.wickPressureLabel}")
            appendLine("User Request: $userQuestion")
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            streamModel.generateContentStream(contextPrompt).collect { chunk ->
                val textChunk = chunk.text.orEmpty()
                if (textChunk.isNotEmpty()) {
                    accumulated.append(textChunk)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini 3.8 Live returned an empty stream.")
        }
    }

    fun streamChartImageAnalysis(
        bitmap: Bitmap,
        pairDisplayName: String
    ): Flow<String> = flow {
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            streamModel.generateContentStream(
                content {
                    image(bitmap)
                    text(
                        "Analyze this Quotex OTC 1-minute chart screenshot for $pairDisplayName using " +
                            "2-Candle Advanced Technology. Identify: (1) Latest 2-candle formation, " +
                            "(2) Wick rejection / S&R zone, (3) Recommended 1M CALL (BUY) / PUT (SELL) or WAIT bias, " +
                            "and (4) Key invalidation risk."
                    )
                }
            ).collect { chunk ->
                val textChunk = chunk.text.orEmpty()
                if (textChunk.isNotEmpty()) {
                    accumulated.append(textChunk)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini 3.8 Vision returned no analysis for this screenshot.")
        }
    }

    suspend fun testConnection(): Result<String> {
        return try {
            val response = withTimeout(30_000L) {
                streamModel.generateContent("Reply with 'ONLINE: Gemini 3.8 Flash Live ready' only.")
            }
            val text = response.text?.trim().orEmpty()
            if (text.isNotEmpty()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty diagnostic response from Gemini 3.8."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 3.8 diagnostic check failed", e)
            Result.failure(e)
        }
    }

    private fun fmt(value: Double, decimals: Int): String = "%.${decimals}f".format(Locale.US, value)
}
