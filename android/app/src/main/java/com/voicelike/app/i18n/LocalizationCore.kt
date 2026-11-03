package com.voicelike.app

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    English("en", "English"),
    SimplifiedChinese("zh-SC", "简体中文"),
    TraditionalChinese("zh-TC", "繁体中文"),
    Korean("ko", "한국어"),
    Japanese("ja", "日本語"),
    French("fr", "Français"),
    German("de", "Deutsch"),
    Italian("it", "Italiano"),
    Portuguese("pt", "Português"),
    Spanish("es", "Español"),
    Russian("ru", "Русский"),
    Ukrainian("uk", "Українська"),
    Polish("pl", "Polski"),
    Czech("cs", "Čeština"),
    Dutch("nl", "Nederlands"),
    Swedish("sv", "Svenska"),
    Danish("da", "Dansk"),
    Norwegian("nb", "Norsk"),
    Finnish("fi", "Suomi"),
    Greek("el", "Ελληνικά"),
    Romanian("ro", "Română"),
    Hungarian("hu", "Magyar"),
    Turkish("tr", "Türkçe"),
    Thai("th", "ไทย"),
    Vietnamese("vi", "Tiếng Việt"),
    Indonesian("id", "Bahasa Indonesia"),
    Malay("ms", "Bahasa Melayu"),
    Arabic("ar", "العربية"),
    Hebrew("he", "עברית");

    // Arabic and Hebrew lay the whole app out right to left.
    val isRtl: Boolean get() = this == Arabic || this == Hebrew

    // For dates and month names; numbers stay in Western digits everywhere.
    val locale: Locale get() = when (this) {
        SimplifiedChinese -> Locale.SIMPLIFIED_CHINESE
        TraditionalChinese -> Locale.TRADITIONAL_CHINESE
        else -> Locale.forLanguageTag(code)
    }
}

class LocalizedStrings(
    val commonStrings: CommonStrings,
    val mediaStrings: MediaStrings,
    val uIStrings: UIStrings,
    val reviewStrings: ReviewStrings,
    val featureStrings: FeatureStrings,
    val compressStrings: CompressStrings,
    val privacyStrings: PrivacyStrings,
    val onboardingStrings: OnboardingStrings,
    val filterStrings: FilterStrings,
    val socialStrings: SocialStrings,
    val statsStrings: StatsStrings,
    val folderStrings: FolderStrings
) {
    val undo: String get() = commonStrings.undo
    val clean: String get() = commonStrings.clean
    val cancel: String get() = commonStrings.cancel
    val delete: String get() = commonStrings.delete
    val done: String get() = commonStrings.done
    val back: String get() = commonStrings.back
    val confirm: String get() = commonStrings.confirm
    val exit: String get() = commonStrings.exit
    val create: String get() = commonStrings.create
    val update: String get() = commonStrings.update
    val remove: String get() = commonStrings.remove
    val send: String get() = commonStrings.send
    val share: String get() = commonStrings.share
    val comment: String get() = commonStrings.comment
    val copy: String get() = commonStrings.copy
    val apply: String get() = commonStrings.apply
    val stop: String get() = commonStrings.stop
    val language: String get() = commonStrings.language
    val settings: String get() = commonStrings.settings
    val skip: String get() = commonStrings.skip
    val trash: String get() = commonStrings.trash
    val processed: String get() = commonStrings.processed
    val deleted: String get() = commonStrings.deleted
    val skippedLabel: String get() = commonStrings.skippedLabel
    val savedSpace: String get() = commonStrings.savedSpace
    val permissionsNeed: String get() = commonStrings.permissionsNeed
    val timeSaved: String get() = commonStrings.timeSaved
    val totalProcessed: String get() = commonStrings.totalProcessed
    val ofGallery: String get() = commonStrings.ofGallery
    val savedSpaceRatioFormat: String get() = commonStrings.savedSpaceRatioFormat // Replaces 'of'
    val pressAgainToExit: String get() = commonStrings.pressAgainToExit
    val version: String get() = commonStrings.version
    val copyright: String get() = commonStrings.copyright
    val openSourceLicenses: String? get() = commonStrings.openSourceLicenses
    val rescan: String get() = commonStrings.rescan
    val checkUpdate: String get() = commonStrings.checkUpdate
    val checkingUpdate: String get() = commonStrings.checkingUpdate
    val newVersionAvailable: String get() = commonStrings.newVersionAvailable
    val noUpdateAvailable: String get() = commonStrings.noUpdateAvailable
    val updateCheckFailed: String get() = commonStrings.updateCheckFailed
    val downloading: String get() = commonStrings.downloading
    val downloadFailed: String get() = commonStrings.downloadFailed
    val date: String get() = mediaStrings.date
    val size: String get() = mediaStrings.size
    val resolution: String get() = mediaStrings.resolution
    val filename: String get() = mediaStrings.filename
    val iso: String get() = mediaStrings.iso
    val shutter: String get() = mediaStrings.shutter
    val location: String get() = mediaStrings.location
    val camera: String get() = mediaStrings.camera
    val live: String get() = mediaStrings.live
    val unknownLocation: String get() = mediaStrings.unknownLocation
    val focalLabel: String get() = mediaStrings.focalLabel
    val apertureLabel: String get() = mediaStrings.apertureLabel
    val stats: String get() = uIStrings.stats
    val emptyStateTitle: String get() = uIStrings.emptyStateTitle
    val emptyStateSubtitle: String get() = uIStrings.emptyStateSubtitle
    val emptyStateButton: String get() = uIStrings.emptyStateButton
    val deleteReviewTitle: String get() = reviewStrings.deleteReviewTitle
    val deleteReviewSubtitle: String get() = reviewStrings.deleteReviewSubtitle
    val deleteReviewWarningBody: String get() = reviewStrings.deleteReviewWarningBody
    val deleteReviewWarningLink: String get() = reviewStrings.deleteReviewWarningLink
    val deleteReviewConfirmDelete: String get() = reviewStrings.deleteReviewConfirmDelete
    val deleteReviewRemoveFromQueue: String get() = reviewStrings.deleteReviewRemoveFromQueue
    val deleteReviewRemoveFromQueueMessage: String get() = reviewStrings.deleteReviewRemoveFromQueueMessage
    val deleteModeTitle: String get() = reviewStrings.deleteModeTitle
    val recycleBinModeTitle: String get() = reviewStrings.recycleBinModeTitle
    val recycleBinModeDesc: String get() = reviewStrings.recycleBinModeDesc
    val directDeleteModeTitle: String get() = reviewStrings.directDeleteModeTitle
    val directDeleteModeDesc: String get() = reviewStrings.directDeleteModeDesc
    val monthly: String get() = reviewStrings.monthly
    val monthlyTitle: String get() = reviewStrings.monthlyTitle
    val monthlyDoneTitle: String get() = reviewStrings.monthlyDoneTitle
    val monthlyDoneButton: String get() = reviewStrings.monthlyDoneButton
    val selectMonth: String get() = reviewStrings.selectMonth
    val selectDay: String get() = reviewStrings.selectDay
    val allDays: String get() = reviewStrings.allDays
    val months: List<String> get() = reviewStrings.months
    val similar: String get() = featureStrings.similar
    val noSimilarPhotosFound: String get() = featureStrings.noSimilarPhotosFound
    val scanning: String get() = featureStrings.scanning
    val keepSelected: String get() = featureStrings.keepSelected
    val keepAll: String get() = featureStrings.keepAll
    val deleteAll: String get() = featureStrings.deleteAll
    val selectBest: String get() = featureStrings.selectBest
    val similarPhotosDone: String get() = featureStrings.similarPhotosDone
    val noSimilarPhotos: String get() = featureStrings.noSimilarPhotos
    val similarPhotosSubtitle: String get() = featureStrings.similarPhotosSubtitle
    val smartOrganizeTitle: String get() = featureStrings.smartOrganizeTitle
    val noClustersFound: String get() = featureStrings.noClustersFound
    val moveTo: String get() = featureStrings.moveTo
    val move: String get() = featureStrings.move
    val collapse: String get() = featureStrings.collapse
    val viewAll: String get() = featureStrings.viewAll
    val smartOrganizeSubtitle: String get() = featureStrings.smartOrganizeSubtitle
    val deepOrganizeTitle: String get() = featureStrings.deepOrganizeTitle
    val deepOrganizeScanning: String get() = featureStrings.deepOrganizeScanning
    val deepOrganizeComplete: String get() = featureStrings.deepOrganizeComplete
    val scanPaused: String get() = featureStrings.scanPaused
    val rescanConfirmMessage: String get() = featureStrings.rescanConfirmMessage
    val deepOrganizeScanningDesc: String get() = featureStrings.deepOrganizeScanningDesc
    val scanningWait: String get() = featureStrings.scanningWait
    val scanningDescriptionDetailed: String get() = featureStrings.scanningDescriptionDetailed
    val lowQuality: String get() = featureStrings.lowQuality
    val blur: String get() = featureStrings.blur
    val underexposed: String get() = featureStrings.underexposed
    val overexposed: String get() = featureStrings.overexposed
    val closedEyes: String get() = featureStrings.closedEyes
    val smallFile: String get() = featureStrings.smallFile
    val lowResolution: String get() = featureStrings.lowResolution
    val scanLowQuality: String get() = featureStrings.scanLowQuality
    val noLowQualityFound: String get() = featureStrings.noLowQualityFound
    val lowQualityDone: String get() = featureStrings.lowQualityDone
    val solidColor: String get() = featureStrings.solidColor
    val lowQualitySubtitle: String get() = featureStrings.lowQualitySubtitle
    val lowQualityCountLabel: (Int) -> String get() = featureStrings.lowQualityCountLabel
    val lowQualityFilteredCountLabel: (Int, Int) -> String get() = featureStrings.lowQualityFilteredCountLabel
    val videoCompressTitle: String get() = compressStrings.videoCompressTitle
    val videoCompressSubtitle: String get() = compressStrings.videoCompressSubtitle
    val videoCompressWarningTitle: String get() = compressStrings.videoCompressWarningTitle
    val videoCompressWarningBody: String get() = compressStrings.videoCompressWarningBody
    val videoCompressTargetLabel: String get() = compressStrings.videoCompressTargetLabel
    val videoCompressBitrateLow: String get() = compressStrings.videoCompressBitrateLow
    val videoCompressBitrateBalanced: String get() = compressStrings.videoCompressBitrateBalanced
    val videoCompressBitrateHigh: String get() = compressStrings.videoCompressBitrateHigh
    val videoCompressEstimate: String get() = compressStrings.videoCompressEstimate
    val videoCompressTimes: String get() = compressStrings.videoCompressTimes
    val videoCompressResultTitle: String get() = compressStrings.videoCompressResultTitle
    val videoCompressResultSaved: String get() = compressStrings.videoCompressResultSaved
    val videoCompressResultCount: String get() = compressStrings.videoCompressResultCount
    val videoCompressDeleteOriginal: String get() = compressStrings.videoCompressDeleteOriginal
    val videoCompressKeepOriginal: String get() = compressStrings.videoCompressKeepOriginal
    val videoCompressDeleteConfirmTitle: String get() = compressStrings.videoCompressDeleteConfirmTitle
    val videoCompressDeleteConfirmBody: String get() = compressStrings.videoCompressDeleteConfirmBody
    val videoCompressBefore: String get() = compressStrings.videoCompressBefore
    val videoCompressAfter: String get() = compressStrings.videoCompressAfter
    val videoCompressRatio: String get() = compressStrings.videoCompressRatio
    val videoCompressAction: String get() = compressStrings.videoCompressAction
    val videoCompressing: String get() = compressStrings.videoCompressing
    val videoCompressSelectHint: String get() = compressStrings.videoCompressSelectHint
    val videoCompressError: String get() = compressStrings.videoCompressError
    val videoCompressThresholdLabel: String get() = compressStrings.videoCompressThresholdLabel
    val imageCompressTitle: String get() = compressStrings.imageCompressTitle
    val imageCompressSubtitle: String get() = compressStrings.imageCompressSubtitle
    val imageCompressWarningTitle: String get() = compressStrings.imageCompressWarningTitle
    val imageCompressWarningBody: String get() = compressStrings.imageCompressWarningBody
    val imageCompressThresholdLabel: String get() = compressStrings.imageCompressThresholdLabel
    val imageCompressResultTitle: String get() = compressStrings.imageCompressResultTitle
    val imageCompressResultSaved: String get() = compressStrings.imageCompressResultSaved
    val imageCompressResultCount: String get() = compressStrings.imageCompressResultCount
    val imageCompressDeleteOriginal: String get() = compressStrings.imageCompressDeleteOriginal
    val imageCompressKeepOriginal: String get() = compressStrings.imageCompressKeepOriginal
    val imageCompressBefore: String get() = compressStrings.imageCompressBefore
    val imageCompressAfter: String get() = compressStrings.imageCompressAfter
    val imageCompressRatio: String get() = compressStrings.imageCompressRatio
    val imageCompressAction: String get() = compressStrings.imageCompressAction
    val imageCompressing: String get() = compressStrings.imageCompressing
    val imageCompressSelectHint: String get() = compressStrings.imageCompressSelectHint
    val imageCompressError: String get() = compressStrings.imageCompressError
    val imageCompressOriginalLabel: String get() = compressStrings.imageCompressOriginalLabel
    val imageCompressOriginalSize: String get() = compressStrings.imageCompressOriginalSize
    val imageCompressSelectAll: String get() = compressStrings.imageCompressSelectAll
    val imageCompressSelected: String get() = compressStrings.imageCompressSelected
    val privacyTitle: String get() = privacyStrings.privacyTitle
    val privacyButton: String get() = privacyStrings.privacyButton
    val privacySection1Title: String get() = privacyStrings.privacySection1Title
    val privacySection1Content: String get() = privacyStrings.privacySection1Content
    val privacySection2Title: String get() = privacyStrings.privacySection2Title
    val privacySection2Content: String get() = privacyStrings.privacySection2Content
    val privacySection3Title: String get() = privacyStrings.privacySection3Title
    val privacySection3Content: String get() = privacyStrings.privacySection3Content
    val privacySection4Title: String get() = privacyStrings.privacySection4Title
    val privacySection4Content: String get() = privacyStrings.privacySection4Content
    val privacyPolicy: String get() = privacyStrings.privacyPolicy
    val termsOfService: String get() = privacyStrings.termsOfService
    val agreeToTerms: String get() = privacyStrings.agreeToTerms
    val onboardingWelcomeTitle: String get() = onboardingStrings.onboardingWelcomeTitle
    val onboardingWelcomeSubtitle: String get() = onboardingStrings.onboardingWelcomeSubtitle
    val onboardingLanguageTitle: String get() = onboardingStrings.onboardingLanguageTitle
    val onboardingLanguageSubtitle: String get() = onboardingStrings.onboardingLanguageSubtitle
    val onboardingPermissionTitle: String get() = onboardingStrings.onboardingPermissionTitle
    val onboardingPermissionSubtitle: String get() = onboardingStrings.onboardingPermissionSubtitle
    val onboardingPermissionButton: String get() = onboardingStrings.onboardingPermissionButton
    val onboardingPermissionGranted: String get() = onboardingStrings.onboardingPermissionGranted
    val selectLanguage: String get() = onboardingStrings.selectLanguage
    val manageFilesPermission: String get() = onboardingStrings.manageFilesPermission
    val manageFilesPermissionDesc: String get() = onboardingStrings.manageFilesPermissionDesc
    val onboardingNext: String get() = onboardingStrings.onboardingNext
    val onboardingBack: String get() = onboardingStrings.onboardingBack
    val onboardingStartTitle: String get() = onboardingStrings.onboardingStartTitle
    val onboardingStartSubtitle: String get() = onboardingStrings.onboardingStartSubtitle
    val onboardingStartButton: String get() = onboardingStrings.onboardingStartButton
    val batchSize: String get() = filterStrings.batchSize
    val queueOrder: String get() = filterStrings.queueOrder
    val orderRandom: String get() = filterStrings.orderRandom
    val orderTime: String get() = filterStrings.orderTime
    val releaseKept: String get() = filterStrings.releaseKept
    val releaseKeptDesc: String get() = filterStrings.releaseKeptDesc
    val released: String get() = filterStrings.released
    val releaseDialogTitle: String get() = filterStrings.releaseDialogTitle
    val releaseDialogContent: String get() = filterStrings.releaseDialogContent
    val filterTitle: String get() = filterStrings.filterTitle
    val filterType: String get() = filterStrings.filterType
    val filterFolder: String get() = filterStrings.filterFolder
    val filterAll: String get() = filterStrings.filterAll
    val filterImage: String get() = filterStrings.filterImage
    val filterVideo: String get() = filterStrings.filterVideo
    val filterLive: String get() = filterStrings.filterLive
    val viewTrash: String get() = filterStrings.viewTrash
    val recycleBinTitle: String get() = filterStrings.recycleBinTitle
    val recycleBinRestore: String get() = filterStrings.recycleBinRestore
    val recycleBinEmpty: String get() = filterStrings.recycleBinEmpty
    val recycleBinSelectAll: String get() = filterStrings.recycleBinSelectAll
    val recycleBinUnselectAll: String get() = filterStrings.recycleBinUnselectAll
    val recycleBinDelete: String get() = filterStrings.recycleBinDelete
    val recycleBinDaysLeftFormat: String get() = filterStrings.recycleBinDaysLeftFormat
    val deletePermanentlyTitle: String get() = filterStrings.deletePermanentlyTitle
    val deletePermanentlyContent: String get() = filterStrings.deletePermanentlyContent
    val filterCompletedTitle: String get() = filterStrings.filterCompletedTitle
    val filterCompletedSubtitle: String get() = filterStrings.filterCompletedSubtitle
    val reviewAgain: String get() = filterStrings.reviewAgain
    val changeFilter: String get() = filterStrings.changeFilter
    val filterMonth: String get() = filterStrings.filterMonth
    val filterDay: String get() = filterStrings.filterDay
    val muteVideos: String get() = filterStrings.muteVideos
    val soundEffects: String get() = filterStrings.soundEffects
    val haptics: String get() = filterStrings.haptics
    val autoCheckUpdate: String get() = filterStrings.autoCheckUpdate
    val autoCheckUpdateDesc: String get() = filterStrings.autoCheckUpdateDesc
    val autoCheckUpdateDialogTitle: String get() = filterStrings.autoCheckUpdateDialogTitle
    val autoCheckUpdateDialogContent: String get() = filterStrings.autoCheckUpdateDialogContent
    val swipePreferences: String get() = filterStrings.swipePreferences
    val swipeLeft: String get() = filterStrings.swipeLeft
    val swipeRight: String get() = filterStrings.swipeRight
    val swipeUp: String get() = filterStrings.swipeUp
    val swipeSpeed: String get() = filterStrings.swipeSpeed
    val swipeNone: String get() = filterStrings.swipeNone
    val swipeSave: String get() = filterStrings.swipeSave
    val swipeAssignAllToast: String get() = filterStrings.swipeAssignAllToast
    val comboAnimation: String get() = filterStrings.comboAnimation
    val likes: String get() = socialStrings.likes
    val viewLikes: String get() = socialStrings.viewLikes
    val commentPlaceholder: String get() = socialStrings.commentPlaceholder
    val commentsHeader: String get() = socialStrings.commentsHeader
    val commentUser: String get() = socialStrings.commentUser
    val noLikes: String get() = socialStrings.noLikes
    val removeLikeTitle: String get() = socialStrings.removeLikeTitle
    val removeLikeMessage: String get() = socialStrings.removeLikeMessage
    val removeLike: String get() = socialStrings.removeLike
    val avatar: String get() = socialStrings.avatar
    val avatarBoy: String get() = socialStrings.avatarBoy
    val avatarGirl: String get() = socialStrings.avatarGirl
    val commentUserMe: String get() = socialStrings.commentUserMe
    val deleteCommentTitle: String get() = socialStrings.deleteCommentTitle
    val deleteCommentMessage: String get() = socialStrings.deleteCommentMessage
    val commentCopied: String get() = socialStrings.commentCopied
    val holidayNewYear: String get() = statsStrings.holidayNewYear
    val holidayValentines: String get() = statsStrings.holidayValentines
    val holidayChildrens: String get() = statsStrings.holidayChildrens
    val holidayLaborDay: String get() = statsStrings.holidayLaborDay
    val holidayNationalDay: String get() = statsStrings.holidayNationalDay
    val holidaySpringFestival: String get() = statsStrings.holidaySpringFestival
    val holidayDragonBoat: String get() = statsStrings.holidayDragonBoat
    val holidayMidAutumn: String get() = statsStrings.holidayMidAutumn
    val groupPhotosTitle: String get() = statsStrings.groupPhotosTitle
    val shootingStatsTitle: String get() = statsStrings.shootingStatsTitle
    val shootingStatsYear: String get() = statsStrings.shootingStatsYear
    val shootingStatsMonth: String get() = statsStrings.shootingStatsMonth
    val shootingStatsSummary: String get() = statsStrings.shootingStatsSummary
    val shootingStatsSummaryMonth: String get() = statsStrings.shootingStatsSummaryMonth
    val shootingStatsItems: String get() = statsStrings.shootingStatsItems
    val shootingStatsVideoDuration: String get() = statsStrings.shootingStatsVideoDuration
    val shootingStatsMovies: String get() = statsStrings.shootingStatsMovies
    val shootingStatsShortDrama: String get() = statsStrings.shootingStatsShortDrama
    val shootingStatsTvDrama: String get() = statsStrings.shootingStatsTvDrama
    val shootingStatsScreenshots: String get() = statsStrings.shootingStatsScreenshots
    val shootingStatsSelfies: String get() = statsStrings.shootingStatsSelfies
    val shootingStatsMostActiveDay: String get() = statsStrings.shootingStatsMostActiveDay
    val shootingStatsVideoTitle: String get() = statsStrings.shootingStatsVideoTitle
    val shootingStatsActiveDayTitle: String get() = statsStrings.shootingStatsActiveDayTitle
    val shootingStatsLocationTitle: String get() = statsStrings.shootingStatsLocationTitle
    val shootingStatsLocationPlaces: String get() = statsStrings.shootingStatsLocationPlaces
    val shootingStatsHeatmapTitle: String get() = statsStrings.shootingStatsHeatmapTitle
    val shootingStatsHeatmapLess: String get() = statsStrings.shootingStatsHeatmapLess
    val shootingStatsHeatmapMore: String get() = statsStrings.shootingStatsHeatmapMore
    val newFolder: String get() = folderStrings.newFolder
    val createNewFolder: String get() = folderStrings.createNewFolder
    val folderName: String get() = folderStrings.folderName
    val undoSuccess: String get() = folderStrings.undoSuccess
    val readyForNextBatch: String get() = folderStrings.readyForNextBatch
    val continueOrganizing: String get() = folderStrings.continueOrganizing
    val currentFolder: String get() = folderStrings.currentFolder
}


private val localizedStringsByLanguage: Map<AppLanguage, LocalizedStrings> = mapOf(
    AppLanguage.English to StringsEn,
    AppLanguage.SimplifiedChinese to StringsZh,
    AppLanguage.TraditionalChinese to StringsZhTw,
    AppLanguage.Korean to StringsKo,
    AppLanguage.Japanese to StringsJa,
    AppLanguage.French to StringsFr,
    AppLanguage.German to StringsDe,
    AppLanguage.Italian to StringsIt,
    AppLanguage.Portuguese to StringsPt,
    AppLanguage.Spanish to StringsEs,
    AppLanguage.Russian to StringsRu,
    AppLanguage.Thai to StringsTh,
    AppLanguage.Vietnamese to StringsVi,
    AppLanguage.Indonesian to StringsId,
    AppLanguage.Arabic to StringsAr,
    AppLanguage.Ukrainian to StringsUk,
    AppLanguage.Polish to StringsPl,
    AppLanguage.Czech to StringsCs,
    AppLanguage.Dutch to StringsNl,
    AppLanguage.Swedish to StringsSv,
    AppLanguage.Danish to StringsDa,
    AppLanguage.Norwegian to StringsNb,
    AppLanguage.Finnish to StringsFi,
    AppLanguage.Greek to StringsEl,
    AppLanguage.Romanian to StringsRo,
    AppLanguage.Hungarian to StringsHu,
    AppLanguage.Turkish to StringsTr,
    AppLanguage.Malay to StringsMs,
    AppLanguage.Hebrew to StringsHe
)

fun getStrings(lang: AppLanguage): LocalizedStrings {
    return localizedStringsByLanguage[lang] ?: StringsEn
}

val LocalAppStrings = compositionLocalOf { StringsEn }
val LocalAppLanguage = compositionLocalOf { AppLanguage.English }
val LocalHapticsEnabled = compositionLocalOf { true }
