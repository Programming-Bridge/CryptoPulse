package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header

interface GateIoApi {
    @GET("api/v4/spot/accounts")
    suspend fun getSpotAccounts(
        @Header("KEY") apiKey: String,
        @Header("Timestamp") timestamp: String,
        @Header("SIGN") signature: String
    ): List<GateIoAccountDto>
}

data class GateIoAccountDto(
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("available")
    val available: String?,
    @SerializedName("locked")
    val locked: String?
)
