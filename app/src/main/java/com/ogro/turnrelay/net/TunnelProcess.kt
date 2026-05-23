package com.ogro.turnrelay.net

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

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

    // VPN service. We need a reference to VpnService to protect
    // TUN socket from route traffic.
    private var activeVpnService: WeakReference<VpnService>? = null

    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    fun registerService(service: VpnService) {
        activeVpnService = WeakReference(service)
    }

    fun unregisterService() {
        activeVpnService = null
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

    /**
     * Protect the TUN socket from route traffic. It call VpnService.protect()
     */
    private fun protect(fd: Int): Boolean {
        if(activeVpnService == null) {
            Log.e(TAG, "VpnService is null")
            return false
        }

        val service = activeVpnService?.get()
        return service?.protect(fd) ?: false
    }
}
