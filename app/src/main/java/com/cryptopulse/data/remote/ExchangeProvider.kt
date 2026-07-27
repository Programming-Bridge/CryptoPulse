package com.cryptopulse.data.remote

interface ExchangeProvider {
    val name: String
    val requiredFields: List<ExchangeField>
    suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo>
    suspend fun fetchPrices(): Map<String, Double>
}

data class BalanceInfo(
    val asset: String,
    val free: Double,
    val locked: Double
)
