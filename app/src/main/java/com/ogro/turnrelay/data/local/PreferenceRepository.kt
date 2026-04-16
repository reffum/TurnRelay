package com.ogro.turnrelay.data.local

import android.content.Context
import androidx.core.content.edit

class PreferenceRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        "settings",
        Context.MODE_PRIVATE
    )

    fun getServerAddress(): String = prefs.getString("server_address", "")!!
    fun getServerPort(): Int = prefs.getInt("server_port", 0)
    fun getTurnAddress(): String = prefs.getString("turn_address", "")!!
    fun getTurnPort(): Int = prefs.getInt("turn_port", 0)
    fun getTurnUsername(): String = prefs.getString("turn_username", "")!!
    fun getTurnPassword(): String = prefs.getString("turn_password", "")!!

    fun saveServerAddress(serverAddress: String) {
        prefs.edit { putString("server_address", serverAddress) }
    }

    fun saveServerPort(serverPort: Int) {
        prefs.edit { putInt("server_port", serverPort) }
    }

    fun saveTurnAddress(turnAddress: String) {
        prefs.edit { putString("turn_address", turnAddress) }
    }

    fun saveTurnPort(turnPort: Int) {
        prefs.edit { putInt("turn_port", turnPort) }
    }

    fun saveTurnUsername(turnUsername: String) {
        prefs.edit { putString("turn_username", turnUsername) }
    }

    fun saveTurnPassword(turnPassword: String) {
        prefs.edit { putString("turn_password", turnPassword) }
    }
}