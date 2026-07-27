# Walkthrough - Fixing Firebase Render Issues in Compose Previews

I have resolved the `IllegalStateException` that prevented several screens from rendering in the Compose Preview. The issue was caused by calling `FirebaseAuth.getInstance()` in an environment where Firebase was not initialized.

## Changes Made

### UI Screens

#### [AuthScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/AuthScreen.kt)
- Verified that `LocalInspectionMode.current` is used to skip `FirebaseAuth` initialization during Previews.
- Added safety checks in `AuthForm` callback to avoid calling Firebase methods in Preview mode.

#### [ReceiveScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/ReceiveScreen.kt)
- Implemented `LocalInspectionMode.current` check to provide a mock `userId` for Previews.
- This prevents the crash when the Composable tries to access `FirebaseAuth.getInstance().currentUser`.

## Verification Results

### Previews
I have verified that the following previews now render correctly:

````carousel
![Auth Screen Preview](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/AuthScreen.kt.png)
<!-- slide -->
![Receive Screen Preview](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/ReceiveScreen.kt.png)
````

> [!TIP]
> Always use `LocalInspectionMode.current` when your Composables depend on third-party SDKs that require initialization (like Firebase, Google Maps, etc.) to ensure your Previews remain functional.
