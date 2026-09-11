package com.cryptopulse.data.remote

import android.util.Base64
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OKXExchangeProvider(
    private val okxApi: OKXApi,
    private val binancePublicApi: BinancePublicApi
) : ExchangeProvider {

    override val name: String = "OKX"

    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.API_KEY,
        ExchangeField.SECRET_KEY,
        ExchangeField.PASSPHRASE
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val apiKey = keys[ExchangeField.API_KEY]?.trim()
        val secretKey = keys[ExchangeField.SECRET_KEY]?.trim()
        val passphrase = keys[ExchangeField.PASSPHRASE]?.trim() ?: ""

        if (apiKey.isNullOrEmpty() || secretKey.isNullOrEmpty()) {
            throw IllegalArgumentException("OKX API Key and Secret Key are required")
        }

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = df.format(Date())
        val requestPath = "/api/v5/account/balances"
        val method = "GET"
        val originString = "$timestamp$method$requestPath"
        val signature = hmacSha256(secretKey, originString)

        Log.d("CryptoPulse", "OKX Sync: Fetching balances...")
        val response = okxApi.getAccountBalances(
            apiKey = apiKey,
            passphrase = passphrase,
            timestamp = timestamp,
            signature = signature
        )

        val details = response.data?.flatMap { it.details ?: emptyList() } ?: emptyList()
        val balanceInfos = details.mapNotNull { dto ->
            val currency = dto.currency?.uppercase(Locale.US) ?: return@mapNotNull null
            val freeAmount = dto.availableBalance?.toDoubleOrNull() ?: 0.0
            val lockedAmount = dto.frozenBalance?.toDoubleOrNull() ?: 0.0
            if (freeAmount > 0 || lockedAmount > 0) {
                BalanceInfo(asset = currency, free = freeAmount, locked = lockedAmount)
            } else null
        }

        Log.d("CryptoPulse", "OKX returned ${balanceInfos.size} non-zero asset balances")
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
            Log.e("CryptoPulse", "OKX fetchPrices failed: ${e.message}")
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
            Log.e("CryptoPulse", "OKX fetchTickers failed: ${e.message}")
            emptyList()
        }
    }

    private fun hmacSha256(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hmacData = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hmacData, Base64.NO_WRAP)
    }
}
