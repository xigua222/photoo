package com.voicelike.app

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoWallLayoutTest {
    private fun item(
        id: Long,
        dateSec: Long,
        width: Int = 3000,
        height: Int = 4000,
    ) = MediaItem(
        id = id,
        uri = FakeUri.INSTANCE,
        type = "photo",
        dateAdded = dateSec,
        size = 1000L,
        width = width,
        height = height,
        mimeType = "image/jpeg",
        path = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg",
        bucketName = "Camera",
    )

    @Test
    fun everyItemLandsInExactlyOneColumnInOrder() {
        val sizes = List(40) { if (it % 3 == 0) 4000 to 3000 else 3000 to 4000 }
        val layout = PhotoWallLayout(sizes, viewAspect = 0.46f, density = 3f)
        assertTrue(layout.columnCount in 1..40)
        val seen = (0 until layout.columnCount).flatMap { layout.columnItems(it) }
        assertEquals((0 until 40).toSet(), seen.toSet())
        assertEquals(40, seen.size)
        for (column in 0 until layout.columnCount) {
            val items = layout.columnItems(column)
            // Newest first inside a column and tops increase downwards.
            assertEquals(items.sorted(), items)
            for (i in 1 until items.size) assertTrue(layout.itemY[items[i]] > layout.itemY[items[i - 1]])
        }
    }

    @Test
    fun extremeAspectRatiosAreClamped() {
        val layout = PhotoWallLayout(listOf(10000 to 100, 100 to 10000), viewAspect = 1f, density = 1f)
        for (index in 0..1) {
            val width = layout.columnWidth[layout.itemColumn[index]]
            val ratio = layout.itemHeight[index] / width
            assertTrue(ratio >= PhotoWallLayout.ASPECT_MIN - 0.02f && ratio <= PhotoWallLayout.ASPECT_MAX + 0.02f)
        }
    }

    @Test
    fun removeThenInsertRestoresTheColumn() {
        val layout = PhotoWallLayout(List(12) { 3000 to 4000 }, viewAspect = 0.5f, density = 2f)
        val column = layout.itemColumn[5]
        val before = layout.columnItems(column).toList()
        val period = layout.columnPeriod[column]
        val position = layout.remove(5)
        assertEquals(before.indexOf(5), position)
        assertTrue(5 !in layout.columnItems(column))
        assertEquals(period - layout.itemHeight[5] - layout.gap, layout.columnPeriod[column], 0.01f)
        layout.insert(5, position!!)
        assertEquals(before, layout.columnItems(column))
        assertEquals(period, layout.columnPeriod[column], 0.01f)
    }

    @Test
    fun floorDivRoundsNegativeQuotientsDown() {
        assertEquals(0, photoWallFloorDiv(3, 4))
        assertEquals(-1, photoWallFloorDiv(-1, 4))
        assertEquals(-1, photoWallFloorDiv(-4, 4))
        assertEquals(-2, photoWallFloorDiv(-5, 4))
    }

    @Test
    fun previewKeepsTheNewestUnorganizedPhotos() {
        val media = (1L..30L).map { item(it, dateSec = (it * 37) % 31) }
        val processed = setOf(30L, 29L)
        val preview = newestPhotoWallPreview(media, processed, limit = 9)
        val expected = media.filter { it.id !in processed }.sortedByDescending { it.dateAdded }.take(9)
        assertEquals(expected.map { it.dateAdded }, preview.map { it.dateAdded })
    }
}
