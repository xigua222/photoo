package com.voicelike.app

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

private val colorCardStoreSnapshotGson = Gson()

internal enum class ColorCardStoreSnapshotSource {
    Missing,
    BinaryPrimary,
    BinaryRecovered,
    LegacyFile,
    LegacyMirror,
    Corrupted
}

internal data class ColorCardStoreSnapshotLoad(
    val palettes: List<SavedColorCardPalette>,
    val source: ColorCardStoreSnapshotSource,
    val snapshotUpdatedAt: Long
) {
    val requiresRewrite: Boolean
        get() = source == ColorCardStoreSnapshotSource.BinaryRecovered ||
            source == ColorCardStoreSnapshotSource.LegacyFile ||
            source == ColorCardStoreSnapshotSource.LegacyMirror
}

internal class ColorCardStorePersistence(
    private val storageFile: File,
    private val legacyStorageFile: File? = null,
    private val logError: (String, Throwable) -> Unit,
    private val logInfo: (String) -> Unit = {}
) {
    companion object {
        private const val FILE_MAGIC = 0x43435244
        private const val FILE_VERSION = 2
        private const val MAX_STRING_BYTES = 16 * 1024
    }

    private val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
    private val backupFile = File(storageFile.parentFile, "${storageFile.name}.bak")
    private val legacyFiles = buildList {
        legacyStorageFile?.let { file ->
            add(file)
            add(File(file.parentFile, "${file.name}.tmp"))
            add(File(file.parentFile, "${file.name}.bak"))
            add(File(file.parentFile, "${file.nameWithoutExtension}.tmp"))
            add(File(file.parentFile, "${file.nameWithoutExtension}.bak"))
        }
    }.distinctBy { candidate -> candidate.absolutePath }

    fun load(): List<SavedColorCardPalette>? {
        val snapshot = loadSnapshot()
        return when (snapshot.source) {
            ColorCardStoreSnapshotSource.Missing,
            ColorCardStoreSnapshotSource.Corrupted -> null
            else -> snapshot.palettes
        }
    }

    fun loadSnapshot(
        legacyMirroredSnapshot: String? = null
    ): ColorCardStoreSnapshotLoad {
        val candidates = ArrayList<ColorCardStoreSnapshotCandidate>(8)
        var discoveredUnreadableSnapshot = false

        listOf(
            storageFile to ColorCardStoreSnapshotSource.BinaryPrimary,
            tempFile to ColorCardStoreSnapshotSource.BinaryRecovered,
            backupFile to ColorCardStoreSnapshotSource.BinaryRecovered
        ).forEach { (candidateFile, source) ->
            val attempt = loadBinaryCandidate(candidateFile)
            if (!attempt.exists) return@forEach
            if (attempt.snapshot != null) {
                candidates += ColorCardStoreSnapshotCandidate(
                    source = source,
                    palettes = attempt.snapshot.palettes,
                    snapshotUpdatedAt = attempt.snapshot.snapshotUpdatedAt
                )
            } else {
                discoveredUnreadableSnapshot = true
            }
        }

        legacyFiles.forEach { candidateFile ->
            val attempt = loadLegacyFileCandidate(candidateFile)
            if (!attempt.exists) return@forEach
            if (attempt.snapshot != null) {
                candidates += ColorCardStoreSnapshotCandidate(
                    source = ColorCardStoreSnapshotSource.LegacyFile,
                    palettes = attempt.snapshot.palettes,
                    snapshotUpdatedAt = attempt.snapshot.snapshotUpdatedAt
                )
            } else {
                discoveredUnreadableSnapshot = true
            }
        }

        loadLegacyMirrorCandidate(legacyMirroredSnapshot)?.let { snapshot ->
            candidates += ColorCardStoreSnapshotCandidate(
                source = ColorCardStoreSnapshotSource.LegacyMirror,
                palettes = snapshot.palettes,
                snapshotUpdatedAt = snapshot.snapshotUpdatedAt
            )
        }

        val selected = candidates.maxWithOrNull(
            compareBy<ColorCardStoreSnapshotCandidate>(
                ColorCardStoreSnapshotCandidate::snapshotUpdatedAt,
                { candidate -> candidate.palettes.size },
                { candidate -> sourcePriority(candidate.source) }
            )
        )

        return when {
            selected != null -> {
                if (selected.source != ColorCardStoreSnapshotSource.BinaryPrimary) {
                    logInfo(
                        "Selected color-card snapshot from ${selected.source} " +
                            "(count=${selected.palettes.size}, updatedAt=${selected.snapshotUpdatedAt})"
                    )
                }
                ColorCardStoreSnapshotLoad(
                    palettes = selected.palettes,
                    source = selected.source,
                    snapshotUpdatedAt = selected.snapshotUpdatedAt
                )
            }

            discoveredUnreadableSnapshot -> {
                ColorCardStoreSnapshotLoad(
                    palettes = emptyList(),
                    source = ColorCardStoreSnapshotSource.Corrupted,
                    snapshotUpdatedAt = 0L
                )
            }

            else -> {
                ColorCardStoreSnapshotLoad(
                    palettes = emptyList(),
                    source = ColorCardStoreSnapshotSource.Missing,
                    snapshotUpdatedAt = 0L
                )
            }
        }
    }

    fun persist(palettes: List<SavedColorCardPalette>): Boolean {
        return persistInternal(
            palettes = palettes.sortedByDescending(SavedColorCardPalette::createdAt).take(ColorCardStore.MAX_PALETTES),
            snapshotUpdatedAt = System.currentTimeMillis().coerceAtLeast(1L)
        )
    }

    private fun persistInternal(
        palettes: List<SavedColorCardPalette>,
        snapshotUpdatedAt: Long
    ): Boolean {
        return runCatching {
            storageFile.parentFile?.mkdirs()
            writeSynced(
                file = tempFile,
                palettes = palettes,
                snapshotUpdatedAt = snapshotUpdatedAt
            )

            if (storageFile.exists() && !moveReplacing(storageFile, backupFile)) {
                error("Failed to move ${storageFile.name} into ${backupFile.name}")
            }
            if (!moveReplacing(tempFile, storageFile)) {
                if (backupFile.exists()) {
                    copyReplacing(backupFile, storageFile)
                }
                error("Failed to replace ${storageFile.name}")
            }
            if (!copyReplacing(storageFile, backupFile)) {
                logError(
                    "Failed to refresh ${backupFile.name} after saving ${storageFile.name}",
                    IllegalStateException("Backup refresh failed")
                )
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }
            true
        }.onFailure { error ->
            runCatching { tempFile.delete() }
            logError("Failed to persist color cards", error)
        }.getOrDefault(false)
    }

    private fun loadBinaryCandidate(
        file: File
    ): ColorCardStoreSnapshotAttempt {
        if (!file.exists()) return ColorCardStoreSnapshotAttempt(exists = false, snapshot = null)
        val snapshot = runCatching {
            DataInputStream(BufferedInputStream(file.inputStream())).use(::readBinarySnapshot)
        }.onFailure { error ->
            logError("Failed to load saved color cards from ${file.name}", error)
        }.getOrNull()
        return ColorCardStoreSnapshotAttempt(exists = true, snapshot = snapshot)
    }

    private fun loadLegacyFileCandidate(
        file: File
    ): ColorCardStoreSnapshotAttempt {
        if (!file.exists()) return ColorCardStoreSnapshotAttempt(exists = false, snapshot = null)
        val snapshot = runCatching {
            val palettes = parseColorCardStoreSnapshot(file.readText(Charsets.UTF_8))
            PersistedColorCardStoreSnapshot(
                palettes = palettes,
                snapshotUpdatedAt = snapshotUpdatedAtOf(palettes)
            )
        }.onFailure { error ->
            logError("Failed to load legacy saved color cards from ${file.name}", error)
        }.getOrNull()
        return ColorCardStoreSnapshotAttempt(exists = true, snapshot = snapshot)
    }

    private fun loadLegacyMirrorCandidate(
        rawSnapshot: String?
    ): PersistedColorCardStoreSnapshot? {
        val normalized = rawSnapshot?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val palettes = parseColorCardStoreSnapshot(normalized)
            PersistedColorCardStoreSnapshot(
                palettes = palettes,
                snapshotUpdatedAt = snapshotUpdatedAtOf(palettes)
            )
        }.onFailure { error ->
            logError("Failed to load mirrored legacy color-card snapshot", error)
        }.getOrNull()
    }

    private fun readBinarySnapshot(
        input: DataInputStream
    ): PersistedColorCardStoreSnapshot {
        val magic = input.readInt()
        val version = input.readInt()
        val snapshotUpdatedAt = input.readLong()
        val entryCount = input.readInt()
        if (magic != FILE_MAGIC || version != FILE_VERSION) {
            error("Invalid color-card snapshot header")
        }
        if (snapshotUpdatedAt < 0L || entryCount < 0 || entryCount > ColorCardStore.MAX_PALETTES) {
            error("Invalid color-card snapshot metadata")
        }

        val palettes = ArrayList<SavedColorCardPalette>(entryCount)
        repeat(entryCount) {
            palettes += readPalette(input)
        }
        return PersistedColorCardStoreSnapshot(
            palettes = palettes.sortedByDescending(SavedColorCardPalette::createdAt),
            snapshotUpdatedAt = snapshotUpdatedAt
        )
    }

    private fun readPalette(
        input: DataInputStream
    ): SavedColorCardPalette {
        val id = input.readNullableString()?.trim().orEmpty()
        val createdAt = input.readLong()
        val sourceMediaId = if (input.readBoolean()) input.readLong() else null
        val colorCount = input.readInt()
        if (id.isBlank() || createdAt < 0L || colorCount !in ColorCardStore.MIN_PALETTE_COLOR_COUNT..ColorCardStore.MAX_PALETTE_COLOR_COUNT) {
            error("Invalid stored palette entry")
        }
        val colors = ArrayList<Int>(colorCount)
        repeat(colorCount) {
            colors += input.readInt()
        }
        val useChineseFont = input.readBoolean()
        val gradientSpec = if (input.readBoolean()) readGradientSpec(input) else null

        return SavedColorCardPalette(
            id = id,
            createdAt = createdAt,
            sourceMediaId = sourceMediaId,
            colors = colors,
            title = input.readNullableString()?.trim().orEmpty().ifBlank { null },
            sourceTag = input.readNullableString()?.trim().orEmpty().ifBlank { null },
            useChineseFont = useChineseFont,
            favoriteKey = input.readNullableString()?.trim().orEmpty().ifBlank { null },
            gradientSpec = gradientSpec
        )
    }

    private fun readGradientSpec(
        input: DataInputStream
    ): ColorCardGradientSpec {
        val angleDegrees = input.readFloat()
        val stopCount = input.readInt()
        if (stopCount < 2 || stopCount > ColorCardStore.MAX_PALETTE_COLOR_COUNT) {
            error("Invalid stored gradient stop count")
        }
        val stops = ArrayList<ColorCardGradientStop>(stopCount)
        repeat(stopCount) {
            stops += ColorCardGradientStop(
                colorInt = input.readInt(),
                positionPercent = input.readFloat()
            )
        }
        return normalizeColorCardGradientSpec(
            ColorCardGradientSpec(
                angleDegrees = angleDegrees,
                stops = stops
            )
        )
    }

    private fun writeSynced(
        file: File,
        palettes: List<SavedColorCardPalette>,
        snapshotUpdatedAt: Long
    ) {
        FileOutputStream(file).use { fileOutput ->
            BufferedOutputStream(fileOutput).use { bufferedOutput ->
                DataOutputStream(bufferedOutput).use { output ->
                    output.writeInt(FILE_MAGIC)
                    output.writeInt(FILE_VERSION)
                    output.writeLong(snapshotUpdatedAt)
                    output.writeInt(palettes.size)
                    palettes.forEach { palette ->
                        writePalette(output, palette)
                    }
                    output.flush()
                    bufferedOutput.flush()
                    fileOutput.fd.sync()
                }
            }
        }
    }

    private fun writePalette(
        output: DataOutputStream,
        palette: SavedColorCardPalette
    ) {
        output.writeNullableString(palette.id)
        output.writeLong(palette.createdAt.coerceAtLeast(0L))
        output.writeBoolean(palette.sourceMediaId != null)
        palette.sourceMediaId?.let(output::writeLong)
        output.writeInt(palette.colors.size.coerceIn(ColorCardStore.MIN_PALETTE_COLOR_COUNT, ColorCardStore.MAX_PALETTE_COLOR_COUNT))
        palette.colors
            .take(ColorCardStore.MAX_PALETTE_COLOR_COUNT)
            .forEach(output::writeInt)
        output.writeBoolean(palette.useChineseFont)
        output.writeBoolean(palette.gradientSpec != null)
        palette.gradientSpec?.let { gradient ->
            output.writeFloat(gradient.angleDegrees)
            output.writeInt(gradient.stops.size.coerceAtMost(ColorCardStore.MAX_PALETTE_COLOR_COUNT))
            gradient.stops
                .take(ColorCardStore.MAX_PALETTE_COLOR_COUNT)
                .forEach { stop ->
                    output.writeInt(stop.colorInt)
                    output.writeFloat(stop.positionPercent)
                }
        }
        output.writeNullableString(palette.title)
        output.writeNullableString(palette.sourceTag)
        output.writeNullableString(palette.favoriteKey)
    }

    private fun moveReplacing(
        source: File,
        target: File
    ): Boolean {
        if (!source.exists()) return false
        target.parentFile?.mkdirs()
        if ((!target.exists() || target.delete()) && source.renameTo(target)) {
            return true
        }
        return copyReplacing(source, target).also { copied ->
            if (copied) {
                runCatching { source.delete() }
            }
        }
    }

    private fun copyReplacing(
        source: File,
        target: File
    ): Boolean {
        return runCatching {
            target.parentFile?.mkdirs()
            FileOutputStream(target, false).use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
                output.fd.sync()
            }
            true
        }.getOrElse { error ->
            logError("Failed to copy ${source.name} to ${target.name}", error)
            false
        }
    }

    private fun sourcePriority(
        source: ColorCardStoreSnapshotSource
    ): Int {
        return when (source) {
            ColorCardStoreSnapshotSource.BinaryPrimary -> 4
            ColorCardStoreSnapshotSource.BinaryRecovered -> 3
            ColorCardStoreSnapshotSource.LegacyFile -> 2
            ColorCardStoreSnapshotSource.LegacyMirror -> 1
            ColorCardStoreSnapshotSource.Missing -> 0
            ColorCardStoreSnapshotSource.Corrupted -> 0
        }
    }

    private fun snapshotUpdatedAtOf(
        palettes: List<SavedColorCardPalette>
    ): Long {
        return palettes.maxOfOrNull(SavedColorCardPalette::createdAt)?.coerceAtLeast(0L) ?: 0L
    }

    private data class ColorCardStoreSnapshotAttempt(
        val exists: Boolean,
        val snapshot: PersistedColorCardStoreSnapshot?
    )

    private data class PersistedColorCardStoreSnapshot(
        val palettes: List<SavedColorCardPalette>,
        val snapshotUpdatedAt: Long
    )

    private data class ColorCardStoreSnapshotCandidate(
        val source: ColorCardStoreSnapshotSource,
        val palettes: List<SavedColorCardPalette>,
        val snapshotUpdatedAt: Long
    )

    private fun DataOutputStream.writeNullableString(value: String?) {
        if (value == null) {
            writeInt(-1)
            return
        }
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_STRING_BYTES) {
            error("String field is too large for color-card snapshot")
        }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readNullableString(): String? {
        val byteCount = readInt()
        if (byteCount < 0) return null
        if (byteCount > MAX_STRING_BYTES) {
            error("Stored string field is too large")
        }
        val bytes = ByteArray(byteCount)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}

internal fun serializeColorCardStoreSnapshot(
    palettes: List<SavedColorCardPalette>
): String {
    return colorCardStoreSnapshotGson.toJson(palettes)
}

internal fun parseColorCardStoreSnapshot(
    rawJson: String
): List<SavedColorCardPalette> {
    val root = JsonParser.parseString(rawJson)
    if (!root.isJsonArray) {
        error("Color card storage root is not an array")
    }
    val array = root.asJsonArray
    val result = ArrayList<SavedColorCardPalette>(array.size())
    for (element in array) {
        val paletteObject = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue
        parseColorCardStorePalette(paletteObject)?.let(result::add)
    }
    return result
        .sortedByDescending { it.createdAt }
        .take(ColorCardStore.MAX_PALETTES)
}

private fun parseColorCardStorePalette(
    paletteObject: JsonObject
): SavedColorCardPalette? {
    val id = paletteObject.storeField("id", "a").coerceString()?.trim().orEmpty()
    val createdAt = paletteObject.storeField("createdAt", "b").coerceLong() ?: return null
    val colorsArray = paletteObject.storeField("colors", "d")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
    val colors = ArrayList<Int>(ColorCardStore.MAX_PALETTE_COLOR_COUNT)

    for (element in colorsArray) {
        element.coerceColor()?.let(colors::add)
        if (colors.size == ColorCardStore.MAX_PALETTE_COLOR_COUNT) {
            break
        }
    }

    if (id.isBlank() || colors.size < ColorCardStore.MIN_PALETTE_COLOR_COUNT) {
        return null
    }

    return SavedColorCardPalette(
        id = id,
        createdAt = createdAt,
        sourceMediaId = paletteObject.storeField("sourceMediaId", "c").coerceLong(),
        colors = colors,
        title = paletteObject.storeField("title", "e").coerceString()?.trim().orEmpty().ifBlank { null },
        sourceTag = paletteObject.storeField("sourceTag", "f").coerceString()?.trim().orEmpty().ifBlank { null },
        useChineseFont = paletteObject.storeField("useChineseFont", "g").coerceBoolean() ?: false,
        favoriteKey = paletteObject.storeField("favoriteKey", "h").coerceString()?.trim().orEmpty().ifBlank { null },
        gradientSpec = parseColorCardStoreGradientSpec(
            paletteObject.storeField("gradientSpec", "i")?.takeIf { it.isJsonObject }?.asJsonObject
        )
    )
}

private fun parseColorCardStoreGradientSpec(
    gradientObject: JsonObject?
): ColorCardGradientSpec? {
    gradientObject ?: return null
    val angleDegrees = gradientObject.storeField("angleDegrees", "a").coerceFloat()
        ?: gradientObject.get("angle").coerceFloat()
        ?: return null
    val stopsArray = gradientObject.storeField("stops", "b")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
    val stops = ArrayList<ColorCardGradientStop>(stopsArray.size())
    for (element in stopsArray) {
        val stopObject = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue
        val colorInt = stopObject.storeField("colorInt", "a").coerceColor()
            ?: stopObject.get("color").coerceColor()
            ?: stopObject.get("hex").coerceColor()
            ?: continue
        val positionPercent = stopObject.storeField("positionPercent", "b").coerceFloat()
            ?: stopObject.get("position").coerceFloat()
            ?: continue
        stops += ColorCardGradientStop(
            colorInt = colorInt,
            positionPercent = positionPercent
        )
    }
    if (stops.size < 2) return null
    return normalizeColorCardGradientSpec(
        ColorCardGradientSpec(
            angleDegrees = angleDegrees,
            stops = stops
        )
    )
}

// Release builds before the Gson keep rule wrote obfuscated field names (a, b, c...); those
// files are still read so saved palettes survive the update. New writes use the real names.
private fun JsonObject.storeField(name: String, obfuscatedName: String): JsonElement? =
    get(name) ?: get(obfuscatedName)

private fun JsonElement?.coerceString(): String? {
    if (this == null || isJsonNull) return null
    return when {
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isString -> primitive.asString
                primitive.isNumber || primitive.isBoolean -> primitive.toString()
                else -> null
            }
        }
        else -> null
    }
}

private fun JsonElement?.coerceLong(): Long? {
    if (this == null || isJsonNull) return null
    return when {
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isNumber -> primitive.asLong
                primitive.isString -> primitive.asString.trim().toLongOrNull()
                    ?: primitive.asString.trim().toDoubleOrNull()?.toLong()
                else -> null
            }
        }
        else -> null
    }
}

private fun JsonElement?.coerceFloat(): Float? {
    if (this == null || isJsonNull) return null
    return when {
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isNumber -> primitive.asFloat
                primitive.isString -> primitive.asString.trim().toFloatOrNull()
                else -> null
            }
        }
        else -> null
    }
}

private fun JsonElement?.coerceBoolean(): Boolean? {
    if (this == null || isJsonNull) return null
    return when {
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isBoolean -> primitive.asBoolean
                primitive.isNumber -> primitive.asInt != 0
                primitive.isString -> {
                    when (primitive.asString.trim().lowercase()) {
                        "true", "1", "yes" -> true
                        "false", "0", "no" -> false
                        else -> null
                    }
                }
                else -> null
            }
        }
        else -> null
    }
}

private fun JsonElement?.coerceColor(): Int? {
    if (this == null || isJsonNull) return null
    return when {
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isNumber -> primitive.asLong.toInt()
                primitive.isString -> parseStoredColorString(primitive.asString)
                else -> null
            }
        }
        else -> null
    }
}

private fun parseStoredColorString(rawValue: String): Int? {
    val normalized = rawValue.trim()
    if (normalized.isBlank()) return null

    if (!normalized.startsWith("#")) {
        return normalized.toLongOrNull()?.toInt()
            ?: normalized.toDoubleOrNull()?.toLong()?.toInt()
    }

    val hex = normalized.removePrefix("#")
    return when (hex.length) {
        3 -> buildHexColor("FF", "${hex[0]}${hex[0]}", "${hex[1]}${hex[1]}", "${hex[2]}${hex[2]}")
        4 -> buildHexColor("${hex[0]}${hex[0]}", "${hex[1]}${hex[1]}", "${hex[2]}${hex[2]}", "${hex[3]}${hex[3]}")
        6 -> buildHexColor("FF", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6))
        8 -> buildHexColor(hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6), hex.substring(6, 8))
        else -> null
    }
}

private fun buildHexColor(
    alpha: String,
    red: String,
    green: String,
    blue: String
): Int? {
    val combined = alpha + red + green + blue
    return combined.toLongOrNull(radix = 16)?.toInt()
}
