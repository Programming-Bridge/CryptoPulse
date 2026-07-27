package com.cryptopulse.data.repository

import com.cryptopulse.data.local.CoinDao
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.data.models.HoldingEntity
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.data.models.SnapshotEntity
import com.cryptopulse.data.models.TransactionEntity
import com.cryptopulse.data.remote.BinanceApi
import com.cryptopulse.data.remote.CoinCapApi
import com.cryptopulse.data.remote.CoinGeckoApi
import com.cryptopulse.data.remote.CoinPaprikaApi
import com.cryptopulse.data.remote.ExchangeField
import com.cryptopulse.data.remote.ExchangeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.crypto.spec.SecretKeySpec
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

class CryptoRepository(
    private val api: CoinGeckoApi,
    private val binanceApi: BinanceApi,
    private val coinCapApi: CoinCapApi,
    private val coinPaprikaApi: CoinPaprikaApi,
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

    private var symbolToIdMap: Map<String, String> = emptyMap()

    private suspend fun resolveUniversalCoinId(
        symbol: String, 
        exchangePrice: Double, 
        existingCoinId: String? = null
    ): VerifiedCandidate? {
        val lowerSymbol = symbol.lowercase()
        val allCoinsInDb = coins.first()

        // 1. If we already have a coin mapped, check if its price is still a good match
        if (existingCoinId != null) {
            val existing = allCoinsInDb.find { it.id == existingCoinId }
            if (existing != null && existing.currentPrice > 0 && exchangePrice > 0) {
                val variance = abs(existing.currentPrice - exchangePrice) / exchangePrice
                if (variance < 0.5) { 
                    return VerifiedCandidate(existing.id, existing.name, existing.symbol, existing.imageUrl)
                }
            }
        }

        // 2. Search for candidates
        try {
            ensureSymbolMap()
            Log.d("CryptoPulse", "Precision Engine: Resolving $symbol (Exchange Price: $$exchangePrice)")
            val searchResults = retry { api.searchCoins(symbol) }
            
            // 2.1 Exact Match Candidates
            var candidates = searchResults.coins.filter { it.symbol.lowercase() == lowerSymbol }
            
            // 2.2 Fuzzy Match Fallback (if no exact symbol match)
            if (candidates.isEmpty()) {
                candidates = searchResults.coins.filter { 
                    it.symbol.lowercase().contains(lowerSymbol) || lowerSymbol.contains(it.symbol.lowercase())
                }
            }

            if (candidates.isEmpty()) return null
            if (candidates.size == 1) {
                return VerifiedCandidate(candidates[0].id, candidates[0].name, candidates[0].symbol, candidates[0].imageUrl)
            }

            // 3. Disambiguate using rank & price
            val candidateIds = candidates.joinToString(",") { it.id }
            val fullData = retry { api.getCoins(ids = candidateIds) }

            var bestCandidate: VerifiedCandidate? = null
            var highestScore = -1.0

            fullData.forEach { coinData ->
                // 1. Rank Score (Logarithmic normalized, Rank 1 = 1.0, Rank 15000 = 0.0)
                val rank = coinData.marketCapRank ?: 15000
                val rankScore = (1.0 / (Math.log10(rank.toDouble().coerceAtLeast(1.0)) + 0.5))

                // 2. Price Match Score (Inverse variance, Exact = 1.0)
                val coinPrice = coinData.currentPrice ?: 0.0
                val priceDiff = abs(coinPrice - exchangePrice)
                val priceScore = 1.0 / (1.0 + (priceDiff / exchangePrice.coerceAtLeast(0.00001)))

                // Weighted Total (80% Rank, 20% Price) - Production Standard
                val totalScore = (rankScore * 0.8) + (priceScore * 0.2)
                
                Log.d("CryptoPulse", "   - Candidate: ${coinData.id}, Rank: $rank, Price: $$coinPrice -> Score: $totalScore")

                if (totalScore > highestScore) {
                    highestScore = totalScore
                    bestCandidate = VerifiedCandidate(
                        id = coinData.id ?: "",
                        name = coinData.name ?: coinData.id ?: "",
                        symbol = coinData.symbol?.uppercase() ?: symbol,
                        thumb = coinData.image ?: "",
                        rank = coinData.marketCapRank
                    )
                }
            }

            return bestCandidate
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Precision Engine: Failed for $symbol", e)
            return null
        }
    }

    private suspend fun ensureSymbolMap() {
        if (symbolToIdMap.isNotEmpty()) return

        // 1. Try loading from Database Cache
        val count = dao.getSymbolMapCount()
        if (count > 1000) {
            val cached = dao.getSymbolMap()
            // Check if cache is reasonably fresh (e.g., < 7 days)
            val firstEntry = cached.firstOrNull()
            if (firstEntry != null && System.currentTimeMillis() - firstEntry.lastUpdated < 7 * 24 * 60 * 60 * 1000L) {
                symbolToIdMap = cached.associate { it.symbol to it.coinId }
                Log.d("CryptoPulse", "Symbol Map loaded from DB (${symbolToIdMap.size} entries)")
                return
            }
        }

        // 2. Fallback to API Fetch
        Log.d("CryptoPulse", "Symbol Map cache empty or stale. Fetching from API...")
        try {
            val list = retry { api.getCoinsList() }
            symbolToIdMap = list.associate { it.symbol.lowercase() to it.id }
            
            // Save to DB for persistence
            val entities = list.map { 
                com.cryptopulse.data.models.SymbolMapEntity(it.symbol.lowercase(), it.id)
            }
            dao.insertSymbolMap(entities)
            Log.d("CryptoPulse", "Symbol Map saved to DB (${entities.size} entries)")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Failed to fetch symbol map", e)
        }
    }

    private suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 2000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    Log.w("CryptoPulse", "Rate limited (429), retrying in $currentDelay ms...")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw e
            }
        }
        return block() // Last attempt
    }

    suspend fun refreshCoins() = withContext(Dispatchers.IO) {
        syncMarketDataWithQuadFallback()
    }

    private suspend fun syncMarketDataWithQuadFallback() {
        Log.d("CryptoPulse", "Quad-Fallback Sync: Starting...")

        // 1. Try CoinGecko (Primary)
        val cgSuccess = withTimeoutOrNull(3000L) {
            try {
                val remoteCoins = retry { api.getCoins() }
                val entities = remoteCoins.mapNotNull { dto ->
                    if (dto.id == null) return@mapNotNull null
                    CoinEntity(
                        id = dto.id,
                        symbol = dto.symbol?.uppercase() ?: "",
                        name = dto.name ?: "",
                        imageUrl = dto.image ?: "",
                        currentPrice = dto.currentPrice ?: 0.0,
                        priceChangePercentage24h = dto.priceChangePercentage24h ?: 0.0,
                        marketCap = dto.marketCap ?: 0.0,
                        totalVolume = dto.totalVolume ?: 0.0,
                        high24h = dto.high24h ?: 0.0,
                        low24h = dto.low24h ?: 0.0,
                        circulatingSupply = dto.circulatingSupply ?: 0.0,
                        sparklineData = dto.sparkline?.price ?: emptyList()
                    )
                }
                dao.insertCoins(entities)
                Log.d("CryptoPulse", "Quad-Fallback Sync: CoinGecko Success (${entities.size} coins)")
                true
            } catch (e: Exception) {
                Log.e("CryptoPulse", "Quad-Fallback Sync: CoinGecko Failed: ${e.message}")
                false
            }
        } ?: false

        if (cgSuccess) return

        // 2. Try CoinCap (Backup 1)
        val ccSuccess = withTimeoutOrNull(3000L) {
            try {
                val response = coinCapApi.getAssets(limit = 500)
                val entities = response.data.map { dto ->
                    CoinEntity(
                        id = dto.id,
                        symbol = dto.symbol.uppercase(),
                        name = dto.name,
                        imageUrl = "https://assets.coincap.io/assets/icons/${dto.symbol.lowercase()}@2x.png",
                        currentPrice = dto.priceUsd.toDoubleOrNull() ?: 0.0,
                        priceChangePercentage24h = dto.changePercent24Hr?.toDoubleOrNull() ?: 0.0,
                        marketCap = dto.marketCapUsd?.toDoubleOrNull() ?: 0.0,
                        totalVolume = dto.volumeUsd24Hr?.toDoubleOrNull() ?: 0.0
                    )
                }
                dao.insertCoins(entities)
                Log.d("CryptoPulse", "Quad-Fallback Sync: CoinCap Success (${entities.size} coins)")
                true
            } catch (e: Exception) {
                Log.e("CryptoPulse", "Quad-Fallback Sync: CoinCap Failed: ${e.message}")
                false
            }
        } ?: false

        if (ccSuccess) return

        // 3. Try CoinPaprika (Backup 2)
        val cpSuccess = withTimeoutOrNull(3000L) {
            try {
                val tickers = coinPaprikaApi.getTickers()
                val entities = tickers.map { dto ->
                    val usdQuote = dto.quotes["USD"]
                    CoinEntity(
                        id = dto.id,
                        symbol = dto.symbol.uppercase(),
                        name = dto.name,
                        imageUrl = "", // Metadata only for backup
                        currentPrice = usdQuote?.price ?: 0.0,
                        priceChangePercentage24h = usdQuote?.percentChange24h ?: 0.0,
                        marketCap = usdQuote?.marketCap ?: 0.0
                    )
                }
                dao.insertCoins(entities)
                Log.d("CryptoPulse", "Quad-Fallback Sync: CoinPaprika Success (${entities.size} coins)")
                true
            } catch (e: Exception) {
                Log.e("CryptoPulse", "Quad-Fallback Sync: CoinPaprika Failed: ${e.message}")
                false
            }
        } ?: false

        if (cpSuccess) return

        // 4. Try Binance (Anchor - Last Resort)
        withTimeoutOrNull(3000L) {
            try {
                val tickers = binanceApi.get24hrTicker()
                val entities = tickers.filter { it.symbol.endsWith("USDT") }.map { dto ->
                    val asset = dto.symbol.removeSuffix("USDT")
                    CoinEntity(
                        id = "binance-$asset",
                        symbol = asset,
                        name = asset,
                        imageUrl = "",
                        currentPrice = dto.lastPrice.toDoubleOrNull() ?: 0.0,
                        priceChangePercentage24h = dto.priceChangePercent.toDoubleOrNull() ?: 0.0
                    )
                }
                dao.insertCoins(entities)
                Log.d("CryptoPulse", "Quad-Fallback Sync: Binance Anchor Success (${entities.size} coins)")
            } catch (e: Exception) {
                Log.e("CryptoPulse", "Quad-Fallback Sync: All Sources Failed")
            }
        }
    }

    suspend fun searchCoins(query: String): List<CoinEntity> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchCoins(query)
            val coinIds = response.coins.take(10).joinToString(",") { it.id }
            val prices = if (coinIds.isNotEmpty()) api.getSimplePrices(coinIds) else emptyMap()

            response.coins.mapNotNull { 
                val livePrice = prices[it.id]?.get("usd") ?: 0.0
                val liveChange = prices[it.id]?.get("usd_24h_change") ?: 0.0
                
                CoinEntity(
                    id = it.id,
                    symbol = it.symbol.uppercase(),
                    name = it.name,
                    imageUrl = it.imageUrl,
                    currentPrice = livePrice,
                    priceChangePercentage24h = liveChange
                )
            }
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error searching coins: ${e.message}", e)
            emptyList()
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
        // Ensure coin info is saved
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
        try {
            val response = api.getMarketChart(id = id, days = days)
            response.prices.mapNotNull { pricePoint ->
                if (pricePoint.size >= 2) {
                    pricePoint[0].toLong() to pricePoint[1]
                } else null
            }
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error fetching market chart: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncExchange(provider: ExchangeProvider, keys: Map<ExchangeField, String>) = withContext(Dispatchers.IO) {
        Log.d("CryptoPulse", "Starting sync for ${provider.name}")
        try {
            val balances = provider.fetchBalances(keys)
            val exchangePrices = provider.fetchPrices()
            val allCoinsInDb = coins.first()
            
            val newCoinIdsFound = mutableListOf<String>()

            balances.forEach { balance ->
                val totalAmount = balance.free + balance.locked
                if (totalAmount <= 0) return@forEach

                val price = exchangePrices[balance.asset.uppercase()] ?: 0.0
                val existingMapping = allCoinsInDb.find { it.symbol.equals(balance.asset, true) }

                // 1. Resolve Symbol to ID using Production Precision Engine
                val verified = resolveUniversalCoinId(balance.asset, price, existingMapping?.id)
                
                if (verified != null) {
                    // 2. Identity Swapping & Stale Data Purge
                    if (existingMapping != null && existingMapping.id != verified.id) {
                        Log.w("CryptoPulse", "Identity Mismatch: Replacing ${existingMapping.id} with verified ${verified.id} for symbol ${balance.asset}")
                        dao.deleteHoldingsByCoinId(existingMapping.id)
                        dao.deleteCoinById(existingMapping.id)
                    }

                    // 3. Forced Metadata Sync (Always Update)
                    val newCoin = CoinEntity(
                        id = verified.id,
                        symbol = balance.asset.uppercase(),
                        name = verified.name,
                        imageUrl = verified.thumb,
                        currentPrice = price,
                        priceChangePercentage24h = 0.0
                    )
                    dao.insertCoins(listOf(newCoin))

                    if (allCoinsInDb.none { it.id == verified.id }) {
                        newCoinIdsFound.add(verified.id)
                    }
                    
                    updateHolding(verified.id, provider.name, totalAmount)
                } else {
                    // Create placeholder only if we really can't find it anywhere
                    val placeholderId = "${provider.name.lowercase()}-${balance.asset.lowercase()}"
                    val placeholderCoin = CoinEntity(
                        id = placeholderId,
                        symbol = balance.asset.uppercase(),
                        name = balance.asset,
                        imageUrl = "", 
                        currentPrice = price,
                        priceChangePercentage24h = 0.0
                    )
                    dao.insertCoins(listOf(placeholderCoin))
                    updateHolding(placeholderId, provider.name, totalAmount)
                }
            }

            // 2. Instant Metadata Fetch for new coins found
            if (newCoinIdsFound.isNotEmpty()) {
                Log.d("CryptoPulse", "Sync: Fetching metadata for ${newCoinIdsFound.size} new coins")
                val chunks = newCoinIdsFound.distinct().chunked(50)
                chunks.forEach { chunk ->
                    try {
                        val entities = api.getCoins(ids = chunk.joinToString(",")).mapNotNull { 
                            CoinEntity(
                                id = it.id!!,
                                symbol = it.symbol?.uppercase() ?: "",
                                name = it.name ?: "",
                                imageUrl = it.image ?: "",
                                currentPrice = it.currentPrice ?: 0.0,
                                priceChangePercentage24h = it.priceChangePercentage24h ?: 0.0,
                                sparklineData = it.sparkline?.price ?: emptyList()
                            )
                        }
                        dao.insertCoins(entities)
                    } catch (e: Exception) {
                        Log.e("CryptoPulse", "Sync: Metadata fetch failed", e)
                    }
                }
            }

            Log.d("CryptoPulse", "Sync for ${provider.name} completed successfully")
        } catch (e: Exception) {
            Log.e("CryptoPulse", "Error syncing ${provider.name}: ${e.message}", e)
            throw e
        }
    }
}
