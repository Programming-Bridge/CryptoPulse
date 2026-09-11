package com.cryptopulse.data.remote

import android.util.Base64
import android.util.Log
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class KuCoinExchangeProvider(
    private val kuCoinApi: KuCoinApi
) : ExchangeProvider {

    override val name: String = "KuCoin"

    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.API_KEY,
        ExchangeField.SECRET_KEY,
        ExchangeField.PASSPHRASE
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val apiKey = keys[ExchangeField.API_KEY] ?: throw IllegalArgumentException("API Key missing")
        val secretKey = keys[ExchangeField.SECRET_KEY] ?: throw IllegalArgumentException("Secret Key missing")
        val passphrase = keys[ExchangeField.PASSPHRASE] ?: throw IllegalArgumentException("Passphrase missing")

        val timestamp = System.currentTimeMillis()
        
        // 1. Sign Passphrase
        val signedPassphrase = generateHmacBase64(passphrase, secretKey)
        
        // 2. Sign Request (timestamp + method + endpoint + body)
        // For GET /api/v1/accounts, body is empty
        val strToSign = "$timestamp" + "GET" + "/api/v1/accounts"
        val signature = generateHmacBase64(strToSign, secretKey)

        Log.d("CryptoPulse", "KuCoin Sync: Fetching accounts...")
        
        val response = kuCoinApi.getAccounts(
            apiKey = apiKey,
            signature = signature,
            timestamp = timestamp,
            passphrase = signedPassphrase
        )

        if (response.code != "200000") {
            Log.e("CryptoPulse", "KuCoin API Error Code: ${response.code}")
            throw Exception("KuCoin API Error: ${response.code}")
        }

        val accounts = response.data
        Log.d("CryptoPulse", "KuCoin returned ${accounts.size} account entries")

        val balances = accounts.map { 
            val free = it.available.toDoubleOrNull() ?: 0.0
            val locked = it.holds.toDoubleOrNull() ?: 0.0
            if (free > 0 || locked > 0) {
                Log.d("CryptoPulse", "KuCoin Asset: ${it.currency}, free=$free, locked=$locked")
            }
            BalanceInfo(
                asset = it.currency,
                free = free,
                locked = locked
            )
        }
        
        return balances
    }

    override suspend fun fetchPrices(): Map<String, Double> {
        return try {
            val response = kuCoinApi.getAllTickers()
            if (response.code == "200000") {
                val prices = mutableMapOf<String, Double>()
                
                // 1. Process USDT pairs first (The most reliable base)
                response.data.ticker.filter { it.symbol.endsWith("-USDT") }.forEach { 
                    val asset = it.symbol.removeSuffix("-USDT")
                    prices[asset] = it.last.toDoubleOrNull() ?: 0.0
                }
                
                // 2. Process BTC pairs for assets not yet found in USDT
                val btcPrice = prices["BTC"] ?: 0.0
                if (btcPrice > 0) {
                    response.data.ticker.filter { it.symbol.endsWith("-BTC") }.forEach {
                        val asset = it.symbol.removeSuffix("-BTC")
                        if (!prices.containsKey(asset)) {
                            val priceInBtc = it.last.toDoubleOrNull() ?: 0.0
                            prices[asset] = priceInBtc * btcPrice
                        }
                    }
                }

                // 3. Special Case: USDT itself
                if (!prices.containsKey("USDT")) {
                    prices["USDT"] = 1.0
                }
                
                Log.d("CryptoPulse", "KuCoin: Fetched ${prices.size} prices from tickers")
                prices
            } else {
                Log.e("CryptoPulse", "KuCoin: AllTickers error: ${response.code}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("CryptoPulse", "KuCoin: AllTickers fetch failed: ${e.message}")
            emptyMap()
        }
    }

    override suspend fun fetchTickers(): List<TickerInfo> {
        return try {
            val response = kuCoinApi.getAllTickers()
            if (response.code == "200000") {
                response.data.ticker
                    .filter { it.symbol.endsWith("-USDT") }
                    .map { 
                        TickerInfo(
                            symbol = it.symbol.removeSuffix("-USDT"),
                            price = it.last.toDoubleOrNull() ?: 0.0,
                            change24h = (it.changeRate.toDoubleOrNull() ?: 0.0) * 100, // API provides decimal like 0.02
                            volume24h = it.volValue.toDoubleOrNull() ?: 0.0
                        )
                    }
                    .sortedByDescending { it.volume24h }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("CryptoPulse", "KuCoin: fetchTickers failed", e)
            emptyList()
        }
    }

    private fun generateHmacBase64(data: String, secret: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        sha256HMAC.init(secretKey)
        val hash = sha256HMAC.doFinal(data.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
