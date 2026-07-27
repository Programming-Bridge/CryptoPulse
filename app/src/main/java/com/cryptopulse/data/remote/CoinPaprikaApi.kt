package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface CoinPaprikaApi {
    @GET("v1/tickers")
    suspend fun getTickers(): List<CoinPaprikaDto>
}

data class CoinPaprikaDto(
    val id: String,
    val name: String,
    val symbol: String,
    val rank: Int,
    val quotes: Map<String, CoinPaprikaQuote>
)

data class CoinPaprikaQuote(
    val price: Double,
    @SerializedName("percent_change_24h")
    val percentChange24h: Double,
    @SerializedName("market_cap")
    val marketCap: Double?
)
