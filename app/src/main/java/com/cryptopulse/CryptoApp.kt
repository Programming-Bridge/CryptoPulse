package com.cryptopulse

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.cryptopulse.BuildConfig
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.cryptopulse.data.local.CryptoDatabase
import com.cryptopulse.data.remote.BinanceApi
import com.cryptopulse.data.remote.BinanceExchangeProvider
import com.cryptopulse.data.remote.CoinGeckoApi
import com.cryptopulse.data.remote.CoinCapApi
import com.cryptopulse.data.remote.CoinPaprikaApi
import com.cryptopulse.data.remote.ExchangeProvider
import com.cryptopulse.data.remote.KuCoinApi
import com.cryptopulse.data.remote.KuCoinExchangeProvider
import com.cryptopulse.data.remote.MockWalletProvider
import com.cryptopulse.data.repository.CryptoRepository
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicLong

class CryptoApp : Application(), ImageLoaderFactory {

    private val rateLimitExpiry = AtomicLong(0)

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            android.util.Log.d("CryptoPulseAPI", message)
        }
        logging.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }

        val rateLimitInterceptor = Interceptor { chain ->
            val now = System.currentTimeMillis()
            if (now < rateLimitExpiry.get()) {
                val waitSeconds = (rateLimitExpiry.get() - now) / 1000
                throw java.io.IOException("Rate limit cooldown active. Try again in $waitSeconds seconds.")
            }

            val response = chain.proceed(chain.request())
            if (response.code == 429) {
                // Set global lock for 60 seconds
                rateLimitExpiry.set(System.currentTimeMillis() + 60_000L)
            }
            response
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(rateLimitInterceptor)
            .build()
    }
    
    val repository: CryptoRepository by lazy {
        val db = Room.databaseBuilder(
            applicationContext,
            CryptoDatabase::class.java,
            "crypto_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

        val cgRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.COINGECKO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val binanceApiRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BINANCE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val api = cgRetrofit.create(CoinGeckoApi::class.java)
        val binanceApi = binanceApiRetrofit.create(BinanceApi::class.java)

        val coinCapRetrofit = Retrofit.Builder()
            .baseUrl("https://api.coincap.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val coinCapApi = coinCapRetrofit.create(CoinCapApi::class.java)

        val paprikaRetrofit = Retrofit.Builder()
            .baseUrl("https://api.coinpaprika.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val coinPaprikaApi = paprikaRetrofit.create(CoinPaprikaApi::class.java)
        
        CryptoRepository(api, binanceApi, coinCapApi, coinPaprikaApi, db.coinDao)
    }

    val providers: List<ExchangeProvider> by lazy {
        val binanceRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BINANCE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val binanceApi = binanceRetrofit.create(BinanceApi::class.java)
        
        val kuCoinRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.KUCOIN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val kuCoinApi = kuCoinRetrofit.create(KuCoinApi::class.java)

        listOf(
            BinanceExchangeProvider(binanceApi),
            KuCoinExchangeProvider(kuCoinApi),
            MockWalletProvider()
        )
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
