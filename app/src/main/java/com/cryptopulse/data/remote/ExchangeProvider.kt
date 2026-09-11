package com.cryptopulse.data.remote

interface ExchangeProvider {
    val name: String
    val requiredFields: List<ExchangeField>
    suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo>
    suspend fun fetchPrices(): Map<String, Double>
    suspend fun fetchTickers(): List<TickerInfo> = emptyList()
}

data class TickerInfo(
    val symbol: String,
    val price: Double,
    val change24h: Double,
    val volume24h: Double
)

data class BalanceInfo(
    val asset: String,
    val free: Double,
    val locked: Double
)
