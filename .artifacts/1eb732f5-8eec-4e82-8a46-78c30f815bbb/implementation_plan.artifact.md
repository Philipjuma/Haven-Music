# Cherie Music: The PJ Haven Signature Edition

This plan evolves **Cherie Music** into a "heavy," modern, and premium audio powerhouse tailored for **PJ Haven**. We are moving away from standard UI patterns towards an "Instagram-level" aesthetic with high-fidelity sound processing.

## User Review Required

> [!IMPORTANT]
> To achieve "Instagram-level" audio, we will integrate Android's **Audio Effects** (Loudness Enhancer, Bass Boost, Virtualizer). This might slightly increase CPU usage but will significantly enhance sound punchiness.
> The "Heavy" UI will use **Bento-style layouts** and **Montserrat** typography for a modern, architectural feel.

## Proposed Changes

### 1. Branding & Identity (PJ Haven)
*   **Custom Icon**: `ic_launcher_foreground.xml` featuring a pink unicorn on a guitar in a cloud-filled sky.
*   **App Signature**: "Designed for PJ Haven" in the settings and boot sequence.

### 2. The "Heavy" Home Experience
#### [NEW] `HomeScreen.kt`
*   **Bento Layout**: A grid of differently sized cards for "Quick Play," "Recommended for You," and "Recent Gems."
*   **Live Waveform**: A small, real-time audio visualizer integrated into the home screen's mini-player.
*   **Artistic Heading**: A bold, large-scale "Cherie Music" header using Montserrat Black.

### 3. Premium Audio Engine (Instagram-Level)
#### [MODIFY] [MusicService.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/MusicService.kt)
*   **Audio Post-Processing**: Initialize `LoudnessEnhancer`, `BassBoost`, and `Virtualizer` attached to the ExoPlayer session.
*   **High-Res Attributes**: Set `AUDIO_CONTENT_TYPE_MUSIC` and `USAGE_MEDIA` with flags for maximum fidelity.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/MainViewModel.kt)
*   **Visualizer Data**: Expose FFT (Fast Fourier Transform) data from the `Visualizer` API to drive the UI waveforms.
*   **Intelligent Suggestions**: Logic to suggest songs based on the time of day or recently played genres.

### 4. Advanced Theming & Settings
#### [NEW] `SettingsScreen.kt`
*   **Visual Modes**: Toggle between "Artistic Glass" (Frosted), "Night Heavy" (Deep Blacks/Neon), and "Cloud White."
*   **Audio Suite**: Controls for the Bass Boost and Virtualizer intensity.

#### [MODIFY] [CherieTheme.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/ui/CherieTheme.kt)
*   Integrate **Google Fonts (Montserrat)**.
*   Refine the dynamic palette to use deeper, "heavier" shadows and higher contrast.

### 5. Interaction & Polish
#### [MODIFY] [NowPlayingScreen.kt](file:///C:/Users/Phy/AndroidStudioProjects/CherieMusic/app/src/main/java/com/example/cheriemusic/ui/NowPlayingScreen.kt)
*   **Interactive Slider**: A custom, thick progress bar that responds to touch with haptic feedback.
*   **Active Controls**: Fully functional play/pause, skip, and seek.

## Verification Plan

### Automated Tests
*   Verify `LoudnessEnhancer` is correctly attached to the player's audio session ID.
*   Test the suggestion engine returns valid `Song` objects when the library is empty.

### Manual Verification
1.  **Audio Quality**: Toggle "Bass Boost" in settings and listen for the depth increase.
2.  **Modern UI**: Navigate between Home and Player; verify transitions are fluid and "Instagram-like."
3.  **Waveform**: Ensure the visualizer moves in sync with the beat.
4.  **Icon**: Verify the Pink Unicorn icon appears on the device launcher.
