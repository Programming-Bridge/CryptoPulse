package com.cryptopulse.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header

interface CoinbaseApi {
    @GET("v2/accounts")
    suspend fun getAccounts(
        @Header("CB-ACCESS-KEY") apiKey: String,
        @Header("CB-ACCESS-SIGN") signature: String,
        @Header("CB-ACCESS-TIMESTAMP") timestamp: String,
        @Header("CB-VERSION") version: String = "2024-01-01"
    ): CoinbaseAccountsDto
}

data class CoinbaseAccountsDto(
    @SerializedName("data")
    val data: List<CoinbaseAccountDataDto>?
)

data class CoinbaseAccountDataDto(
    @SerializedName("currency")
    val currency: CoinbaseCurrencyDto?,
    @SerializedName("balance")
    val balance: CoinbaseBalanceDto?
)

data class CoinbaseCurrencyDto(
    @SerializedName("code")
    val code: String?
)

data class CoinbaseBalanceDto(
    @SerializedName("amount")
    val amount: String?
)
