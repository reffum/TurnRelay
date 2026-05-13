package com.ogro.turnrelay.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Common application diagnostic log, that we show in UI.
 */
object StatusLog {
    private var _logLines = MutableStateFlow(listOf<String>())
    val logLines = _logLines.asStateFlow()

    fun Log(message: String) {
        _logLines.value += message
    }
}