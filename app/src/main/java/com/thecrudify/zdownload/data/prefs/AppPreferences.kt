package com.thecrudify.zdownload.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** What the player needs to pick up where it left off after the process is recreated. */
data class PlaybackSnapshot(
    val queue: List<String> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0,
    val repeatMode: Int = 0,
    val shuffle: Boolean = false,
    val speed: Float = 1f,
    val volume: Float = 1f
)

/** Lightweight settings only. Library data lives in Room. */
class AppPreferences(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val defaultFormat = stringPreferencesKey("default_format")
        val librarySort = stringPreferencesKey("library_sort")
        val queue = stringPreferencesKey("playback_queue")
        val index = intPreferencesKey("playback_index")
        val position = longPreferencesKey("playback_position")
        val repeat = intPreferencesKey("playback_repeat")
        val shuffle = booleanPreferencesKey("playback_shuffle")
        val speed = floatPreferencesKey("playback_speed")
        val volume = floatPreferencesKey("playback_volume")
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.language] ?: "ID" }
    val defaultFormat: Flow<String> = context.dataStore.data.map { it[Keys.defaultFormat] ?: "ORIGINAL" }
    val librarySort: Flow<String> = context.dataStore.data.map { it[Keys.librarySort] ?: "DATE_ADDED" }

    suspend fun setLanguage(value: String) = set(Keys.language, value)
    suspend fun setDefaultFormat(value: String) = set(Keys.defaultFormat, value)
    suspend fun setLibrarySort(value: String) = set(Keys.librarySort, value)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun playbackSnapshot(): PlaybackSnapshot {
        val p = context.dataStore.data.first()
        return PlaybackSnapshot(
            queue = p[Keys.queue]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty(),
            index = p[Keys.index] ?: 0,
            positionMs = p[Keys.position] ?: 0,
            repeatMode = p[Keys.repeat] ?: 0,
            shuffle = p[Keys.shuffle] ?: false,
            speed = p[Keys.speed] ?: 1f,
            volume = p[Keys.volume] ?: 1f
        )
    }

    suspend fun savePlaybackSnapshot(s: PlaybackSnapshot) {
        context.dataStore.edit {
            it[Keys.queue] = s.queue.joinToString("\n")
            it[Keys.index] = s.index
            it[Keys.position] = s.positionMs
            it[Keys.repeat] = s.repeatMode
            it[Keys.shuffle] = s.shuffle
            it[Keys.speed] = s.speed
            it[Keys.volume] = s.volume
        }
    }
}
