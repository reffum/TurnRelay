package com.ogro.turnrelay.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.ogro.turnrelay.TurnRelayApplication
import com.ogro.turnrelay.data.StatusLog
import com.ogro.turnrelay.services.TurnVpnService
import com.ogro.turnrelay.util.portIsValid

class StartVpnServerError(errorMessage: String): RuntimeException(errorMessage)

class MainViewModel(application: Application): AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val preferenceRepository = getApplication<TurnRelayApplication>().preferenceRepository

    var serviceEnabled by mutableStateOf(false)
        private set

    var serverAddress by mutableStateOf(preferenceRepository.getServerAddress())
        private set

    var serverPort by mutableIntStateOf(preferenceRepository.getServerPort())
        private set

    var turnAddress by mutableStateOf(preferenceRepository.getTurnAddress())
        private set

    var turnPort by mutableIntStateOf(preferenceRepository.getTurnPort())
        private set

    var turnUsername by mutableStateOf(preferenceRepository.getTurnUsername())
        private set

    var turnPass by mutableStateOf(preferenceRepository.getTurnPassword())
        private set

    val logLines = StatusLog.logLines

    private var serviceMessenger: Messenger? = null
    private var serviceIsBound = false

    private val serviceMessageHandler = Messenger(Handler(Looper.getMainLooper()) { msg ->
        if(msg.what == TurnVpnService.MESSAGE_LOG) {
            val message = msg.obj as String
            StatusLog.Log(message)
        }
        true
    })

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "Service connected")
            serviceMessenger = Messenger(service)
            serviceIsBound = true

            val msg = Message.obtain(
                null,
                TurnVpnService.MESSAGE_REGISTER_CLIENT
            )

            msg.replyTo = serviceMessageHandler
            serviceMessenger?.send(msg)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "Service disconnected")
            serviceMessenger = null
            serviceIsBound = false
        }
    }

    fun bindService() {
        val intent = Intent(getApplication(), TurnVpnService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun serverAddressChanged(it: String) {
        serverAddress = it
        preferenceRepository.saveServerAddress(it)
    }

    fun serverPortChanged(port: Int) {
        serverPort = port
        preferenceRepository.saveServerPort(port)
    }

    fun turnAddressChanged(address: String) {
        turnAddress = address
        preferenceRepository.saveTurnAddress(address)
    }

    fun turnPortChanged(port: Int) {
        turnPort = port
        preferenceRepository.saveTurnPort(port)
    }

    fun turnUserChanged(username: String) {
        turnUsername = username
        preferenceRepository.saveTurnUsername(username)
    }

    fun turnPassChanged(pass: String) {
        turnPass = pass
        preferenceRepository.saveTurnPassword(pass)
    }

    fun startService() {
        val context = getApplication<Application>().applicationContext

        if(!checkConfig()) {
            throw StartVpnServerError("Invalid configuration")
        }

        Log.i(TAG, "Start the vpn service")

        val intent = Intent(context, VpnService::class.java).apply {
            putExtra("SERVER_ADDR", serverAddress)
            putExtra("SERVER_PORT", serverPort)
            putExtra("TURN_ADDRESS", turnAddress)
            putExtra("TURN_PORT", turnPort)
            putExtra("TURN_USER", turnUsername)
            putExtra("TURN_PASS", turnPass)
        }

        context.startService(intent)
        bindService()
    }

    fun stopService() {
        val applicationContext = getApplication<Application>().applicationContext
        val intent = Intent(applicationContext, TurnVpnService::class.java)
        applicationContext.stopService(intent)
        unbindService()
    }

    override fun onCleared() {
        unbindService()
    }

    private fun checkConfig(): Boolean {
        if(serverAddress.isEmpty()) {
            return false
        }

        if(turnAddress.isEmpty()) {
            return false
        }

        if(turnUsername.isEmpty() || turnPass.isEmpty()) {
            return false
        }

        if(!portIsValid(serverPort) || !portIsValid(turnPort)) {
            return false
        }

        return true
    }

    private fun unbindService() {
        if(serviceIsBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceIsBound = false
        }
    }
}
