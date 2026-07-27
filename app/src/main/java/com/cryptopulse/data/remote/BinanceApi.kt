package com.cryptopulse.data.remote

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface BinanceApi {
    @GET("api/v3/account")
    suspend fun getAccountInfo(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Query("recvWindow") recvWindow: Long = 60000
    ): BinanceAccountResponse

    @POST("sapi/v3/asset/getUserAsset")
    suspend fun getUserAssets(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Body body: RequestBody
    ): List<BinanceUserAsset>

    @GET("sapi/v1/simple-earn/flexible/position")
    suspend fun getSimpleEarnFlexiblePosition(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Query("recvWindow") recvWindow: Long = 60000
    ): BinanceSimpleEarnResponse

    @GET("sapi/v1/simple-earn/locked/position")
    suspend fun getSimpleEarnLockedPosition(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Query("recvWindow") recvWindow: Long = 60000
    ): BinanceSimpleEarnResponse

    @GET("api/v3/ticker/price")
    suspend fun getTickerPrices(): List<BinanceTickerPrice>

    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTicker(): List<Binance24hrTicker>
}

data class Binance24hrTicker(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String,
    val marketCap: String? = null // Binance doesn't return MC in this endpoint, but we can store price
)

data class BinanceAccountResponse(
    val balances: List<BinanceBalance>
)

data class BinanceBalance(
    val asset: String,
    val free: String,
    val locked: String
)

data class BinanceUserAsset(
    val asset: String,
    val free: String,
    val locked: String,
    val freeze: String,
    val withdrawing: String,
    val ipoing: String,
    val ipoable: String,
    val storage: String
)

data class BinanceSimpleEarnResponse(
    val rows: List<BinanceEarnRow>,
    val total: Int
)

data class BinanceEarnRow(
    val asset: String,
    val totalAmount: String,
    val freeAmount: String,
    val lockedAmount: String? = null
)

data class BinanceTickerPrice(
    val symbol: String,
    val price: String
)
