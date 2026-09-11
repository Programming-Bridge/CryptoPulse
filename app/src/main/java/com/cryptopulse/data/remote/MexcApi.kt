package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MexcApi {
    @GET("api/v3/account")
    suspend fun getAccount(
        @Header("X-MEXC-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): MexcAccountResponseDto
}

data class MexcAccountResponseDto(
    @SerializedName("balances")
    val balances: List<MexcBalanceDto>?
)

data class MexcBalanceDto(
    @SerializedName("asset")
    val asset: String?,
    @SerializedName("free")
    val free: String?,
    @SerializedName("locked")
    val locked: String?
)
