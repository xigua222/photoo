package com.voicelike.app

data class AppStats(
    val processed: Int = 0,
    val trashed: Int = 0,
    val skipped: Int = 0,
    val savedSize: Long = 0L
)
