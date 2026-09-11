package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface BinancePrivateApi {
    @GET("api/v3/account")
    suspend fun getAccount(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): BinanceAccountResponseDto
}

data class BinanceAccountResponseDto(
    @SerializedName("balances")
    val balances: List<BinanceBalanceDto>
)

data class BinanceBalanceDto(
    @SerializedName("asset")
    val asset: String,
    @SerializedName("free")
    val free: String,
    @SerializedName("locked")
    val locked: String
)
