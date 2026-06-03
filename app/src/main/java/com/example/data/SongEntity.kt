package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val audioPath: String, // File path or relative internal filename
    val lyricsContent: String, // String containing LRC lyrics or raw lines
    val category: String, // e.g., "My Songs", "Retro Beat", "Classical"
    val backgroundPath: String?, // String (gradient key or custom image private file path)
    val isDemo: Boolean = false,
    val durationMs: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
