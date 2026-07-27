package com.cryptopulse.data.remote

import kotlinx.coroutines.delay

class MockWalletProvider : ExchangeProvider {
    override val name: String = "MetaMask"
    
    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.WALLET_ADDRESS
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val address = keys[ExchangeField.WALLET_ADDRESS] ?: throw IllegalArgumentException("Address missing")
        
        // Simulate API call
        delay(1000)
        
        return listOf(
            BalanceInfo("ETH", 1.5, 0.0),
            BalanceInfo("LINK", 100.0, 0.0)
        )
    }

    override suspend fun fetchPrices(): Map<String, Double> = emptyMap()
}
