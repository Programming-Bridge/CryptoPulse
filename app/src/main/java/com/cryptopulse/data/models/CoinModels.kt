package com.cryptopulse.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class CoinDto(
    val id: String?,
    val symbol: String?,
    val name: String?,
    val image: String?,
    @SerializedName("current_price")
    val currentPrice: Double?,
    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: Double?,
    @SerializedName("market_cap")
    val marketCap: Double?,
    @SerializedName("market_cap_rank")
    val marketCapRank: Int?,
    @SerializedName("total_volume")
    val totalVolume: Double?,
    @SerializedName("high_24h")
    val high24h: Double?,
    @SerializedName("low_24h")
    val low24h: Double?,
    @SerializedName("circulating_supply")
    val circulatingSupply: Double?,
    @SerializedName("sparkline_in_7d")
    val sparkline: SparklineDto?
)

data class SparklineDto(
    val price: List<Double>?
)

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double,
    val priceChangePercentage24h: Double,
    val marketCap: Double = 0.0,
    val totalVolume: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val circulatingSupply: Double = 0.0,
    val sparklineData: List<Double> = emptyList(),
    val isFavorite: Boolean = false,
    val portfolioAmount: Double = 0.0
)

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val source: String, // "Manual", "Binance", "Coinbase", etc.
    val amount: Double
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val coinName: String,
    val coinSymbol: String,
    val date: String,
    val amount: Double,
    val value: Double,
    val type: String, // "In" or "Out"
    val source: String = "Manual"
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val coinName: String,
    val targetPrice: Double,
    val isAbove: Boolean,
    val isActive: Boolean = true
)

@Entity(tableName = "portfolio_snapshots")
data class SnapshotEntity(
    @PrimaryKey val date: String, // "MMM dd" or "YYYY-MM-DD"
    val totalValue: Double
)

