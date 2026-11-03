package com.voicelike.app

private const val CN_PRIVACY_POLICY_URL = "https://superwatermelon.cn/terms/privacy-policy.html?lang=cn"
private const val CN_USER_AGREEMENT_URL = "https://superwatermelon.cn/terms/user-agreement.html"
private const val OVERSEAS_PRIVACY_POLICY_BASE_URL = "https://docs.photooapp.com/privacy-policy?lang="
private const val OVERSEAS_USER_AGREEMENT_BASE_URL = "https://docs.photooapp.com/user-agreement?lang="

fun privacyPolicyUrlFor(language: AppLanguage): String {
    if (language == AppLanguage.SimplifiedChinese) {
        return CN_PRIVACY_POLICY_URL
    }
    return OVERSEAS_PRIVACY_POLICY_BASE_URL + agreementLangCodeFor(language)
}

fun userAgreementUrlFor(language: AppLanguage): String {
    if (language == AppLanguage.SimplifiedChinese) {
        return CN_USER_AGREEMENT_URL
    }
    return OVERSEAS_USER_AGREEMENT_BASE_URL + agreementLangCodeFor(language)
}

private fun agreementLangCodeFor(language: AppLanguage): String = when (language) {
    AppLanguage.English -> "en"
    AppLanguage.TraditionalChinese -> "tc"
    AppLanguage.Korean -> "ko"
    AppLanguage.Japanese -> "ja"
    AppLanguage.French -> "fr"
    AppLanguage.Italian -> "it"
    AppLanguage.Portuguese -> "pt"
    AppLanguage.Russian -> "ru"
    AppLanguage.Vietnamese -> "vi"
    AppLanguage.Indonesian -> "id"
    AppLanguage.Arabic -> "ar"
    AppLanguage.Thai -> "th"
    AppLanguage.German,
    AppLanguage.Spanish,
    AppLanguage.SimplifiedChinese -> "en"
    // No hosted agreement in these languages yet.
    else -> "en"
}
