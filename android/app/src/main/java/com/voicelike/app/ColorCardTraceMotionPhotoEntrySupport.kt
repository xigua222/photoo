package com.voicelike.app

import com.voicelike.app.motionphoto.MOTION_PHOTO_FILTER_ENTRY_ID

private val COLOR_CARD_TRACE_MOTION_PHOTO_ENTRY_COLORS = listOf(
    0xFF141414.toInt(),
    0xFF5C5C5C.toInt(),
    0xFFEAEAEA.toInt(),
    0xFFFFFFFF.toInt()
)

internal fun buildColorCardTraceMotionPhotoEntryCandidate(): ColorCardTraceFilterCandidate {
    return ColorCardTraceFilterCandidate(
        paletteId = MOTION_PHOTO_FILTER_ENTRY_ID,
        kind = ColorCardTraceFilterKind.MotionPhotoEntry,
        title = "彩色苏醒",
        sourceTag = "Live Effect",
        colors = COLOR_CARD_TRACE_MOTION_PHOTO_ENTRY_COLORS,
        score = -1.0
    )
}
