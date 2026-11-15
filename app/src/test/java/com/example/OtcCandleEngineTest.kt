package com.example

import com.example.data.engine.OtcCandleEngine
import com.example.domain.Candle
import com.example.domain.OtcMarketCatalog
import com.example.domain.SignalDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtcCandleEngineTest {

    private val engine = OtcCandleEngine()

    @Test
    fun `detects 2-candle bullish engulfing sweep`() {
        val pair = OtcMarketCatalog.findBySymbol("EURUSD_OTC")
        val baseCandles = engine.getCandles(pair, 1_800_000L).dropLast(2)
        val setup = Candle(
            epochMinute = 29L,
            open = 1.08420,
            high = 1.08430,
            low = 1.08390,
            close = 1.08400,
            tickVolume = 80
        )
        val trigger = Candle(
            epochMinute = 30L,
            open = 1.08398,
            high = 1.08455,
            low = 1.08395,
            close = 1.08450,
            tickVolume = 130
        )

        val result = engine.analyzeTwoCandleSetup(pair, baseCandles + setup + trigger)
        assertEquals("2C Bullish Engulfing Sweep", result.patternName)
        assertEquals(SignalDirection.BUY, result.suggestedDirection)
        assertTrue(result.quantConfidence >= 75)
    }

    @Test
    fun `detects 2-candle bearish engulfing sweep`() {
        val pair = OtcMarketCatalog.findBySymbol("EURUSD_OTC")
        val baseCandles = engine.getCandles(pair, 1_800_000L).dropLast(2)
        val setup = Candle(
            epochMinute = 29L,
            open = 1.08400,
            high = 1.08430,
            low = 1.08395,
            close = 1.08420,
            tickVolume = 80
        )
        val trigger = Candle(
            epochMinute = 30L,
            open = 1.08422,
            high = 1.08426,
            low = 1.08370,
            close = 1.08375,
            tickVolume = 140
        )

        val result = engine.analyzeTwoCandleSetup(pair, baseCandles + setup + trigger)
        assertEquals("2C Bearish Engulfing Sweep", result.patternName)
        assertEquals(SignalDirection.SELL, result.suggestedDirection)
        assertTrue(result.quantConfidence >= 75)
    }
}
