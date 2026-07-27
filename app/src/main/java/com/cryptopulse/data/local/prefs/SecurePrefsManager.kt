package com.cryptopulse.data.local.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cryptopulse.data.remote.ExchangeField

class SecurePrefsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_crypto_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredential(exchange: String, field: ExchangeField, value: String) {
        sharedPreferences.edit().putString("${exchange}_${field.name}", value).apply()
    }

    fun getCredential(exchange: String, field: ExchangeField): String? {
        return sharedPreferences.getString("${exchange}_${field.name}", null)
    }

    fun deleteCredentials(exchange: String) {
        val editor = sharedPreferences.edit()
        ExchangeField.entries.forEach { field ->
            editor.remove("${exchange}_${field.name}")
        }
        editor.apply()
    }

    fun isExchangeConnected(exchange: String): Boolean {
        // Simple check: if any field for this exchange exists
        return ExchangeField.entries.any { getCredential(exchange, it) != null }
    }
}
