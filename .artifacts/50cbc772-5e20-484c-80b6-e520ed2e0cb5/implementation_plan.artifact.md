# Implementation Plan — UX Refinement & Skip Silence Integration

I will refine the player gestures to be more deliberate and tactile, enhance the settings visual identity with colorful subtexts, and replace the dual-player crossfade with a native "Skip Silence" feature.

## Proposed Changes

### [Audio & Playback]

#### [MODIFY] [MusicService.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/MusicService.kt)
- **Revert to Single Player**: Remove the `CrossfadeManager` and revert to a standard single `ExoPlayer` instance. This simplifies the architecture and improves reliability.
- **Skip Silence**: Enable the native Media3 silence skipping feature (`exoPlayer.setSkipSilenceEnabled(true)`) to automatically trim trailing and leading silence in tracks.
- **Independent Resume**: Refine the connectivity logic to support separate "Auto-play on Bluetooth" and "Auto-play on Headset" preferences.

#### [DELETE] [audio/CrossfadeManager.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/audio/CrossfadeManager.kt)
- Remove this file as the dual-player logic is being replaced by native silence skipping.

### [User Interface & Experience]

#### [MODIFY] [NowPlayingScreen.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/ui/NowPlayingScreen.kt)
- **Tactile Album Art**:
    - Implement a `scale` state that reacts in real-time to the user's drag gesture.
    - Change the `AnimatedContent` transition to a deliberate **Zoom (Scale In/Out)**.
    - Ensure skips (Next/Previous) only trigger *after* the finger is lifted and a threshold (e.g., 120dp) is met.
- **Gesture Reliability**: Add a visual preview (scaling) during the drag to indicate an impending skip.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/ui/SettingsScreen.kt)
- **Colorful Subtexts**: Update the `SettingsRow` and `SettingsSection` to use vibrant, room-themed colors for subtitles (e.g., Amber for Audio, Green for Intelligence/Connectivity).
- **Settings Layout**: Replace "Crossfade" slider with a "Skip Silence" toggle.

#### [MODIFY] [MusicListScreen.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/ui/MusicListScreen.kt)
- **Search Button**: Maintain the green, larger styling for the library search icon.

#### [MODIFY] [DiscoverScreen.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/haven/music/ui/DiscoverScreen.kt)
- **Remove Hints**: Delete all `CherieHintBox` implementations to provide a cleaner, "pro" discovery experience.

## Verification Plan

### Manual Verification
- **Skip Silence**: Play a track with silence at the end and verify the player moves to the next track faster than usual.
- **Tactile Zoom**: Drag the album art horizontally. Verify it zooms in/out slightly during the drag and only skips when the drag is released past the threshold.
- **Settings Visuals**: Verify subtitles in settings are now colorful and consistent with the EQ section's style.
- **Connectivity**: Test separate BT and Wired resume toggles.
- **Cleanup**: Verify the Discover screen is free of onboarding hints.
