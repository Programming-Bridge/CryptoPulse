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
import com.cryptopulse.data.remote.BitgetApi
import com.cryptopulse.data.remote.BitgetExchangeProvider
import com.cryptopulse.data.remote.BybitApi
import com.cryptopulse.data.remote.BybitExchangeProvider
import com.cryptopulse.data.remote.CoinbaseApi
import com.cryptopulse.data.remote.CoinbaseExchangeProvider
import com.cryptopulse.data.remote.ExchangeProvider
import com.cryptopulse.data.remote.ExchangeRegistry
import com.cryptopulse.data.remote.GateIoApi
import com.cryptopulse.data.remote.GateIoExchangeProvider
import com.cryptopulse.data.remote.KuCoinApi
import com.cryptopulse.data.remote.KuCoinExchangeProvider
import com.cryptopulse.data.remote.MexcApi
import com.cryptopulse.data.remote.MexcExchangeProvider
import com.cryptopulse.data.remote.OKXApi
import com.cryptopulse.data.remote.OKXExchangeProvider
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

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val binancePublicApi: BinancePublicApi by lazy {
        createRetrofit(BuildConfig.BINANCE_BASE_URL).create(BinancePublicApi::class.java)
    }

    val binancePrivateApi: BinancePrivateApi by lazy {
        createRetrofit(BuildConfig.BINANCE_BASE_URL).create(BinancePrivateApi::class.java)
    }

    val kuCoinApi: KuCoinApi by lazy {
        createRetrofit(BuildConfig.KUCOIN_BASE_URL).create(KuCoinApi::class.java)
    }

    val coinbaseApi: CoinbaseApi by lazy {
        createRetrofit("https://api.coinbase.com/").create(CoinbaseApi::class.java)
    }

    val bybitApi: BybitApi by lazy {
        createRetrofit("https://api.bybit.com/").create(BybitApi::class.java)
    }

    val bitgetApi: BitgetApi by lazy {
        createRetrofit("https://api.bitget.com/").create(BitgetApi::class.java)
    }

    val okxApi: OKXApi by lazy {
        createRetrofit("https://www.okx.com/").create(OKXApi::class.java)
    }

    val gateIoApi: GateIoApi by lazy {
        createRetrofit("https://api.gateio.ws/").create(GateIoApi::class.java)
    }

    val mexcApi: MexcApi by lazy {
        createRetrofit("https://api.mexc.com/").create(MexcApi::class.java)
    }

    val exchangeRegistry: ExchangeRegistry by lazy {
        ExchangeRegistry(
            listOf(
                BinanceExchangeProvider(binancePrivateApi, binancePublicApi),
                CoinbaseExchangeProvider(coinbaseApi, binancePublicApi),
                KuCoinExchangeProvider(kuCoinApi),
                BybitExchangeProvider(bybitApi, binancePublicApi),
                BitgetExchangeProvider(bitgetApi, binancePublicApi),
                OKXExchangeProvider(okxApi, binancePublicApi),
                GateIoExchangeProvider(gateIoApi, binancePublicApi),
                MexcExchangeProvider(mexcApi, binancePublicApi)
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
