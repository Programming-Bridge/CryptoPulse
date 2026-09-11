package com.cryptopulse.data.remote

import android.util.Log
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Locale

class CoinbaseExchangeProvider(
    private val coinbaseApi: CoinbaseApi,
    private val binancePublicApi: BinancePublicApi
) : ExchangeProvider {

    override val name: String = "Coinbase"

    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.API_KEY,
        ExchangeField.SECRET_KEY
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val apiKey = keys[ExchangeField.API_KEY]?.trim()
        val secretKey = keys[ExchangeField.SECRET_KEY]?.trim()

        if (apiKey.isNullOrEmpty() || secretKey.isNullOrEmpty()) {
            throw IllegalArgumentException("Coinbase API Key and Secret Key are required")
        }

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val requestPath = "/v2/accounts"
        val method = "GET"
        val body = ""
        val message = "$timestamp$method$requestPath$body"
        val signature = hmacSha256(secretKey, message)

        Log.d("CryptoPulse", "Coinbase Sync: Fetching accounts...")
        val response = coinbaseApi.getAccounts(
            apiKey = apiKey,
            signature = signature,
            timestamp = timestamp
        )

        val accounts = response.data ?: emptyList()
        val balanceInfos = accounts.mapNotNull { account ->
            val currencyCode = account.currency?.code?.uppercase(Locale.US) ?: return@mapNotNull null
            val amount = account.balance?.amount?.toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                BalanceInfo(asset = currencyCode, free = amount, locked = 0.0)
            } else null
        }

        Log.d("CryptoPulse", "Coinbase returned ${balanceInfos.size} non-zero asset balances")
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
            Log.e("CryptoPulse", "Coinbase fetchPrices failed: ${e.message}")
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
            Log.e("CryptoPulse", "Coinbase fetchTickers failed: ${e.message}")
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
