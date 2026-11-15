package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.LockedSignal
import com.example.domain.SignalDirection
import com.example.domain.SignalOutcome

@Entity(
    tableName = "locked_signals",
    indices = [
        Index(value = ["pairSymbol", "candleEpochMinute"], unique = true)
    ]
)
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pairSymbol: String,
    val pairDisplayName: String,
    val candleEpochMinute: Long,
    val candleTimeLabel: String,
    val direction: String,
    val entryPrice: Double,
    val closePrice: Double?,
    val confidence: Int,
    val patternName: String,
    val aiRationale: String,
    val outcome: String,
    val payoutPercent: Int,
    val lockedAtMillis: Long,
    val modelTag: String
) {
    fun toDomain(): LockedSignal = LockedSignal(
        id = id,
        pairSymbol = pairSymbol,
        pairDisplayName = pairDisplayName,
        candleEpochMinute = candleEpochMinute,
        candleTimeLabel = candleTimeLabel,
        direction = runCatching { SignalDirection.valueOf(direction) }.getOrDefault(SignalDirection.STANDBY),
        entryPrice = entryPrice,
        closePrice = closePrice,
        confidence = confidence,
        patternName = patternName,
        aiRationale = aiRationale,
        outcome = runCatching { SignalOutcome.valueOf(outcome) }.getOrDefault(SignalOutcome.PENDING),
        payoutPercent = payoutPercent,
        lockedAtMillis = lockedAtMillis,
        modelTag = modelTag
    )

    companion object {
        fun fromDomain(signal: LockedSignal): SignalEntity = SignalEntity(
            id = signal.id,
            pairSymbol = signal.pairSymbol,
            pairDisplayName = signal.pairDisplayName,
            candleEpochMinute = signal.candleEpochMinute,
            candleTimeLabel = signal.candleTimeLabel,
            direction = signal.direction.name,
            entryPrice = signal.entryPrice,
            closePrice = signal.closePrice,
            confidence = signal.confidence,
            patternName = signal.patternName,
            aiRationale = signal.aiRationale,
            outcome = signal.outcome.name,
            payoutPercent = signal.payoutPercent,
            lockedAtMillis = signal.lockedAtMillis,
            modelTag = signal.modelTag
        )
    }
}
