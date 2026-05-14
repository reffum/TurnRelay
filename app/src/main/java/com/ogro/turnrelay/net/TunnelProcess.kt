package com.ogro.turnrelay.net

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//
// Allocate the TURN connection, connect to remote server.
// All data read from the server write to TUN device. All data
// from the TUN device send to the server.
//
object TunnelProcess {
    const val TAG = "TunnelProcess"

    enum class ConnectionState {
        DISCONNECT,
        CONNECTED,
        ERROR
    }

    const val LIBRARY_NAME = "turn_tunnel_lib"

    class StartError(message: String): RuntimeException(message)

    // Current connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECT)
    val connectionState = _connectionState.asStateFlow()

    private var tunnelPtr: Long = 0

    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    // Throw StartError if start failed
    external fun start(
        turnServerAddress: String,
        turnSererPort: Int,
        turnUsername: String,
        turnPassword: String,
        serverAddress: String,
        serverPort: Int,
        tunFd: Int
    )

    external fun stop()

    private fun updateState(stateId: Int) {
        val state = ConnectionState.entries[stateId]
        Log.d(TAG, "state: $state")
        CoroutineScope(Dispatchers.Main).launch {
            _connectionState.value = state
        }
    }
}
