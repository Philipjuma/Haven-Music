package com.haven.music.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun WelcomeScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    
    val slides = listOf(
        WelcomeSlide(
            title = "WELCOME TO HAVEN",
            subtitle = "A new experience of music, built around personality and precision.",
            icon = Icons.Default.MusicNote,
            accentColor = Color(0xFFFF9800)
        ),
        WelcomeSlide(
            title = "THE LIBRARY",
            subtitle = "Your collection, organized perfectly. Add folders and find your favorites with ease.",
            icon = Icons.Default.LibraryMusic,
            accentColor = Color(0xFF2196F3)
        ),
        WelcomeSlide(
            title = "THE PLAYER",
            subtitle = "High-fidelity sound with custom physical punch and tactile 3D controls.",
            icon = Icons.Default.PlayCircleFilled,
            accentColor = Color(0xFFFF5722)
        ),
        WelcomeSlide(
            title = "DISCOVER",
            subtitle = "Smart mixes that learn your habits and powerful online search to find what's missing.",
            icon = Icons.Default.Explore,
            accentColor = Color(0xFF4CAF50)
        ),
        WelcomeSlide(
            title = "READY TO PLAY?",
            subtitle = "Haven is ready when you are. Let's find something beautiful.",
            icon = Icons.Default.AutoAwesome,
            accentColor = Color(0xFFBB86FC)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Background Gradient (High-Performance Shader-like approach)
            val currentSlide = slides[pagerState.currentPage]
            val animatedColor by animateColorAsState(
                targetValue = currentSlide.accentColor,
                animationSpec = tween(1000),
                label = "bgColor"
            )

            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(renderEffect = null) // Ensure hardware acceleration
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(animatedColor.copy(alpha = 0.15f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width / 2, y = size.height / 3),
                                radius = size.minDimension * 1.5f
                            )
                        )
                    }
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Main Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    pageSpacing = 0.dp
                ) { page ->
                    val slide = slides[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    
                    OnboardingSlideContent(
                        slide = slide,
                        offset = pageOffset
                    )
                }

                // Footer Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pager Indicators
                    Row(
                        modifier = Modifier.padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) { i ->
                            val active = pagerState.currentPage == i
                            val width by animateDpAsState(if (active) 24.dp else 8.dp, label = "indicator")
                            Box(
                                modifier = Modifier
                                    .size(width, 8.dp)
                                    .clip(CircleShape)
                                    .background(if (active) currentSlide.accentColor else Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }

                    // Bottom Action
                    if (pagerState.currentPage == 4) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .tactilePress(onClick = onComplete),
                            colors = ButtonDefaults.buttonColors(containerColor = currentSlide.accentColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("START LISTENING", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onComplete) {
                                Text("SKIP", color = Color.White.copy(alpha = 0.5f))
                            }
                            
                            FloatingActionButton(
                                onClick = { 
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                },
                                containerColor = currentSlide.accentColor,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp).tactilePress(onClick = { 
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                })
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingSlideContent(
    slide: WelcomeSlide,
    offset: Float
) {
    val alpha = (1f - (offset * 2)).coerceIn(0f, 1f)
    val scale = 0.8f + (0.2f * (1f - offset))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Container
        Surface(
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            color = slide.accentColor.copy(alpha = 0.1f),
            border = BorderStroke(2.dp, slide.accentColor.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    tint = slide.accentColor,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = havenTransform(slide.title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = havenTransform(slide.subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

data class WelcomeSlide(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)
