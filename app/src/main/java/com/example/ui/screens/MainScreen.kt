package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.*
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.viewmodel.MemorizeMode
import com.example.ui.viewmodel.SongViewModel
import com.example.util.LyricLine
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: SongViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel State Collection
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeSong by viewModel.activeSong.collectAsStateWithLifecycle()
    val backgroundMode by viewModel.appBackgroundMode.collectAsStateWithLifecycle()
    val backgroundCustomPath by viewModel.appBackgroundCustomPath.collectAsStateWithLifecycle()

    // Dialog Dialog Triggers
    var showImportDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }

    // Floating UI container supporting responsive theme backgrounds
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Wallpaper / Background Rendering
        BackgroundWallpaper(
            mode = backgroundMode,
            customPath = backgroundCustomPath
        )

        // Main layout Content
        Scaffold(
            containerColor = Color.Transparent, // Let global wallpaper shine through transparently
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // If there is an active song, we overlay the full Player/Memorizer View
                // Otherwise we show the dashboard library
                AnimatedContent(
                    targetState = activeSong,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    },
                    label = "ActiveViewTransition"
                ) { song ->
                    if (song != null) {
                        PlayerMemorizerView(
                            song = song,
                            viewModel = viewModel,
                            onBack = { viewModel.closeActiveSong() }
                        )
                    } else {
                        SongLibraryDashboard(
                            songs = songs,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            viewModel = viewModel,
                            onImportClick = { showImportDialog = true },
                            onManageCategories = { showCategoryDialog = true },
                            onPickBackground = { showBackgroundPicker = true }
                        )
                    }
                }
            }
        }

        // Add Category Dialog Modals
        if (showCategoryDialog) {
            ManageCategoriesDialog(
                categories = categories,
                onAddCategory = { viewModel.addNewCategory(it) },
                onDeleteCategory = { viewModel.deleteCategory(it) },
                onDismiss = { showCategoryDialog = false }
            )
        }

        // Background Picker Dialog Modal
        if (showBackgroundPicker) {
            BackgroundGridPickerDialog(
                currentMode = backgroundMode,
                onChangeMode = { mode, path -> viewModel.changeBackgroundMode(mode, path) },
                onDismiss = { showBackgroundPicker = false }
            )
        }

        // Import Song Dialog Modal
        if (showImportDialog) {
            ImportSongFileDialog(
                categories = categories,
                onImport = { title, artist, lyrics, cat, audioUri, bgUri ->
                    viewModel.importSong(title, artist, lyrics, cat, audioUri, bgUri)
                    Toast.makeText(context, "Import initialized...", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showImportDialog = false }
            )
        }
    }
}

@Composable
fun BackgroundWallpaper(
    mode: String,
    customPath: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (mode) {
            "default_gradient" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3E4759),
                                    Color(0xFF1B1B1F),
                                    Color(0xFF121316)
                                )
                            )
                        )
                )
            }
            "nature_gradient" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF06140D),
                                    Color(0xFF0F3220),
                                    Color(0xFF030A06)
                                )
                            )
                        )
                )
            }
            "warm_gradient" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E0A05),
                                    Color(0xFF451910),
                                    Color(0xFF100300)
                                )
                            )
                        )
                )
            }
            "custom_image" -> {
                if (!customPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = customPath,
                        contentDescription = "Custom Background Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Semi-transparent overlay to ensure extreme text readability and high contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                } else {
                    // Fallback to default sleek gradient if custom wallpaper not successfully found
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF3E4759),
                                        Color(0xFF1B1B1F),
                                        Color(0xFF121316)
                                    )
                                )
                            )
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                )
            }
        }
    }
}

// Float helper parsing because compile-times
fun Float.copy(x: Float) = x
private fun Float.Companion.fromRaw(x: Float) = x
private fun Float.Companion.from(value: Double) = value.toFloat()
private fun Double.toF() = this.toFloat()
private fun Int.toF() = this.toFloat()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongLibraryDashboard(
    songs: List<SongEntity>,
    categories: List<CategoryEntity>,
    selectedCategory: String,
    viewModel: SongViewModel,
    onImportClick: () -> Unit,
    onManageCategories: () -> Unit,
    onPickBackground: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(songs, selectedCategory, searchQuery) {
        songs.filter { song ->
            val matchesCategory = selectedCategory == "All" || song.category == selectedCategory
            val matchesSearch = song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Premium Header Layout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Audio Memorizer",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Tap to sync, seek, and test your lyrics recall",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }

            // Quick Background Selector Icon Button
            IconButton(
                onClick = onPickBackground,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Select Wallpaper",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Fill textfield
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs or artists...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Category Selection Title & Management Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(
                onClick = onManageCategories,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Manage", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage")
            }
        }

        // Horizontal Category Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" Item Chip
            val isAllSelected = selectedCategory == "All"
            SuggestionChip(
                onClick = { viewModel.selectCategory("All") },
                label = { Text("All Songs", color = if (isAllSelected) Color.Black else Color.White) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isAllSelected) Color.White else Color.White.copy(alpha = 0.06f)
                ),
                border = null,
                shape = RoundedCornerShape(16.dp)
            )

            // Dynamic Categories
            for (cat in categories) {
                val isSelected = selectedCategory == cat.name
                SuggestionChip(
                    onClick = { viewModel.selectCategory(cat.name) },
                    label = { Text(cat.name, color = if (isSelected) Color.Black else Color.White) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.06f)
                    ),
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Song Cards Grid List
        if (filteredSongs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Empty library",
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No songs matching filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Import local audio tracks or try our default synthbeats!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredSongs) { _, song ->
                    SongEntityItemCard(
                        song = song,
                        onSongSelect = { viewModel.selectSong(song) },
                        onSongDelete = { viewModel.deleteSong(song) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large CTA Import Song Button
        Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF00315B)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Import Audio")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Song & Lyrics File", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun SongEntityItemCard(
    song: SongEntity,
    onSongSelect: () -> Unit,
    onSongDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSongSelect)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Mini Album Art simulation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (song.category) {
                            "Folk" -> Brush.linearGradient(listOf(Color(0xFFE5A65E), Color(0xFFC75F3A)))
                            "Retro Synth" -> Brush.linearGradient(listOf(Color(0xFF9C4FDB), Color(0xFF4C75E5)))
                            "Electronic" -> Brush.linearGradient(listOf(Color(0xFF3AC7AD), Color(0xFF386ED3)))
                            else -> Brush.linearGradient(listOf(Color(0xFF888888), Color(0xFF444444)))
                        }
                    )
            ) {
                Icon(
                    imageVector = if (song.isDemo) Icons.Default.Hearing else Icons.Default.MusicNote,
                    contentDescription = "Audio Art",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Embedded Category tag
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = song.category.uppercase(),
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            // Seconds representation
            val durationSecs = song.durationMs / 1000
            val minutes = durationSecs / 60
            val seconds = durationSecs % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Delete Custom Imported Songs
            IconButton(
                onClick = onSongDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Song",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedVisualizerWaveHeight(
    isPlaying: Boolean,
    targetVal: Int,
    speedMs: Int,
    transition: InfiniteTransition,
    labelName: String
): State<Float> {
    return if (isPlaying) {
        transition.animateFloat(
            initialValue = (targetVal * 0.4f),
            targetValue = (targetVal * 1.0f),
            animationSpec = infiniteRepeatable(
                animation = tween(speedMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = labelName
        )
    } else {
        remember { mutableStateOf(targetVal * 0.7f) }
    }
}

@Composable
fun SleekVisualizerCard(
    isPlaying: Boolean,
    playbackPositionMs: Long,
    audioDurationMs: Long
) {
    val durationSecs = audioDurationMs / 1000
    val progressSecs = playbackPositionMs / 1000
    
    val infiniteTransition = rememberInfiniteTransition(label = "Visualizer")
    
    val h1 = AnimatedVisualizerWaveHeight(isPlaying, 48, 800, infiniteTransition, "w1")
    val h2 = AnimatedVisualizerWaveHeight(isPlaying, 80, 1000, infiniteTransition, "w2")
    val h3 = AnimatedVisualizerWaveHeight(isPlaying, 128, 1200, infiniteTransition, "w3")
    val h4 = AnimatedVisualizerWaveHeight(isPlaying, 64, 750, infiniteTransition, "w4")
    val h5 = AnimatedVisualizerWaveHeight(isPlaying, 96, 1100, infiniteTransition, "w5")
    val h6 = AnimatedVisualizerWaveHeight(isPlaying, 48, 900, infiniteTransition, "w6")
    val h7 = AnimatedVisualizerWaveHeight(isPlaying, 112, 1300, infiniteTransition, "w7")
    val h8 = AnimatedVisualizerWaveHeight(isPlaying, 56, 850, infiniteTransition, "w8")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1B1B1F))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
    ) {
        // Absolute center-aligned flex-alike wave
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(4.dp).height(h1.value.dp).clip(CircleShape).background(Color(0xFFD0E4FF)))
            Box(Modifier.width(4.dp).height(h2.value.dp).clip(CircleShape).background(Color(0xFFD0E4FF)))
            Box(Modifier.width(4.dp).height(h3.value.dp).clip(CircleShape).background(Color(0xFFD0E4FF).copy(alpha = 0.8f)))
            Box(Modifier.width(4.dp).height(h4.value.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Box(Modifier.width(4.dp).height(h5.value.dp).clip(CircleShape).background(Color(0xFFD0E4FF)))
            Box(Modifier.width(4.dp).height(h6.value.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Box(Modifier.width(4.dp).height(h7.value.dp).clip(CircleShape).background(Color(0xFFD0E4FF)))
            Box(Modifier.width(4.dp).height(h8.value.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
        }

        // Timer badge at bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = String.format("%02d:%02d / %02d:%02d", progressSecs / 60, progressSecs % 60, durationSecs / 60, durationSecs % 60),
                color = Color(0xFFD0E4FF),
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PlayerMemorizerView(
    song: SongEntity,
    viewModel: SongViewModel,
    onBack: () -> Unit
) {
    val currentLineIndex by viewModel.currentLineIndex.collectAsStateWithLifecycle()
    val activeLyricsLines by viewModel.activeLyricsLines.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val audioDurationMs by viewModel.audioDurationMs.collectAsStateWithLifecycle()
    
    // Memorizer State configs
    val memorizeMode by viewModel.memorizeMode.collectAsStateWithLifecycle()
    val revealedWords by viewModel.revealedWords.collectAsStateWithLifecycle()
    val isLoopingLine by viewModel.isLoopingLine.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    // Fully automated center auto-scroll whenever active lyric line advances
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && activeLyricsLines.isNotEmpty()) {
            val centerOffsetIndex = (currentLineIndex - 2).coerceAtLeast(0)
            lazyListState.animateScrollToItem(centerOffsetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Player Action Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close player", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NOW MEMORIZING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD0E4FF).copy(alpha = 0.72f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "${song.title} — ${song.artist}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Intensive Line-by-Line Looper toggle
            IconButton(
                onClick = { viewModel.toggleLoopLine() },
                modifier = Modifier
                    .background(
                        if (isLoopingLine) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isLoopingLine) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RepeatOne,
                    contentDescription = "Loop active line",
                    tint = if (isLoopingLine) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MEMORIZER MODE CAPSULE PICKER PINS
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // NORMAL
                val normalSelected = memorizeMode == MemorizeMode.NORMAL
                Button(
                    onClick = { viewModel.setMemorizeMode(MemorizeMode.NORMAL) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (normalSelected) Color.White else Color.Transparent,
                        contentColor = if (normalSelected) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Read", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Read", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // FIRST LETTER
                val firstSelected = memorizeMode == MemorizeMode.FIRST_LETTER
                Button(
                    onClick = { viewModel.setMemorizeMode(MemorizeMode.FIRST_LETTER) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (firstSelected) Color.White else Color.Transparent,
                        contentColor = if (firstSelected) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Spellcheck, contentDescription = "Clues", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hints", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // BLIND/MASKED REVEAL CARDS
                val maskedSelected = memorizeMode == MemorizeMode.MASKED
                Button(
                    onClick = { viewModel.setMemorizeMode(MemorizeMode.MASKED) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (maskedSelected) Color.White else Color.Transparent,
                        contentColor = if (maskedSelected) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = "Blind", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Blind", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sleek audio-visualizer display card matching the sleek aspect ratios and pulse indicators
        SleekVisualizerCard(
            isPlaying = isPlaying,
            playbackPositionMs = playbackPositionMs,
            audioDurationMs = audioDurationMs
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Center Lyric Display Area designed as the "Interactive Subtitles" card with borders and indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            // Interactive Subtitles Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "INTERACTIVE SUBTITLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0E4FF),
                    letterSpacing = 0.1.sp
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD0E4FF).copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Sync Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD0E4FF)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (activeLyricsLines.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            "No subtitle captions found.\nPress 'Play' to start song.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(vertical = 80.dp), // Massive centering cushions
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(activeLyricsLines) { lineIdx, line ->
                            val isHighlighted = lineIdx == currentLineIndex
                            
                            LyricLineSentenceRow(
                                line = line,
                                lineIdx = lineIdx,
                                isHighlighted = isHighlighted,
                                memorizeMode = memorizeMode,
                                revealedWords = revealedWords,
                                onWordClick = { wordIdx ->
                                    // Tap seek to start of lyric timestamp
                                    viewModel.seekTo(line.timestampMs)
                                    if (memorizeMode == MemorizeMode.MASKED) {
                                        viewModel.toggleWordReveal(lineIdx, wordIdx)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Looping line alert banner if active
        if (isLoopingLine && currentLineIndex >= 0 && currentLineIndex < activeLyricsLines.size) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Loop, contentDescription = "Looping", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Looping current lyrics segment indefinitely for active recall practice",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Active Player Timeline & Progress Slider Control
        PlayerProgressControls(
            playbackPositionMs = playbackPositionMs,
            audioDurationMs = audioDurationMs,
            onSeekChange = { targetPos -> viewModel.seekTo(targetPos) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Main Player Playbacks FAB panel
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // REWIND 5s
            IconButton(
                onClick = { viewModel.seekRelative(-5) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .size(46.dp)
            ) {
                Icon(Icons.Default.Replay5, contentDescription = "-5 Sec", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(28.dp))

            // PLAY PAUSE MAIN BIG BUTTON
            FloatingActionButton(
                onClick = { viewModel.togglePlayPause() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF00315B),
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause track" else "Play track",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            // FORWARD 5s
            IconButton(
                onClick = { viewModel.seekRelative(5) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .size(46.dp)
            ) {
                Icon(Icons.Default.Forward5, contentDescription = "+5 Sec", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricLineSentenceRow(
    line: LyricLine,
    lineIdx: Int,
    isHighlighted: Boolean,
    memorizeMode: MemorizeMode,
    revealedWords: Map<String, Boolean>,
    onWordClick: (Int) -> Unit
) {
    // Elegant fade out of past and future lyrics so visual hierarchy is clear
    val alphaColor = if (isHighlighted) 1.0f else 0.42f
    val scaleMultiplier = if (isHighlighted) 1.12f else 0.95f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                scaleX = scaleMultiplier
                scaleY = scaleMultiplier
                alpha = alphaColor
            }
    ) {
        // Individual FlowRow for clickability of words inside a sentence
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            line.words.forEachIndexed { wordIdx, word ->
                val key = "${lineIdx}_${wordIdx}"
                val isRevealed = revealedWords[key] ?: false

                val displayText = remember(word, memorizeMode, isRevealed) {
                    when (memorizeMode) {
                        MemorizeMode.NORMAL -> word
                        MemorizeMode.FIRST_LETTER -> {
                            // Strips words leaving first alphabet
                            if (word.isEmpty()) "" else {
                                val first = word[0]
                                val underscores = "_".repeat((word.length - 1).coerceAtLeast(1))
                                "$first$underscores"
                            }
                        }
                        MemorizeMode.MASKED -> {
                            if (isRevealed) word else "[ ? ]"
                        }
                    }
                }

                // Individual word capsule wrapping clickable item with "Sleek Interface" active contrast
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                memorizeMode == MemorizeMode.MASKED && !isRevealed -> Color.White.copy(alpha = 0.10f)
                                isHighlighted -> Color(0xFFD0E4FF)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            1.dp,
                            if (isHighlighted) Color.Transparent else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = { onWordClick(wordIdx) }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayText,
                        color = when {
                            isHighlighted -> Color(0xFF00315B)
                            memorizeMode == MemorizeMode.MASKED && !isRevealed -> Color(0xFFD0E4FF).copy(alpha = 0.8f)
                            else -> Color(0xFFE3E2E6)
                        },
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isHighlighted) 18.sp else 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerProgressControls(
    playbackPositionMs: Long,
    audioDurationMs: Long,
    onSeekChange: (Long) -> Unit
) {
    val durationSecs = audioDurationMs / 1000
    val progressSecs = playbackPositionMs / 1000

    val sliderValue = if (audioDurationMs > 0) playbackPositionMs.toFloat() / audioDurationMs else 0.0f

    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider track
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                val newPosition = (value * audioDurationMs).toLong()
                onSeekChange(newPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time durations Text label
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = String.format("%02d:%02d", progressSecs / 60, progressSecs % 60),
                color = Color.LightGray,
                fontSize = 12.sp
            )
            
            Text(
                text = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60),
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ManageCategoriesDialog(
    categories: List<CategoryEntity>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCatName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1A24)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Manage Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Insertion input row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text("New category...", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (newCatName.isNotBlank()) {
                                onAddCategory(newCatName)
                                newCatName = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF00315B)
                        )
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Current Categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scroll list of categories with delete icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (cat in categories) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat.name, color = Color.White, fontSize = 14.sp)
                                
                                IconButton(
                                    onClick = { onDeleteCategory(cat.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BackgroundGridPickerDialog(
    currentMode: String,
    onChangeMode: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Gallery picker launcher for standard custom wallpapers
    val customImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onChangeMode("custom_image", uri.toString())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A2B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Background Wallpaper",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Choose an ambient coloring skin or custom image",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Grid layout details
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Option 1: Cosmic Violet
                    BackgroundPresettedRow(
                        title = "Cosmic Violet",
                        isSelected = currentMode == "default_gradient",
                        colors = listOf(Color(0xFF0F0B1E), Color(0xFF231645)),
                        onClick = { onChangeMode("default_gradient", null) }
                    )

                    // Option 2: Pine Forest
                    BackgroundPresettedRow(
                        title = "Tranquil Forest",
                        isSelected = currentMode == "nature_gradient",
                        colors = listOf(Color(0xFF06140D), Color(0xFF0F3220)),
                        onClick = { onChangeMode("nature_gradient", null) }
                    )

                    // Option 3: Sunrise Sunset Coral
                    BackgroundPresettedRow(
                        title = "Sunset Ember",
                        isSelected = currentMode == "warm_gradient",
                        colors = listOf(Color(0xFF1E0A05), Color(0xFF451910)),
                        onClick = { onChangeMode("warm_gradient", null) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Import custom Wallpaper button
                    Button(
                        onClick = {
                            customImagePicker.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == "custom_image") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentMode == "custom_image") "Wallpaper: Custom Image" else "Select Custom Image...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BackgroundPresettedRow(
    title: String,
    isSelected: Boolean,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                1.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(colors))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportSongFileDialog(
    categories: List<CategoryEntity>,
    onImport: (String, String, String, String, Uri, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    // Media Import Fields
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var lyricsText by remember { mutableStateOf("") }
    
    var selectedCategory by remember { 
        mutableStateOf(if (categories.isNotEmpty()) categories[0].name else "Folk") 
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Chosen URIs
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBackgroundUri by remember { mutableStateOf<Uri?>(null) }

    // Activity Content Launcher contracts
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedAudioUri = uri }

    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedBackgroundUri = uri }

    val lyricsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Handle read file
    }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A2F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Import Song Media",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Select local audio and paste timing subtitles",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Artist Input
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist / Vocalist", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category selector dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Category: $selectedCategory", color = Color.White)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle", tint = Color.LightGray)
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF27233D))
                    ) {
                        for (cat in categories) {
                            DropdownMenuItem(
                                text = { Text(cat.name, color = Color.White) },
                                onClick = {
                                    selectedCategory = cat.name
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Required Audio File attachment picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { audioPicker.launch("audio/*") }
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (selectedAudioUri != null) Icons.Default.CheckCircle else Icons.Default.LibraryMusic,
                        contentDescription = "Audio Selection",
                        tint = if (selectedAudioUri != null) Color.Green else Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedAudioUri != null) "Audio Attached Successfully" else "Select Audio File *",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (selectedAudioUri != null) "Filename: ${selectedAudioUri?.path?.substringAfterLast("/")}" else "Tap to choose mp3/wav/ogg track",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Background Image attachment selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { bgPicker.launch("image/*") }
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (selectedBackgroundUri != null) Icons.Default.CheckCircle else Icons.Default.Wallpaper,
                        contentDescription = "Wallpaper selection",
                        tint = if (selectedBackgroundUri != null) Color.Green else Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedBackgroundUri != null) "Custom Wallpaper Attached" else "Select Custom Backdrop Image",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (selectedBackgroundUri != null) "Custom background skin chosen" else "Optional backdrop for details board",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Paste Subtitles text input
                Text(
                    text = "Lyrics Content (Supported: Timed LRC & Raw text lines)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = lyricsText,
                    onValueChange = { lyricsText = it },
                    placeholder = { 
                        Text(
                            "[00:02.00]First Lyric Line\n[00:07.50]Second Lyric Line\n\n(Tip: Or paste plain text lines and we will auto-distribute timelines!)", 
                            color = Color.DarkGray,
                            fontSize = 11.sp
                        ) 
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Form submission row actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        enabled = selectedAudioUri != null && title.isNotBlank() && lyricsText.isNotBlank(),
                        onClick = {
                            val audioUri = selectedAudioUri
                            if (audioUri != null) {
                                onImport(
                                    title,
                                    artist,
                                    lyricsText,
                                    selectedCategory,
                                    audioUri,
                                    selectedBackgroundUri
                                )
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add to Library")
                    }
                }
            }
        }
    }
}
