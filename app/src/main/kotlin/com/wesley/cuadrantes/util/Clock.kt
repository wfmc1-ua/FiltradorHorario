package com.wesley.cuadrantes.util

import java.time.LocalDateTime

interface Clock {
    fun now(): LocalDateTime
}

class SystemClock : Clock {
    override fun now(): LocalDateTime = LocalDateTime.now()
}
