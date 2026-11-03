package com.voicelike.app

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class OrganizeRangeRulesTest {

    private fun secondsOf(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month - 1, day, hour, 0, 0)
        return calendar.timeInMillis / 1000L
    }

    private fun item(
        id: Long,
        dateSec: Long = secondsOf(2025, 3, 5),
        type: String = "photo",
        bucket: String = "Camera",
        name: String = "IMG_$id.jpg",
        path: String = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg",
        mime: String = "image/jpeg",
        size: Long = 1000L,
        width: Int = 3000,
        height: Int = 4000,
        live: Boolean = false
    ) = MediaItem(
        id = id,
        uri = FakeUri.INSTANCE,
        type = type,
        dateAdded = dateSec,
        size = size,
        width = width,
        height = height,
        mimeType = mime,
        displayName = name,
        path = path,
        bucketName = bucket,
        isMotionPhoto = live
    )

    @Test
    fun rangeDeckIsTheWholeRangeButPlainDeckIsOneBatch() {
        assertEquals(16, organizeQueueLimit(16, MediaFilterCriteria()))
        assertEquals(ORGANIZE_RANGE_QUEUE_CAP, organizeQueueLimit(16, MediaFilterCriteria(type = OrganizeKind.VIDEO)))
        assertEquals(
            ORGANIZE_RANGE_QUEUE_CAP,
            organizeQueueLimit(16, MediaFilterCriteria(timePreset = OrganizeTimePreset.TODAY))
        )
    }

    @Test
    fun selectedHiddenAlbumStillCounts() {
        val hidden = setOf("WeChat", "Downloads")
        assertEquals(hidden, effectiveHiddenFolders(hidden, MediaFilterCriteria()))
        assertEquals(setOf("Downloads"), effectiveHiddenFolders(hidden, MediaFilterCriteria(folder = "WeChat")))
    }

    @Test
    fun kindsAreTellApart() {
        val photo = item(1)
        val screenshot = item(2, bucket = "Screenshots", path = "/storage/emulated/0/Pictures/Screenshots/a.png")
        val live = item(3, live = true)
        val recording = item(4, type = "video", path = "/storage/emulated/0/Movies/ScreenRecord/r.mp4", bucket = "ScreenRecord")
        val gif = item(5, name = "a.gif", mime = "image/gif")
        val raw = item(6, name = "a.dng", mime = "image/x-adobe-dng")
        val pano = item(7, name = "PANO_20250305.jpg")

        assertTrue(mediaMatchesKind(photo, OrganizeKind.PHOTO))
        assertFalse(mediaMatchesKind(screenshot, OrganizeKind.PHOTO))
        assertTrue(mediaMatchesKind(screenshot, OrganizeKind.SCREENSHOT))
        assertFalse(mediaMatchesKind(live, OrganizeKind.PHOTO))
        assertTrue(mediaMatchesKind(live, OrganizeKind.LIVE))
        assertTrue(mediaMatchesKind(recording, OrganizeKind.VIDEO))
        assertTrue(mediaMatchesKind(recording, OrganizeKind.SCREEN_RECORDING))
        assertTrue(mediaMatchesKind(gif, OrganizeKind.ANIMATED))
        assertTrue(mediaMatchesKind(raw, OrganizeKind.RAW))
        assertTrue(mediaMatchesKind(pano, OrganizeKind.PANORAMA))
        assertFalse(mediaMatchesKind(photo, OrganizeKind.PANORAMA))
    }

    @Test
    fun timePresetsMatchByLocalDay() {
        val today = todayDayValue()
        val year = today / 10000
        val month = (today / 100) % 100
        val day = today % 100
        val todayItem = item(1, dateSec = secondsOf(year, month, day))
        val lastYear = item(2, dateSec = secondsOf(year - 1, month, day))
        val other = item(3, dateSec = secondsOf(year - 1, if (month == 1) 2 else 1, 15))

        val todayRange = MediaFilterCriteria(timePreset = OrganizeTimePreset.TODAY)
        assertTrue(mediaMatchesFilterCriteria(todayItem, todayRange))
        assertFalse(mediaMatchesFilterCriteria(lastYear, todayRange))

        val onThisDay = MediaFilterCriteria(timePreset = OrganizeTimePreset.ON_THIS_DAY)
        assertFalse(mediaMatchesFilterCriteria(todayItem, onThisDay))
        assertTrue(mediaMatchesFilterCriteria(lastYear, onThisDay))
        assertFalse(mediaMatchesFilterCriteria(other, onThisDay))

        val randomDay = MediaFilterCriteria(timePreset = OrganizeTimePreset.RANDOM_DAY, day = dayKeyOf(mediaDayValue(other)))
        assertTrue(mediaMatchesFilterCriteria(other, randomDay))
        assertFalse(mediaMatchesFilterCriteria(todayItem, randomDay))
    }

    @Test
    fun sizeOrderPutsUnknownSizesLast() {
        val media = listOf(item(1, size = 10), item(2, size = 0), item(3, size = 30), item(4, size = 20))
        assertEquals(listOf(3L, 4L, 1L, 2L), orderMediaForQueue(media, "size_desc").map { it.id })
        assertEquals(listOf(1L, 4L, 3L, 2L), orderMediaForQueue(media, "size_asc").map { it.id })
    }

    @Test
    fun coordinatorLoadsWholeRangeAsOneDeck() {
        val media = (1L..40L).map { item(it, type = if (it % 2 == 0L) "video" else "photo") }
        val coordinator = HomeFeedCoordinator()
        val plain = coordinator.rebuildFromAllMedia(media, MediaFilterCriteria(), 16, emptySet())
        assertEquals(16, plain.displayMedia.size)
        val videos = coordinator.rebuildFromAllMedia(media, MediaFilterCriteria(type = OrganizeKind.VIDEO), 16, emptySet())
        assertEquals(20, videos.displayMedia.size)
        assertTrue(videos.displayMedia.all { it.type == "video" })
    }

    @Test
    fun facetsCountRemainingAndKeepHiddenAlbumsApart() {
        val media = listOf(
            item(1, bucket = "Camera"),
            item(2, bucket = "Camera", type = "video"),
            item(3, bucket = "WeChat"),
            item(4, bucket = "Camera")
        )
        val facets = buildOrganizeRangeFacets(
            source = media,
            processedIds = setOf(4L),
            hiddenFolders = setOf("WeChat"),
            prioritizedFolders = emptySet(),
            criteria = MediaFilterCriteria()
        )
        assertEquals(2, facets.total)
        assertEquals(listOf("Camera"), facets.albums.map { it.name })
        assertEquals(2, facets.albums.single().remaining)
        assertEquals(3, facets.albums.single().total)
        assertEquals(listOf("WeChat"), facets.hiddenAlbums.map { it.name })
        assertEquals(1, facets.kindCounts[OrganizeKind.VIDEO])
        assertEquals(1, facets.kindCounts[OrganizeKind.PHOTO])

        val withHiddenSelected = buildOrganizeRangeFacets(
            source = media,
            processedIds = emptySet(),
            hiddenFolders = setOf("WeChat"),
            prioritizedFolders = emptySet(),
            criteria = MediaFilterCriteria(folder = "WeChat")
        )
        assertEquals(1, withHiddenSelected.total)
        assertTrue(withHiddenSelected.albums.any { it.name == "WeChat" })
    }

    @Test
    fun reorganizeLeavesLikedMovedAndPendingDeleteAlone() {
        val media = (1L..5L).map { item(it) }
        val candidates = selectReorganizeCandidates(
            allMedia = media,
            processedIds = setOf(1L, 2L, 3L, 4L),
            pendingTrashIds = setOf(2L),
            likedIds = setOf(3L),
            movedIds = setOf(4L),
            hiddenFolders = emptySet()
        )
        assertEquals(listOf(1L), candidates.map { it.id })
    }
}
