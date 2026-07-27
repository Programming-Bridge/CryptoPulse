package com.cryptopulse.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KuCoinApi {
    @GET("api/v1/accounts")
    suspend fun getAccounts(
        @Header("KC-API-KEY") apiKey: String,
        @Header("KC-API-SIGN") signature: String,
        @Header("KC-API-TIMESTAMP") timestamp: Long,
        @Header("KC-API-PASSPHRASE") passphrase: String,
        @Header("KC-API-KEY-VERSION") keyVersion: String = "2"
    ): KuCoinResponse<List<KuCoinAccount>>

    @GET("api/v1/prices")
    suspend fun getFiatPrices(
        @Query("base") base: String = "USD"
    ): KuCoinResponse<Map<String, String>>

    @GET("api/v1/market/allTickers")
    suspend fun getAllTickers(): KuCoinResponse<KuCoinAllTickers>
}

data class KuCoinAllTickers(
    val time: Long,
    val ticker: List<KuCoinTicker>
)

data class KuCoinTicker(
    val symbol: String,
    val symbolName: String,
    val last: String,
    val changeRate: String,
    val vol: String,
    val volValue: String
)

data class KuCoinResponse<T>(
    val code: String,
    val data: T
)

data class KuCoinAccount(
    val currency: String,
    val type: String,
    val balance: String,
    val available: String,
    val holds: String
)
