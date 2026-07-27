package com.cryptopulse.data.remote

import android.util.Log
import okhttp3.FormBody
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BinanceExchangeProvider(
    private val binanceApi: BinanceApi
) : ExchangeProvider {
    
    override val name: String = "Binance"
    
    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.API_KEY,
        ExchangeField.SECRET_KEY
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val apiKey = keys[ExchangeField.API_KEY] ?: throw IllegalArgumentException("API Key missing")
        val secretKey = keys[ExchangeField.SECRET_KEY] ?: throw IllegalArgumentException("Secret Key missing")
        
        val balanceMap = mutableMapOf<String, BalanceInfo>()

        fun addBalance(asset: String, free: Double, locked: Double) {
            val existing = balanceMap[asset]
            if (existing != null) {
                balanceMap[asset] = existing.copy(
                    free = existing.free + free,
                    locked = existing.locked + locked
                )
            } else {
                balanceMap[asset] = BalanceInfo(asset, free, locked)
            }
        }

        fun getTimestamp() = System.currentTimeMillis()

        try {
            // 1. Spot Balances
            val ts = getTimestamp()
            val query = "timestamp=$ts&recvWindow=60000"
            val sig = generateSignature(query, secretKey)
            val accountInfo = binanceApi.getAccountInfo(apiKey, ts, sig)
            accountInfo.balances.forEach { 
                addBalance(it.asset, it.free.toDoubleOrNull() ?: 0.0, it.locked.toDoubleOrNull() ?: 0.0)
            }
            Log.d("CryptoPulse", "Fetched Spot balances")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error fetching Spot balances: ${e.message}")
        }

        try {
            // 2. Funding / User Assets (Consolidated)
            val ts = getTimestamp()
            val payload = "timestamp=$ts&needBtcValuation=false&recvWindow=60000"
            val sig = generateSignature(payload, secretKey)
            
            val body = FormBody.Builder()
                .add("timestamp", ts.toString())
                .add("needBtcValuation", "false")
                .add("recvWindow", "60000")
                .add("signature", sig) // MUST BE LAST
                .build()

            val userAssets = binanceApi.getUserAssets(apiKey, body)
            userAssets.forEach { 
                addBalance(it.asset, it.free.toDoubleOrNull() ?: 0.0, it.locked.toDoubleOrNull() ?: 0.0)
            }
            Log.d("CryptoPulse", "Fetched Funding balances")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error fetching Funding balances: ${e.message}")
        }

        try {
            // 3. Simple Earn Flexible
            val ts = getTimestamp()
            val query = "timestamp=$ts&recvWindow=60000"
            val sig = generateSignature(query, secretKey)
            val flexible = binanceApi.getSimpleEarnFlexiblePosition(apiKey, ts, sig)
            flexible.rows.forEach { 
                addBalance(it.asset, it.totalAmount.toDoubleOrNull() ?: 0.0, 0.0)
            }
            Log.d("CryptoPulse", "Fetched Simple Earn Flexible balances")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error fetching Earn Flexible: ${e.message}")
        }

        try {
            // 4. Simple Earn Locked
            val ts = getTimestamp()
            val query = "timestamp=$ts&recvWindow=60000"
            val sig = generateSignature(query, secretKey)
            val locked = binanceApi.getSimpleEarnLockedPosition(apiKey, ts, sig)
            locked.rows.forEach { 
                addBalance(it.asset, it.totalAmount.toDoubleOrNull() ?: 0.0, 0.0)
            }
            Log.d("CryptoPulse", "Fetched Simple Earn Locked balances")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error fetching Earn Locked: ${e.message}")
        }

        val finalBalances = balanceMap.values.toList()
        Log.d("CryptoPulse", "Total unique assets aggregated: ${finalBalances.size}")
        
        finalBalances.forEach { 
            if (it.free > 0 || it.locked > 0) {
                Log.d("CryptoPulse", "Aggregated Asset: ${it.asset}, total=${it.free + it.locked}")
            }
        }
        
        return finalBalances
    }

    override suspend fun fetchPrices(): Map<String, Double> {
        return try {
            val tickers = binanceApi.getTickerPrices()
            // Map USDT pairs (e.g., BTCUSDT -> BTC) to prices
            tickers.filter { it.symbol.endsWith("USDT") }
                .associate { 
                    it.symbol.removeSuffix("USDT") to (it.price.toDoubleOrNull() ?: 0.0) 
                }
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Binance: Price fetch failed: ${e.message}")
            emptyMap()
        }
    }

    private fun generateSignature(data: String, secret: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        sha256HMAC.init(secretKey)
        return sha256HMAC.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
