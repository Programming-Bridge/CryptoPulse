# Implementation Plan - Fix Firebase Initialization in Compose Previews

The goal is to resolve `IllegalStateException: Default FirebaseApp is not initialized` when rendering Compose Previews. This occurs because `FirebaseAuth.getInstance()` is called during composition in the Preview environment, which does not initialize Firebase.

## User Review Required

> [!IMPORTANT]
> I will be using `LocalInspectionMode.current` to detect if the Composable is running in a Preview and bypass Firebase initialization. This is a common and safe way to handle Firebase in Compose Previews without requiring extensive refactoring of the dependency injection layer.

## Proposed Changes

### UI Screens

#### [MODIFY] [AuthScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/AuthScreen.kt)
- Use `LocalInspectionMode.current` to conditionally initialize `FirebaseAuth`.
- Make `auth` variable nullable and add a null check before use in the authentication lambda.

#### [MODIFY] [ReceiveScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/ReceiveScreen.kt)
- Use `LocalInspectionMode.current` to provide a fallback `userId` for Previews, avoiding the call to `FirebaseAuth.getInstance()`.

## Verification Plan

### Automated Tests
- I will use the `render_compose_preview` tool to verify that `AuthScreenPreview` and `ReceiveScreenPreview` render successfully without errors.

### Manual Verification
- None required as the fix is specific to the Preview environment.
