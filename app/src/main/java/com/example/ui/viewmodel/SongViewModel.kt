package com.example.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.AudioSynthesizer
import com.example.util.LyricLine
import com.example.util.LyricParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream

enum class MemorizeMode {
    NORMAL,       // Regular lyrics
    FIRST_LETTER, // "H___ w___ t_ t__ s___"
    MASKED        // Clicking specific words reveals them (starts all hidden or random 50%)
}

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = SongRepository(database.songDao())

    // UI States
    val songs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeSong = MutableStateFlow<SongEntity?>(null)
    val activeSong: StateFlow<SongEntity?> = _activeSong.asStateFlow()

    // Lyrics State
    private val _activeLyricsLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val activeLyricsLines: StateFlow<List<LyricLine>> = _activeLyricsLines.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    // Player State
    private var mediaPlayer: MediaPlayer? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _audioDurationMs = MutableStateFlow(0L)
    val audioDurationMs: StateFlow<Long> = _audioDurationMs.asStateFlow()

    // Memorizer Settings
    private val _memorizeMode = MutableStateFlow(MemorizeMode.NORMAL)
    val memorizeMode: StateFlow<MemorizeMode> = _memorizeMode.asStateFlow()

    // Track which indexes of words in a line are explicitly revealed by the user
    // Key: "lineIndex_wordIndex", Value: Boolean (is revealed)
    private val _revealedWords = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val revealedWords: StateFlow<Map<String, Boolean>> = _revealedWords.asStateFlow()

    // Loop active line state
    private val _isLoopingLine = MutableStateFlow(false)
    val isLoopingLine: StateFlow<Boolean> = _isLoopingLine.asStateFlow()

    // Custom background state
    private val _appBackgroundMode = MutableStateFlow<String>("default_gradient") // default_gradient, nature_gradient, warm_gradient, custom_image
    val appBackgroundMode: StateFlow<String> = _appBackgroundMode.asStateFlow()

    private val _appBackgroundCustomPath = MutableStateFlow<String?>(null)
    val appBackgroundCustomPath: StateFlow<String?> = _appBackgroundCustomPath.asStateFlow()

    // Coroutine Job for tracking time progression
    private var progressTrackerJob: Job? = null

    init {
        // Initialize setup (categories and built-in demo tracks if empty)
        viewModelScope.launch(Dispatchers.IO) {
            setupCategoriesAndDemos()
        }
        
        // Load app theme from shared prefs if any
        val sp = context.getSharedPreferences("song_memorizer_prefs", Application.MODE_PRIVATE)
        _appBackgroundMode.value = sp.getString("bg_mode", "default_gradient") ?: "default_gradient"
        _appBackgroundCustomPath.value = sp.getString("bg_custom_path", null)
    }

    private suspend fun setupCategoriesAndDemos() {
        // Create initial default categories
        val defaultCats = listOf("Electronic", "Folk", "Retro Synth", "Custom Beats")
        for (cat in defaultCats) {
            repository.insertCategory(CategoryEntity(cat))
        }

        // Check if songs database is empty
        repository.allSongs.first().let { currentSongs ->
            if (currentSongs.isEmpty()) {
                // Synthesize file 1: Demo Retro Cyberpunk
                val demoFile1 = File(context.cacheDir, "demo_synth_wave.wav")
                val success1 = AudioSynthesizer.generateDemoAudio(demoFile1, 35)
                
                if (success1) {
                    val lyrics1 = """
                        [00:00.00]Welcome to the Audio Memorizer!
                        [00:04.00]This is an offline ambient synthbeat.
                        [00:08.50]Click any word to seek into that timeline.
                        [00:13.00]Our mind memorizes rhythm and spacing.
                        [00:17.50]Toggle Blind Mode at the top of the interface.
                        [00:22.00]Tap covered words with your finger to reveal them.
                        [00:26.50]You are mastering this lyrics progression!
                        [00:31.00]Keep singing and practicing every day.
                    """.trimIndent()
                    
                    repository.insertSong(
                        SongEntity(
                            title = "Retro Cyberpunk Drive",
                            artist = "Synthesizer Engine",
                            audioPath = demoFile1.absolutePath,
                            lyricsContent = lyrics1,
                            category = "Retro Synth",
                            backgroundPath = "default_gradient",
                            isDemo = true,
                            durationMs = 35000L
                        )
                    )
                }

                // Synthesize file 2: Acoustic Morning Chords
                val demoFile2 = File(context.cacheDir, "demo_folk_chords.wav")
                val success2 = AudioSynthesizer.generateDemoAudio(demoFile2, 40)
                if (success2) {
                    val lyrics2 = """
                        [00:00.00]Soft morning chords are awakening.
                        [00:05.00]Let the warm tones touch your heart.
                        [00:10.00]The sun climbs higher than before.
                        [00:15.00]We can memorize this simple line.
                        [00:20.00]Looping specific sentences reinforces memory.
                        [00:25.00]Every word aligns with the inner rhythm.
                        [00:30.00]Enjoy the beauty of acoustic sounds.
                        [00:35.00]The folk chords fade into the sunrise.
                    """.trimIndent()
                    
                    repository.insertSong(
                        SongEntity(
                            title = "Morning Acoustic Serene",
                            artist = "Synth Folk Ensemble",
                            audioPath = demoFile2.absolutePath,
                            lyricsContent = lyrics2,
                            category = "Folk",
                            backgroundPath = "warm_gradient",
                            isDemo = true,
                            durationMs = 40000L
                        )
                    )
                }
            }
        }
    }

    // Setters
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setMemorizeMode(mode: MemorizeMode) {
        _memorizeMode.value = mode
        _revealedWords.value = emptyMap() // Reset reveals
    }

    fun toggleWordReveal(lineIndex: Int, wordIndex: Int) {
        val key = "${lineIndex}_${wordIndex}"
        val current = _revealedWords.value
        val updated = current.toMutableMap()
        updated[key] = !(current[key] ?: false)
        _revealedWords.value = updated
    }

    fun resetRevealStates() {
        _revealedWords.value = emptyMap()
    }

    fun toggleLoopLine() {
        _isLoopingLine.value = !_isLoopingLine.value
    }

    fun closeActiveSong() {
        stopPlayer()
        _activeSong.value = null
        _activeLyricsLines.value = emptyList()
        _currentLineIndex.value = -1
    }

    fun changeBackgroundMode(mode: String, pathUri: String? = null) {
        _appBackgroundMode.value = mode
        _appBackgroundCustomPath.value = pathUri
        
        val sp = context.getSharedPreferences("song_memorizer_prefs", Application.MODE_PRIVATE)
        sp.edit().apply {
            putString("bg_mode", mode)
            putString("bg_custom_path", pathUri)
            apply()
        }
    }

    // Active song selection and Player integration
    fun selectSong(song: SongEntity) {
        if (_activeSong.value?.id == song.id) return
        
        stopPlayer()
        _activeSong.value = song
        _isLoopingLine.value = false
        _revealedWords.value = emptyMap()
        _currentLineIndex.value = -1
        
        // Parse lyrics
        val parsed = LyricParser.parse(song.lyricsContent, song.durationMs)
        _activeLyricsLines.value = parsed
        
        // Initialize Player in IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            setupPlayer(song.audioPath)
        }
    }

    private suspend fun setupPlayer(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val file = java.io.File(path)
                if (file.exists()) {
                    setDataSource(file.absolutePath)
                } else {
                    // Try parsing as content/system Uri
                    setDataSource(context, Uri.parse(path))
                }
                prepare()
                
                withContext(Dispatchers.Main) {
                    _audioDurationMs.value = duration.toLong()
                    _playbackPositionMs.value = 0L
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e("SongViewModel", "Error initialising Player: ${e.message}")
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        viewModelScope.launch(Dispatchers.Main) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopTrackingProgress()
            } else {
                player.start()
                _isPlaying.value = true
                startTrackingProgress()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            player.seekTo(positionMs.toInt())
            withContext(Dispatchers.Main) {
                _playbackPositionMs.value = positionMs
                updateActiveLineIndex(positionMs)
            }
        }
    }

    fun seekRelative(seconds: Int) {
        val player = mediaPlayer ?: return
        val current = player.currentPosition
        val target = (current + seconds * 1000).coerceIn(0, player.duration)
        seekTo(target.toLong())
    }

    private fun startTrackingProgress() {
        stopTrackingProgress()
        progressTrackerJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition.toLong()
                        _playbackPositionMs.value = currentPos
                        updateActiveLineIndex(currentPos)
                        
                        // Handle line looping boundary if enabled
                        handleLineLooping(currentPos)
                    } else {
                        _isPlaying.value = false
                    }
                }
                delay(150)
            }
        }
    }

    private fun handleLineLooping(currentPos: Long) {
        val activeLines = _activeLyricsLines.value
        val activeIndex = _currentLineIndex.value
        if (!_isLoopingLine.value || activeIndex < 0 || activeIndex >= activeLines.size) return
        
        val currentLine = activeLines[activeIndex]
        val nextLineStartMs = if (activeIndex + 1 < activeLines.size) {
            activeLines[activeIndex + 1].timestampMs
        } else {
            _audioDurationMs.value
        }
        
        // If progress moves past this line, seek back to line start!
        if (currentPos >= nextLineStartMs || currentPos < currentLine.timestampMs) {
            seekTo(currentLine.timestampMs)
        }
    }

    private fun updateActiveLineIndex(positionMs: Long) {
        val lines = _activeLyricsLines.value
        if (lines.isEmpty()) {
            _currentLineIndex.value = -1
            return
        }

        var matchIndex = 0
        for (i in lines.indices) {
            if (positionMs >= lines[i].timestampMs) {
                matchIndex = i
            } else {
                break
            }
        }
        _currentLineIndex.value = matchIndex
    }

    private fun stopTrackingProgress() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    private fun stopPlayer() {
        stopTrackingProgress()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                // Ignored
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playbackPositionMs.value = 0L
    }

    // Database Actions
    fun addNewCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(CategoryEntity(name.trim()))
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(name)
        }
    }

    fun importSong(
        title: String,
        artist: String,
        lyricsText: String,
        category: String,
        audioFileUri: Uri,
        bgImageUri: Uri?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Copy audio content to app secure private folder
                val audioPrivateFile = File(context.filesDir, "audio_${System.currentTimeMillis()}.bin")
                context.contentResolver.openInputStream(audioFileUri)?.use { input ->
                    FileOutputStream(audioPrivateFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Copy background image content to private folder if specified
                var bgPath: String? = null
                if (bgImageUri != null) {
                    val bgPrivateFile = File(context.filesDir, "bg_${System.currentTimeMillis()}.bin")
                    context.contentResolver.openInputStream(bgImageUri)?.use { input ->
                        FileOutputStream(bgPrivateFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    bgPath = bgPrivateFile.absolutePath
                }

                // Get dynamic duration using a temporary MediaMetadataRetriever
                var duration = 0L
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(audioPrivateFile.absolutePath)
                    val timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    duration = timeStr?.toLongOrNull() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    Log.e("SongViewModel", "retriever error duration: ${e.message}")
                    // Fallback to estimation or 120 secs
                    duration = 120000L
                }

                val song = SongEntity(
                    title = title.ifBlank { "Untitled track" },
                    artist = artist.ifBlank { "Unknown Artist" },
                    audioPath = audioPrivateFile.absolutePath,
                    lyricsContent = lyricsText.ifBlank { "[00:00]Plain Lyrics format" },
                    category = category,
                    backgroundPath = bgPath,
                    isDemo = false,
                    durationMs = duration
                )
                repository.insertSong(song)
            } catch (e: Exception) {
                Log.e("SongViewModel", "Failed importing: ${e.message}")
            }
        }
    }

    fun deleteSong(song: SongEntity) {
        if (_activeSong.value?.id == song.id) {
            stopPlayer()
            _activeSong.value = null
            _activeLyricsLines.value = emptyList()
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSong(song)
            // Delete accompanying local audio files
            if (!song.isDemo) {
                try {
                    File(song.audioPath).delete()
                    song.backgroundPath?.let {
                        File(it).delete()
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayer()
    }
}
