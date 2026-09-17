package com.haven.music.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haven.music.audio.EqualizerManager

private val Amber = Color(0xFFF5C542)
private val Orange = Color(0xFFFF8A2B)
private val CardBg = Color(0xFF161311)
private val TextMuted = Color(0xFFA89A8F)

// New Requested Colors
private val PunchColor = Color(0xFF90EE90)  // Light Green
private val ImmerseColor = Color(0xFFADD8E6) // Light Blue
private val SpaceColor = Color(0xFFFFFACD)   // Light Yellow
private val AuraColor = Color(0xFFFFB6C1)    // Light Pink

@Composable
fun EqualizerScreen(eqManager: EqualizerManager, viewModel: com.haven.music.MainViewModel, onBack: () -> Unit) {
    val isEnabled by eqManager.isEnabled
    val bassStrength by eqManager.bassStrength
    val virtualizerStrength by eqManager.virtualizerStrength
    val reverbPreset by eqManager.reverbPreset
    val ambience by eqManager.ambience
    val bandLevels by eqManager.bandLevels
    val bandFrequencies by eqManager.bandFrequencies
    
    val visibleHints = viewModel.visibleHints

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0D0C))
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Equalizer", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { eqManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Orange, checkedTrackColor = Orange.copy(alpha = 0.4f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // MAIN EQ BANDS - Full Height utilization
                if (bandFrequencies.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f) // Extra weight for the detailed bands
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = CardBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 24.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            bandFrequencies.forEachIndexed { index, freq ->
                                BandSlider(
                                    frequencyHz = freq,
                                    levelMillibel = bandLevels.getOrElse(index) { 0 },
                                    minLevel = eqManager.minLevelMillibel,
                                    maxLevel = eqManager.maxLevelMillibel,
                                    onLevelChange = { eqManager.setBandLevel(index, it) },
                                    enabled = isEnabled
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ADVANCED EFFECTS SUITE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Punch (Bass Boost)
                    EffectCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speaker,
                        title = "Punch",
                        value = "${(bassStrength / 10f).toInt()}%",
                        sliderValue = bassStrength.toFloat(),
                        onValueChange = { eqManager.setBassStrength(it.toInt()) },
                        valueRange = 0f..1000f,
                        accentColor = PunchColor,
                        enabled = isEnabled
                    )
                    
                    // Immerse (Virtualizer)
                    EffectCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AllInclusive,
                        title = "Immerse",
                        value = "${(virtualizerStrength / 10f).toInt()}%",
                        sliderValue = virtualizerStrength.toFloat(),
                        onValueChange = { eqManager.setVirtualizerStrength(it.toInt()) },
                        valueRange = 0f..1000f,
                        accentColor = ImmerseColor,
                        enabled = isEnabled
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Space (Reverb)
                    val reverbNames = listOf("None", "Small", "Medium", "Large", "Hall", "Plate")
                    EffectCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.HomeWork,
                        title = "Space",
                        value = reverbNames.getOrElse(reverbPreset.toInt()) { "None" },
                        sliderValue = reverbPreset.toFloat(),
                        onValueChange = { eqManager.setReverbPreset(it.toInt()) },
                        valueRange = 0f..5f,
                        steps = 4,
                        accentColor = SpaceColor,
                        enabled = isEnabled
                    )

                    // Aura (Ambience)
                    EffectCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoAwesome,
                        title = "Aura",
                        value = "$ambience%",
                        sliderValue = ambience.toFloat(),
                        onValueChange = { eqManager.setAmbience(it.toInt()) },
                        valueRange = 0f..100f,
                        accentColor = AuraColor,
                        enabled = isEnabled
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { eqManager.resetFlat() }) {
                        Text("Reset to Flat", color = TextMuted, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Contextual Hints
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (visibleHints["eq_stacking"] == true) {
                HavenHintBox(
                    title = "Pro Tip",
                    message = "These effects stack on top of the PJ Haven engine for maximum power.",
                    icon = Icons.Default.Bolt,
                    accentColor = Color(0xFFBB86FC),
                    titleColor = Color(0xFFE1BEE7),
                    messageColor = Color.White,
                    onDismiss = { viewModel.dismissHint("eq_stacking") }
                )
            }
        }
    }
}

@Composable
private fun BandSlider(
    frequencyHz: Int,
    levelMillibel: Short,
    minLevel: Int,
    maxLevel: Int,
    onLevelChange: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        val dbValue = (levelMillibel / 100)
        Text(
            text = "${if (dbValue > 0) "+" else ""}$dbValue",
            color = if (enabled) Orange else TextMuted.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .width(44.dp)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = levelMillibel.toFloat(),
                onValueChange = { if (enabled) onLevelChange(it.toInt()) },
                valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = if (enabled) Color.White else Color.Gray,
                    activeTrackColor = if (enabled) Amber else Color.DarkGray,
                    inactiveTrackColor = Color(0xFF25201E)
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalSlider(),
                enabled = enabled
            )
        }
        val label = if (frequencyHz >= 1000) "${frequencyHz / 1000}k" else "$frequencyHz"
        Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Modifier.verticalSlider() = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth
        )
    )
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        ) {
            rotationZ = -90f
        }
    }
}

@Composable
private fun EffectCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    accentColor: Color,
    enabled: Boolean
) {
    Surface(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(20.dp),
        color = CardBg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if (enabled) accentColor else TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(value, color = if (enabled) accentColor else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = if (enabled) accentColor else Color.Gray,
                    activeTrackColor = if (enabled) accentColor else Color.DarkGray,
                    inactiveTrackColor = Color(0xFF25201E)
                ),
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )
        }
    }
}
