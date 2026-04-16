package com.ogro.turnrelay

import android.app.Application
import com.ogro.turnrelay.data.local.PreferenceRepository

class TurnRelayApplication: Application() {
    val preferenceRepository by lazy { PreferenceRepository(this) }
}