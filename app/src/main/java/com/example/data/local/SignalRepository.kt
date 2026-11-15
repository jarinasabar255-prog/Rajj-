package com.example.data.local

import com.example.domain.LockedSignal
import com.example.domain.SignalOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SignalRepository(private val signalDao: SignalDao) {

    val allSignals: Flow<List<LockedSignal>> = signalDao.observeAllSignals()
        .map { list -> list.map { it.toDomain() } }

    fun signalsForPair(pairSymbol: String): Flow<List<LockedSignal>> =
        signalDao.observeSignalsForPair(pairSymbol)
            .map { list -> list.map { it.toDomain() } }

    suspend fun lockSignalNoRepaint(signal: LockedSignal): Boolean {
        val insertedId = signalDao.insertNoRepaintSignal(SignalEntity.fromDomain(signal))
        return insertedId != -1L
    }

    suspend fun getPendingSignals(): List<LockedSignal> =
        signalDao.getPendingSignals().map { it.toDomain() }

    suspend fun settleSignal(id: Long, closePrice: Double, outcome: SignalOutcome) {
        signalDao.settleSignalOutcome(id = id, closePrice = closePrice, outcome = outcome.name)
    }

    suspend fun clearAllSignals() {
        signalDao.clearAll()
    }
}
