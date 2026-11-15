package com.example.data.engine

import com.example.domain.Candle
import com.example.domain.OtcMarketCatalog
import com.example.domain.OtcPair
import com.example.domain.SignalDirection
import com.example.domain.TwoCandleSetup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class OtcCandleEngine {

    private val candlesBySymbol = mutableMapOf<String, MutableList<Candle>>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun getCandles(pair: OtcPair, nowMillis: Long = System.currentTimeMillis()): List<Candle> {
        val currentMinute = nowMillis / 60_000L
        val list = candlesBySymbol.getOrPut(pair.symbol) {
            seedInitialCandles(pair, currentMinute)
        }
        // Advance if minute boundary rolled forward
        while (list.isNotEmpty() && list.last().epochMinute < currentMinute) {
            val prev = list.last()
            val nextMinute = prev.epochMinute + 1L
            val nextCandle = generateNextCandle(pair, prev.close, nextMinute)
            list.add(nextCandle)
            if (list.size > 40) list.removeAt(0)
        }
        return list.toList()
    }

    fun tickLiveCandle(pair: OtcPair, nowMillis: Long = System.currentTimeMillis()): List<Candle> {
        val currentMinute = nowMillis / 60_000L
        val list = candlesBySymbol.getOrPut(pair.symbol) {
            seedInitialCandles(pair, currentMinute)
        }

        // Roll forward any elapsed minutes
        while (list.isNotEmpty() && list.last().epochMinute < currentMinute) {
            val prev = list.last()
            val nextMinute = prev.epochMinute + 1L
            val nextCandle = generateNextCandle(pair, prev.close, nextMinute)
            list.add(nextCandle)
            if (list.size > 40) list.removeAt(0)
        }

        if (list.isNotEmpty()) {
            val idx = list.lastIndex
            val active = list[idx]
            val trendBias = computeMicroTrendBias(list)
            val noise = (Random.nextDouble(-1.0, 1.0) * 0.42 + trendBias * 0.16) * pair.pipScale
            val newClose = (active.close + noise).coerceAtLeast(pair.pipScale * 10)
            val newHigh = max(active.high, newClose)
            val newLow = min(active.low, newClose)
            list[idx] = active.copy(
                high = newHigh,
                low = newLow,
                close = newClose,
                tickVolume = active.tickVolume + Random.nextInt(1, 5)
            )
        }
        return list.toList()
    }

    fun analyzeTwoCandleSetup(pair: OtcPair, candles: List<Candle>): TwoCandleSetup {
        val safeCandles = if (candles.size >= 3) candles else getCandles(pair)
        // Use the two most recently completed/active candles for live 2-candle advanced analysis:
        // Setup candle = N-2 (closed), Trigger candle = N-1 (latest closed or forming near close)
        val n = safeCandles.size
        val setup = safeCandles[(n - 2).coerceAtLeast(0)]
        val trigger = safeCandles[(n - 1).coerceAtLeast(0)]

        val closes = safeCandles.map { it.close }
        val ema9 = calculateEma(closes, 9)
        val ema21 = calculateEma(closes, 21)
        val rsi14 = calculateRsi(closes, 14)

        val momentumDeltaBps = if (setup.open != 0.0) {
            ((trigger.close - setup.open) / setup.open) * 10_000.0
        } else 0.0

        // 2-Candle Advanced Technology Pattern Engine
        val isBullishEngulfing = !setup.isBullish && trigger.isBullish &&
            trigger.bodySize > setup.bodySize * 1.08 &&
            trigger.close >= setup.open
        val isBearishEngulfing = setup.isBullish && !trigger.isBullish &&
            trigger.bodySize > setup.bodySize * 1.08 &&
            trigger.close <= setup.open

        val hasStrongLowerWick = trigger.lowerWickRatio >= 0.42 && trigger.close > trigger.low + trigger.range * 0.55
        val hasStrongUpperWick = trigger.upperWickRatio >= 0.42 && trigger.close < trigger.high - trigger.range * 0.55

        val isTwinBullImpulse = setup.isBullish && trigger.isBullish &&
            trigger.close > setup.close && trigger.bodyRatio >= 0.48
        val isTwinBearImpulse = !setup.isBullish && !trigger.isBullish &&
            trigger.close < setup.close && trigger.bodyRatio >= 0.48

        val emaBullish = ema9 > ema21
        val emaBearish = ema9 < ema21

        var bullScore = 50
        var bearScore = 50
        val patternName: String
        val wickPressure: String

        when {
            isBullishEngulfing -> {
                patternName = "2C Bullish Engulfing Sweep"
                bullScore += 28
                if (rsi14 < 58.0) bullScore += 7
                if (emaBullish) bullScore += 5
                wickPressure = "Buyers absorbed prior red body (+${"%.0f".format(trigger.bodyRatio * 100)}% body)"
            }
            isBearishEngulfing -> {
                patternName = "2C Bearish Engulfing Sweep"
                bearScore += 28
                if (rsi14 > 42.0) bearScore += 7
                if (emaBearish) bearScore += 5
                wickPressure = "Sellers engulfed prior green body (+${"%.0f".format(trigger.bodyRatio * 100)}% body)"
            }
            hasStrongLowerWick && (!setup.isBullish || rsi14 < 45.0) -> {
                patternName = "2C Pin-Bar Demand Rejection"
                bullScore += 24
                if (emaBullish) bullScore += 6
                if (rsi14 < 40.0) bullScore += 6
                wickPressure = "Lower wick rejection ${"%.0f".format(trigger.lowerWickRatio * 100)}% off support"
            }
            hasStrongUpperWick && (setup.isBullish || rsi14 > 55.0) -> {
                patternName = "2C Shooting-Star Supply Trap"
                bearScore += 24
                if (emaBearish) bearScore += 6
                if (rsi14 > 60.0) bearScore += 6
                wickPressure = "Upper wick rejection ${"%.0f".format(trigger.upperWickRatio * 100)}% off resistance"
            }
            isTwinBullImpulse && rsi14 < 74.0 -> {
                patternName = "2C Twin-Candle Bull Impulse"
                bullScore += 22
                if (emaBullish) bullScore += 8
                wickPressure = "Consecutive higher-close institutional push"
            }
            isTwinBearImpulse && rsi14 > 26.0 -> {
                patternName = "2C Twin-Candle Bear Impulse"
                bearScore += 22
                if (emaBearish) bearScore += 8
                wickPressure = "Consecutive lower-close distribution wave"
            }
            rsi14 >= 72.0 -> {
                patternName = "2C Overbought Exhaustion Fade"
                bearScore += 21
                wickPressure = "RSI-14 stretched (${"%.1f".format(rsi14)}), mean reversion favored"
            }
            rsi14 <= 28.0 -> {
                patternName = "2C Oversold Exhaustion Bounce"
                bullScore += 21
                wickPressure = "RSI-14 compressed (${"%.1f".format(rsi14)}), snap-back favored"
            }
            else -> {
                if (trigger.close >= setup.close && emaBullish) {
                    patternName = "2C Micro-Structure Uptick"
                    bullScore += 15
                    wickPressure = "Mild bullish EMA-9 slope alignment"
                } else if (trigger.close < setup.close && emaBearish) {
                    patternName = "2C Micro-Structure Downdraft"
                    bearScore += 15
                    wickPressure = "Mild bearish EMA-9 slope alignment"
                } else {
                    patternName = "2C Compression / Indecision"
                    wickPressure = "Balanced wicks; awaiting breakout confirmation"
                }
            }
        }

        val direction = when {
            bullScore >= bearScore + 10 -> SignalDirection.BUY
            bearScore >= bullScore + 10 -> SignalDirection.SELL
            else -> SignalDirection.STANDBY
        }
        val confidence = max(bullScore, bearScore).coerceIn(58, 94)

        return TwoCandleSetup(
            pair = pair,
            setupCandle = setup,
            triggerCandle = trigger,
            patternName = patternName,
            suggestedDirection = direction,
            quantConfidence = confidence,
            rsi14 = rsi14,
            ema9 = ema9,
            ema21 = ema21,
            wickPressureLabel = wickPressure,
            momentumDeltaBps = momentumDeltaBps
        )
    }

    fun formatCandleTime(epochMinute: Long): String {
        return timeFormat.format(Date(epochMinute * 60_000L))
    }

    fun formatPrice(price: Double, decimals: Int): String {
        return "%.${decimals}f".format(Locale.US, price)
    }

    private fun seedInitialCandles(pair: OtcPair, currentMinute: Long): MutableList<Candle> {
        val list = mutableListOf<Candle>()
        val startMinute = currentMinute - 27L
        var runningPrice = pair.basePrice
        val seedBase = pair.symbol.hashCode().toLong()

        for (i in 0..27) {
            val minute = startMinute + i
            val wave = sin((minute + seedBase) * 0.37) * 0.7 + cos((minute * 0.19)) * 0.4
            val candle = generateDeterministicCandle(pair, runningPrice, minute, wave)
            list.add(candle)
            runningPrice = candle.close
        }
        return list
    }

    private fun generateNextCandle(pair: OtcPair, openPrice: Double, epochMinute: Long): Candle {
        val wave = sin(epochMinute * 0.41 + pair.symbol.hashCode()) * 0.6
        return generateDeterministicCandle(pair, openPrice, epochMinute, wave)
    }

    private fun generateDeterministicCandle(
        pair: OtcPair,
        openPrice: Double,
        epochMinute: Long,
        waveBias: Double
    ): Candle {
        val rng = Random(epochMinute * 31L + pair.symbol.hashCode())
        val bodyDelta = (rng.nextDouble(-1.4, 1.4) + waveBias * 0.85) * pair.pipScale
        val close = (openPrice + bodyDelta).coerceAtLeast(pair.pipScale * 10)
        val upperWick = rng.nextDouble(0.15, 1.15) * pair.pipScale
        val lowerWick = rng.nextDouble(0.15, 1.15) * pair.pipScale
        val high = max(openPrice, close) + upperWick
        val low = (min(openPrice, close) - lowerWick).coerceAtLeast(pair.pipScale * 5)
        return Candle(
            epochMinute = epochMinute,
            open = openPrice,
            high = high,
            low = low,
            close = close,
            tickVolume = rng.nextInt(45, 195)
        )
    }

    private fun computeMicroTrendBias(list: List<Candle>): Double {
        if (list.size < 4) return 0.0
        val recent = list.takeLast(4)
        val diff = recent.last().close - recent.first().open
        return when {
            diff > 0 -> 0.35
            diff < 0 -> -0.35
            else -> 0.0
        }
    }

    private fun calculateEma(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val k = 2.0 / (period + 1.0)
        var ema = values.first()
        for (i in 1 until values.size) {
            ema = values[i] * k + ema * (1.0 - k)
        }
        return ema
    }

    private fun calculateRsi(closes: List<Double>, period: Int): Double {
        if (closes.size <= period) return 50.0
        var gains = 0.0
        var losses = 0.0
        val start = closes.size - period
        for (i in start until closes.size) {
            val delta = closes[i] - closes[i - 1]
            if (delta >= 0) gains += delta else losses += abs(delta)
        }
        val avgGain = gains / period
        val avgLoss = losses / period
        if (avgLoss == 0.0) return 95.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(5.0, 95.0)
    }
}
