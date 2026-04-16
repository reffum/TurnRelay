package com.ogro.turnrelay.util

fun portIsValid(port: Int): Boolean {
    return port in 0..65535
}
