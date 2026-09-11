package com.cryptopulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptopulse.data.models.CoinEntity
import com.cryptopulse.data.models.PriceAlertEntity
import com.cryptopulse.data.models.TransactionEntity
import com.cryptopulse.data.repository.CryptoRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log

import com.cryptopulse.data.local.prefs.SecurePrefsManager
import com.cryptopulse.data.models.ExchangeSyncStatus
import com.cryptopulse.data.remote.ExchangeField
import com.cryptopulse.data.remote.ExchangeProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class CryptoViewModel(
    private val repository: CryptoRepository,
    private val securePrefs: SecurePrefsManager,
    val availableProviders: List<ExchangeProvider>
) : ViewModel() {

    val coins: StateFlow<List<CoinEntity>> = repository.coins.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val priceAlerts: StateFlow<List<PriceAlertEntity>> = repository.priceAlerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val holdings = repository.allHoldings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("Initializing...")
    val syncMessage = _syncMessage.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress = _loadingProgress.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents = _connectionEvents.asSharedFlow()

    val exchangeSyncStatuses: StateFlow<Map<String, ExchangeSyncStatus>> =
        repository.exchangeSyncStatuses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<DashboardUiState> = combine(
        coins,
        holdings,
        transactions,
        isSyncing
    ) { coinList, holdingList, txList, syncing ->
        if (syncing && holdingList.isEmpty()) {
            return@combine DashboardUiState.Loading
        }

        val groupedHoldings = holdingList.groupBy { it.coinId }
        val portfolio = groupedHoldings.mapNotNull { (coinId, coinHoldings) ->
            val totalAmount = coinHoldings.sumOf { it.amount }
            
            val coin = coinList.find { it.id == coinId } ?: CoinEntity(
                id = coinId,
                symbol = coinId.substringAfter("-").uppercase(),
                name = coinId.substringAfter("-").replaceFirstChar { it.uppercase() },
                imageUrl = "",
                currentPrice = 0.0,
                priceChangePercentage24h = 0.0
            )

            val inTransactions = txList.filter { it.coinId == coin.id && it.type == "In" }
            val totalAmountIn = inTransactions.sumOf { it.amount }
            val totalCostIn = inTransactions.sumOf { it.value }
            
            val avgBuyPrice = if (totalAmountIn > 0) totalCostIn / totalAmountIn else coin.currentPrice
            val unrealizedPL = (coin.currentPrice - avgBuyPrice) * totalAmount
            val plPercentage = if (avgBuyPrice > 0) ((coin.currentPrice / avgBuyPrice) - 1) * 100 else 0.0

            PortfolioItem(
                coin = coin,
                totalAmount = totalAmount,
                totalValue = totalAmount * coin.currentPrice,
                sources = coinHoldings.map { it.source },
                avgBuyPrice = avgBuyPrice,
                unrealizedPL = unrealizedPL,
                plPercentage = plPercentage
            )
        }.sortedByDescending { it.totalValue }

        if (portfolio.isEmpty() && !syncing) {
            DashboardUiState.Empty
        } else {
            DashboardUiState.Success(
                portfolio = portfolio,
                totalBalance = portfolio.sumOf { it.totalValue },
                performance24h = if (portfolio.isEmpty()) 0.0 else portfolio.sumOf { (it.totalValue / portfolio.sumOf { p -> p.totalValue }.coerceAtLeast(1.0)) * it.coin.priceChangePercentage24h }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    val unifiedPortfolio: StateFlow<List<PortfolioItem>> = uiState.map { state ->
        if (state is DashboardUiState.Success) state.portfolio else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalProfitLoss: StateFlow<Double> = unifiedPortfolio.map { portfolio ->
        portfolio.sumOf { it.unrealizedPL }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalProfitLossPercentage: StateFlow<Double> = unifiedPortfolio.map { portfolio ->
        val totalCost = portfolio.sumOf { it.avgBuyPrice * it.totalAmount }
        if (totalCost > 0) (portfolio.sumOf { it.unrealizedPL } / totalCost) * 100 else 0.0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalBalance: StateFlow<Double> = uiState.map { state ->
        if (state is DashboardUiState.Success) state.totalBalance else 0.0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val portfolioAllocation: StateFlow<List<AllocationInfo>> = unifiedPortfolio.map { portfolio ->
        val total = portfolio.sumOf { it.totalValue }
        if (total == 0.0) emptyList()
        else {
            portfolio.map { 
                AllocationInfo(
                    symbol = it.coin.symbol,
                    percentage = (it.totalValue / total).toFloat(),
                    value = it.totalValue
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val portfolioPerformance24h: StateFlow<Double> = uiState.map { state ->
        if (state is DashboardUiState.Success) state.performance24h else 0.0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private val _chartData = MutableStateFlow<List<Pair<Long, Double>>>(emptyList())
    val chartData = _chartData.asStateFlow()

    private val _isChartLoading = MutableStateFlow(false)
    val isChartLoading = _isChartLoading.asStateFlow()

    private val _connectedExchanges = MutableStateFlow<Set<String>>(emptySet())
    val connectedExchanges = _connectedExchanges.asStateFlow()

    private val _isInitialSyncComplete = MutableStateFlow(false)
    val isInitialSyncComplete = _isInitialSyncComplete.asStateFlow()

    private val AUTO_SYNC_INTERVAL = 60_000L // 1 minute stable interval

    val availableSources = listOf("Manual", "Binance", "Coinbase", "Kraken", "MetaMask")

    private val chartCache = mutableMapOf<String, List<Pair<Long, Double>>>()
    private var pollingJob: Job? = null
    private var autoSyncJob: Job? = null

    init {
        refreshCoins()
        startGlobalAutoSync()
    }

    private fun startGlobalAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_SYNC_INTERVAL)
                if (!_isSyncing.value) {
                    Log.d("CryptoPulse", "Auto-Sync: Triggering background refresh...")
                    refreshCoins()
                }
            }
        }
    }

    private fun getActiveExchangeProviders(): Map<ExchangeProvider, Map<ExchangeField, String>> {
        val activeMap = mutableMapOf<ExchangeProvider, Map<ExchangeField, String>>()
        availableProviders.forEach { provider ->
            val credentials = mutableMapOf<ExchangeField, String>()
            provider.requiredFields.forEach { field ->
                val value = securePrefs.getCredential(provider.name, field)
                if (value != null) credentials[field] = value
            }
            if (credentials.size == provider.requiredFields.size) {
                activeMap[provider] = credentials
            }
        }
        return activeMap
    }

    fun refreshCoins() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _loadingProgress.value = 0.1f
            
            val activeProviders = getActiveExchangeProviders()
            _syncMessage.value = if (activeProviders.isNotEmpty()) "Syncing from exchanges..." else "Fetching market prices..."
            _loadingProgress.value = 0.5f
            
            try {
                repository.refreshCoins(activeProviders)
                _connectedExchanges.value = activeProviders.keys.map { it.name }.toSet()
            } catch (e: Exception) {
                Log.e("CryptoPulse", "Refresh failed", e)
            }

            _loadingProgress.value = 1.0f
            _syncMessage.value = "Up to date"
            _isInitialSyncComplete.value = true
            _isSyncing.value = false
        }
    }

    suspend fun searchCoins(query: String): List<CoinEntity> {
        return repository.searchCoins(query)
    }

    fun updatePortfolio(id: String, amount: Double) {
        viewModelScope.launch {
            repository.updatePortfolio(id, amount)
        }
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun addPriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            repository.addPriceAlert(alert)
        }
    }

    fun toggleAlertStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleAlertStatus(id, isActive)
        }
    }

    fun deletePriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            repository.deletePriceAlert(alert)
        }
    }

    fun deletePortfolioItem(coinId: String) {
        viewModelScope.launch {
            repository.deleteHoldingsByCoinId(coinId)
            repository.deleteTransactionsByCoinId(coinId)
        }
    }

    fun getLatestTransactionDate(coinId: String, source: String): String? {
        return transactions.value
            .filter { it.coinId == coinId && it.source == source }
            .firstOrNull()?.date
    }

    fun connectExchange(provider: ExchangeProvider, credentials: Map<ExchangeField, String>) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Connecting to ${provider.name}..."
            
            credentials.forEach { (field, value) ->
                securePrefs.saveCredential(provider.name, field, value)
            }
            
            try {
                refreshCoins()
                _connectionEvents.emit(ConnectionEvent.Success(provider.name))
            } catch (e: Exception) {
                Log.e("CryptoPulse", "Connection failed for ${provider.name}", e)
                _syncMessage.value = "Connection failed: ${e.message}"
                securePrefs.deleteCredentials(provider.name)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun disconnectExchange(name: String) {
        securePrefs.deleteCredentials(name)
        _connectedExchanges.value = _connectedExchanges.value - name
        refreshCoins()
    }

    fun fetchMarketChart(coinId: String, days: String, isPolling: Boolean = false) {
        val cacheKey = "$coinId-$days"
        if (!isPolling && chartCache.containsKey(cacheKey)) {
            _chartData.value = chartCache[cacheKey]!!
        }

        viewModelScope.launch {
            if (_chartData.value.isEmpty()) {
                _isChartLoading.value = true
            }
            val newData = repository.getMarketChart(coinId, days)
            if (newData.isNotEmpty()) {
                chartCache[cacheKey] = newData
                _chartData.value = newData
            }
            _isChartLoading.value = false
        }
    }

    fun startPricePolling(coinId: String, days: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(15000)
                fetchMarketChart(coinId, days, isPolling = true)
            }
        }
    }

    fun stopPricePolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun addManualTransaction(
        coin: CoinEntity,
        amount: Double,
        price: Double,
        type: String,
        source: String,
        date: String,
        isUpdate: Boolean = false
    ) {
        viewModelScope.launch {
            val signedAmount = if (type == "Out") -amount else amount
            val transaction = TransactionEntity(
                coinId = coin.id,
                coinName = coin.name,
                coinSymbol = coin.symbol,
                date = date,
                amount = signedAmount,
                value = amount * price,
                type = type,
                source = source
            )
            repository.addTransaction(transaction)
            val currentHolding = holdings.value.find { it.coinId == coin.id && it.source == source }
            val currentAmount = currentHolding?.amount ?: 0.0
            val newAmount = if (isUpdate) amount else (currentAmount + signedAmount).coerceAtLeast(0.0)
            repository.updateHoldingWithCoin(coin, source, newAmount)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        autoSyncJob?.cancel()
    }
}

data class PortfolioItem(
    val coin: CoinEntity,
    val totalAmount: Double,
    val totalValue: Double,
    val sources: List<String>,
    val avgBuyPrice: Double = 0.0,
    val unrealizedPL: Double = 0.0,
    val plPercentage: Double = 0.0
)

data class AllocationInfo(
    val symbol: String,
    val percentage: Float,
    val value: Double
)

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Empty : DashboardUiState()
    data class Success(
        val portfolio: List<PortfolioItem>,
        val totalBalance: Double,
        val performance24h: Double
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

sealed class ConnectionEvent {
    data class Success(val providerName: String) : ConnectionEvent()
}
