package com.cryptopulse

import android.app.Application
import android.util.Log
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.cryptopulse.BuildConfig
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.cryptopulse.data.local.CryptoDatabase
import com.cryptopulse.data.remote.BinanceExchangeProvider
import com.cryptopulse.data.remote.BinancePrivateApi
import com.cryptopulse.data.remote.BinancePublicApi
import com.cryptopulse.data.remote.CoinbaseApi
import com.cryptopulse.data.remote.CoinbaseExchangeProvider
import com.cryptopulse.data.remote.ExchangeProvider
import com.cryptopulse.data.remote.ExchangeRegistry
import com.cryptopulse.data.remote.KuCoinApi
import com.cryptopulse.data.remote.KuCoinExchangeProvider
import com.cryptopulse.data.repository.CryptoRepository
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class CryptoApp : Application(), ImageLoaderFactory {

    private val rateLimitExpiry = AtomicLong(0)

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("CryptoPulseAPI", message)
        }
        logging.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }

        val rateLimitInterceptor = Interceptor { chain ->
            val now = System.currentTimeMillis()
            val expiry = rateLimitExpiry.get()
            if (now < expiry) {
                val waitSeconds = (expiry - now) / 1000
                throw IOException("Rate limit cooldown active. Try again in $waitSeconds seconds.")
            }

            val response = chain.proceed(chain.request())
            if (response.code == 429) {
                // Set global lock for 60 seconds
                rateLimitExpiry.set(System.currentTimeMillis() + 60_000L)
            }
            response
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(rateLimitInterceptor)
            .build()
    }

    private val binanceRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BINANCE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val kuCoinRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.KUCOIN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val coinbaseRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coinbase.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val binancePublicApi: BinancePublicApi by lazy {
        binanceRetrofit.create(BinancePublicApi::class.java)
    }

    val binancePrivateApi: BinancePrivateApi by lazy {
        binanceRetrofit.create(BinancePrivateApi::class.java)
    }

    val kuCoinApi: KuCoinApi by lazy {
        kuCoinRetrofit.create(KuCoinApi::class.java)
    }

    val coinbaseApi: CoinbaseApi by lazy {
        coinbaseRetrofit.create(CoinbaseApi::class.java)
    }

    val exchangeRegistry: ExchangeRegistry by lazy {
        ExchangeRegistry(
            listOf(
                BinanceExchangeProvider(binancePrivateApi, binancePublicApi),
                CoinbaseExchangeProvider(coinbaseApi, binancePublicApi),
                KuCoinExchangeProvider(kuCoinApi)
            )
        )
    }

    val providers: List<ExchangeProvider> by lazy {
        exchangeRegistry.getAllProviders()
    }

    val repository: CryptoRepository by lazy {
        val db = Room.databaseBuilder(
            applicationContext,
            CryptoDatabase::class.java,
            "crypto_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

        CryptoRepository(binancePublicApi, db.coinDao)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
    }
}
