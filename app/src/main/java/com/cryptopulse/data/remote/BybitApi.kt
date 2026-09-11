package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface BybitApi {
    @GET("v5/account/wallet-balance")
    suspend fun getWalletBalance(
        @Header("X-BAPI-API-KEY") apiKey: String,
        @Header("X-BAPI-TIMESTAMP") timestamp: String,
        @Header("X-BAPI-SIGN") signature: String,
        @Header("X-BAPI-RECV-WINDOW") recvWindow: String = "5000",
        @Query("accountType") accountType: String = "SPOT"
    ): BybitResponseDto
}

data class BybitResponseDto(
    @SerializedName("result")
    val result: BybitResultDto?
)

data class BybitResultDto(
    @SerializedName("list")
    val list: List<BybitAccountListDto>?
)

data class BybitAccountListDto(
    @SerializedName("coin")
    val coin: List<BybitCoinDto>?
)

data class BybitCoinDto(
    @SerializedName("coin")
    val coin: String?,
    @SerializedName("walletBalance")
    val walletBalance: String?,
    @SerializedName("free")
    val free: String?,
    @SerializedName("locked")
    val locked: String?
)
