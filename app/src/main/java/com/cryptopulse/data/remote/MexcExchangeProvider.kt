package com.cryptopulse.data.remote

import android.util.Log
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Locale

class MexcExchangeProvider(
    private val mexcApi: MexcApi,
    private val binancePublicApi: BinancePublicApi
) : ExchangeProvider {

    override val name: String = "MEXC"

    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.API_KEY,
        ExchangeField.SECRET_KEY
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val apiKey = keys[ExchangeField.API_KEY]?.trim()
        val secretKey = keys[ExchangeField.SECRET_KEY]?.trim()

        if (apiKey.isNullOrEmpty() || secretKey.isNullOrEmpty()) {
            throw IllegalArgumentException("MEXC API Key and Secret Key are required")
        }

        val timestamp = System.currentTimeMillis()
        val queryString = "timestamp=$timestamp"
        val signature = hmacSha256(secretKey, queryString)

        Log.d("CryptoPulse", "MEXC Sync: Fetching balances...")
        val response = mexcApi.getAccount(
            apiKey = apiKey,
            timestamp = timestamp,
            signature = signature
        )

        val balances = response.balances ?: emptyList()
        val balanceInfos = balances.mapNotNull { dto ->
            val currency = dto.asset?.uppercase(Locale.US) ?: return@mapNotNull null
            val freeAmount = dto.free?.toDoubleOrNull() ?: 0.0
            val lockedAmount = dto.locked?.toDoubleOrNull() ?: 0.0
            if (freeAmount > 0 || lockedAmount > 0) {
                BalanceInfo(asset = currency, free = freeAmount, locked = lockedAmount)
            } else null
        }

        Log.d("CryptoPulse", "MEXC returned ${balanceInfos.size} non-zero asset balances")
        return balanceInfos
    }

    override suspend fun fetchPrices(): Map<String, Double> {
        return try {
            val tickers = binancePublicApi.get24hrTickers()
            tickers.mapNotNull { dto ->
                if (dto.symbol.endsWith("USDT")) {
                    val baseSymbol = dto.symbol.removeSuffix("USDT").uppercase(Locale.US)
                    val price = dto.lastPrice?.toDoubleOrNull() ?: 0.0
                    if (price > 0) baseSymbol to price else null
                } else null
            }.toMap()
        } catch (e: Exception) {
            Log.e("CryptoPulse", "MEXC fetchPrices failed: ${e.message}")
            emptyMap()
        }
    }

    override suspend fun fetchTickers(): List<TickerInfo> {
        return try {
            val raw = binancePublicApi.get24hrTickers()
            raw.filter { it.symbol.endsWith("USDT") }.mapNotNull { dto ->
                val baseSymbol = dto.symbol.removeSuffix("USDT").uppercase(Locale.US)
                val price = dto.lastPrice?.toDoubleOrNull() ?: 0.0
                val change = dto.priceChangePercent?.toDoubleOrNull() ?: 0.0
                val vol = dto.quoteVolume?.toDoubleOrNull() ?: 0.0
                if (price > 0) {
                    TickerInfo(symbol = baseSymbol, price = price, change24h = change, volume24h = vol)
                } else null
            }
        } catch (e: Exception) {
            Log.e("CryptoPulse", "MEXC fetchTickers failed: ${e.message}")
            emptyList()
        }
    }

    private fun hmacSha256(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hmacData = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hmacData.joinToString("") { "%02x".format(it) }
    }
}
