package com.ogro.turnrelay.util

/**
 * Valid UDP/TCP port
 */
fun portIsValid(port: Int): Boolean {
    return port in 1..65535
}

fun portIsValid(portStr: String): Boolean {
    try{
        val port = portStr.toInt()
        return portIsValid(port)
    } catch (_: NumberFormatException) {
        return false
    }
}