package com.haven.music.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.haven.music.Playlist
import com.haven.music.Song
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun Modifier.tactilePress(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    val translationY by animateFloatAsState(
        targetValue = if (pressed && enabled) 2f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressTranslation"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.translationY = translationY
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                },
                onTap = { onClick() },
                onLongPress = { onLongClick?.invoke() }
            )
        }
}

private val orangeAccent = Color(0xFFFF9800)
private val philipPurple = Color(0xFFBB86FC)
private val specialArtists = listOf("Jhené", "Aiko", "Kehlani", "Jhené Aiko")

@Composable
fun havenTransform(text: String, isArtistName: Boolean = false): AnnotatedString {
    return remember(text, isArtistName) {
        buildAnnotatedString {
            if (isArtistName && specialArtists.contains(text)) {
                withStyle(style = SpanStyle(color = orangeAccent)) {
                    append(text)
                }
            } else if (text.isNotEmpty() && (text[0] == 'P' || text[0] == 'p')) {
                withStyle(style = SpanStyle(color = philipPurple)) {
                    append(text[0])
                }
                append(text.substring(1))
            } else {
                append(text)
            }
        }
    }
}

@Composable
fun getRotatingSearchPlaceholder(): String {
    val messages = remember {
        listOf(
            "Looking for something?",
            "Can't find it here?",
            "Try a song, artist or album.",
            "Your next song might be hiding here.",
            "What's playing in your head?",
            "Looking for something specific?",
            "Not in your library? We can look elsewhere.",
            "Type it. Let's find it.",
            "Your library has plenty. But maybe not everything.",
            "Can't find that one?",
            "Go on. Search for it.",
            "Something missing?",
            "Tell Haven what you're looking for.",
            "Looking for a particular song?"
        )
    }
    return remember { messages.random() }
}

@Composable
fun getPersonalityGreeting(artistName: String? = null, viewModel: com.haven.music.MainViewModel? = null): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    val timeGreetings = when (hour) {
        in 5..10 -> listOf<String>(
            "Rise and shine.", "Coffee and beats?", "Morning rhythm.", "Let's start the day right.", 
            "Early bird session?", "Fresh tracks for a fresh morning.", "Wake up with music.", "Good morning, Haven is ready."
        )
        in 11..16 -> listOf<String>(
            "Afternoon groove.", "Mid-day melody.", "Keeping the energy up.", "Work mode?", 
            "Lunchtime playlist?", "Sunshine and songs.", "Chill afternoon vibes.", "The day is halfway through."
        )
        in 17..22 -> listOf<String>(
            "Evening wind down.", "Sunset session.", "Night mode engaged.", "Relaxing rhythm.", 
            "Evening vibe check.", "Smooth evening.", "The night is young.", "Let's dim the lights."
        )
        else -> listOf<String>(
            "Insomnia session?", "Deep night vibes.", "Moonlight music.", "Keeping it quiet.", 
            "Late night discovery.", "Why are we still awake?", "Dreamy tracks only.", "Soft beats for the dark."
        )
    }

    val genericGreetings = listOf(
        "Your library is calling.", "Let's find a hidden gem.", "Any requests?", "Ready for some ooomph?", 
        "What's the plan?", "Let's make some noise.", "Music is the answer.", "Philip's secret mix?", 
        "Still learning your taste.", "You've got good taste.", "Let's dive deep.", "Discover something beautiful.", 
        "The stage is yours.", "Music for your soul.", "High-fidelity happiness.", "Haven loves this part.", 
        "Press play and forget the rest.", "Your soundtrack for today.", "Bringing the vibe.", "Personality in every note.", 
        "Haven + You = Magic.", "Let the music speak.", "Unlocking new memories.", "A classic choice.", 
        "Reliability meets rhythm.", "Your music, your rules.", "Haven is at your service.", "What's in your heart?", 
        "A new experience awaits.", "The perfect song is here.", "Let's explore your collection.", "Rhythm and soul.", 
        "Haven is feeling musical.", "Another session, another story.", "Your ears will thank you.", "Pure audio bliss."
    )

    val artistStrings = if (artistName != null) {
        listOf(
            "$artistName sounds like a good idea.", "You seem to really like $artistName.", 
            "More $artistName today?", "Haven is feeling $artistName vibes.",
            "Keeping it chill with $artistName."
        )
    } else emptyList()
    
    return remember(artistName, hour) {
        (timeGreetings + genericGreetings + artistStrings).random()
    }
}

@Composable
fun OnlineTrackItem(track: com.haven.music.OnlineTrack, onClick: () -> Unit) {
    val context = LocalContext.current
    val accentColor = when (track.provider) {
        "MusicBrainz" -> Color(0xFF2196F3)
        "Deezer" -> Color(0xFFFF5722) // Deep Orange
        "iTunes" -> Color(0xFFE91E63) // Pink
        "Baquir" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF4CAF50) // Green for Audius/Jamendo
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .tactilePress(onClick = onClick),
        color = accentColor.copy(alpha = 0.05f).compositeOver(Color.Black.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track.artUrl)
                    .size(250)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                if (track.duration > 0) {
                    Text(
                        text = formatDuration(track.duration),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = track.provider,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ShimmeringTrackItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        color = Color.White.copy(alpha = alpha).compositeOver(Color.Black.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )
            
            Column(modifier = Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp, 20.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun DeepInsightSlideshow(insights: com.haven.music.DeepInsight) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 3 // Song, Production, Artist
    
    LaunchedEffect(insights) {
        while (true) {
            delay(8000) // Even slower (8 seconds)
            currentPage = (currentPage + 1) % totalPages
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.08f).compositeOver(Color.Black.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp), 
        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.15f))
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(500))
            },
            label = "InsightTransition"
        ) { page ->
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                Text(
                    text = when(page) {
                        0 -> "SONG KNOWLEDGE"
                        1 -> "PRODUCTION"
                        else -> "ARTIST STORY"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 9.sp),
                    color = Color(0xFF2196F3)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                when(page) {
                    0 -> SongMetadataLayer(insights)
                    1 -> ProductionLayer(insights)
                    else -> ArtistLayer(insights.artistInfo)
                }
            }
        }
    }
}

@Composable
fun SongMetadataLayer(insight: com.haven.music.DeepInsight) {
    val recording = insight.recordingInfo
    val title = recording?.optString("title", "Unknown") ?: "Unknown"
    val releaseDate = insight.year ?: "..."
    val genre = insight.genre ?: "..."
    val label = insight.label ?: "..."
    
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoChip(label = "Released", value = releaseDate, color = Color(0xFF2196F3))
            InfoChip(label = "Genre", value = genre, color = Color(0xFF2196F3))
            InfoChip(label = "Label", value = label, color = Color(0xFF2196F3))
        }
    }
}

@Composable
fun ProductionLayer(insight: com.haven.music.DeepInsight) {
    val producers = insight.producers
    val producerText = if (producers.isNotEmpty()) producers.joinToString(", ") else "Unknown Producer"

    Column {
        Text(text = "Mind Behind", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        Text(
            text = producerText, 
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
            color = Color.White, 
            modifier = Modifier.padding(top = 2.dp),
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Official Data", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color(0xFF2196F3).copy(alpha = 0.4f))
    }
}

@Composable
fun ArtistLayer(artist: org.json.JSONObject?) {
    val name = artist?.optString("name", "Unknown") ?: "Unknown"
    val country = artist?.optString("country", "Unknown") ?: "..."
    val lifeSpan = artist?.optJSONObject("life-span")
    val born = lifeSpan?.optString("begin", "Unknown") ?: "..."
    
    Column {
        Text(text = name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoChip(label = "From", value = country, color = Color(0xFF2196F3))
            InfoChip(label = "Born", value = born.take(4), color = Color(0xFF2196F3))
        }
    }
}

@Composable
fun InfoChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 0.5.sp), color = Color.White.copy(alpha = 0.5f))
            Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = havenTransform(message),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun ExternalSearchState(query: String, onExternalSearch: (String, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Can't find what you're looking for?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        
        Text(
            text = "Search elsewhere",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val providers = listOf(
                Triple("YouTube", Icons.Default.PlayArrow, Color(0xFFFF0000)),
                Triple("Spotify", Icons.Default.MusicNote, Color(0xFF1DB954)),
                Triple("Deezer", Icons.AutoMirrored.Filled.QueueMusic, Color(0xFF8E24AA)), // Deezer Purple
                Triple("Google", Icons.Default.Search, Color(0xFF4285F4))
            )
            
            providers.forEach { (name, icon, color) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onExternalSearch(query, name) }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    index: Int,
    song: Song, 
    isSelected: Boolean, 
    isFavorite: Boolean,
    itemColor: Color,
    cachedBitmap: Bitmap? = null,
    onBitmapLoaded: (Bitmap) -> Unit = {},
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val animatedColor by animateColorAsState(
        targetValue = itemColor,
        animationSpec = tween(300),
        label = "itemColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp), // Maximize reach
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. TRACK NUMBER (Independent, outside card, compact for 4 digits)
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (index >= 1000) 9.sp else 12.sp
            ),
            color = Color(0xFFFF9800),
            modifier = Modifier.width(32.dp).padding(start = 2.dp),
            textAlign = TextAlign.Center
        )

        // 2. SONG CARD (Tactile + Max Width + Marquee + End Duration)
        Surface(
            modifier = Modifier
                .weight(1f) // Fills ALL available space
                .tactilePress(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            color = animatedColor.copy(alpha = 0.06f).compositeOver(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                1.dp, 
                if (isSelected) Color(0xFFFF9800).copy(alpha = 0.4f) 
                else Color(0xFFFF9800).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cachedBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = cachedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = remember(song.albumArtUri) {
                            ImageRequest.Builder(context)
                                .data(song.albumArtUri)
                                .size(500) // Performance: High-fidelity decoding
                                .allowHardware(true) // Memory & Speed efficiency
                                .crossfade(false)
                                .build()
                        },
                        onSuccess = { state ->
                            (state.result.drawable as? BitmapDrawable)?.bitmap?.let { onBitmapLoaded(it) }
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = havenTransform(song.title),
                        color = if (isSelected) Color(0xFFFF9800) else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = if (isSelected) Modifier.basicMarquee() else Modifier
                    )
                    
                    Text(
                        text = havenTransform(song.artist, isArtistName = true),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // DURATION (Far right edge of card, matching rail font size)
                Text(
                    text = formatDuration(song.duration),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFFF9800).copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DetailSongItem(
    song: Song,
    itemColor: Color?,
    cachedBitmap: Bitmap? = null,
    onBitmapLoaded: (Bitmap) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // Fix: Ensure a deep, high-contrast background to prevent "white-wash"
    val itemBg = itemColor?.copy(alpha = 0.12f)?.compositeOver(Color.Black.copy(alpha = 0.4f)) 
        ?: Color.White.copy(alpha = 0.03f)

    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        color = itemBg.compositeOver(surfaceColor),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cachedBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = cachedBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = remember(song.albumArtUri) {
                        ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .size(500)
                            .allowHardware(true)
                            .crossfade(false)
                            .build()
                    },
                    onSuccess = { state ->
                        (state.result.drawable as? BitmapDrawable)?.bitmap?.let { onBitmapLoaded(it) }
                    },
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    text = havenTransform(song.title), 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                    color = Color.White, 
                    maxLines = 1
                )
                Text(
                    text = havenTransform(song.artist, isArtistName = true), 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.White.copy(alpha = 0.5f), 
                    maxLines = 1
                )
            }
            Icon(
                Icons.Default.PlayArrow, 
                contentDescription = null, 
                tint = Color.White.copy(alpha = 0.3f), 
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionMenu(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGoToArtist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onLyricsClick: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            // Song Header
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = havenTransform(song.title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(text = havenTransform(song.artist, isArtistName = true), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
            
            // Actions
            MenuActionItem(icon = Icons.Default.PlayArrow, label = "Play", onClick = { onPlay(); onDismiss() })
            MenuActionItem(icon = Icons.AutoMirrored.Filled.PlaylistAdd, label = "Add to Playlist", onClick = { onAddToPlaylist(); onDismiss() })
            
            if (onRemoveFromPlaylist != null) {
                MenuActionItem(icon = Icons.Default.PlaylistRemove, label = "Remove from Playlist", onClick = { onRemoveFromPlaylist(); onDismiss() })
            }
            
            if (onRemoveFromQueue != null) {
                MenuActionItem(icon = Icons.Default.DeleteSweep, label = "Remove from Queue", onClick = { onRemoveFromQueue(); onDismiss() })
            }

            MenuActionItem(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                label = if (isFavorite) "Remove from Favorites" else "Add to Favorites", 
                onClick = { onToggleFavorite(); onDismiss() }
            )
            MenuActionItem(icon = Icons.Default.Person, label = "Go to Artist", onClick = { onGoToArtist(); onDismiss() })
            MenuActionItem(icon = Icons.Default.Album, label = "Go to Album", onClick = { onGoToAlbum(); onDismiss() })
            
            if (onLyricsClick != null) {
                MenuActionItem(icon = Icons.Default.FormatQuote, label = "Lyrics", onClick = { onLyricsClick(); onDismiss() })
            }
            
            MenuActionItem(icon = Icons.AutoMirrored.Filled.QueueMusic, label = "Add to Queue", onClick = { onAddToQueue(); onDismiss() })
            MenuActionItem(icon = Icons.Default.SkipNext, label = "Play Next", onClick = { onPlayNext(); onDismiss() })
            
            // Song Info
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "INFO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = Color(0xFFFF9800).copy(alpha = 0.6f)
            )
            Text(
                text = "Album: ${song.album}\nDuration: ${formatDuration(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun MenuActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFFF9800).copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Text(
            text = havenTransform(label),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumActionMenu(
    album: com.haven.music.Album,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp)) {
            Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = album.artUri, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = album.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(text = album.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
            MenuActionItem(icon = Icons.Default.Album, label = "Open Album", onClick = { onOpen(); onDismiss() })
            MenuActionItem(icon = Icons.Default.PlayArrow, label = "Play", onClick = { onPlay(); onDismiss() })
            MenuActionItem(icon = Icons.AutoMirrored.Filled.QueueMusic, label = "Add to Queue", onClick = { onAddToQueue(); onDismiss() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistActionMenu(
    artist: com.haven.music.Artist,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp)) {
            Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = artist.artUri, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = artist.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(text = "${artist.songs.size} songs", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 16.dp))
            MenuActionItem(icon = Icons.Default.Person, label = "Open Artist", onClick = { onOpen(); onDismiss() })
            MenuActionItem(icon = Icons.Default.PlayArrow, label = "Play", onClick = { onPlay(); onDismiss() })
            MenuActionItem(icon = Icons.AutoMirrored.Filled.QueueMusic, label = "Add to Queue", onClick = { onAddToQueue(); onDismiss() })
        }
    }
}

@Composable
fun Tactile3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CircleShape, // Default to circle for "cuteness"
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val verticalOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = verticalOffset.toPx()
                this.scaleX = scale
                this.scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Soft Drop Shadow
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 3.dp),
            shape = shape,
            color = Color.Black.copy(alpha = 0.4f),
            content = {}
        )
        
        // Main Surface with Polished Black Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF322E2B), Color(0xFF0F0D0C))
                    ),
                    shape = shape
                )
                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)), shape),
            contentAlignment = Alignment.Center
        ) {
            // Inset highlight at the top edge for 3D depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.5.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                            endY = 20f
                        ),
                        shape = shape
                    )
            )
            content()
        }
    }
}

@Composable
fun HavenLogo(modifier: Modifier = Modifier) {
    Text(
        text = "HAVEN MUSIC",
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            letterSpacing = 4.sp
        ),
        color = Color.White
    )
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun BlinkingGreenDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(Color(0xFF4CAF50), CircleShape)
    )
}

@Composable
fun HavenHintBox(
    title: String,
    message: String,
    icon: ImageVector,
    accentColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = accentColor,
    messageColor: Color = Color.White.copy(alpha = 0.9f)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1A1A1A).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        shadowElevation = 24.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(28.dp))
                }
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 2.sp,
                        fontSize = 14.sp
                    ),
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 20.sp,
                        fontSize = 15.sp
                    ),
                    color = messageColor
                )
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Helper to composite colors
fun Color.compositeOver(base: Color): Color {
    val alpha = this.alpha
    return Color(
        red = this.red * alpha + base.red * (1f - alpha),
        green = this.green * alpha + base.green * (1f - alpha),
        blue = this.blue * alpha + base.blue * (1f - alpha),
        alpha = 1f
    )
}
