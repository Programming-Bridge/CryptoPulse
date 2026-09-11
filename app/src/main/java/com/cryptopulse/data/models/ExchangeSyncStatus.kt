package com.cryptopulse.data.models

sealed class ExchangeSyncStatus {
    object Connected : ExchangeSyncStatus()
    object Syncing : ExchangeSyncStatus()
    data class AuthError(val message: String = "Authentication failed. Invalid API Key, Secret, or Passphrase.") : ExchangeSyncStatus()
    data class NetworkError(val message: String = "Network unreachable. Please check connection.") : ExchangeSyncStatus()
}
