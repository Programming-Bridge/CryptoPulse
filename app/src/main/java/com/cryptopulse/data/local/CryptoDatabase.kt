package com.cryptopulse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.data.models.HoldingEntity
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.data.models.SnapshotEntity
import com.cryptopulse.data.models.TransactionEntity

@Database(entities = [CoinEntity::class, TransactionEntity::class, PriceAlertEntity::class, HoldingEntity::class, SnapshotEntity::class], version = 9, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class CryptoDatabase : RoomDatabase() {
    abstract val coinDao: CoinDao
}
