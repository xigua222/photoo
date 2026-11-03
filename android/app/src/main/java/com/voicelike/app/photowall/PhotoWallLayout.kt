package com.voicelike.app

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Photo wall layout (iOS PhotoWallLayout). The organize queue, newest first, drops into whichever
// column is currently shortest; column widths follow a repeating pattern so the wall does not read
// as a grid. Units are canvas pixels: one canvas unit is one screen pixel at scale 1.
internal class PhotoWallLayout(
    // Width / height of each item in display orientation; 0 when unknown.
    itemSizes: List<Pair<Int, Int>>,
    viewAspect: Float,
    density: Float,
) {
    val gap: Float = (GAP_DP * density).roundToInt().toFloat()

    var columnX = FloatArray(0)
        private set
    var columnWidth = FloatArray(0)
        private set

    // Items of each column top to bottom (indices into the item list).
    private var columns: Array<MutableList<Int>> = emptyArray()

    // Per item: top within its column, height, column and position in the column. A thrown-away
    // item keeps these so undo can put it back where it was.
    var itemY = FloatArray(0)
        private set
    var itemHeight = FloatArray(0)
        private set
    var itemColumn = IntArray(0)
        private set
    var itemPosition = IntArray(0)
        private set

    // Height of one vertical cycle of each column (every item takes height + gap); the column
    // repeats vertically at this period.
    var columnPeriod = FloatArray(0)
        private set

    // Width of one horizontal cycle (every column takes width + gap).
    var periodWidth = 0f
        private set

    val columnCount: Int get() = columns.size

    fun columnItems(column: Int): List<Int> = columns[column]

    init {
        val count = itemSizes.size
        if (count > 0) {
            val baseWidth = BASE_COLUMN_DP * density
            val meanWidth = baseWidth * WIDTH_PATTERN.average().toFloat()
            val meanHeight = meanWidth * 1.08f
            // One cycle of the wall roughly matches the screen's proportions, so both directions
            // start repeating at about the same time.
            val estimate = sqrt(count * (meanHeight + gap) / (meanWidth + gap) * max(viewAspect, 0.3f))
            val columnTotal = max(1, min(count, estimate.roundToInt()))

            var x = 0f
            columnX = FloatArray(columnTotal)
            columnWidth = FloatArray(columnTotal)
            for (c in 0 until columnTotal) {
                val width = (baseWidth * WIDTH_PATTERN[c % WIDTH_PATTERN.size]).roundToInt().toFloat()
                columnX[c] = x
                columnWidth[c] = width
                x += width + gap
            }
            periodWidth = x
            columns = Array(columnTotal) { mutableListOf() }
            columnPeriod = FloatArray(columnTotal)
            itemY = FloatArray(count)
            itemHeight = FloatArray(count)
            itemColumn = IntArray(count)
            itemPosition = IntArray(count)

            for (index in 0 until count) {
                // Shortest column first: photos taken around the same time form a band across the
                // wall and get older further down.
                var target = 0
                for (c in 1 until columnTotal) {
                    if (columnPeriod[c] < columnPeriod[target] - 0.5f) target = c
                }
                val width = columnWidth[target]
                val (pixelW, pixelH) = itemSizes[index]
                val ratio =
                    if (pixelW > 0 && pixelH > 0) {
                        (pixelH.toFloat() / pixelW.toFloat()).coerceIn(ASPECT_MIN, ASPECT_MAX)
                    } else {
                        1f
                    }
                val height = (width * ratio).roundToInt().toFloat()
                itemY[index] = columnPeriod[target]
                itemHeight[index] = height
                itemColumn[index] = target
                itemPosition[index] = columns[target].size
                columns[target].add(index)
                columnPeriod[target] += height + gap
            }
        }
    }

    // Takes an item out of its column; returns its position there so undo can restore it.
    fun remove(index: Int): Int? {
        val column = itemColumn[index]
        val position = columns[column].indexOf(index)
        if (position < 0) return null
        columns[column].removeAt(position)
        restack(column)
        return position
    }

    fun insert(
        index: Int,
        position: Int,
    ) {
        val column = itemColumn[index]
        columns[column].add(min(position, columns[column].size), index)
        restack(column)
    }

    private fun restack(column: Int) {
        var y = 0f
        columns[column].forEachIndexed { position, index ->
            itemY[index] = y
            itemPosition[index] = position
            y += itemHeight[index] + gap
        }
        columnPeriod[column] = y
    }

    companion object {
        const val GAP_DP = 8f
        const val BASE_COLUMN_DP = 128f
        val WIDTH_PATTERN = floatArrayOf(1.0f, 1.32f, 0.86f, 1.18f, 0.94f, 1.26f, 0.9f)

        // Panoramas never thin into a strip and long screenshots never stand as a pillar.
        const val ASPECT_MIN = 0.66f
        const val ASPECT_MAX = 1.6f
    }
}

// Floor division that rounds negative quotients down too: which cycle a strip or row is in.
internal fun photoWallFloorDiv(
    a: Int,
    b: Int,
): Int {
    val q = a / b
    return if (a % b != 0 && (a < 0) != (b < 0)) q - 1 else q
}
