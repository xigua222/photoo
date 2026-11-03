package com.voicelike.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ColorCardStorePersistenceTest {

    @Test
    fun persistAndReloadKeepsSavedPalettes() {
        val tempDir = createTempDirectory("color-card-store-").toFile()
        try {
            val storageFile = File(tempDir, "color_cards_v2.bin")
            val persistence = newPersistence(storageFile)
            val palettes = listOf(
                palette(id = "older", createdAt = 10L, colors = listOf(0xFF112233.toInt())),
                palette(id = "newer", createdAt = 20L, colors = listOf(0xFF445566.toInt(), 0xFF778899.toInt()))
            )

            assertTrue(persistence.persist(palettes))

            val reloaded = newPersistence(storageFile).loadSnapshot()
            assertEquals(ColorCardStoreSnapshotSource.BinaryPrimary, reloaded.source)
            assertEquals(palettes.sortedByDescending { it.createdAt }, reloaded.palettes)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun loadRecoversFromBackupWhenPrimaryIsCorrupted() {
        val tempDir = createTempDirectory("color-card-store-").toFile()
        try {
            val storageFile = File(tempDir, "color_cards_v2.bin")
            val persistence = newPersistence(storageFile)
            val palettes = listOf(
                palette(id = "base", createdAt = 30L, colors = listOf(0xFF010203.toInt()))
            )
            val updated = palettes + palette(id = "second", createdAt = 40L, colors = listOf(0xFF040506.toInt()))

            assertTrue(persistence.persist(palettes))
            assertTrue(persistence.persist(updated))

            storageFile.writeText("{broken", Charsets.UTF_8)

            val recovered = newPersistence(storageFile).loadSnapshot()
            assertEquals(ColorCardStoreSnapshotSource.BinaryRecovered, recovered.source)
            assertEquals(updated.sortedByDescending { it.createdAt }, recovered.palettes)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun loadMigratesLegacyJsonSnapshotWhenBinaryMissing() {
        val tempDir = createTempDirectory("color-card-store-").toFile()
        try {
            val storageFile = File(tempDir, "color_cards_v2.bin")
            val legacyFile = File(tempDir, "color_cards.json")
            val palettes = listOf(
                palette(id = "legacy", createdAt = 50L, colors = listOf(0xFF0A0B0C.toInt()))
            )
            legacyFile.writeText(serializeColorCardStoreSnapshot(palettes), Charsets.UTF_8)

            val recovered = newPersistence(storageFile, legacyFile).loadSnapshot()
            assertEquals(ColorCardStoreSnapshotSource.LegacyFile, recovered.source)
            assertEquals(palettes, recovered.palettes)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun loadMigratesLegacyMirrorSnapshotWhenDiskMissing() {
        val tempDir = createTempDirectory("color-card-store-").toFile()
        try {
            val storageFile = File(tempDir, "color_cards_v2.bin")
            val palettes = listOf(
                palette(id = "mirror", createdAt = 60L, colors = listOf(0xFFABCDEF.toInt()))
            )

            val recovered = newPersistence(storageFile).loadSnapshot(
                legacyMirroredSnapshot = serializeColorCardStoreSnapshot(palettes)
            )
            assertEquals(ColorCardStoreSnapshotSource.LegacyMirror, recovered.source)
            assertEquals(palettes, recovered.palettes)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun newerBinaryEmptySnapshotWinsOverOlderLegacyData() {
        val tempDir = createTempDirectory("color-card-store-").toFile()
        try {
            val storageFile = File(tempDir, "color_cards_v2.bin")
            val legacyFile = File(tempDir, "color_cards.json")
            val persistence = newPersistence(storageFile, legacyFile)
            val staleLegacy = listOf(
                palette(id = "legacy", createdAt = 70L, colors = listOf(0xFF111111.toInt()))
            )

            assertTrue(persistence.persist(staleLegacy))
            Thread.sleep(5L)
            assertTrue(persistence.persist(emptyList()))
            legacyFile.writeText(serializeColorCardStoreSnapshot(staleLegacy), Charsets.UTF_8)

            val loaded = newPersistence(storageFile, legacyFile).loadSnapshot()
            assertEquals(ColorCardStoreSnapshotSource.BinaryPrimary, loaded.source)
            assertTrue(loaded.palettes.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun unreadableSnapshotIsMarkedCorruptedInsteadOfPretendingMissing() {
        val tempDir = createTempDirectory("color-card-store-").toFile()
        try {
            val storageFile = File(tempDir, "color_cards_v2.bin")
            storageFile.writeText("not-a-binary-snapshot", Charsets.UTF_8)

            val loaded = newPersistence(storageFile).loadSnapshot()
            assertEquals(ColorCardStoreSnapshotSource.Corrupted, loaded.source)
            assertTrue(loaded.palettes.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun newPersistence(
        storageFile: File,
        legacyFile: File? = null
    ): ColorCardStorePersistence {
        return ColorCardStorePersistence(
            storageFile = storageFile,
            legacyStorageFile = legacyFile,
            logError = { _, _ -> },
            logInfo = {}
        )
    }

    private fun palette(
        id: String,
        createdAt: Long,
        colors: List<Int>
    ): SavedColorCardPalette {
        return SavedColorCardPalette(
            id = id,
            createdAt = createdAt,
            sourceMediaId = null,
            colors = colors
        )
    }
}
