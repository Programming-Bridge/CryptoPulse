package com.cryptopulse.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface BinancePublicApi {
    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTickers(): List<Binance24hrTickerDto>

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1h",
        @Query("limit") limit: Int = 100
    ): List<List<JsonElement>>
}

data class Binance24hrTickerDto(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("lastPrice")
    val lastPrice: String?,
    @SerializedName("priceChangePercent")
    val priceChangePercent: String?,
    @SerializedName("quoteVolume")
    val quoteVolume: String?,
    @SerializedName("highPrice")
    val highPrice: String?,
    @SerializedName("lowPrice")
    val lowPrice: String?,
    @SerializedName("volume")
    val volume: String?
)
