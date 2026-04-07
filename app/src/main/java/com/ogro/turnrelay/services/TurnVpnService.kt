package com.ogro.turnrelay.services

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class TurnVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        closeInterface()
        super.onRevoke()
    }

    override fun onDestroy() {
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