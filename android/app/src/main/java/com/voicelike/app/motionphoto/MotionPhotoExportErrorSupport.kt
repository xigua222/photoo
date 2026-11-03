package com.voicelike.app.motionphoto

internal class MotionPhotoExportException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

internal fun Throwable.toDynamicPhotoExportToastMessage(label: String): String {
    val reason = resolveDynamicPhotoExportReason()
    return if (reason.isNullOrBlank()) {
        "${label}导出失败"
    } else {
        "${label}导出失败：$reason"
    }
}

private fun Throwable.resolveDynamicPhotoExportReason(): String? {
    generateSequence(this) { it.cause }.forEach { error ->
        when (error) {
            is OutOfMemoryError -> return "内存不足，请关闭其他应用后重试"
            is SecurityException -> return "没有相册访问权限"
        }
        val message = error.message.orEmpty().trim()
        if (message.isBlank()) return@forEach
        val lowered = message.lowercase()
        when {
            "nothing to save in this mode" in lowered -> return "当前模式没有可导出的内容"
            "unable to decode source image" in lowered -> return "无法读取原图"
            "unable to decode source video frame" in lowered -> return "无法读取动态照片视频帧"
            "unable to resolve source video" in lowered -> return "找不到动态照片视频"
            "unable to open gallery output stream" in lowered -> return "系统相册写入失败"
            "unable to open output stream for" in lowered -> return "系统相册写入失败"
            "failed to save motion photo" in lowered -> return "保存到系统相册失败"
            "failed to save vivo live photo" in lowered -> return "保存 Live Photo 失败"
            "failed to save vendor live photo" in lowered -> return "保存动态照片失败"
            "mediastore insert returned null" in lowered -> return "系统相册创建文件失败"
            "unable to open source audio descriptor" in lowered -> return "无法读取原始动态照片音频"
            "rendered video has no video track" in lowered -> return "导出视频轨道异常"
            "source dynamic photo has no audio track" in lowered -> return "原始动态照片没有音频轨道"
            "missing jpeg file" in lowered -> return "封面文件丢失"
            "missing video file" in lowered -> return "视频文件丢失"
        }
        return message.replace('\n', ' ').replace('\r', ' ').take(48)
    }
    return null
}
