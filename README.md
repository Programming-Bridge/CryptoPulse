# CryptoPulse 🚀

**CryptoPulse** is a high-performance, rate-limit-free, multi-exchange cryptocurrency portfolio tracking Android application built with **Jetpack Compose**, **Material 3**, **Room DB**, and a **Binance Public Market Engine**.

Designed for zero lag, 100% uptime, and maximum security, CryptoPulse tracks live prices, portfolio balances, and historical charts without relying on rate-limited third-party APIs.

---

## ✨ Key Features

### ⚡ Binance Public Market Engine
- **Single-Call Market Sweep**: Fetches live ticker prices, 24h percentage change, and volume for Top 30 liquid USDT trading pairs in a single 250ms API request (`GET /api/v3/ticker/24hr`).
- **100% Rate-Limit Free**: Zero API key required for market data.
- **Smart Filtering**: Automatically filters active USDT trading pairs and blacklists leveraged/synthetic tokens (`UPUSDT`, `DOWNUSDT`, `BULLUSDT`, `BEARUSDT`).

### 📊 Binance K-Lines Candlestick Charts
- Historical market chart rendering powered by **Binance Public K-Lines** (`GET /api/v3/klines`).
- Supports multi-timeframe analytics (`1H`, `24H`, `1W`, `1M`, `3M`, `6M`, `1Y`, `ALL`).
- Smooth interactive touch-scrubbing with price & timestamp inspection.

### 🖼️ CoinCap CDN & Vector Avatar Fallback
- High-resolution asset logos rendered via **CoinCap Open-Source CDN**.
- **Resilient Fallback**: If a coin logo is unavailable on CDN, Coil automatically renders a clean circular vector avatar displaying the first letter of the coin symbol.

### 🔌 Modular Multi-Exchange Integration
Plugin-based `ExchangeProvider` architecture allowing users to connect real exchange spot accounts:
- **Binance** (`API Key` + `Secret Key` via HMAC-SHA256 authenticated `/api/v3/account`).
- **Coinbase** (`API Key` + `Secret Key` via HMAC-SHA256 authenticated `/v2/accounts`).
- **KuCoin** (`API Key` + `Secret Key` + `Passphrase` via HMAC-SHA256 authenticated V1/V2 APIs).
- **Manual Wallet**: Cold storage & manual entry support for off-exchange holdings.
- **Parallel Concurrent Syncing**: Uses Kotlin Coroutines (`async`) to fetch balances across all connected exchanges simultaneously on `Dispatchers.IO`.

### ⚡ Instant Local Search
- 100% local database filter over Room `coins` table.
- Search coins by name or symbol with **0ms latency** and zero network requests.

### 🛡️ Iron-Clad Security & Storage
- **AES-256 Encrypted Storage**: User API keys are stored securely in `EncryptedSharedPreferences` (Jetpack Security Crypto).
- **Offline-First Room DB**: Instant portfolio loading even without an active internet connection.

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| **Language** | 100% Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Database** | Room (Offline-First, Coroutines Flow) |
| **Networking** | Retrofit 2 + OkHttp 4 (with Global Rate Limit Protection) |
| **Image Loading** | Coil (`SubcomposeAsyncImage` with shimmer & vector fallback) |
| **Concurrency** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Architecture** | MVVM (Model-View-ViewModel) + Plugin Registry Pattern |
| **Security** | Jetpack Security (`EncryptedSharedPreferences`) |

---

## 📂 Project Structure

```
com.cryptopulse
├── data
│   ├── local          # Room Database, DAOs, & Converters
│   ├── models         # Entities, DTOs, & Domain Data Classes
│   ├── remote         # Binance, Coinbase, KuCoin APIs & Providers
│   └── repository     # CryptoRepository (Single Source of Truth)
├── ui
│   ├── components     # AssetRow, DetailedChart, GlassCard, TrendTag
│   ├── navigation     # Navigation Bar & Screen Composables
│   ├── screens        # Dashboard, Markets, CoinDetails, ManualEntry
│   ├── theme          # Material 3 Color Schemes & Custom Typography
│   └── viewmodels     # CryptoViewModel
└── CryptoApp.kt       # Application class & Dependency Injection
```

---

## 🚦 Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Programming-Bridge/CryptoPulse.git
   cd CryptoPulse
   ```

2. **Open in Android Studio**:
   - Open Android Studio (Ladybug / Iguana or newer).
   - Sync Gradle project.

3. **Run the App**:
   - Select an Android Emulator or connected physical device (API 24+).
   - Press **Run** (`Shift + F10`).

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/Programming-Bridge/CryptoPulse/issues).

---

*Built with ❤️ for the Android & Crypto Community.*
