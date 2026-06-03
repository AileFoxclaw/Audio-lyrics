package com.example.data

import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val allCategories: Flow<List<CategoryEntity>> = songDao.getAllCategories()

    fun getSongsByCategory(category: String): Flow<List<SongEntity>> = 
        songDao.getSongsByCategory(category)

    suspend fun getSongById(id: Long): SongEntity? = 
        songDao.getSongById(id)

    suspend fun insertSong(song: SongEntity): Long = 
        songDao.insertSong(song)

    suspend fun deleteSong(song: SongEntity) = 
        songDao.deleteSong(song)

    suspend fun insertCategory(category: CategoryEntity) = 
        songDao.insertCategory(category)

    suspend fun deleteCategory(name: String) = 
        songDao.deleteCategory(name)
}
