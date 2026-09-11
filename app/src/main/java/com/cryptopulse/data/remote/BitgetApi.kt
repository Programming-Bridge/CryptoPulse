package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header

interface BitgetApi {
    @GET("api/v2/spot/account/assets")
    suspend fun getSpotAssets(
        @Header("ACCESS-KEY") apiKey: String,
        @Header("ACCESS-PASSPHRASE") passphrase: String,
        @Header("ACCESS-TIMESTAMP") timestamp: String,
        @Header("ACCESS-SIGN") signature: String
    ): BitgetAssetsDto
}

data class BitgetAssetsDto(
    @SerializedName("data")
    val data: List<BitgetCoinAssetDto>?
)

data class BitgetCoinAssetDto(
    @SerializedName("coin")
    val coin: String?,
    @SerializedName("available")
    val available: String?,
    @SerializedName("frozen")
    val frozen: String?,
    @SerializedName("locked")
    val locked: String?
)
