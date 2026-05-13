package com.ogro.turnrelay.data.local

import android.content.Context
import androidx.core.content.edit

private const val SERVER_ADDR_KEY = "server_address"

private const val SERVER_PORT_KEY = "server_port"

private const val TURN_ADDRESS_KEY = "turn_address"

private const val TURN_PORT_KEY = "turn_port"

private const val TURN_USERNAME_KEY = "turn_username"

private const val TURN_PASSWORD_KEY = "turn_password"

class PreferenceRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        "settings",
        Context.MODE_PRIVATE
    )

    fun getServerAddress(): String = prefs.getString(SERVER_ADDR_KEY, "")!!
    fun getServerPort(): Int = prefs.getInt(SERVER_PORT_KEY, 0)
    fun getTurnAddress(): String = prefs.getString(TURN_ADDRESS_KEY, "")!!
    fun getTurnPort(): Int = prefs.getInt(TURN_PORT_KEY, 0)
    fun getTurnUsername(): String = prefs.getString(TURN_USERNAME_KEY, "")!!
    fun getTurnPassword(): String = prefs.getString(TURN_PASSWORD_KEY, "")!!

    fun saveServerAddress(serverAddress: String) {
        prefs.edit { putString(SERVER_ADDR_KEY, serverAddress) }
    }

    fun saveServerPort(serverPort: Int) {
        prefs.edit { putInt(SERVER_PORT_KEY, serverPort) }
    }

    fun saveTurnAddress(turnAddress: String) {
        prefs.edit { putString(TURN_ADDRESS_KEY, turnAddress) }
    }

    fun saveTurnPort(turnPort: Int) {
        prefs.edit { putInt(TURN_PORT_KEY, turnPort) }
    }

    fun saveTurnUsername(turnUsername: String) {
        prefs.edit { putString(TURN_USERNAME_KEY, turnUsername) }
    }

    fun saveTurnPassword(turnPassword: String) {
        prefs.edit { putString(TURN_PASSWORD_KEY, turnPassword) }
    }
}