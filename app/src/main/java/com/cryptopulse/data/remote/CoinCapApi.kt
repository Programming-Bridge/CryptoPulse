package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinCapApi {
    @GET("v2/assets")
    suspend fun getAssets(
        @Query("limit") limit: Int = 500
    ): CoinCapResponse
}

data class CoinCapResponse(
    val data: List<CoinCapDto>,
    val timestamp: Long
)

data class CoinCapDto(
    val id: String,
    val rank: String,
    val symbol: String,
    val name: String,
    val priceUsd: String,
    val changePercent24Hr: String?,
    val marketCapUsd: String?,
    val volumeUsd24Hr: String?,
    val supply: String?
)
