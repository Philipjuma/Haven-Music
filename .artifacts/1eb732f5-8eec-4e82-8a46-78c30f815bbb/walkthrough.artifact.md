# Cherie Music Walkthrough

We've successfully built a beautiful, dynamic, and lightweight music app for your sister. It moves away from the "bare bones" Android look by using modern design principles and high-fidelity audio.

## Key Features

### 1. Dynamic Artistic Theming
Using the **Palette API**, Cherie Music extracts the dominant color from each song's album art. The entire app's theme (backgrounds, accents, and text) shifts seamlessly when you switch songs, making the app feel like a living extension of the music.

### 2. Glassmorphic Now Playing Screen
The player interface features:
- A **blurred album art background** for depth.
- **Frosted glass** cards for the music info.
- Clean, minimal controls that don't clutter the screen.

### 3. Premium Audio Engine
Built on **Jetpack Media3 (ExoPlayer)**, the app supports high-quality playback and maintains a stable connection to the Android system for background play and lock-screen controls.

## Technical Summary

- **Media3 Service**: [MusicService.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/MusicService.kt) handles the playback session.
- **Color Extraction**: [MainViewModel.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/MainViewModel.kt) uses Coil to fetch album art and Palette to find the mood.
- **Dynamic UI**: [CherieTheme.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/ui/CherieTheme.kt) applies the extracted colors using Material 3.

## How to Test
1.  **Launch the App**: Run it on a device with some music files.
2.  **Grant Permission**: It will ask for storage access to find your sister's music.
3.  **Play a Song**: Select a song from the library and watch the UI transform!

> [!TIP]
> Try songs with contrasting album art (e.g., one bright blue, one deep red) to see the dynamic theme in action!
