# Implementation Plan - Welcome Onboarding Experience

Introduce a beautiful, premium welcome screen for first-time users to help them navigate the app and start their musical journey.

## User Review Required

> [!IMPORTANT]
> The Welcome Screen will appear as a full-screen overlay on the very first launch. It will consist of 3-4 interactive slides that guide the user through the app's three primary rooms (Library, Player, and Discover). I will use the `LibraryPersistenceManager` to store a `first_launch_complete` flag to ensure this only happens once.

## Proposed Changes

### Persistence

#### [MODIFY] [LibraryPersistenceManager.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/LibraryPersistenceManager.kt)
- Add `fun isFirstLaunch(): Boolean` and `fun setFirstLaunchComplete()`.
- Store the flag in the existing `cherie_library` SharedPreferences.

### Logic & State

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/MainViewModel.kt)
- Add `showWelcomeScreen` state initialized from `libraryPersistence.isFirstLaunch()`.
- Add `completeOnboarding()` method to update the persistence and hide the screen.

### UI Components

#### [NEW] [WelcomeScreen.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/ui/WelcomeScreen.kt)
- Create a premium onboarding UI using `HorizontalPager`.
- **Slide 1: Intro**: "Welcome to Cherie Music" with high-fidelity logo.
- **Slide 2: Library**: Explaining how to manage folders and find music.
- **Slide 3: Player**: Highlighting the tactile 3D controls and physical bass engines.
- **Slide 4: Discover**: Explaining smart mixes and online search.
- **Slide 5: Start**: A prominent "START LISTENING" button.
- Apply consistent glass-like theming and orange accents.

### Integration

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/MainActivity.kt)
- Wrap the main UI in a `Box`.
- Overlay the `WelcomeScreen` using `AnimatedVisibility` based on the ViewModel state.
- Ensure the welcome screen blocks interaction with the rest of the app until completed.

## Verification Plan

### Automated Tests
- Build the project to verify new file integration and persistence logic.

### Manual Verification
1. **First Launch**: Clear app data. Open the app and verify the Welcome Screen appears immediately.
2. **Navigation**: Swipe through all onboarding slides. Verify animations are smooth.
3. **Completion**: Tap "START LISTENING" on the last slide. Verify the overlay disappears and the main app is accessible.
4. **Second Launch**: Force close and re-open the app. Verify the Welcome Screen does **not** appear again.
