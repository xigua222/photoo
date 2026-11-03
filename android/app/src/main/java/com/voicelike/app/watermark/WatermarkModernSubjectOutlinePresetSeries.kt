package com.voicelike.app

internal fun buildWatermarkModernSubjectOutlinePresetSeries(): List<WatermarkModernPresetSeries> {
    val basePresets = listOf(
        WatermarkModernPresetSpec(
            key = "subject_outline_ink_solid",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u58a8\u9ed1\u5b9e\u7ebf",
            enLabel = "Ink Solid",
            thinBorderColorIndex = 8,
            borderImageScaleFactor = 0.9f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 7,
                lineStyle = WatermarkSubjectOutlineLineStyle.Solid,
                widthRatio = 0.0105f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_chalk_white",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u7c89\u7b14\u767d\u7ebf",
            enLabel = "Chalk White",
            thinBorderColorIndex = 7,
            borderImageScaleFactor = 0.89f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 8,
                lineStyle = WatermarkSubjectOutlineLineStyle.Solid,
                widthRatio = 0.013f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_coral_dash",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u73ca\u745a\u865a\u7ebf",
            enLabel = "Coral Dash",
            thinBorderColorIndex = 1,
            borderImageScaleFactor = 0.88f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 0,
                lineStyle = WatermarkSubjectOutlineLineStyle.Dashed,
                widthRatio = 0.0115f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_sky_loop",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u84dd\u7a7a\u56f4\u7ebf",
            enLabel = "Sky Loop",
            thinBorderColorIndex = 8,
            borderCornerMode = BorderCornerMode.InnerRounded,
            borderImageScaleFactor = 0.87f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 5,
                lineStyle = WatermarkSubjectOutlineLineStyle.Solid,
                widthRatio = 0.015f
            )
        )
    )
    val artPresets = listOf(
        WatermarkModernPresetSpec(
            key = "subject_outline_candy_crayon",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u7cd6\u7eb8\u8721\u7b14",
            enLabel = "Candy Crayon",
            thinBorderColorIndex = 7,
            borderImageScaleFactor = 0.88f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 1,
                lineStyle = WatermarkSubjectOutlineLineStyle.Crayon,
                widthRatio = 0.0145f,
                offsetRatio = 0.006f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_chalk_dust",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u7c89\u7b14\u7070\u5c18",
            enLabel = "Chalk Dust",
            thinBorderColorIndex = 8,
            borderImageScaleFactor = 0.88f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 8,
                lineStyle = WatermarkSubjectOutlineLineStyle.Chalk,
                widthRatio = 0.0135f,
                offsetRatio = 0.004f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_pencil_draft",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u624b\u7a3f\u94c5\u7b14",
            enLabel = "Pencil Draft",
            thinBorderColorIndex = 7,
            borderImageScaleFactor = 0.89f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 7,
                lineStyle = WatermarkSubjectOutlineLineStyle.Pencil,
                widthRatio = 0.0105f,
                offsetRatio = 0.003f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_soft_marker",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u67d4\u5149\u9a6c\u514b\u7b14",
            enLabel = "Soft Marker",
            thinBorderColorIndex = 8,
            borderImageScaleFactor = 0.88f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 5,
                lineStyle = WatermarkSubjectOutlineLineStyle.Marker,
                widthRatio = 0.013f,
                offsetRatio = 0.005f
            )
        ),
        WatermarkModernPresetSpec(
            key = "subject_outline_double_doodle",
            template = WatermarkTemplatePreset.ThinWhiteBorder,
            zhLabel = "\u53cc\u7ebf\u6d82\u9e26",
            enLabel = "Double Doodle",
            thinBorderColorIndex = 8,
            borderImageScaleFactor = 0.87f,
            subjectOutlineConfig = WatermarkSubjectOutlineConfig(
                enabled = true,
                colorIndex = 2,
                lineStyle = WatermarkSubjectOutlineLineStyle.DoubleSketch,
                widthRatio = 0.012f,
                offsetRatio = 0.006f
            )
        )
    )
    return listOf(
        WatermarkModernPresetSeries(
            key = "subject_outline",
            zhTitle = "\u4e3b\u4f53\u63cf\u8fb9",
            enTitle = "Subject Outline",
            zhSubtitle = "\u81ea\u52a8\u56f4\u4f4f\u4e3b\u4f53\u8f6e\u5ed3",
            enSubtitle = "Auto outlines around the subject",
            headlineEmoji = "\uD83D\uDD8D\uFE0F",
            headlineZhLabel = "\u4e3b\u4f53\u63cf\u8fb9",
            headlineEnLabel = "Subject Outline",
            generateRenderedPreviews = false,
            presets = basePresets
        ),
        WatermarkModernPresetSeries(
            key = "subject_outline_art",
            zhTitle = "\u827a\u672f\u4e3b\u4f53\u63cf\u8fb9",
            enTitle = "Art Subject Outline",
            zhSubtitle = "\u8721\u7b14\u3001\u7c89\u7b14\u548c\u624b\u7a3f\u8d28\u611f",
            enSubtitle = "Crayon, chalk, and sketch outline textures",
            headlineEmoji = "\uD83D\uDD8D\uFE0F",
            headlineZhLabel = "\u827a\u672f\u63cf\u8fb9",
            headlineEnLabel = "Art Outlines",
            generateRenderedPreviews = true,
            presets = artPresets
        )
    )
}
