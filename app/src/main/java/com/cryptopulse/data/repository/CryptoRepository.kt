package com.cryptopulse.data.repository

import android.util.Log
import com.cryptopulse.data.local.CoinDao
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.data.models.HoldingEntity
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.data.models.SnapshotEntity
import com.cryptopulse.data.models.TransactionEntity
import com.cryptopulse.data.remote.BinancePublicApi
import com.cryptopulse.data.remote.ExchangeField
import com.cryptopulse.data.remote.ExchangeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class CryptoRepository(
    private val binancePublicApi: BinancePublicApi,
    private val dao: CoinDao
) {
    data class VerifiedCandidate(
        val id: String, 
        val name: String, 
        val symbol: String, 
        val thumb: String,
        val rank: Int? = null
    )

    val coins: Flow<List<CoinEntity>> = dao.getAllCoins()
    val transactions: Flow<List<TransactionEntity>> = dao.getTransactions()
    val priceAlerts: Flow<List<PriceAlertEntity>> = dao.getPriceAlerts()
    val allHoldings: Flow<List<HoldingEntity>> = dao.getAllHoldings()
    val portfolioHistory: Flow<List<SnapshotEntity>> = dao.getAllSnapshots()

    private val knownWinners = mapOf(
        "btc" to "bitcoin",
        "eth" to "ethereum",
        "usdt" to "tether",
        "bnb" to "binancecoin",
        "sol" to "solana",
        "xrp" to "ripple",
        "ada" to "cardano",
        "avax" to "avalanche-2",
        "dot" to "polkadot",
        "doge" to "dogecoin"
    )

    private val symbolAliasMap = mapOf(
        "miota" to "iota",
        "matic" to "pol"
    )

    private suspend fun resolveUniversalCoinId(
        symbol: String, 
        existingCoinId: String? = null
    ): VerifiedCandidate? {
        val lowerSymbol = symbol.lowercase(Locale.US)
        val allCoinsInDb = coins.first()

        // 1. Known Winners Priority
        val winnerId = knownWinners[lowerSymbol]
        if (winnerId != null) {
            val existing = allCoinsInDb.find { it.id == winnerId }
            if (existing != null) {
                return VerifiedCandidate(existing.id, existing.name, existing.symbol, existing.imageUrl)
            }
            return VerifiedCandidate(winnerId, symbol, symbol.uppercase(Locale.US), getCdnLogoUrl(symbol))
        }

        // 2. Check existing mapping in DB
        if (existingCoinId != null) {
            val existing = allCoinsInDb.find { it.id == existingCoinId }
            if (existing != null) {
                return VerifiedCandidate(existing.id, existing.name, existing.symbol, existing.imageUrl)
            }
        }

        // 3. Check for symbol match in current DB
        val dbMatch = allCoinsInDb.find { it.symbol.lowercase(Locale.US) == lowerSymbol }
        if (dbMatch != null) {
            return VerifiedCandidate(dbMatch.id, dbMatch.name, dbMatch.symbol, dbMatch.imageUrl)
        }

        // Default candidate with CDN logo fallback
        val fallbackId = "coin-${lowerSymbol}"
        return VerifiedCandidate(fallbackId, symbol.uppercase(Locale.US), symbol.uppercase(Locale.US), getCdnLogoUrl(symbol))
    }

    private fun getCdnLogoUrl(symbol: String): String {
        val rawSymbol = symbol.lowercase(Locale.US)
        val normalizedSymbol = symbolAliasMap[rawSymbol] ?: rawSymbol
        return "https://assets.coincap.io/assets/icons/$normalizedSymbol@2x.png"
    }

    private fun isLeveragedToken(symbol: String): Boolean {
        val upper = symbol.uppercase(Locale.US)
        return upper.endsWith("UPUSDT") || upper.endsWith("DOWNUSDT") || 
               upper.endsWith("BULLUSDT") || upper.endsWith("BEARUSDT")
    }

    suspend fun refreshCoins(
        activeProviders: Map<ExchangeProvider, Map<ExchangeField, String>> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        // 1. Always update Top 30 Market data via Binance Public API & CoinCap CDN
        syncMarketData()
        
        // 2. Sync dynamic portfolio data from exchanges if connected
        if (activeProviders.isNotEmpty()) {
            syncDynamicPortfolioData(activeProviders)
        }
    }

    private suspend fun syncMarketData() {
        Log.d("CryptoPulse", "Market Sync: Starting Binance Public Market Sweep...")
        try {
            syncMarketDataFromBinance()
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Binance Public Market Sweep failed: ${e.message}", e)
        }
    }

    private suspend fun syncMarketDataFromBinance() = withContext(Dispatchers.Default) {
        val rawTickers = binancePublicApi.get24hrTickers()
        val allCoinsInDb = coins.first()

        // Filtering: Active USDT pairs, non-leveraged
        val top200Tickers = rawTickers
            .filter { dto ->
                dto.symbol.endsWith("USDT") && !isLeveragedToken(dto.symbol)
            }
            .sortedByDescending { dto ->
                dto.quoteVolume?.toDoubleOrNull() ?: 0.0
            }
            .take(200)

        val marketEntities = top200Tickers.map { dto ->
            val baseAsset = dto.symbol.removeSuffix("USDT").uppercase(Locale.US)
            val existing = allCoinsInDb.find { it.symbol.equals(baseAsset, ignoreCase = true) }
            val winnerId = knownWinners[baseAsset.lowercase(Locale.US)]
            val coinId = winnerId ?: existing?.id ?: "coin-${baseAsset.lowercase(Locale.US)}"
            val name = existing?.name?.takeIf { it.isNotBlank() && it != baseAsset } ?: baseAsset
            val cdnLogoUrl = getCdnLogoUrl(baseAsset)

            CoinEntity(
                id = coinId,
                symbol = baseAsset,
                name = name,
                imageUrl = cdnLogoUrl,
                currentPrice = dto.lastPrice?.toDoubleOrNull() ?: 0.0,
                priceChangePercentage24h = dto.priceChangePercent?.toDoubleOrNull() ?: 0.0,
                marketCap = 0.0,
                totalVolume = dto.quoteVolume?.toDoubleOrNull() ?: 0.0,
                high24h = dto.highPrice?.toDoubleOrNull() ?: 0.0,
                low24h = dto.lowPrice?.toDoubleOrNull() ?: 0.0
            )
        }

        // Ephemeral Market Cache Upsert using Room OnConflictStrategy.REPLACE
        dao.insertCoins(marketEntities)

        // Purge market cache entries that are not in Top 200 AND not in holdings
        val top200Ids = marketEntities.map { it.id }
        dao.purgeUnusedCoins(top200Ids)

        Log.d("CryptoPulse", "Market Sync: Binance Public Sweep Success (${marketEntities.size} Top 200 USDT pairs)")
    }

    private suspend fun syncDynamicPortfolioData(
        activeProviders: Map<ExchangeProvider, Map<ExchangeField, String>>
    ) = coroutineScope {
        Log.d("CryptoPulse", "Portfolio Sync: Starting parallel sync for ${activeProviders.size} providers...")
        val mergedPrices = ConcurrentHashMap<String, Double>()

        val jobs = activeProviders.map { (provider, keys) ->
            async(Dispatchers.IO) {
                try {
                    syncExchange(provider, keys)
                    val exchangeTickers = provider.fetchPrices()
                    mergedPrices.putAll(exchangeTickers)
                } catch (e: Exception) {
                    Log.e("CryptoPulse", "Portfolio Sync: Failed for ${provider.name}", e)
                }
            }
        }

        jobs.awaitAll()

        if (mergedPrices.isNotEmpty()) {
            val allCoinsInDb = coins.first()
            val updatedEntities = allCoinsInDb.mapNotNull { coin ->
                val newPrice = mergedPrices[coin.symbol.uppercase(Locale.US)]
                if (newPrice != null && newPrice > 0) {
                    coin.copy(currentPrice = newPrice)
                } else null
            }
            if (updatedEntities.isNotEmpty()) {
                dao.insertCoins(updatedEntities)
                Log.d("CryptoPulse", "Portfolio Sync: Updated prices from exchanges")
            }
        }
    }

    suspend fun searchCoins(query: String): List<CoinEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val allCoinsInDb = coins.first()
        allCoinsInDb.filter { coin ->
            coin.name.contains(trimmed, ignoreCase = true) ||
            coin.symbol.contains(trimmed, ignoreCase = true)
        }
    }

    suspend fun updatePortfolio(id: String, amount: Double) = withContext(Dispatchers.IO) {
        dao.updatePortfolio(id, amount)
    }

    suspend fun updateHolding(coinId: String, source: String, amount: Double) = withContext(Dispatchers.IO) {
        val existing = dao.getHolding(coinId, source)
        if (existing != null) {
            dao.updateHoldingAmount(coinId, source, amount)
        } else {
            dao.insertHolding(HoldingEntity(coinId = coinId, source = source, amount = amount))
        }
    }

    suspend fun updateHoldingWithCoin(coin: CoinEntity, source: String, amount: Double) = withContext(Dispatchers.IO) {
        dao.insertCoins(listOf(coin))
        updateHolding(coin.id, source, amount)
    }

    suspend fun addTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.insertTransaction(transaction)
    }

    suspend fun addPriceAlert(alert: PriceAlertEntity) = withContext(Dispatchers.IO) {
        dao.insertPriceAlert(alert)
    }

    suspend fun deleteHoldingsByCoinId(coinId: String) = withContext(Dispatchers.IO) {
        dao.deleteHoldingsByCoinId(coinId)
    }

    suspend fun deleteTransactionsByCoinId(coinId: String) = withContext(Dispatchers.IO) {
        dao.deleteTransactionsByCoinId(coinId)
    }

    suspend fun toggleAlertStatus(id: Int, isActive: Boolean) = withContext(Dispatchers.IO) {
        dao.updateAlertStatus(id, isActive)
    }

    suspend fun deletePriceAlert(alert: PriceAlertEntity) = withContext(Dispatchers.IO) {
        dao.deletePriceAlert(alert)
    }

    suspend fun saveSnapshot(snapshot: SnapshotEntity) = withContext(Dispatchers.IO) {
        dao.insertSnapshot(snapshot)
    }

    suspend fun getMarketChart(id: String, days: String): List<Pair<Long, Double>> = withContext(Dispatchers.IO) {
        val allCoinsInDb = coins.first()
        val foundCoin = allCoinsInDb.find { it.id.equals(id, ignoreCase = true) || it.symbol.equals(id, ignoreCase = true) }
        val rawSymbol = foundCoin?.symbol ?: id.removePrefix("coin-").removePrefix("placeholder-")
        val symbol = rawSymbol.uppercase(Locale.US)

        if (symbol.isBlank()) return@withContext emptyList()

        val pairSymbol = if (symbol.endsWith("USDT")) symbol else "${symbol}USDT"
        val (interval, limit) = when (days) {
            "1" -> "15m" to 96
            "7" -> "4h" to 42
            "30" -> "1d" to 30
            "90" -> "1d" to 90
            "180" -> "1d" to 180
            "365" -> "1w" to 52
            "max" -> "1w" to 100
            else -> "1h" to 24
        }

        try {
            val klines = binancePublicApi.getKlines(symbol = pairSymbol, interval = interval, limit = limit)
            klines.mapNotNull { kline ->
                if (kline.size >= 5) {
                    val openTime = kline[0].asLong
                    val closePrice = kline[4].asString.toDoubleOrNull() ?: 0.0
                    openTime to closePrice
                } else null
            }
        } catch (e: Exception) {
            Log.w("CryptoPulse", "Binance K-Line chart unavailable for $pairSymbol: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncExchange(provider: ExchangeProvider, keys: Map<ExchangeField, String>) = withContext(Dispatchers.IO) {
        Log.d("CryptoPulse", "Starting sync for ${provider.name}")
        try {
            val balances = provider.fetchBalances(keys)
            val exchangePrices = provider.fetchPrices()
            val allCoinsInDb = coins.first()
            
            val newHoldings = mutableListOf<HoldingEntity>()
            val newCoinEntities = mutableListOf<CoinEntity>()

            balances.forEach { balance ->
                val totalAmount = balance.free + balance.locked
                if (totalAmount <= 0) return@forEach
                val price = exchangePrices[balance.asset.uppercase(Locale.US)] ?: 0.0
                val existingMapping = allCoinsInDb.find { it.symbol.equals(balance.asset, ignoreCase = true) }
                val verified = resolveUniversalCoinId(balance.asset, existingMapping?.id)
                
                if (verified != null) {
                    val imageUrl = verified.thumb.ifEmpty { getCdnLogoUrl(balance.asset) }
                    val newCoin = CoinEntity(
                        id = verified.id,
                        symbol = balance.asset.uppercase(Locale.US),
                        name = verified.name,
                        imageUrl = imageUrl,
                        currentPrice = price,
                        priceChangePercentage24h = 0.0
                    )
                    newCoinEntities.add(newCoin)
                    newHoldings.add(HoldingEntity(coinId = verified.id, source = provider.name, amount = totalAmount))
                } else {
                    val placeholderId = "placeholder-${balance.asset.lowercase(Locale.US)}"
                    val placeholderCoin = CoinEntity(
                        id = placeholderId,
                        symbol = balance.asset.uppercase(Locale.US),
                        name = balance.asset,
                        imageUrl = getCdnLogoUrl(balance.asset), 
                        currentPrice = price,
                        priceChangePercentage24h = 0.0
                    )
                    newCoinEntities.add(placeholderCoin)
                    newHoldings.add(HoldingEntity(coinId = placeholderId, source = provider.name, amount = totalAmount))
                }
            }

            if (newCoinEntities.isNotEmpty()) {
                dao.insertCoins(newCoinEntities)
            }
            dao.replaceHoldingsForSource(provider.name, newHoldings)

            Log.d("CryptoPulse", "Sync for ${provider.name} completed successfully")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error syncing ${provider.name}: ${e.message}", e)
            throw e
        }
    }
}
