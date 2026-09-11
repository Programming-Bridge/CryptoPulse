package com.cryptopulse.data.remote

class ExchangeRegistry(
    private val providers: List<ExchangeProvider>
) {
    fun getAllProviders(): List<ExchangeProvider> = providers

    fun getProvider(name: String): ExchangeProvider? {
        return providers.find { it.name.equals(name, ignoreCase = true) }
    }
}
