package com.cryptopulse.data.remote

import com.cryptopulse.data.models.CoinDto
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("coins/markets")
    suspend fun getCoins(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String? = null,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 500,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") priceChange: String = "24h"
    ): List<CoinDto>

    @GET("coins/{id}")
    suspend fun getCoinDetails(
        @Path("id") id: String,
        @Query("localization") localization: Boolean = false,
        @Query("tickers") tickers: Boolean = false,
        @Query("market_data") marketData: Boolean = true,
        @Query("community_data") communityData: Boolean = false,
        @Query("developer_data") developer_data: Boolean = false,
        @Query("sparkline") sparkline: Boolean = false
    ): CoinDto

    @GET("search")
    suspend fun searchCoins(
        @Query("query") query: String
    ): SearchResponse

    @GET("simple/price")
    suspend fun getSimplePrices(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrency: String = "usd",
        @Query("include_24hr_change") includeChange: Boolean = true
    ): Map<String, Map<String, Double>>

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String,
        @Query("interval") interval: String? = null
    ): MarketChartDto

    @GET("coins/list")
    suspend fun getCoinsList(): List<CoinListDto>
}

data class CoinListDto(
    val id: String,
    val symbol: String,
    val name: String
)

data class MarketChartDto(
    val prices: List<List<Double>>
)

data class SearchResponse(
    val coins: List<SearchCoinDto>
)

data class SearchCoinDto(
    val id: String,
    val name: String,
    val symbol: String,
    @SerializedName("large")
    val imageUrl: String
)
