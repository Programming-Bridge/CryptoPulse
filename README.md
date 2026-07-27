# CryptoPulse 🚀

**CryptoPulse** is a professional-grade, multi-exchange cryptocurrency portfolio tracker built with modern Android standards. It ensures 100% uptime and high-precision data through a decentralized "Quad-Fallback" architecture.

## ✨ Key Features

### 🏛️ Quad-Fallback Market Data Engine
Never worry about API bans or slow connections. CryptoPulse intelligently rotates between 4 industry-standard data sources:
1.  **CoinGecko** (Primary - Best Metadata & Logos)
2.  **CoinCap.io** (Backup 1 - High-Speed Aggregation)
3.  **CoinPaprika** (Backup 2 - Institutional Data)
4.  **Binance Public API** (Anchor - Infinite Public Tickers)
*Includes a strict 3-second timeout mechanism to prevent UI hangs.*

### ⚡ Live Auto-Sync
Stay ahead of the market with "Always Live" pricing. The app automatically fetches fresh prices every **60 seconds** in the background, providing a real-time "Ticker" feel without manual refreshing.

### 🔌 Multi-Exchange Integration
Directly connect your exchange accounts using secure API keys:
*   **Binance** (Spot, Funding, and Simple Earn support)
*   **KuCoin** (V2 Authentication with 100% price mirroring)
*   **Manual Entry** (For cold wallets and off-exchange holdings)

### 🛡️ Iron-Clad Security
*   **Encrypted Storage:** Sensitive user API keys are stored in `EncryptedSharedPreferences` (AES-256 GCM).
*   **Secrets Management:** App configuration and base URLs are injected via `local.properties` and `BuildConfig`, making the source code safe for public GitHub repositories.

### 📱 Premium UI/UX
*   **Modern Design:** Fully built with **Jetpack Compose** and **Material 3**.
*   **Edge-to-Edge:** A seamless experience where content flows elegantly under the status and navigation bars.
*   **Offline-First:** Collects data directly from a local **Room Database**. Load your portfolio instantly, even without an internet connection.

## 🛠️ Tech Stack
*   **Language:** 100% Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Database:** Room (Offline-First)
*   **Networking:** Retrofit + OkHttp (with Global Rate Limit Protection)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Architecture:** Clean MVVM (Model-View-ViewModel)
*   **Image Loading:** Coil

## 🚦 Getting Started
To build this project locally, you must create a `local.properties` file in the root directory and add the following:
```properties
COINGECKO_BASE_URL=https://api.coingecko.com/api/v3/
BINANCE_BASE_URL=https://api.binance.com/
KUCOIN_BASE_URL=https://api.kucoin.com/
```

---

*Built with ❤️ for the Crypto Community.*
