package com.ogro.turnrelay.util

/**
 * Valid UDP/TCP port
 */
fun portIsValid(port: Int): Boolean {
    return port in 1..65535
}
