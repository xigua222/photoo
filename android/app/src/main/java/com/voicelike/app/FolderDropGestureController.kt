package com.voicelike.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

internal data class FolderDropGestureFrame(
    val nowMs: Long,
    val cardOffset: Offset,
    val cardCenter: Offset,
    val cardBounds: Rect?,
    val pointerInRoot: Offset?,
    val targetRects: Map<String, Rect>,
    val validTargetIds: Set<String>
)

private const val NEW_FOLDER_TARGET_ID = "new_folder"

internal class FolderDropGestureController(
    @Suppress("UNUSED_PARAMETER") private val dropThresholdPx: Float
) {
    private var activeId: String? = null
    private var lastSwitchMs: Long = 0L
    private var pendingClearSinceMs: Long = 0L

    fun reset(): String? {
        activeId = null
        lastSwitchMs = 0L
        pendingClearSinceMs = 0L
        return null
    }

    fun update(frame: FolderDropGestureFrame): String? {
        val validRects = frame.targetRects
            .filterKeys { it in frame.validTargetIds }
            .entries
            .sortedBy { it.value.left }
        if (validRects.isEmpty()) {
            return reset()
        }

        if (activeId != null && validRects.none { it.key == activeId }) {
            activeId = null
        }

        val rects = validRects.map { it.value }
        val targetWidth = median(rects.map { it.width }).coerceAtLeast(1f)
        val targetHeight = median(rects.map { it.height }).coerceAtLeast(1f)
        val rowLeft = rects.minOf { it.left }
        val rowRight = rects.maxOf { it.right }
        val rowTop = rects.minOf { it.top }
        val rowBottom = rects.maxOf { it.bottom }

        val cardBottom = frame.cardBounds?.let {
            Offset(
                x = (it.left + it.right) / 2f,
                y = it.bottom
            )
        } ?: frame.cardCenter
        val pointer = frame.pointerInRoot

        val rowPadTop = (targetHeight * 1.55f).coerceIn(88f, 168f)
        val rowPadBottom = (targetHeight * 1.15f).coerceIn(68f, 140f)
        val pointerNearRow = pointer?.let {
            it.y >= rowTop - rowPadTop && it.y <= rowBottom + rowPadBottom
        } == true
        val cardNearRow = cardBottom.y >= rowTop - rowPadTop &&
            cardBottom.y <= rowBottom + (rowPadBottom * 1.35f)
        if (!pointerNearRow && !cardNearRow) {
            return clearWithHold(frame.nowMs, holdMs = 90L)
        }

        val pointerX = pointer?.x ?: cardBottom.x
        val cardX = cardBottom.x
        val selectionX = pointerX.coerceIn(rowLeft, rowRight)
        val cardSelectionX = cardX.coerceIn(rowLeft, rowRight)
        val rowEdgePad = (targetWidth * 0.95f).coerceIn(52f, 120f)
        val withinRowReach = pointerX in (rowLeft - rowEdgePad)..(rowRight + rowEdgePad) ||
            cardX in (rowLeft - rowEdgePad)..(rowRight + rowEdgePad)
        if (!withinRowReach) {
            return clearWithHold(frame.nowMs, holdMs = 90L)
        }

        // Targets sit ~1.1 widths apart, so switching needs a clear lead, not a few pixels.
        val switchBias = (targetWidth * 0.2f).coerceIn(16f, 40f)
        val activeBonus = targetWidth * 0.25f
        val minSwitchIntervalMs = 90L
        val activeExitPad = (targetWidth * 0.85f).coerceIn(48f, 108f)

        val candidates = validRects.mapNotNull { (id, rect) ->
            // "New folder" never catches a near miss: only a finger right on it selects it.
            if (id == NEW_FOLDER_TARGET_ID && (pointer == null || pointerX !in rect.left..rect.right)) {
                return@mapNotNull null
            }
            // Wider (pinned) targets get a proportionally wider catchment.
            val widthWeight = (rect.width / targetWidth).coerceIn(0.75f, 1.5f)
            val selectionDist = abs(selectionX - rect.center.x) / widthWeight
            val cardDist = abs(cardSelectionX - rect.center.x) / widthWeight
            val pointerInside = pointerX >= rect.left - activeExitPad && pointerX <= rect.right + activeExitPad
            val cardInside = cardX >= rect.left - activeExitPad && cardX <= rect.right + activeExitPad
            Candidate(
                id = id,
                rect = rect,
                score = selectionDist + (cardDist * 0.18f) - if (id == activeId) activeBonus else 0f,
                pointerInside = pointerInside,
                cardInside = cardInside
            )
        }
        val best = candidates.minByOrNull { it.score } ?: return clearWithHold(frame.nowMs, holdMs = 90L)
        val current = activeId?.let { id -> candidates.firstOrNull { it.id == id } }

        if (activeId == null) {
            activate(best.id, frame.nowMs)
            return activeId
        }

        if (best.id == activeId) {
            pendingClearSinceMs = 0L
            return activeId
        }

        val shouldKeepCurrent = current != null &&
            (current.pointerInside || current.cardInside) &&
            best.score + switchBias >= current.score
        if (shouldKeepCurrent) {
            pendingClearSinceMs = 0L
            return activeId
        }

        if (frame.nowMs - lastSwitchMs >= minSwitchIntervalMs) {
            activate(best.id, frame.nowMs)
        }
        return activeId
    }

    private fun activate(id: String, nowMs: Long) {
        activeId = id
        lastSwitchMs = nowMs
        pendingClearSinceMs = 0L
    }

    private fun clearNow(): String? {
        activeId = null
        pendingClearSinceMs = 0L
        return null
    }

    private fun clearWithHold(nowMs: Long, holdMs: Long): String? {
        if (activeId == null) {
            pendingClearSinceMs = 0L
            return null
        }
        if (pendingClearSinceMs == 0L) {
            pendingClearSinceMs = nowMs
        }
        if (nowMs - pendingClearSinceMs >= holdMs) {
            return clearNow()
        }
        return activeId
    }

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }

    private data class Candidate(
        val id: String,
        val rect: Rect,
        val score: Float,
        val pointerInside: Boolean,
        val cardInside: Boolean
    )
}
