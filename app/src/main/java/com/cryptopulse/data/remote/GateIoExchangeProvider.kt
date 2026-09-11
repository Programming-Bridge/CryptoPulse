package com.cryptopulse.data.remote

import android.util.Log
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class GateIoExchangeProvider(
    private val gateIoApi: GateIoApi,
    private val binancePublicApi: BinancePublicApi
) : ExchangeProvider {

    override val name: String = "Gate.io"

    override val requiredFields: List<ExchangeField> = listOf(
        ExchangeField.API_KEY,
        ExchangeField.SECRET_KEY
    )

    override suspend fun fetchBalances(keys: Map<ExchangeField, String>): List<BalanceInfo> {
        val apiKey = keys[ExchangeField.API_KEY]?.trim()
        val secretKey = keys[ExchangeField.SECRET_KEY]?.trim()

        if (apiKey.isNullOrEmpty() || secretKey.isNullOrEmpty()) {
            throw IllegalArgumentException("Gate.io API Key and Secret Key are required")
        }

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val method = "GET"
        val requestPath = "/api/v4/spot/accounts"
        val queryString = ""
        val bodyHash = sha512Hex("")

        val signString = "$method\n$requestPath\n$queryString\n$bodyHash\n$timestamp"
        val signature = hmacSha512(secretKey, signString)

        Log.d("CryptoPulse", "Gate.io Sync: Fetching spot accounts...")
        val accounts = gateIoApi.getSpotAccounts(
            apiKey = apiKey,
            timestamp = timestamp,
            signature = signature
        )

        val balanceInfos = accounts.mapNotNull { dto ->
            val currency = dto.currency?.uppercase(Locale.US) ?: return@mapNotNull null
            val freeAmount = dto.available?.toDoubleOrNull() ?: 0.0
            val lockedAmount = dto.locked?.toDoubleOrNull() ?: 0.0
            if (freeAmount > 0 || lockedAmount > 0) {
                BalanceInfo(asset = currency, free = freeAmount, locked = lockedAmount)
            } else null
        }

        Log.d("CryptoPulse", "Gate.io returned ${balanceInfos.size} non-zero asset balances")
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
            Log.e("CryptoPulse", "Gate.io fetchPrices failed: ${e.message}")
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
            Log.e("CryptoPulse", "Gate.io fetchTickers failed: ${e.message}")
            emptyList()
        }
    }

    private fun sha512Hex(data: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(data.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha512(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA512")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA512")
        mac.init(secretKeySpec)
        val hmacData = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hmacData.joinToString("") { "%02x".format(it) }
    }
}
