package com.ogro.turnrelay.services

import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

@SuppressLint("VpnServicePolicy")
class TurnVpnService : VpnService() {
    companion object {
        const val TAG = "TurnVpnService"
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "TurnVpnService started")

        // Configure the VPN
        val builder = Builder()
            .setSession("TurnVpnService")
            .addAddress("10.8.0.2", 32)
            .addRoute("0.0.0.0", 0)

        // Establish the VPN connection
        vpnInterface = builder.establish()

        return START_STICKY
    }

    override fun onRevoke() {
        Log.i(TAG, "Service revoked")
        closeInterface()
        super.onRevoke()
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        closeInterface()
        super.onDestroy()
    }

    private fun closeInterface() {
        try {
            vpnInterface?.close()
        } finally {
            vpnInterface = null
        }
    }
}