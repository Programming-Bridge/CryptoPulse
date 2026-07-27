# Tasks - "Auto-Sync First" Tracker Experience

- [ ] Dependency & Infrastructure Setup
    - [ ] Add `androidx.security:security-crypto` to `libs.versions.toml`
    - [ ] Add dependency to `app/build.gradle.kts`
    - [ ] Create `SecurePrefsManager.kt` for encrypted storage
- [ ] Navigation Refactoring
    - [ ] Add "Connect" to `bottomNavItems` in `MainScreen.kt`
    - [ ] Move `ConnectExchangeScreen` to `MainScreen`'s nested NavHost
    - [ ] Update `MainScreen` layout to handle the 5-item bottom bar
- [ ] Dashboard Cleanup
    - [ ] Remove "Buy", "Sell", "Convert" buttons from `DashboardScreen.kt`
    - [ ] Adjust header layout for a cleaner "Tracker" look
- [ ] Feature Integration
    - [ ] Implement functional "Connect" logic in `ConnectExchangeScreen.kt`
    - [ ] Link `SecurePrefsManager` to save/retrieve API keys
- [ ] Verification
    - [ ] Verify bottom nav navigation
    - [ ] Verify API keys are saved securely
    - [ ] Verify clean Dashboard UI
