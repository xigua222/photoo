package com.voicelike.app

import java.util.Locale

// RAW 扩展名唯一来源：扫描查询、总数统计、格式徽章、分析准入共用，
// 覆盖各大厂商主流 RAW（Canon cr2/cr3/crw、Nikon nef/nrw、Sony arw/sr2/srf、
// Fuji raf、Olympus orf、Panasonic rw2、Pentax pef、Samsung srw、
// Sigma x3f、Hasselblad 3fr、PhaseOne iiq、Leica rwl、GoPro gpr、通用 dng/raw）
val RAW_PHOTO_EXTENSIONS = listOf(
    "dng", "cr2", "cr3", "crw", "nef", "nrw", "arw", "sr2", "srf",
    "raf", "orf", "rw2", "pef", "srw", "x3f", "3fr", "iiq", "rwl", "gpr", "raw"
)

fun isRawPhotoFileName(lowercaseName: String): Boolean {
    return RAW_PHOTO_EXTENSIONS.any { lowercaseName.endsWith(".$it") }
}

fun isSpecialPhotoFormat(item: MediaItem): Boolean {
    if (item.type != "photo") return false
    val mime = item.mimeType.lowercase(Locale.getDefault())
    val name = item.displayName.lowercase(Locale.getDefault())
    return mime.contains("gif") ||
        name.endsWith(".gif") ||
        mime.contains("svg") ||
        name.endsWith(".svg") ||
        mime.contains("raw") ||
        isRawPhotoFileName(name)
}

fun shouldAnalyzeAsStandardPhoto(item: MediaItem): Boolean {
    return item.type == "photo" &&
        !item.isLivePhoto &&
        !item.isMotionPhoto &&
        !isSpecialPhotoFormat(item)
}
