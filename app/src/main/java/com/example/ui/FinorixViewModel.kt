package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiLiveSignalService
import com.example.data.engine.OtcCandleEngine
import com.example.data.local.FinorixDatabase
import com.example.data.local.SettingsRepository
import com.example.data.local.SignalRepository
import com.example.domain.AiEngineState
import com.example.domain.BotSettings
import com.example.domain.Candle
import com.example.domain.LockedSignal
import com.example.domain.OtcMarketCatalog
import com.example.domain.OtcPair
import com.example.domain.SignalDirection
import com.example.domain.SignalOutcome
import com.example.domain.TwoCandleSetup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "FinorixViewModel"

enum class LedgerFilter {
    ALL,
    BUY,
    SELL,
    ITM_WIN,
    OTM_LOSS
}

data class LedgerStats(
    val totalSignals: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val pending: Int = 0,
    val winRatePercent: Int = 0,
    val currentStreak: Int = 0
)

data class TerminalUiState(
    val activePair: OtcPair = OtcMarketCatalog.pairs.first(),
    val candles: List<Candle> = emptyList(),
    val currentSetup: TwoCandleSetup? = null,
    val secondsRemaining: Int = 60,
    val currentEpochMinute: Long = System.currentTimeMillis() / 60_000L,
    val activeLockedSignal: LockedSignal? = null,
    val aiState: AiEngineState = AiEngineState.Idle,
    val diagnosticMessage: String? = null,
    val isTestingConnection: Boolean = false
)

class FinorixViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FinorixDatabase.getInstance(application)
    private val signalRepository = SignalRepository(database.signalDao())
    private val settingsRepository = SettingsRepository(application)
    private val candleEngine = OtcCandleEngine()
    private val geminiService = GeminiLiveSignalService()

    private val _terminalState = MutableStateFlow(TerminalUiState())
    val terminalState: StateFlow<TerminalUiState> = _terminalState.asStateFlow()

    private val _ledgerFilter = MutableStateFlow(LedgerFilter.ALL)
    val ledgerFilter: StateFlow<LedgerFilter> = _ledgerFilter.asStateFlow()

    val settings: StateFlow<BotSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BotSettings()
        )

    val allSignals: StateFlow<List<LockedSignal>> = signalRepository.allSignals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filteredSignals: StateFlow<List<LockedSignal>> = combine(allSignals, _ledgerFilter) { list, filter ->
        when (filter) {
            LedgerFilter.ALL -> list
            LedgerFilter.BUY -> list.filter { it.direction == SignalDirection.BUY }
            LedgerFilter.SELL -> list.filter { it.direction == SignalDirection.SELL }
            LedgerFilter.ITM_WIN -> list.filter { it.outcome == SignalOutcome.ITM_WIN }
            LedgerFilter.OTM_LOSS -> list.filter { it.outcome == SignalOutcome.OTM_LOSS }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val ledgerStats: StateFlow<LedgerStats> = allSignals
        .combine(_ledgerFilter) { list, _ ->
            val wins = list.count { it.outcome == SignalOutcome.ITM_WIN }
            val losses = list.count { it.outcome == SignalOutcome.OTM_LOSS }
            val pending = list.count { it.outcome == SignalOutcome.PENDING }
            val settled = wins + losses
            val winRate = if (settled > 0) ((wins * 100.0) / settled).toInt() else 0
            var streak = 0
            for (item in list) {
                when (item.outcome) {
                    SignalOutcome.ITM_WIN -> streak++
                    SignalOutcome.OTM_LOSS -> break
                    else -> continue
                }
            }
            LedgerStats(
                totalSignals = list.size,
                wins = wins,
                losses = losses,
                pending = pending,
                winRatePercent = winRate,
                currentStreak = streak
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LedgerStats()
        )

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var lastAutoScannedMinute: Long = -1L
    private var scanJob: Job? = null
    private var streamJob: Job? = null

    init {
        initTextToSpeech(application)
        observePairSelection()
        startLiveMarketLoop()
    }

    private fun initTextToSpeech(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    private fun observePairSelection() {
        viewModelScope.launch {
            settings.collectLatest { botSettings ->
                val pair = OtcMarketCatalog.findBySymbol(botSettings.selectedPairSymbol)
                val candles = candleEngine.getCandles(pair)
                val setup = candleEngine.analyzeTwoCandleSetup(pair, candles)
                val currentMinute = System.currentTimeMillis() / 60_000L
                val existingLock = allSignals.value.firstOrNull {
                    it.pairSymbol == pair.symbol && it.candleEpochMinute == currentMinute
                }
                _terminalState.value = _terminalState.value.copy(
                    activePair = pair,
                    candles = candles,
                    currentSetup = setup,
                    activeLockedSignal = existingLock
                )
            }
        }
    }

    private fun startLiveMarketLoop() {
        viewModelScope.launch {
            while (true) {
                val nowMillis = System.currentTimeMillis()
                val currentMinute = nowMillis / 60_000L
                val secondsIntoMinute = ((nowMillis % 60_000L) / 1_000L).toInt()
                val secondsRemaining = (60 - secondsIntoMinute).coerceIn(1, 60)

                val pair = _terminalState.value.activePair
                val updatedCandles = candleEngine.tickLiveCandle(pair, nowMillis)
                val setup = candleEngine.analyzeTwoCandleSetup(pair, updatedCandles)

                // Settle any pending locked signals whose 1-minute candle has completed
                settleExpiredSignals(currentMinute, nowMillis)

                val activeLock = allSignals.value.firstOrNull {
                    it.pairSymbol == pair.symbol && it.candleEpochMinute == currentMinute
                }

                _terminalState.value = _terminalState.value.copy(
                    candles = updatedCandles,
                    currentSetup = setup,
                    secondsRemaining = secondsRemaining,
                    currentEpochMinute = currentMinute,
                    activeLockedSignal = activeLock
                )

                // Auto-scan with Gemini 3.8 at the start of a new 1M candle when enabled
                val currentSettings = settings.value
                if (
                    currentSettings.autoLockAtCandleOpen &&
                    activeLock == null &&
                    secondsIntoMinute in 1..4 &&
                    lastAutoScannedMinute != currentMinute &&
                    _terminalState.value.aiState !is AiEngineState.Analyzing
                ) {
                    lastAutoScannedMinute = currentMinute
                    scanWithGemini38(isAutoTriggered = true)
                }

                delay(500L)
            }
        }
    }

    private suspend fun settleExpiredSignals(currentMinute: Long, nowMillis: Long) {
        val pending = signalRepository.getPendingSignals()
        if (pending.isEmpty()) return

        for (signal in pending) {
            if (currentMinute > signal.candleEpochMinute) {
                val pair = OtcMarketCatalog.findBySymbol(signal.pairSymbol)
                val pairCandles = candleEngine.getCandles(pair, nowMillis)
                val targetCandle = pairCandles.firstOrNull { it.epochMinute == signal.candleEpochMinute }
                val closePrice = targetCandle?.close ?: pairCandles.lastOrNull()?.close ?: signal.entryPrice
                val outcome = when {
                    closePrice == signal.entryPrice -> SignalOutcome.DOJI_TIE
                    signal.direction == SignalDirection.BUY && closePrice > signal.entryPrice -> SignalOutcome.ITM_WIN
                    signal.direction == SignalDirection.SELL && closePrice < signal.entryPrice -> SignalOutcome.ITM_WIN
                    else -> SignalOutcome.OTM_LOSS
                }
                signalRepository.settleSignal(signal.id, closePrice, outcome)
            }
        }
    }

    fun selectPair(pair: OtcPair) {
        viewModelScope.launch {
            settingsRepository.updateSelectedPair(pair.symbol)
        }
    }

    fun setLedgerFilter(filter: LedgerFilter) {
        _ledgerFilter.value = filter
    }

    fun updateMinConfidence(value: Int) {
        viewModelScope.launch { settingsRepository.updateMinConfidence(value) }
    }

    fun updateAutoLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoLock(enabled) }
    }

    fun updateVoiceCallout(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateVoiceCallout(enabled) }
    }

    fun clearLedgerHistory() {
        viewModelScope.launch {
            signalRepository.clearAllSignals()
        }
    }

    fun scanWithGemini38(isAutoTriggered: Boolean = false) {
        val state = _terminalState.value
        val setup = state.currentSetup ?: return
        val currentMinute = state.currentEpochMinute

        // Enforce strict No-Repaint lock: if this candle minute already has a locked signal, do not overwrite!
        if (state.activeLockedSignal != null) {
            if (!isAutoTriggered) {
                _terminalState.value = state.copy(
                    aiState = AiEngineState.Ready(
                        summaryText = "No-Repaint Lock active for ${state.activeLockedSignal.candleTimeLabel}: " +
                            "${state.activeLockedSignal.direction.name} (${state.activeLockedSignal.confidence}%) — ${state.activeLockedSignal.aiRationale}"
                    )
                )
            }
            return
        }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _terminalState.value = _terminalState.value.copy(aiState = AiEngineState.Analyzing)
            val botSettings = settings.value
            val result = geminiService.evaluateTwoCandleSignal(
                setup = setup,
                minConfidenceThreshold = botSettings.minConfidenceThreshold
            )

            result.fold(
                onSuccess = { verdict ->
                    val entryPrice = setup.triggerCandle.close
                    val candleTimeLabel = candleEngine.formatCandleTime(currentMinute)
                    val isActionable = verdict.direction != SignalDirection.STANDBY &&
                        verdict.confidence >= botSettings.minConfidenceThreshold

                    if (isActionable) {
                        val locked = LockedSignal(
                            pairSymbol = setup.pair.symbol,
                            pairDisplayName = setup.pair.displayName,
                            candleEpochMinute = currentMinute,
                            candleTimeLabel = candleTimeLabel,
                            direction = verdict.direction,
                            entryPrice = entryPrice,
                            closePrice = null,
                            confidence = verdict.confidence,
                            patternName = verdict.patternTitle,
                            aiRationale = "${verdict.rationale} ${verdict.riskTip}",
                            outcome = SignalOutcome.PENDING,
                            payoutPercent = setup.pair.payoutPercent,
                            lockedAtMillis = System.currentTimeMillis()
                        )
                        val inserted = signalRepository.lockSignalNoRepaint(locked)
                        if (inserted && botSettings.voiceCalloutEnabled) {
                            speakCallout(
                                "${setup.pair.displayName}. One minute ${if (verdict.direction == SignalDirection.BUY) "Buy Call" else "Sell Put"} locked at ${verdict.confidence} percent confidence."
                            )
                        }
                        _terminalState.value = _terminalState.value.copy(
                            activeLockedSignal = locked,
                            aiState = AiEngineState.Ready(
                                summaryText = "${verdict.patternTitle} • ${verdict.rationale} ${verdict.riskTip}"
                            )
                        )
                    } else {
                        _terminalState.value = _terminalState.value.copy(
                            aiState = AiEngineState.Ready(
                                summaryText = "STANDBY (${verdict.confidence}% < ${botSettings.minConfidenceThreshold}% min): ${verdict.rationale}"
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _terminalState.value = _terminalState.value.copy(
                        aiState = AiEngineState.Error(
                            error.localizedMessage ?: "Gemini 3.8 Live signal scan failed."
                        )
                    )
                }
            )
        }
    }

    fun askGeminiLive(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val setup = _terminalState.value.currentSetup ?: return

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _terminalState.value = _terminalState.value.copy(aiState = AiEngineState.Analyzing)
            try {
                geminiService.streamLiveCommentary(setup, trimmed)
                    .catch { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Streaming live commentary error", e)
                        _terminalState.value = _terminalState.value.copy(
                            aiState = AiEngineState.Error(e.localizedMessage ?: "Gemini 3.8 Live stream failed.")
                        )
                    }
                    .collect { partial ->
                        _terminalState.value = _terminalState.value.copy(
                            aiState = AiEngineState.Streaming(partial)
                        )
                    }

                val finalState = _terminalState.value.aiState
                if (finalState is AiEngineState.Streaming) {
                    _terminalState.value = _terminalState.value.copy(
                        aiState = AiEngineState.Ready(finalState.partialText)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "askGeminiLive failed", e)
                _terminalState.value = _terminalState.value.copy(
                    aiState = AiEngineState.Error(e.localizedMessage ?: "Gemini 3.8 Live error.")
                )
            }
        }
    }

    fun analyzeChartScreenshot(bitmap: Bitmap) {
        val pairName = _terminalState.value.activePair.displayName
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _terminalState.value = _terminalState.value.copy(aiState = AiEngineState.Analyzing)
            try {
                geminiService.streamChartImageAnalysis(bitmap, pairName)
                    .catch { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Chart image analysis error", e)
                        _terminalState.value = _terminalState.value.copy(
                            aiState = AiEngineState.Error(e.localizedMessage ?: "Chart vision scan failed.")
                        )
                    }
                    .collect { partial ->
                        _terminalState.value = _terminalState.value.copy(
                            aiState = AiEngineState.Streaming(partial)
                        )
                    }

                val finalState = _terminalState.value.aiState
                if (finalState is AiEngineState.Streaming) {
                    _terminalState.value = _terminalState.value.copy(
                        aiState = AiEngineState.Ready(finalState.partialText)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "analyzeChartScreenshot failed", e)
                _terminalState.value = _terminalState.value.copy(
                    aiState = AiEngineState.Error(e.localizedMessage ?: "Chart image scan failed.")
                )
            }
        }
    }

    fun runDiagnosticCheck() {
        viewModelScope.launch {
            _terminalState.value = _terminalState.value.copy(
                isTestingConnection = true,
                diagnosticMessage = null
            )
            val result = geminiService.testConnection()
            _terminalState.value = _terminalState.value.copy(
                isTestingConnection = false,
                diagnosticMessage = result.fold(
                    onSuccess = { it },
                    onFailure = { "Error: ${it.localizedMessage ?: "App Check / Network error"}" }
                )
            )
        }
    }

    fun formatPrice(price: Double, decimals: Int): String = candleEngine.formatPrice(price, decimals)

    private fun speakCallout(message: String) {
        if (ttsReady) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "finorix_signal_callout")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
