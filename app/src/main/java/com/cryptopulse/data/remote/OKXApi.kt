package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header

interface OKXApi {
    @GET("api/v5/account/balances")
    suspend fun getAccountBalances(
        @Header("OK-ACCESS-KEY") apiKey: String,
        @Header("OK-ACCESS-PASSPHRASE") passphrase: String,
        @Header("OK-ACCESS-TIMESTAMP") timestamp: String,
        @Header("OK-ACCESS-SIGN") signature: String
    ): OKXBalancesDto
}

data class OKXBalancesDto(
    @SerializedName("data")
    val data: List<OKXDataDto>?
)

data class OKXDataDto(
    @SerializedName("details")
    val details: List<OKXDetailDto>?
)

data class OKXDetailDto(
    @SerializedName("ccy")
    val currency: String?,
    @SerializedName("availBal")
    val availableBalance: String?,
    @SerializedName("frozenBal")
    val frozenBalance: String?
)
