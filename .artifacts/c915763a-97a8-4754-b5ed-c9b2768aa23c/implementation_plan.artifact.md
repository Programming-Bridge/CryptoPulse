# Implementation Plan - "Auto-Sync First" Tracker Experience

Sahi hai! "Connect Exchange" ko Bottom Nav mein daalna ek behtareen idea hai. Is se app ka pura focus **Auto-Sync** par aa jayega aur ye ek professional **Portfolio Tracker** ban jayegi.

## Proposed Strategy

1.  **Primary Navigation**: "Connect Exchange" ko bottom navigation bar ka hissa banayenge taake user asani se apne exchanges link aur manage kar sake.
2.  **Auto-Sync Focus**: Dashboard par main buttons trading ke bajaye "Sync Status" aur "Portfolio Overview" par honge.
3.  **Encrypted Security**: API keys ko **EncryptedSharedPreferences** mein save karenge taake top-level security rahe.

## Proposed Changes

### 1. Navigation & UI Refactoring

#### [MODIFY] [MainScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/MainScreen.kt)
- **Bottom Nav Update**: `bottomNavItems` list mein `Connect Exchange` (Wallet/Link icon ke saath) add karenge.
- **Main Layout**: `MainScreen` ki nested navigation host mein `ConnectExchange` screen ko as a primary destination handle karenge.

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/DashboardScreen.kt)
- **Remove Trading Buttons**: "Buy", "Sell", "Convert" ko bilkul hata denge.
- **Simplified Actions**: Iski jagah sirf "Price Alerts" ya "Sync Status" jaise tracking-related shortcuts rakhenge.

### 2. Secure Infrastructure

#### [MODIFY] [libs.versions.toml] & [build.gradle.kts]
- Add `androidx.security:security-crypto` library.

#### [NEW] [SecurePrefsManager.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/data/local/prefs/SecurePrefsManager.kt)
- API Keys ko AES-256 se encrypt karke save karne ka logic implement karenge.

### 3. Feature Integration

#### [MODIFY] [ConnectExchangeScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/ConnectExchangeScreen.kt)
- Keys ko `SecurePrefsManager` mein save karne ka functional logic add karenge.
- Success par automatic portfolio sync trigger karenge.

## Verification Plan

### Manual Verification
1.  **Bottom Nav Check**: Verify karein ke "Connect" ka option ab Dashboard ke barabar mein bottom bar par nazar aa raha hai.
2.  **Sync Flow**: Ek exchange connect karein aur verify karein ke Dashboard par coins khud-ba-khud load ho rahe hain.
3.  **Security Check**: Verify karein ke API keys save ho rahi hain aur encryption system perfectly kaam kar raha hai.
