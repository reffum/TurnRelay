package com.ogro.turnrelay.services

import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ogro.turnrelay.net.TunnelProcess

@SuppressLint("VpnServicePolicy")
class TurnVpnService : VpnService() {
    companion object {
        const val MESSAGE_REGISTER_CLIENT = 1
        const val MESSAGE_LOG =2

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        private const val TAG = "TurnVpnService"

    }

    private var vpnInterface: ParcelFileDescriptor? = null

    private var clientMessanger: Messenger? = null

    private val serviceHandler  = Messenger(Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            1 -> { // Action: register the client
                Log.i(TAG, "Client registered")
                clientMessanger = msg.replyTo
                sendMessageToClient("Client registered")
            }
        }
        true
    })

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "TurnVpnService started")

        if(intent!!.action == ACTION_START) {
            val turnAddress = intent!!.getStringExtra("TURN_ADDRESS")!!
            val turnPort = intent.getIntExtra("TURN_PORT", 0)
            val turnUsername = intent.getStringExtra("TURN_USER")!!
            val turnPassword = intent.getStringExtra("TURN_PASS")!!
            val serverAddr = intent.getStringExtra("SERVER_ADDR")!!
            val serverPort = intent.getIntExtra("SERVER_PORT", 0)

            // Configure the VPN
            val builder = Builder()
                .setSession("TurnVpnService")
                .addAddress("10.0.8.2", 24)
                .addRoute("10.0.8.0", 24)

            // Establish the VPN connection
            vpnInterface = builder.establish()

            TunnelProcess.start(
                turnServerAddress = turnAddress,
                turnSererPort = turnPort,
                turnUsername = turnUsername,
                turnPassword = turnPassword,
                serverAddress = serverAddr,
                serverPort = serverPort,
                tunFd = vpnInterface!!.fd,
            )

            sendMessageToClient("VPN connection established")
            return START_REDELIVER_INTENT
        } else {
            Log.i(TAG, "TurnVpnService stopped")
            closeInterface()
            stopSelf()
            return START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return serviceHandler.binder
    }

    override fun onRevoke() {
        Log.i(TAG, "Service revoked")
        sendMessageToClient("VPN Service turn off")
        closeInterface()
        super.onRevoke()
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        sendMessageToClient("VPN Service turn off")
        closeInterface()
        super.onDestroy()
    }

    private fun closeInterface() {
        try {
            TunnelProcess.stop()
            vpnInterface?.close()
        } finally {
            vpnInterface = null
            sendMessageToClient("VPN interface closed")
        }
    }

    private fun sendMessageToClient(message: String) {
        val msg = Message.obtain(null, MESSAGE_LOG)
        msg.obj = message
        try {
            clientMessanger?.send(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to client", e)
            clientMessanger = null
        }
    }
}