package com.cryptopulse.data.local

import androidx.room.*
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.data.models.HoldingEntity
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.data.models.SnapshotEntity
import com.cryptopulse.data.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Query("SELECT * FROM coins")
    fun getAllCoins(): Flow<List<CoinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<CoinEntity>)

    @Query("UPDATE coins SET portfolioAmount = :amount WHERE id = :id")
    suspend fun updatePortfolio(id: String, amount: Double)

    // Holdings
    @Query("SELECT * FROM holdings")
    fun getAllHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE coinId = :coinId")
    fun getHoldingsByCoin(coinId: String): Flow<List<HoldingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: HoldingEntity)

    @Query("SELECT * FROM holdings WHERE coinId = :coinId AND source = :source LIMIT 1")
    suspend fun getHolding(coinId: String, source: String): HoldingEntity?

    @Query("UPDATE holdings SET amount = :amount WHERE coinId = :coinId AND source = :source")
    suspend fun updateHoldingAmount(coinId: String, source: String, amount: Double)

    @Query("DELETE FROM holdings WHERE coinId = :coinId")
    suspend fun deleteHoldingsByCoinId(coinId: String)

    @Query("DELETE FROM transactions WHERE coinId = :coinId")
    suspend fun deleteTransactionsByCoinId(coinId: String)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Price Alerts
    @Query("SELECT * FROM price_alerts")
    fun getPriceAlerts(): Flow<List<PriceAlertEntity>>

    @Insert
    suspend fun insertPriceAlert(alert: PriceAlertEntity)

    @Query("UPDATE price_alerts SET isActive = :isActive WHERE id = :id")
    suspend fun updateAlertStatus(id: Int, isActive: Boolean)

    @Delete
    suspend fun deletePriceAlert(alert: PriceAlertEntity)

    @Query("DELETE FROM coins WHERE id = :id")
    suspend fun deleteCoinById(id: String)

    // Snapshots
    @Query("SELECT * FROM portfolio_snapshots ORDER BY date ASC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    // Symbol Map Caching
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbolMap(mapping: List<com.cryptopulse.data.models.SymbolMapEntity>)

    @Query("SELECT * FROM symbol_map")
    suspend fun getSymbolMap(): List<com.cryptopulse.data.models.SymbolMapEntity>

    @Query("SELECT COUNT(*) FROM symbol_map")
    suspend fun getSymbolMapCount(): Int
}
