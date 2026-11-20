package com.androiddoctor.pro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scan_history")

class ScanHistoryRepository(private val context: Context) {
    private val gson = Gson()
    private val historyKey = stringPreferencesKey("scan_sessions")

    val historyFlow: Flow<List<ScanSession>> = context.dataStore.data.map { prefs ->
        val json = prefs[historyKey] ?: "[]"
        try {
            val type = object : TypeToken<List<ScanSession>>() {}.type
            gson.fromJson<List<ScanSession>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSession(session: ScanSession) {
        context.dataStore.edit { prefs ->
            val current = try {
                val json = prefs[historyKey] ?: "[]"
                val type = object : TypeToken<MutableList<ScanSession>>() {}.type
                gson.fromJson<MutableList<ScanSession>>(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            current.add(0, session)
            val trimmed = current.take(50)
            prefs[historyKey] = gson.toJson(trimmed)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.remove(historyKey) }
    }
}
