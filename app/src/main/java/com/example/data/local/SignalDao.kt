package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM locked_signals ORDER BY candleEpochMinute DESC, lockedAtMillis DESC")
    fun observeAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM locked_signals WHERE pairSymbol = :pairSymbol ORDER BY candleEpochMinute DESC LIMIT 20")
    fun observeSignalsForPair(pairSymbol: String): Flow<List<SignalEntity>>

    /**
     * IGNORE conflict strategy ensures an already-locked candle signal is NEVER overwritten or repainted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoRepaintSignal(entity: SignalEntity): Long

    @Query("SELECT * FROM locked_signals WHERE outcome = 'PENDING'")
    suspend fun getPendingSignals(): List<SignalEntity>

    @Query("UPDATE locked_signals SET closePrice = :closePrice, outcome = :outcome WHERE id = :id AND outcome = 'PENDING'")
    suspend fun settleSignalOutcome(id: Long, closePrice: Double, outcome: String)

    @Query("DELETE FROM locked_signals")
    suspend fun clearAll()
}
