package com.haven.music.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haven.music.LibrarySection
import com.haven.music.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionName: String,
    libraryStats: String,
    isScanning: Boolean,
    rememberPosition: Boolean,
    sleepTimerMinutes: Int?,
    selectedEngine: com.haven.music.AudioEngine,
    equalizerMode: com.haven.music.EqualizerMode,
    musicFolders: Set<String>,
    onBack: () -> Unit,
    onEqualizerClick: () -> Unit,
    onEqualizerModeSelected: (com.haven.music.EqualizerMode) -> Unit,
    onAddFolderClick: () -> Unit,
    onRemoveFolderClick: (String) -> Unit,
    onRescanClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onToggleRememberPosition: (Boolean) -> Unit,
    skipSilenceEnabled: Boolean,
    onToggleSkipSilence: (Boolean) -> Unit,
    resumeOnBT: Boolean,
    onToggleResumeOnBT: (Boolean) -> Unit,
    resumeOnHeadset: Boolean,
    onToggleResumeOnHeadset: (Boolean) -> Unit,
    audioSafeEnabled: Boolean,
    onToggleAudioSafe: (Boolean) -> Unit,
    adaptiveControlsEnabled: Boolean,
    onToggleAdaptiveControls: (Boolean) -> Unit,
    onClearCacheClick: () -> Unit,
    onEngineSelected: (com.haven.music.AudioEngine) -> Unit,
    onAppEqualizerClick: () -> Unit
) {
    var showEngineSelection by remember { mutableStateOf(false) }
    var showEqualizerSelection by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                // App Icon Container
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    border = BorderStroke(1.5.dp, Color(0xFFFF9800).copy(alpha = 0.6f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = R.mipmap.ic_launcher_round,
                            contentDescription = "Haven App Icon",
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "HAVEN MUSIC",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            letterSpacing = 4.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = havenTransform("Personality in every note"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // AUDIO
                item {
                    SettingsSection(title = "AUDIO") {
                        SettingsRow(
                            icon = Icons.Default.Tune,
                            title = "Equalizer",
                            subtitle = if (equalizerMode == com.haven.music.EqualizerMode.App) "App Equalizer active" else "Using System Equalizer",
                            subtitleColor = if (equalizerMode == com.haven.music.EqualizerMode.App) Color(0xFFFF9800) else Color(0xFFBB86FC),
                            onClick = { 
                                if (equalizerMode == com.haven.music.EqualizerMode.App) {
                                    onAppEqualizerClick()
                                } else {
                                    showEqualizerSelection = true 
                                }
                            }
                        )
                        SettingsRow(
                            icon = Icons.Default.SettingsSuggest,
                            title = "Sound Engine",
                            subtitle = when (selectedEngine) {
                                com.haven.music.AudioEngine.PJ_Haven_2_0 -> "PJ Haven 2.0 active"
                                com.haven.music.AudioEngine.PJ_Fern -> "PJ Fern active"
                                else -> "Native / Jetpack Media3"
                            },
                            subtitleColor = Color(0xFFFF9800),
                            onClick = { showEngineSelection = true }
                        )
                        SettingsRow(
                            icon = Icons.Default.Palette,
                            title = "Adaptive Player Buttons",
                            subtitle = "Controls match current album art",
                            trailing = {
                                Switch(
                                    checked = adaptiveControlsEnabled,
                                    onCheckedChange = onToggleAdaptiveControls,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                                )
                            },
                            onClick = { onToggleAdaptiveControls(!adaptiveControlsEnabled) }
                        )
                        SettingsRow(
                            icon = Icons.Default.GppGood,
                            title = "Haven Audiosafe",
                            subtitle = "Warning when volume exceeds 70%",
                            subtitleColor = Color(0xFFF44336),
                            trailing = {
                                Switch(
                                    checked = audioSafeEnabled,
                                    onCheckedChange = onToggleAudioSafe,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                                )
                            },
                            onClick = { onToggleAudioSafe(!audioSafeEnabled) }
                        )
                    }
                }

                // MUSIC LIBRARY
                item {
                    SettingsSection(title = "MUSIC FOLDERS") {
                        if (musicFolders.isEmpty()) {
                            Text(
                                text = "No folders selected.",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            musicFolders.forEach { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF2196F3).copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = folder,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2196F3).copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                        maxLines = 1
                                    )
                                    IconButton(onClick = { onRemoveFolderClick(folder) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                        
                        SettingsRow(
                            icon = Icons.Default.Add,
                            title = "Add Music Folder",
                            subtitle = "Select another directory",
                            subtitleColor = Color(0xFF2196F3),
                            onClick = onAddFolderClick
                        )
                        
                        SettingsRow(
                            icon = Icons.Default.Refresh,
                            title = "Rescan Music",
                            subtitle = if (isScanning) "Scanning device..." else "Refresh your library",
                            subtitleColor = Color(0xFF2196F3),
                            onClick = onRescanClick,
                            trailing = {
                                if (isScanning) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2196F3))
                            }
                        )
                    }
                }

                // PLAYBACK
                item {
                    SettingsSection(title = "PLAYBACK") {
                        SettingsRow(
                            icon = Icons.Default.Timer,
                            title = "Sleep Timer",
                            subtitle = if (sleepTimerMinutes != null) "Stops in $sleepTimerMinutes minutes" else "Off",
                            subtitleColor = Color(0xFF4CAF50),
                            onClick = onSleepTimerClick
                        )
                        SettingsRow(
                            icon = Icons.Default.History,
                            title = "Remember playback position",
                            subtitle = "Resume where you left off",
                            subtitleColor = Color(0xFF4CAF50),
                            trailing = {
                                Switch(
                                    checked = rememberPosition,
                                    onCheckedChange = onToggleRememberPosition,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                                )
                            },
                            onClick = { onToggleRememberPosition(!rememberPosition) }
                        )
                    }
                }

                // SMART CONNECTIVITY
                item {
                    SettingsSection(title = "SMART CONNECTIVITY") {
                        SettingsRow(
                            icon = Icons.Default.BluetoothConnected,
                            title = "Auto-play on Bluetooth",
                            subtitle = "Resume when BT device connects",
                            subtitleColor = Color(0xFFE91E63),
                            trailing = {
                                Switch(
                                    checked = resumeOnBT,
                                    onCheckedChange = onToggleResumeOnBT,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                                )
                            },
                            onClick = { onToggleResumeOnBT(!resumeOnBT) }
                        )
                        
                        SettingsRow(
                            icon = Icons.Default.Headset,
                            title = "Auto-play on Headset",
                            subtitle = "Resume when headphones plugged in",
                            subtitleColor = Color(0xFFE91E63),
                            trailing = {
                                Switch(
                                    checked = resumeOnHeadset,
                                    onCheckedChange = onToggleResumeOnHeadset,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                                )
                            },
                            onClick = { onToggleResumeOnHeadset(!resumeOnHeadset) }
                        )

                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.VolumeOff,
                            title = "Skip Silence",
                            subtitle = "Trim gaps between tracks",
                            subtitleColor = Color(0xFFE91E63),
                            trailing = {
                                Switch(
                                    checked = skipSilenceEnabled,
                                    onCheckedChange = onToggleSkipSilence,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9800), checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                                )
                            },
                            onClick = { onToggleSkipSilence(!skipSilenceEnabled) }
                        )
                    }
                }

                // MAINTENANCE
                item {
                    SettingsSection(title = "LIBRARY MAINTENANCE") {
                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Clear Cached Artwork",
                            subtitle = "Free up device storage",
                            subtitleColor = Color(0xFFBB86FC),
                            onClick = onClearCacheClick
                        )
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "Music Library",
                            subtitle = libraryStats,
                            subtitleColor = Color(0xFFBB86FC),
                            onClick = {}
                        )
                    }
                }

                // APP (Uncategorized)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(havenTransform("About Haven Music"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(
                            text = "An immersive high-fidelity music experience crafted for those who value sound quality and aesthetic precision.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Built by PJ10 Industries", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text("Developer: Philip J.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Version $versionName", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9800))
                    }
                }

                // DIAGNOSTICS (Secondary)
                item {
                    SettingsSection(title = "DIAGNOSTICS") {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Audio Information", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.5f))
                            Text("Engine: ${selectedEngine.name}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                            Text("Equalizer: ${equalizerMode.name}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                            Text("Status: Healthy", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }

                // Footer
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFFF9800).copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                        Text(havenTransform("Haven Music"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Text(havenTransform("PJ10 Industries"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                    }
                }
            }
        }

        if (showEngineSelection) {
            ModalBottomSheet(
                onDismissRequest = { showEngineSelection = false },
                containerColor = Color(0xFF1E1E1E),
                scrimColor = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp)) {
                    Text(
                        text = "SELECT SOUND ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = Color(0xFFFF9800).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    EngineOption(
                        title = "Native",
                        description = "Pure neutral sound.",
                        icon = Icons.Default.Smartphone,
                        isSelected = selectedEngine == com.haven.music.AudioEngine.Media3,
                        onClick = { onEngineSelected(com.haven.music.AudioEngine.Media3); showEngineSelection = false }
                    )
                    EngineOption(
                        title = "PJ Haven 2.0",
                        description = "Punch + Clarity. A powerful signature with deep impact and crisp detail.",
                        icon = Icons.Default.Speaker,
                        isSelected = selectedEngine == com.haven.music.AudioEngine.PJ_Haven_2_0,
                        onClick = { onEngineSelected(com.haven.music.AudioEngine.PJ_Haven_2_0); showEngineSelection = false }
                    )
                    EngineOption(
                        title = "PJ Fern",
                        description = "Energetic, with enhanced instrumental detection and boost for clearer detail.",
                        icon = Icons.Default.MusicNote,
                        isSelected = selectedEngine == com.haven.music.AudioEngine.PJ_Fern,
                        onClick = { onEngineSelected(com.haven.music.AudioEngine.PJ_Fern); showEngineSelection = false }
                    )
                }
            }
        }

        if (showEqualizerSelection) {
            ModalBottomSheet(
                onDismissRequest = { showEqualizerSelection = false },
                containerColor = Color(0xFF1E1E1E),
                scrimColor = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp)) {
                    Text(
                        text = "EQUALIZER MODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = Color(0xFFFF9800).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    EngineOption(
                        title = "App Equalizer",
                        description = "Use Haven's internal audio processing.",
                        icon = Icons.Default.Tune,
                        isSelected = equalizerMode == com.haven.music.EqualizerMode.App,
                        onClick = { onEqualizerModeSelected(com.haven.music.EqualizerMode.App); showEqualizerSelection = false }
                    )
                    EngineOption(
                        title = "Use System Equalizer",
                        description = "Use your device's built-in sound effects panel.",
                        icon = Icons.Default.Settings,
                        isSelected = equalizerMode == com.haven.music.EqualizerMode.System,
                        onClick = { 
                            onEqualizerModeSelected(com.haven.music.EqualizerMode.System)
                            showEqualizerSelection = false
                            onEqualizerClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EngineOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFFF9800).copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF9800).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (isSelected) Color(0xFFFF9800).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = if (isSelected) Color(0xFFFF9800) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (isSelected) Color(0xFFFF9800) else Color.White)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = Color(0xFFFF9800).copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    subtitleColor: Color = Color.White.copy(alpha = 0.5f),
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (enabled) Color(0xFFFF9800).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (enabled) Color(0xFFFF9800) else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = havenTransform(title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f))
            Text(text = havenTransform(subtitle), style = MaterialTheme.typography.bodySmall, color = if (enabled) subtitleColor else Color.White.copy(alpha = 0.2f))
        }
        if (trailing != null) {
            trailing()
        } else if (enabled) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}
