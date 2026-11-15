package com.example.domain

import kotlin.math.abs

enum class SignalDirection {
    BUY,
    SELL,
    STANDBY
}

enum class SignalOutcome {
    PENDING,
    ITM_WIN,
    OTM_LOSS,
    DOJI_TIE
}

data class OtcPair(
    val symbol: String,
    val displayName: String,
    val category: String,
    val payoutPercent: Int,
    val decimals: Int,
    val basePrice: Double,
    val pipScale: Double
)

object OtcMarketCatalog {
    val pairs: List<OtcPair> = listOf(
        OtcPair("EURUSD_OTC", "EUR/USD OTC", "FX OTC", 92, 5, 1.08420, 0.00018),
        OtcPair("GBPUSD_OTC", "GBP/USD OTC", "FX OTC", 90, 5, 1.27150, 0.00022),
        OtcPair("USDJPY_OTC", "USD/JPY OTC", "FX OTC", 89, 3, 154.620, 0.025),
        OtcPair("AUDCAD_OTC", "AUD/CAD OTC", "FX OTC", 91, 5, 0.91480, 0.00016),
        OtcPair("EURJPY_OTC", "EUR/JPY OTC", "FX OTC", 88, 3, 167.480, 0.028),
        OtcPair("NZDCHF_OTC", "NZD/CHF OTC", "FX OTC", 87, 5, 0.52640, 0.00014),
        OtcPair("XAUUSD_OTC", "GOLD OTC", "COMMODITY", 93, 2, 2618.40, 0.85),
        OtcPair("BTCUSD_OTC", "BTC/USD OTC", "CRYPTO OTC", 86, 2, 91240.00, 38.0)
    )

    fun findBySymbol(symbol: String): OtcPair =
        pairs.firstOrNull { it.symbol == symbol } ?: pairs.first()
}

data class Candle(
    val epochMinute: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val tickVolume: Int
) {
    val isBullish: Boolean get() = close >= open
    val range: Double get() = (high - low).coerceAtLeast(1e-9)
    val bodySize: Double get() = abs(close - open)
    val upperWick: Double get() = high - maxOf(open, close)
    val lowerWick: Double get() = minOf(open, close) - low
    val bodyRatio: Double get() = (bodySize / range).coerceIn(0.0, 1.0)
    val upperWickRatio: Double get() = (upperWick / range).coerceIn(0.0, 1.0)
    val lowerWickRatio: Double get() = (lowerWick / range).coerceIn(0.0, 1.0)
}

data class TwoCandleSetup(
    val pair: OtcPair,
    val setupCandle: Candle,
    val triggerCandle: Candle,
    val patternName: String,
    val suggestedDirection: SignalDirection,
    val quantConfidence: Int,
    val rsi14: Double,
    val ema9: Double,
    val ema21: Double,
    val wickPressureLabel: String,
    val momentumDeltaBps: Double
)

data class LockedSignal(
    val id: Long = 0L,
    val pairSymbol: String,
    val pairDisplayName: String,
    val candleEpochMinute: Long,
    val candleTimeLabel: String,
    val direction: SignalDirection,
    val entryPrice: Double,
    val closePrice: Double?,
    val confidence: Int,
    val patternName: String,
    val aiRationale: String,
    val outcome: SignalOutcome,
    val payoutPercent: Int,
    val lockedAtMillis: Long,
    val modelTag: String = "gemini-3.8-flash"
)

data class BotSettings(
    val selectedPairSymbol: String = "EURUSD_OTC",
    val minConfidenceThreshold: Int = 78,
    val autoLockAtCandleOpen: Boolean = true,
    val voiceCalloutEnabled: Boolean = true
)

sealed interface AiEngineState {
    data object Idle : AiEngineState
    data object Analyzing : AiEngineState
    data class Streaming(val partialText: String) : AiEngineState
    data class Ready(val summaryText: String, val lastModelUsed: String = "gemini-3.8-flash") : AiEngineState
    data class Error(val message: String) : AiEngineState
}
