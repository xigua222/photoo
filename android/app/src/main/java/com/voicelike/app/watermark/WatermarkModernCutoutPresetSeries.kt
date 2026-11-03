package com.voicelike.app

internal enum class WatermarkCutoutOverlayStyle {
    None,
    CleanLift,
    SoftShadow,
    WarmGallery,
    OffsetLift
}

internal fun buildWatermarkModernCutoutPresetSeries(): List<WatermarkModernPresetSeries> = listOf(
    WatermarkModernPresetSeries(
        key = "cutout",
        zhTitle = "冲破边框",
        enTitle = "Cutout Breakout",
        zhSubtitle = "主体从边框里冲出来",
        enSubtitle = "Subjects stepping outside the frame",
        headlineEmoji = "✨",
        headlineZhLabel = "冲破边框",
        headlineEnLabel = "Cutout Breakout",
        generateRenderedPreviews = false,
        presets = listOf(
            WatermarkModernPresetSpec(
                key = "cutout_clean_breakout",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "留白冲出",
                enLabel = "Clean Breakout",
                thinBorderColorIndex = 3,
                borderImageScaleFactor = 0.86f,
                cutoutOverlayStyle = WatermarkCutoutOverlayStyle.CleanLift,
                cutoutSubjectScaleFactor = 1.18f
            ),
            WatermarkModernPresetSpec(
                key = "cutout_shadow_black",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "黑边投影",
                enLabel = "Shadow Black",
                thinBorderColorIndex = 7,
                borderImageScaleFactor = 0.88f,
                cutoutOverlayStyle = WatermarkCutoutOverlayStyle.SoftShadow,
                cutoutSubjectScaleFactor = 1.2f
            ),
            WatermarkModernPresetSpec(
                key = "cutout_warm_gallery",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "展墙米白",
                enLabel = "Warm Gallery",
                thinBorderColorIndex = 1,
                borderCornerMode = BorderCornerMode.InnerRounded,
                borderImageScaleFactor = 0.84f,
                cutoutOverlayStyle = WatermarkCutoutOverlayStyle.WarmGallery,
                cutoutSubjectScaleFactor = 1.17f
            ),
            WatermarkModernPresetSpec(
                key = "cutout_offset_air",
                template = WatermarkTemplatePreset.ThinWhiteBorder,
                zhLabel = "错位冲出",
                enLabel = "Offset Lift",
                thinBorderColorIndex = 5,
                borderImageScaleFactor = 0.87f,
                cutoutOverlayStyle = WatermarkCutoutOverlayStyle.OffsetLift,
                cutoutSubjectScaleFactor = 1.22f
            )
        )
    )
)
