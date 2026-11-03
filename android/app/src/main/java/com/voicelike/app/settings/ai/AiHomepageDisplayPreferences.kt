package com.voicelike.app

import android.content.Context

private const val AI_HOME_PREFS_NAME = "photoo_prefs"
private const val KEY_LENSY_HOME_VISIBLE = "ai_home_lensy_visible"
private const val KEY_IMAGE_LABELS_HOME_VISIBLE = "ai_home_image_labels_visible"

internal fun isLensySupported(language: AppLanguage): Boolean {
    return language == AppLanguage.SimplifiedChinese
}

internal data class LensyInfoCopy(
    val message: String,
    val closeLabel: String
)

internal fun getLensyInfoCopy(language: AppLanguage): LensyInfoCopy = when (language) {
    AppLanguage.SimplifiedChinese -> LensyInfoCopy(
        message = "Lensy 会在主页卡片上展示轻量 AI 陪伴文案。你可以在设置中关闭它。",
        closeLabel = "关闭"
    )
    AppLanguage.TraditionalChinese -> LensyInfoCopy(
        message = "Lensy 會在首頁卡片上顯示輕量 AI 陪伴文案。你可以在設定中將它關閉。",
        closeLabel = "關閉"
    )
    AppLanguage.Korean -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "닫기"
    )
    AppLanguage.Japanese -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "閉じる"
    )
    AppLanguage.French -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Fermer"
    )
    AppLanguage.German -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Schließen"
    )
    AppLanguage.Italian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Chiudi"
    )
    AppLanguage.Portuguese -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Fechar"
    )
    AppLanguage.Spanish -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Cerrar"
    )
    AppLanguage.Russian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Закрыть"
    )
    AppLanguage.Thai -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "ปิด"
    )
    AppLanguage.Vietnamese -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Đóng"
    )
    AppLanguage.Indonesian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Tutup"
    )
    AppLanguage.Arabic -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "إغلاق"
    )
    AppLanguage.Ukrainian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Закрити"
    )
    AppLanguage.Polish -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Zamknij"
    )
    AppLanguage.Czech -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Zavřít"
    )
    AppLanguage.Dutch -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Sluit"
    )
    AppLanguage.Swedish -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Stäng"
    )
    AppLanguage.Danish -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Luk"
    )
    AppLanguage.Norwegian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Lukk"
    )
    AppLanguage.Finnish -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Sulje"
    )
    AppLanguage.Greek -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Κλείσιμο"
    )
    AppLanguage.Romanian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Închideți"
    )
    AppLanguage.Hungarian -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Bezárás"
    )
    AppLanguage.Turkish -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Kapat"
    )
    AppLanguage.Malay -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Tutup"
    )
    AppLanguage.Hebrew -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "סגירה"
    )
    AppLanguage.English -> LensyInfoCopy(
        message = "Lensy shows lightweight AI companion copy on home cards. You can turn it off in Settings.",
        closeLabel = "Close"
    )
}

// These toggles only control homepage visibility.
// They must not change analysis, persistence, or background processing.
class AiHomepageDisplayPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(AI_HOME_PREFS_NAME, Context.MODE_PRIVATE)

    var lensyEnabled: Boolean
        get() = prefs.getBoolean(KEY_LENSY_HOME_VISIBLE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_LENSY_HOME_VISIBLE, value).apply()
        }

    var imageResultsEnabled: Boolean
        get() = prefs.getBoolean(KEY_IMAGE_LABELS_HOME_VISIBLE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_IMAGE_LABELS_HOME_VISIBLE, value).apply()
        }
}
