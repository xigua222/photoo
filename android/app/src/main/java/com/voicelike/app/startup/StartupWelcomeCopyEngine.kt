package com.voicelike.app.startup

import com.voicelike.app.AppLanguage
import com.voicelike.app.HolidayUtils
import com.voicelike.app.LocalizedStrings
import com.voicelike.app.MediaItem
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

data class StartupWelcomeCopy(
    val key: String,
    val title: String,
    val subtitle: String,
    val buttonLabel: String,
    val buttonLoadingLabel: String
)

data class StartupGreetingContext(
    val language: AppLanguage,
    val recentPhotoCount14d: Int,
    val consecutiveShootingDays: Int,
    val pendingPhotoCount: Int,
    val processedCount: Int,
    val savedSizeBytes: Long
)

private data class GreetingCandidate(
    val key: String,
    val title: String,
    val subtitle: String
)

fun countRecentPhotos14d(media: List<MediaItem>): Int {
    val nowSec = System.currentTimeMillis() / 1000L
    val windowStart = nowSec - 14L * 24L * 60L * 60L
    return media.count { it.type == "photo" && it.dateAdded >= windowStart }
}

fun countConsecutiveShootingDays(media: List<MediaItem>): Int {
    val photos = media.asSequence().filter { it.type == "photo" }.toList()
    if (photos.isEmpty()) return 0
    val cal = Calendar.getInstance()
    val seenDays = photos.mapNotNull { item ->
        if (item.dateAdded <= 0L) return@mapNotNull null
        cal.timeInMillis = item.dateAdded * 1000L
        Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }.toSet()
    val cursor = Calendar.getInstance()
    var streak = 0
    while (true) {
        val key = Triple(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH), cursor.get(Calendar.DAY_OF_MONTH))
        if (!seenDays.contains(key)) break
        streak++
        cursor.add(Calendar.DAY_OF_MONTH, -1)
    }
    return streak
}

fun buildStartupWelcomeCopy(
    context: StartupGreetingContext,
    variationIndex: Int,
    lastGreetingKey: String?,
    strings: LocalizedStrings
): StartupWelcomeCopy {
    val language = context.language
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val weekday = now.get(Calendar.DAY_OF_WEEK)
    val holidayKey = HolidayUtils.getHoliday(Date(), strings)
    val holidayName = holidayKey?.let { HolidayUtils.getLocalizedHolidayName(it, strings) }

    val candidates = when {
        holidayKey != null -> holidayCandidates(language, holidayKey, holidayName ?: holidayKey)
        context.consecutiveShootingDays >= 3 -> streakCandidates(language, context.consecutiveShootingDays)
        context.processedCount >= 1000 -> processedCandidates(language, context.processedCount)
        context.savedSizeBytes >= 512L * 1024L * 1024L -> savedCandidates(language, formatReadableSize(context.savedSizeBytes))
        context.pendingPhotoCount >= 80 -> pendingCandidates(language, context.pendingPhotoCount)
        context.recentPhotoCount14d > 0 -> recentCandidates(language, context.recentPhotoCount14d)
        weekday == Calendar.MONDAY -> mondayCandidates(language)
        weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY -> weekendCandidates(language)
        else -> timeCandidates(language, hour)
    }
    val chosen = chooseCandidate(candidates, variationIndex, lastGreetingKey)
    return StartupWelcomeCopy(
        key = chosen.key,
        title = chosen.title,
        subtitle = chosen.subtitle,
        buttonLabel = resolveStartupButtonLabel(language),
        buttonLoadingLabel = resolveStartupLoadingLabel(language)
    )
}

private fun resolveStartupButtonLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "开启"
    AppLanguage.Japanese -> "開始"
    AppLanguage.Korean -> "시작"
    AppLanguage.French -> "Démarrer"
    AppLanguage.German -> "Starten"
    AppLanguage.Italian -> "Inizia"
    AppLanguage.Spanish -> "Iniciar"
    AppLanguage.Portuguese -> "Começar"
    AppLanguage.Russian -> "Начать"
    AppLanguage.Thai -> "เริ่มต้น"
    AppLanguage.Vietnamese -> "Bắt đầu"
    AppLanguage.Indonesian -> "Mulai"
    AppLanguage.Arabic -> "ابدأ"
    AppLanguage.Ukrainian -> "Почати"
    AppLanguage.Polish -> "Zaczynamy"
    AppLanguage.Czech -> "Začít"
    AppLanguage.Dutch -> "Begin"
    AppLanguage.Swedish -> "Sätt igång"
    AppLanguage.Danish -> "Start"
    AppLanguage.Norwegian -> "Start"
    AppLanguage.Finnish -> "Aloita"
    AppLanguage.Greek -> "Έναρξη"
    AppLanguage.Romanian -> "Începeți"
    AppLanguage.Hungarian -> "Kezdés"
    AppLanguage.Turkish -> "Başla"
    AppLanguage.Malay -> "Mula"
    AppLanguage.Hebrew -> "התחלה"
    else -> "Start"
}

private fun resolveStartupLoadingLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> "准备中..."
    AppLanguage.Japanese -> "準備中..."
    AppLanguage.Korean -> "준비 중..."
    AppLanguage.French -> "Préparation..."
    AppLanguage.German -> "Vorbereitung..."
    AppLanguage.Italian -> "Preparazione..."
    AppLanguage.Spanish -> "Preparando..."
    AppLanguage.Portuguese -> "Preparando..."
    AppLanguage.Russian -> "Подготовка..."
    AppLanguage.Thai -> "กำลังเตรียม..."
    AppLanguage.Vietnamese -> "Đang chuẩn bị..."
    AppLanguage.Indonesian -> "Menyiapkan..."
    AppLanguage.Arabic -> "جاري التحضير..."
    AppLanguage.Ukrainian -> "Підготовка..."
    AppLanguage.Polish -> "Przygotowywanie..."
    AppLanguage.Czech -> "Příprava..."
    AppLanguage.Dutch -> "Voorbereiden..."
    AppLanguage.Swedish -> "Förbereder..."
    AppLanguage.Danish -> "Forbereder..."
    AppLanguage.Norwegian -> "Forbereder..."
    AppLanguage.Finnish -> "Valmistellaan..."
    AppLanguage.Greek -> "Προετοιμασία..."
    AppLanguage.Romanian -> "Se pregătește..."
    AppLanguage.Hungarian -> "Előkészítés..."
    AppLanguage.Turkish -> "Hazırlanıyor..."
    AppLanguage.Malay -> "Menyediakan..."
    AppLanguage.Hebrew -> "בהכנה..."
    else -> "Preparing..."
}

private fun csPlural(n: Int, one: String, few: String, many: String): String = when {
    n == 1 -> one
    n in 2..4 -> few
    else -> many
}

private fun chooseCandidate(
    options: List<GreetingCandidate>,
    variationIndex: Int,
    lastKey: String?
): GreetingCandidate {
    require(options.isNotEmpty()) { "options must not be empty" }
    var idx = kotlin.math.abs(variationIndex) % options.size
    if (!lastKey.isNullOrBlank() && options.size > 1 && options[idx].key == lastKey) {
        idx = (idx + 1) % options.size
    }
    return options[idx]
}

private fun timeCandidates(language: AppLanguage, hour: Int): List<GreetingCandidate> {
    return when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> timeCandidatesZh(hour)
        AppLanguage.Japanese -> timeCandidatesJa(hour)
        AppLanguage.Korean -> timeCandidatesKo(hour)
        AppLanguage.Ukrainian -> timeCandidatesUk(hour)
        AppLanguage.Polish -> timeCandidatesPl(hour)
        AppLanguage.Czech -> timeCandidatesCs(hour)
        AppLanguage.Dutch -> timeCandidatesNl(hour)
        AppLanguage.Swedish -> timeCandidatesSv(hour)
        AppLanguage.Danish -> timeCandidatesDa(hour)
        AppLanguage.Norwegian -> timeCandidatesNb(hour)
        AppLanguage.Finnish -> timeCandidatesFi(hour)
        AppLanguage.Greek -> timeCandidatesEl(hour)
        AppLanguage.Romanian -> timeCandidatesRo(hour)
        AppLanguage.Hungarian -> timeCandidatesHu(hour)
        AppLanguage.Turkish -> timeCandidatesTr(hour)
        AppLanguage.Malay -> timeCandidatesMs(hour)
        AppLanguage.Hebrew -> timeCandidatesHe(hour)
        else -> timeCandidatesEn(hour)
    }
}

private fun timeCandidatesZh(hour: Int): List<GreetingCandidate> {
    return when {
            hour in 5..8 -> listOf(
                GreetingCandidate("time_morning_1", "\u65e9\u5b89", "\u65e9\u4e0a\u82b1\u4e00\u5206\u949f\uff0c\u5148\u8fc7\u4e00\u5c0f\u7ec4\u7167\u7247\u3002"),
                GreetingCandidate("time_morning_2", "\u65e9\u4e0a\u597d", "\u5f00\u59cb\u524d\u5feb\u901f\u6574\u7406\u4e00\u8f6e\uff0c\u4e00\u5929\u66f4\u6e05\u723d\u3002"),
                GreetingCandidate("time_morning_3", "\u6b22\u8fce\u56de\u6765", "\u4ece\u4e00\u5c0f\u53e0\u5361\u7247\u5f00\u59cb\u5c31\u597d\u3002")
            )
            hour in 9..11 -> listOf(
                GreetingCandidate("time_forenoon_1", "\u4e0a\u5348\u597d", "\u8d81\u73b0\u5728\u5feb\u901f\u8fc7\u4e00\u8f6e\uff0c\u4eca\u5929\u4f1a\u66f4\u8f7b\u677e\u3002"),
                GreetingCandidate("time_forenoon_2", "\u65e9\u4e0a\u597d", "\u5148\u6574\u7406\u4e00\u5c0f\u6279\uff0c\u540e\u9762\u5c31\u4e0d\u79ef\u538b\u4e86\u3002"),
                GreetingCandidate("time_forenoon_3", "\u6b22\u8fce\u56de\u6765", "\u4eca\u5929\u4ece\u8fd9\u4e00\u7ec4\u7167\u7247\u5f00\u59cb\u3002")
            )
            hour in 12..13 -> listOf(
                GreetingCandidate("time_noon_1", "\u4e2d\u5348\u597d", "\u4e2d\u573a\u6574\u7406\u4e00\u6ce2\uff0c\u665a\u4e0a\u5c31\u4e0d\u7d2f\u4e86\u3002"),
                GreetingCandidate("time_noon_2", "\u4e0b\u5348\u597d", "\u73b0\u5728\u5feb\u901f\u8fc7\u4e00\u8f6e\uff0c\u6548\u679c\u5f88\u597d\u3002"),
                GreetingCandidate("time_noon_3", "\u6b22\u8fce\u56de\u6765", "\u62bd\u4e00\u5206\u949f\uff0c\u5148\u6e05\u4e00\u5c0f\u53e0\u3002")
            )
            hour in 14..17 -> listOf(
                GreetingCandidate("time_afternoon_1", "\u4e0b\u5348\u597d", "\u4e0b\u5348\u6b63\u9002\u5408\u505a\u4e00\u6b21\u8f7b\u91cf\u6574\u7406\u3002"),
                GreetingCandidate("time_afternoon_2", "\u6b22\u8fce\u56de\u6765", "\u73b0\u5728\u901f\u901f\u8fc7\u4e00\u7ec4\uff0c\u665a\u4e0a\u4f1a\u8f7b\u677e\u5f88\u591a\u3002"),
                GreetingCandidate("time_afternoon_3", "\u4e0b\u5348\u597d", "\u5148\u5904\u7406\u4e00\u6279\uff0c\u4eca\u5929\u7684\u8282\u594f\u5c31\u7a33\u4e86\u3002")
            )
            hour in 18..22 -> listOf(
                GreetingCandidate("time_evening_1", "\u665a\u4e0a\u597d", "\u7ed9\u4eca\u5929\u7684\u7167\u7247\u505a\u4e2a\u6536\u5c3e\u3002"),
                GreetingCandidate("time_evening_2", "\u6b22\u8fce\u56de\u6765", "\u4eca\u665a\u82b1\u4e00\u5206\u949f\uff0c\u628a\u56de\u5fc6\u7406\u987a\u3002"),
                GreetingCandidate("time_evening_3", "\u665a\u4e0a\u597d", "\u665a\u95f4\u5c0f\u6574\u7406\uff0c\u660e\u5929\u66f4\u4ece\u5bb9\u3002")
            )
            hour == 23 -> listOf(
                GreetingCandidate("time_late_1", "\u591c\u6df1\u4e86", "\u592a\u665a\u4e86\uff0c\u5148\u5904\u7406\u8fd9\u4e00\u5c0f\u7ec4\u5c31\u597d\u3002"),
                GreetingCandidate("time_late_2", "\u665a\u5b89", "\u6574\u7406\u5b8c\u8fd9\u7ec4\u5c31\u4f11\u606f\u5427\u3002"),
                GreetingCandidate("time_late_3", "\u6b22\u8fce\u56de\u6765", "\u591c\u95f4\u4e5f\u522b\u592a\u8f9b\u82e6\uff0c\u8f7b\u8f7b\u8fc7\u4e00\u8f6e\u3002")
            )
            hour in 0..2 -> listOf(
                GreetingCandidate("time_deepnight_1", "\u591c\u6df1\u4e86", "\u8be5\u4f11\u606f\u4e86\uff0c\u5148\u5feb\u901f\u8fc7\u5b8c\u8fd9\u4e00\u7ec4\u3002"),
                GreetingCandidate("time_deepnight_2", "\u8be5\u4f11\u606f\u4e86", "\u5f88\u665a\u4e86\uff0c\u505a\u4e00\u8f6e\u5c31\u6536\u5de5\u3002"),
                GreetingCandidate("time_deepnight_3", "\u6b22\u8fce\u56de\u6765", "\u4eca\u665a\u5c11\u91cf\u6574\u7406\uff0c\u660e\u5929\u518d\u7ee7\u7eed\u3002")
            )
            else -> listOf(
                GreetingCandidate("time_dawn_1", "\u591c\u6df1\u4e86", "\u5f88\u665a\u4e86\uff0c\u8fc7\u5b8c\u8fd9\u4e00\u7ec4\u5c31\u4f11\u606f\u3002"),
                GreetingCandidate("time_dawn_2", "\u591c\u6df1\u4e86", "\u5148\u6574\u7406\u8fd9\u4e00\u8f6e\uff0c\u7136\u540e\u597d\u597d\u4f11\u606f\u3002"),
                GreetingCandidate("time_dawn_3", "\u6b22\u8fce\u56de\u6765", "\u8fd9\u4e2a\u70b9\u4e5f\u8bb0\u5f97\u6574\u7406\uff0c\u771f\u4e0d\u5bb9\u6613\u3002")
            )
        }
}

private fun timeCandidatesEn(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_en_morning_1", "Good morning", "Start with one small stack."),
        GreetingCandidate("time_en_morning_2", "Welcome back", "A quick pass now keeps the day light."),
        GreetingCandidate("time_en_morning_3", "Morning", "One minute now, less clutter later.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_en_afternoon_1", "Good afternoon", "A short cleanup now helps tonight."),
        GreetingCandidate("time_en_afternoon_2", "Welcome back", "Do one quick batch and move on."),
        GreetingCandidate("time_en_afternoon_3", "Afternoon", "Keep your gallery light with one pass.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_en_evening_1", "Good evening", "Wrap up today's captures."),
        GreetingCandidate("time_en_evening_2", "Welcome back", "One minute to tidy today's memories."),
        GreetingCandidate("time_en_evening_3", "Evening", "A short pass tonight helps tomorrow.")
    )
    else -> listOf(
        GreetingCandidate("time_en_late_1", "It's late", "Finish one small set and get some rest."),
        GreetingCandidate("time_en_late_2", "Late night", "Very late now, just one quick pass."),
        GreetingCandidate("time_en_late_3", "Welcome back", "Do a light cleanup and call it a day.")
    )
}

private fun timeCandidatesJa(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_ja_morning_1", "おはようございます", "まず小さなスタックから始めましょう。"),
        GreetingCandidate("time_ja_morning_2", "おかえりなさい", "今すぐ軽く回すと、一日が楽になります。"),
        GreetingCandidate("time_ja_morning_3", "朝", "今1分、少しだけ整理しておきましょう。")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_ja_afternoon_1", "こんにちは", "今少し片付けておくと、夜が楽になります。"),
        GreetingCandidate("time_ja_afternoon_2", "おかえりなさい", "今すぐ1回さっと済ませれば、夜はかなり楽に。"),
        GreetingCandidate("time_ja_afternoon_3", "午後", "先に1バッチ処理して、今日のペースを整えましょう。")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_ja_evening_1", "こんばんは", "今日の写真を締めくくりましょう。"),
        GreetingCandidate("time_ja_evening_2", "おかえりなさい", "今夜1分で、思い出を整理しましょう。"),
        GreetingCandidate("time_ja_evening_3", "夜", "夜の軽い整理で、明日が楽になります。")
    )
    else -> listOf(
        GreetingCandidate("time_ja_late_1", "遅くなりました", "この1組だけ処理して休みましょう。"),
        GreetingCandidate("time_ja_late_2", "おやすみなさい", "これを終えて休憩しましょう。"),
        GreetingCandidate("time_ja_late_3", "おかえりなさい", "夜も無理せず、軽く1回だけ。")
    )
}

private fun timeCandidatesKo(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_ko_morning_1", "좋은 아침입니다", "작은 스택부터 시작하세요."),
        GreetingCandidate("time_ko_morning_2", "돌아오신 것을 환영합니다", "지금 빠르게 돌리면 하루가 가벼워집니다."),
        GreetingCandidate("time_ko_morning_3", "아침", "지금 1분만 투자해 정리해보세요.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_ko_afternoon_1", "좋은 오후입니다", "지금 조금 정리하면 저녁이 편해져요."),
        GreetingCandidate("time_ko_afternoon_2", "돌아오신 것을 환영합니다", "지금 빠르게 한 번 돌리면 저녁이 훨씬 가벼워요."),
        GreetingCandidate("time_ko_afternoon_3", "오후", "먼저 한 배치 처리하고 오늘의 리듬을 잡으세요.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_ko_evening_1", "좋은 저녕입니다", "오늘의 사진을 마무리하세요."),
        GreetingCandidate("time_ko_evening_2", "돌아오신 것을 환영합니다", "오늘 밤 1분 만에 추억을 정리하세요."),
        GreetingCandidate("time_ko_evening_3", "저녁", "저녁에 가볍게 정리하면 내일이 편해져요.")
    )
    else -> listOf(
        GreetingCandidate("time_ko_late_1", "늦었습니다", "이 작은 세트만 처리하고 쉬세요."),
        GreetingCandidate("time_ko_late_2", "안녕히 주무세요", "이걸 끝내고 휴식하세요."),
        GreetingCandidate("time_ko_late_3", "돌아오신 것을 환영합니다", "밤에 무리하지 말고 가볍게 한 바퀴만.")
    )
}

private fun timeCandidatesUk(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_uk_morning_1", "Доброго ранку", "Почніть з однієї невеликої добірки."),
        GreetingCandidate("time_uk_morning_2", "З поверненням", "Швидкий розбір зараз, і день буде легшим."),
        GreetingCandidate("time_uk_morning_3", "Ранок", "Хвилина зараз, менше безладу потім.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_uk_afternoon_1", "Добрий день", "Короткий розбір зараз полегшить вечір."),
        GreetingCandidate("time_uk_afternoon_2", "З поверненням", "Одна швидка партія, і можна далі."),
        GreetingCandidate("time_uk_afternoon_3", "День у розпалі", "Один прохід, і галерея стане легшою.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_uk_evening_1", "Добрий вечір", "Підсумуйте сьогоднішні знімки."),
        GreetingCandidate("time_uk_evening_2", "З поверненням", "Хвилина, щоб упорядкувати спогади дня."),
        GreetingCandidate("time_uk_evening_3", "Вечір", "Спокійний розбір перед відпочинком.")
    )
    else -> listOf(
        GreetingCandidate("time_uk_late_1", "Ще не спите?", "Одна невелика партія, і час відпочивати."),
        GreetingCandidate("time_uk_late_2", "З поверненням", "Тиха хвилина, щоб розібрати кілька фото.")
    )
}

private fun timeCandidatesPl(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_pl_morning_1", "Dzień dobry", "Zacznij od jednej małej partii."),
        GreetingCandidate("time_pl_morning_2", "Witaj ponownie", "Szybki przegląd teraz i dzień będzie lżejszy."),
        GreetingCandidate("time_pl_morning_3", "Miłego poranka", "Minuta teraz to mniej bałaganu później.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_pl_afternoon_1", "Miłego popołudnia", "Krótkie porządki teraz ułatwią wieczór."),
        GreetingCandidate("time_pl_afternoon_2", "Witaj ponownie", "Jedna szybka partia i wracasz do swoich spraw."),
        GreetingCandidate("time_pl_afternoon_3", "Cześć", "Jeden przegląd i galeria znów jest lżejsza.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_pl_evening_1", "Dobry wieczór", "Podsumuj dzisiejsze zdjęcia."),
        GreetingCandidate("time_pl_evening_2", "Witaj ponownie", "Minuta na uporządkowanie dzisiejszych wspomnień."),
        GreetingCandidate("time_pl_evening_3", "Spokojnego wieczoru", "Spokojny przegląd przed odpoczynkiem.")
    )
    else -> listOf(
        GreetingCandidate("time_pl_late_1", "Jeszcze nie śpisz?", "Jedna mała partia, a potem odpocznij."),
        GreetingCandidate("time_pl_late_2", "Witaj ponownie", "Cicha chwila, by uporządkować kilka zdjęć.")
    )
}

private fun timeCandidatesCs(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_cs_morning_1", "Dobré ráno", "Začněte jednou malou dávkou."),
        GreetingCandidate("time_cs_morning_2", "Vítejte zpět", "Rychlé projití teď a celý den bude lehčí."),
        GreetingCandidate("time_cs_morning_3", "Krásné ráno", "Minuta teď, méně nepořádku později.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_cs_afternoon_1", "Dobré odpoledne", "Krátký úklid teď vám uleví večer."),
        GreetingCandidate("time_cs_afternoon_2", "Vítejte zpět", "Jedna rychlá dávka a můžete dál."),
        GreetingCandidate("time_cs_afternoon_3", "Odpolední pauza", "Jedno projití a galerie zůstane lehká.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_cs_evening_1", "Dobrý večer", "Projděte si dnešní snímky."),
        GreetingCandidate("time_cs_evening_2", "Vítejte zpět", "Minutka na úklid dnešních vzpomínek."),
        GreetingCandidate("time_cs_evening_3", "Klidný večer", "Klidné projití, než si odpočinete.")
    )
    else -> listOf(
        GreetingCandidate("time_cs_late_1", "Ještě vzhůru?", "Jedna malá dávka a pak hurá spát."),
        GreetingCandidate("time_cs_late_2", "Vítejte zpět", "Tichá chvíle na úklid pár fotek.")
    )
}

private fun timeCandidatesNl(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_nl_morning_1", "Goedemorgen", "Begin met één klein stapeltje."),
        GreetingCandidate("time_nl_morning_2", "Welkom terug", "Even snel nu, dan blijft je dag licht."),
        GreetingCandidate("time_nl_morning_3", "Morgen!", "Eén minuut nu, minder rommel straks.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_nl_afternoon_1", "Goedemiddag", "Nu even opruimen scheelt vanavond."),
        GreetingCandidate("time_nl_afternoon_2", "Welkom terug", "Doe één snelle reeks en ga weer verder."),
        GreetingCandidate("time_nl_afternoon_3", "Middag!", "Houd je bibliotheek licht met één ronde.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_nl_evening_1", "Goedenavond", "Rond de foto's van vandaag af."),
        GreetingCandidate("time_nl_evening_2", "Welkom terug", "Eén minuut om de herinneringen van vandaag te ordenen."),
        GreetingCandidate("time_nl_evening_3", "Avond!", "Een rustige ronde voordat je gaat slapen.")
    )
    else -> listOf(
        GreetingCandidate("time_nl_late_1", "Nog wakker?", "Eén klein reeksje, en dan lekker slapen."),
        GreetingCandidate("time_nl_late_2", "Welkom terug", "Een stil moment om een paar foto's op te ruimen.")
    )
}

private fun timeCandidatesSv(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_sv_morning_1", "God morgon", "Börja med en liten hög."),
        GreetingCandidate("time_sv_morning_2", "Välkommen tillbaka", "En snabb runda nu gör dagen lättare."),
        GreetingCandidate("time_sv_morning_3", "En ny dag", "En minut nu, mindre röra sen.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_sv_afternoon_1", "God eftermiddag", "En kort städning nu gör kvällen lättare."),
        GreetingCandidate("time_sv_afternoon_2", "Välkommen tillbaka", "Ta en snabb omgång och gå vidare."),
        GreetingCandidate("time_sv_afternoon_3", "Eftermiddagspaus", "Håll galleriet lätt med en runda.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_sv_evening_1", "God kväll", "Gå igenom dagens bilder."),
        GreetingCandidate("time_sv_evening_2", "Välkommen tillbaka", "En minut för att städa bland dagens minnen."),
        GreetingCandidate("time_sv_evening_3", "Kvällsstund", "En lugn runda innan du vilar.")
    )
    else -> listOf(
        GreetingCandidate("time_sv_late_1", "Fortfarande vaken?", "En liten omgång, sen är det dags att sova."),
        GreetingCandidate("time_sv_late_2", "Välkommen tillbaka", "En lugn stund att städa bland några bilder.")
    )
}

private fun timeCandidatesDa(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_da_morning_1", "Godmorgen", "Start med en lille bunke."),
        GreetingCandidate("time_da_morning_2", "Velkommen tilbage", "En hurtig runde nu gør dagen lettere."),
        GreetingCandidate("time_da_morning_3", "Morgen", "Ét minut nu, mindre rod senere.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_da_afternoon_1", "God eftermiddag", "Lidt oprydning nu hjælper i aften."),
        GreetingCandidate("time_da_afternoon_2", "Velkommen tilbage", "Tag en hurtig omgang, og kom videre."),
        GreetingCandidate("time_da_afternoon_3", "Eftermiddag", "Hold dit galleri let med én runde.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_da_evening_1", "Godaften", "Afslut dagens billeder."),
        GreetingCandidate("time_da_evening_2", "Velkommen tilbage", "Ét minut til at rydde op i dagens minder."),
        GreetingCandidate("time_da_evening_3", "Aften", "En rolig runde, før du går til ro.")
    )
    else -> listOf(
        GreetingCandidate("time_da_late_1", "Stadig vågen?", "En lille omgang, og så ind under dynen."),
        GreetingCandidate("time_da_late_2", "Velkommen tilbage", "Et stille øjeblik til at rydde op i et par billeder.")
    )
}

private fun timeCandidatesNb(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_nb_morning_1", "God morgen", "Start med en liten bunke."),
        GreetingCandidate("time_nb_morning_2", "Velkommen tilbake", "En rask runde nå gjør dagen lettere."),
        GreetingCandidate("time_nb_morning_3", "Morn", "Ett minutt nå, mindre rot senere.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_nb_afternoon_1", "God ettermiddag", "En kort opprydding nå hjelper i kveld."),
        GreetingCandidate("time_nb_afternoon_2", "Velkommen tilbake", "Ta én rask runde og gå videre."),
        GreetingCandidate("time_nb_afternoon_3", "Ettermiddagspause", "Hold biblioteket lett med én runde.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_nb_evening_1", "God kveld", "Rund av dagens bilder."),
        GreetingCandidate("time_nb_evening_2", "Velkommen tilbake", "Ett minutt til å rydde i dagens minner."),
        GreetingCandidate("time_nb_evening_3", "Kveldsstund", "En rolig runde før du hviler.")
    )
    else -> listOf(
        GreetingCandidate("time_nb_late_1", "Fortsatt våken?", "En liten bunke, så er det sengetid."),
        GreetingCandidate("time_nb_late_2", "Velkommen tilbake", "Et stille øyeblikk til å rydde noen bilder.")
    )
}

private fun timeCandidatesFi(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_fi_morning_1", "Hyvää huomenta", "Aloita yhdellä pienellä pinolla."),
        GreetingCandidate("time_fi_morning_2", "Tervetuloa takaisin", "Pieni kierros nyt keventää koko päivää."),
        GreetingCandidate("time_fi_morning_3", "Huomenta", "Minuutti nyt, vähemmän sotkua myöhemmin.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_fi_afternoon_1", "Hyvää iltapäivää", "Pieni siivous nyt helpottaa iltaa."),
        GreetingCandidate("time_fi_afternoon_2", "Tervetuloa takaisin", "Käy läpi yksi nopea erä ja jatka päivääsi."),
        GreetingCandidate("time_fi_afternoon_3", "Iltapäivää", "Pidä kuvagalleria kevyenä yhdellä kierroksella.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_fi_evening_1", "Hyvää iltaa", "Käy läpi tämän päivän kuvat."),
        GreetingCandidate("time_fi_evening_2", "Tervetuloa takaisin", "Minuutti päivän muistojen järjestämiseen."),
        GreetingCandidate("time_fi_evening_3", "Iltaa", "Rauhallinen kierros ennen lepoa.")
    )
    else -> listOf(
        GreetingCandidate("time_fi_late_1", "Vielä hereillä?", "Yksi pieni erä, sitten nukkumaan."),
        GreetingCandidate("time_fi_late_2", "Tervetuloa takaisin", "Hiljainen hetki muutaman kuvan järjestämiseen.")
    )
}

private fun timeCandidatesEl(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_el_morning_1", "Καλημέρα", "Ξεκινήστε με μια μικρή στοίβα."),
        GreetingCandidate("time_el_morning_2", "Καλώς ήρθατε ξανά", "Μια γρήγορη ματιά τώρα κάνει τη μέρα πιο ελαφριά."),
        GreetingCandidate("time_el_morning_3", "Καλό πρωί", "Ένα λεπτό τώρα, λιγότερη ακαταστασία αργότερα.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_el_afternoon_1", "Καλό απόγευμα", "Ένας σύντομος καθαρισμός τώρα βοηθά το βράδυ."),
        GreetingCandidate("time_el_afternoon_2", "Καλώς ήρθατε ξανά", "Μία γρήγορη παρτίδα και συνεχίζετε."),
        GreetingCandidate("time_el_afternoon_3", "Καλό μεσημέρι", "Με μία ματιά, η συλλογή σας μένει ελαφριά.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_el_evening_1", "Καλησπέρα", "Κλείστε τις σημερινές λήψεις."),
        GreetingCandidate("time_el_evening_2", "Καλώς ήρθατε ξανά", "Ένα λεπτό για να τακτοποιήσετε τις σημερινές αναμνήσεις."),
        GreetingCandidate("time_el_evening_3", "Καλό βράδυ", "Μια ήρεμη ματιά πριν ξεκουραστείτε.")
    )
    else -> listOf(
        GreetingCandidate("time_el_late_1", "Ακόμη ξύπνιοι;", "Μία μικρή παρτίδα και μετά ξεκούραση."),
        GreetingCandidate("time_el_late_2", "Καλώς ήρθατε ξανά", "Μια ήσυχη στιγμή για να τακτοποιήσετε λίγες φωτογραφίες.")
    )
}

private fun timeCandidatesRo(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_ro_morning_1", "Bună dimineața", "Începeți cu un teanc mic de poze."),
        GreetingCandidate("time_ro_morning_2", "Bine ați revenit", "O trecere rapidă acum ușurează toată ziua."),
        GreetingCandidate("time_ro_morning_3", "Dimineață bună", "Un minut acum, mai puțină dezordine mai târziu.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_ro_afternoon_1", "Bună ziua", "O ordine scurtă acum vă ajută diseară."),
        GreetingCandidate("time_ro_afternoon_2", "Bine ați revenit", "Un lot rapid și treceți mai departe."),
        GreetingCandidate("time_ro_afternoon_3", "După-amiază plăcută", "O singură trecere păstrează galeria ușoară.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_ro_evening_1", "Bună seara", "Încheiați pozele de azi."),
        GreetingCandidate("time_ro_evening_2", "Bine ați revenit", "Un minut pentru a ordona amintirile de azi."),
        GreetingCandidate("time_ro_evening_3", "Seară liniștită", "O trecere calmă înainte de odihnă.")
    )
    else -> listOf(
        GreetingCandidate("time_ro_late_1", "Nu dormiți încă?", "Un lot mic, apoi la culcare."),
        GreetingCandidate("time_ro_late_2", "Bine ați revenit", "Un moment liniștit pentru câteva poze.")
    )
}

private fun timeCandidatesHu(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_hu_morning_1", "Jó reggelt", "Kezdje egy kis csomaggal."),
        GreetingCandidate("time_hu_morning_2", "Üdv újra", "Egy gyors átnézés most könnyebbé teszi a napot."),
        GreetingCandidate("time_hu_morning_3", "Szép reggelt", "Egy perc most, kevesebb rendetlenség később.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_hu_afternoon_1", "Jó napot", "Egy rövid takarítás most jól jön ma estére."),
        GreetingCandidate("time_hu_afternoon_2", "Üdv újra", "Egy gyors csomag, és mehet tovább."),
        GreetingCandidate("time_hu_afternoon_3", "Szép délutánt", "Egy átnézéssel könnyű marad a galéria.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_hu_evening_1", "Jó estét", "Zárja le a mai felvételeket."),
        GreetingCandidate("time_hu_evening_2", "Üdv újra", "Egy perc a mai emlékek rendezésére."),
        GreetingCandidate("time_hu_evening_3", "Szép estét", "Egy nyugodt átnézés pihenés előtt.")
    )
    else -> listOf(
        GreetingCandidate("time_hu_late_1", "Még fent van?", "Egy kis csomag, aztán pihenjen."),
        GreetingCandidate("time_hu_late_2", "Üdv újra", "Csendes pillanat néhány fotó rendezésére.")
    )
}

private fun timeCandidatesTr(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_tr_morning_1", "Günaydın", "Küçük bir yığınla başlayalım."),
        GreetingCandidate("time_tr_morning_2", "Tekrar hoş geldin", "Şimdi kısa bir tur, gün boyu hafiflik."),
        GreetingCandidate("time_tr_morning_3", "Sabah", "Şimdi bir dakika, sonra daha az dağınıklık.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_tr_afternoon_1", "İyi öğlenler", "Şimdi kısa bir temizlik, akşama iyi gelir."),
        GreetingCandidate("time_tr_afternoon_2", "Tekrar hoş geldin", "Hızlı bir tur yap, sonra devam et."),
        GreetingCandidate("time_tr_afternoon_3", "Öğleden sonra", "Tek turla galerin hafif kalsın.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_tr_evening_1", "İyi akşamlar", "Bugünün çekimlerini toparla."),
        GreetingCandidate("time_tr_evening_2", "Tekrar hoş geldin", "Bugünün anılarını düzenlemek için bir dakika."),
        GreetingCandidate("time_tr_evening_3", "Akşam", "Bu akşam kısa bir tur, yarına iyi gelir.")
    )
    else -> listOf(
        GreetingCandidate("time_tr_late_1", "Geç oldu", "Küçük bir grubu bitir, sonra dinlen."),
        GreetingCandidate("time_tr_late_2", "Gece geç saatler", "Çok geç oldu, sadece hızlı bir tur."),
        GreetingCandidate("time_tr_late_3", "Tekrar hoş geldin", "Hafif bir düzenleme yap ve günü kapat.")
    )
}

private fun timeCandidatesMs(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_ms_morning_1", "Selamat pagi", "Mulakan dengan satu kelompok kecil."),
        GreetingCandidate("time_ms_morning_2", "Selamat kembali", "Susun sekejap sekarang, hari anda lebih ringan."),
        GreetingCandidate("time_ms_morning_3", "Pagi yang baik", "Seminit sekarang, kurang bersepah kemudian.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_ms_afternoon_1", "Selamat petang", "Kemas sebentar sekarang, malam nanti lebih lega."),
        GreetingCandidate("time_ms_afternoon_2", "Selamat kembali", "Siapkan satu kelompok pantas, kemudian teruskan hari anda."),
        GreetingCandidate("time_ms_afternoon_3", "Petang yang baik", "Satu pusingan pantas, galeri anda kekal ringan.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_ms_evening_1", "Selamat malam", "Kemaskan foto yang dirakam hari ini."),
        GreetingCandidate("time_ms_evening_2", "Selamat kembali", "Seminit untuk mengemas kenangan hari ini."),
        GreetingCandidate("time_ms_evening_3", "Malam yang tenang", "Satu pusingan santai sebelum anda berehat.")
    )
    else -> listOf(
        GreetingCandidate("time_ms_late_1", "Belum tidur?", "Satu kelompok kecil, kemudian rehatlah."),
        GreetingCandidate("time_ms_late_2", "Selamat kembali", "Masa yang sunyi untuk mengemas beberapa foto.")
    )
}

private fun timeCandidatesHe(hour: Int): List<GreetingCandidate> = when {
    hour in 5..11 -> listOf(
        GreetingCandidate("time_he_morning_1", "בוקר טוב", "מתחילים עם ערימה קטנה אחת."),
        GreetingCandidate("time_he_morning_2", "טוב שחזרת", "מעבר קצר עכשיו, ויום קליל יותר."),
        GreetingCandidate("time_he_morning_3", "בוקר", "דקה אחת עכשיו, פחות בלגן אחר כך.")
    )
    hour in 12..17 -> listOf(
        GreetingCandidate("time_he_afternoon_1", "צהריים טובים", "סידור קצר עכשיו יעזור בערב."),
        GreetingCandidate("time_he_afternoon_2", "טוב שחזרת", "סבב קצר אחד, וממשיכים הלאה."),
        GreetingCandidate("time_he_afternoon_3", "אחר הצהריים", "מעבר אחד, והגלריה נשארת קלילה.")
    )
    hour in 18..22 -> listOf(
        GreetingCandidate("time_he_evening_1", "ערב טוב", "סוגרים את הצילומים של היום."),
        GreetingCandidate("time_he_evening_2", "טוב שחזרת", "דקה אחת לסדר את הזיכרונות של היום."),
        GreetingCandidate("time_he_evening_3", "ערב", "מעבר רגוע לפני המנוחה.")
    )
    else -> listOf(
        GreetingCandidate("time_he_late_1", "עוד לא ישנים?", "סבב קטן אחד, ואז לישון."),
        GreetingCandidate("time_he_late_2", "טוב שחזרת", "רגע שקט לסדר כמה תמונות.")
    )
}

private fun holidayCandidates(language: AppLanguage, holidayKey: String, holidayName: String): List<GreetingCandidate> {
    if (language != AppLanguage.SimplifiedChinese && language != AppLanguage.TraditionalChinese) {
        return when (holidayKey) {
            "New Year's Day" -> listOf(
                GreetingCandidate("h_ny_1", "Happy New Year", "Wishing you a smooth and joyful start."),
                GreetingCandidate("h_ny_2", "Happy New Year", "A fresh year, a fresh gallery.")
            )
            "Dragon Boat Festival" -> listOf(
                GreetingCandidate("h_dbf_1", "Happy Dragon Boat Festival", "Wishing you health and peace."),
                GreetingCandidate("h_dbf_2", "Happy Dragon Boat Festival", "May you stay safe and well.")
            )
            "Mid-Autumn Festival" -> listOf(
                GreetingCandidate("h_maf_1", "Happy Mid-Autumn Festival", "Wishing you reunion and moonlight."),
                GreetingCandidate("h_maf_2", "Happy Mid-Autumn Festival", "May your night be full of warmth.")
            )
            else -> listOf(
                GreetingCandidate("h_other_1", "Happy $holidayName", "Wishing you a wonderful day."),
                GreetingCandidate("h_other_2", "Happy $holidayName", "Hope your day is full of good moments.")
            )
        }
    }
    return when (holidayKey) {
        "New Year's Day" -> listOf(
            GreetingCandidate("h_ny_1", "\u65b0\u5e74\u597d", "\u65b0\u7684\u4e00\u5e74\uff0c\u613f\u4f60\u62cd\u4e0b\u66f4\u591a\u7f8e\u597d\u77ac\u95f4\u3002"),
            GreetingCandidate("h_ny_2", "\u65b0\u5e74\u597d", "\u65b0\u5e74\u65b0\u6c14\u8c61\uff0c\u613f\u6bcf\u5929\u90fd\u6709\u597d\u5fc3\u60c5\u3002")
        )
        "Valentine's Day" -> listOf(
            GreetingCandidate("h_v_1", "\u60c5\u4eba\u8282\u5feb\u4e50", "\u4eca\u5929\u7684\u751c\u871c\u77ac\u95f4\uff0c\u90fd\u503c\u5f97\u88ab\u597d\u597d\u4fdd\u5b58\u3002"),
            GreetingCandidate("h_v_2", "\u60c5\u4eba\u8282\u5feb\u4e50", "\u613f\u4f60\u6536\u5230\u6ee1\u6ee1\u7684\u7231\u4e0e\u6e29\u6696\u3002")
        )
        "Children's Day" -> listOf(
            GreetingCandidate("h_c_1", "\u513f\u7ae5\u8282\u5feb\u4e50", "\u613f\u7ae5\u5fc3\u5e38\u5728\uff0c\u5feb\u4e50\u88ab\u957f\u4e45\u8bb0\u4f4f\u3002"),
            GreetingCandidate("h_c_2", "\u513f\u7ae5\u8282\u5feb\u4e50", "\u613f\u4f60\u4eca\u5929\u4e5f\u50cf\u5b69\u5b50\u4e00\u6837\u5f00\u5fc3\u3002")
        )
        "Labor Day Holiday" -> listOf(
            GreetingCandidate("h_l_1", "\u52b3\u52a8\u8282\u5feb\u4e50", "\u5411\u52aa\u529b\u751f\u6d3b\u7684\u4f60\u81f4\u656c\uff0c\u4e5f\u522b\u5fd8\u8f7b\u677e\u4e00\u4e0b\u3002"),
            GreetingCandidate("h_l_2", "\u52b3\u52a8\u8282\u5feb\u4e50", "\u613f\u4f60\u52b3\u6709\u6240\u5f97\uff0c\u4e5f\u6709\u65f6\u95f4\u597d\u597d\u4f11\u606f\u3002")
        )
        "National Day Holiday" -> listOf(
            GreetingCandidate("h_n_1", "\u56fd\u5e86\u5feb\u4e50", "\u613f\u5047\u671f\u7684\u98ce\u666f\u4e0e\u6b22\u7b11\uff0c\u90fd\u88ab\u597d\u597d\u73cd\u85cf\u3002"),
            GreetingCandidate("h_n_2", "\u56fd\u5e86\u5feb\u4e50", "\u795d\u4f60\u5047\u65e5\u6109\u5feb\uff0c\u53bb\u8bb0\u5f55\u66f4\u591a\u597d\u98ce\u666f\u3002")
        )
        "Spring Festival" -> listOf(
            GreetingCandidate("h_sf_1", "\u65b0\u6625\u5feb\u4e50", "\u613f\u65b0\u5e74\u5e73\u5b89\u559c\u4e50\uff0c\u9616\u5bb6\u5e78\u798f\u56e2\u5706\u3002"),
            GreetingCandidate("h_sf_2", "\u65b0\u6625\u5feb\u4e50", "\u613f\u4f60\u65b0\u5e74\u5927\u5409\uff0c\u4e8b\u4e8b\u987a\u610f\u3002")
        )
        "Dragon Boat Festival" -> listOf(
            GreetingCandidate("h_dbf_1", "\u7aef\u5348\u5b89\u5eb7", "\u613f\u4f60\u5e73\u5b89\u5b89\u5eb7\uff0c\u987a\u5fc3\u987a\u610f\u3002"),
            GreetingCandidate("h_dbf_2", "\u7aef\u5348\u5b89\u5eb7", "\u7cae\u9999\u6ee1\u6ee1\uff0c\u795d\u4f60\u7aef\u5348\u5b89\u5eb7\u3002")
        )
        "Mid-Autumn Festival" -> listOf(
            GreetingCandidate("h_maf_1", "\u4e2d\u79cb\u56e2\u5706", "\u613f\u660e\u6708\u5e38\u5728\uff0c\u4e0e\u5bb6\u4eba\u5171\u4eab\u56e2\u5706\u65f6\u5149\u3002"),
            GreetingCandidate("h_maf_2", "\u4e2d\u79cb\u56e2\u5706", "\u613f\u4eba\u6708\u4e24\u56e2\u5706\uff0c\u5fc3\u60c5\u5982\u6708\u5149\u822c\u660e\u4eae\u3002")
        )
        else -> listOf(
            GreetingCandidate("h_other_1", "${holidayName}\u5feb\u4e50", "\u613f\u4eca\u5929\u6709\u66f4\u591a\u503c\u5f97\u6536\u85cf\u7684\u65f6\u523b\u3002"),
            GreetingCandidate("h_other_2", "${holidayName}\u5feb\u4e50", "\u795d\u4f60\u5ea6\u8fc7\u6109\u5feb\u800c\u96be\u5fd8\u7684\u4e00\u5929\u3002")
        )
    }
}

private fun streakCandidates(language: AppLanguage, days: Int): List<GreetingCandidate> {
    return when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
            GreetingCandidate("streak_1", "\u6b22\u8fce\u56de\u6765", "\u4f60\u5df2\u8fde\u7eed\u62cd\u6444 $days \u5929\uff0c\u7075\u611f\u5728\u7ebf\u3002"),
            GreetingCandidate("streak_2", "\u7ee7\u7eed\u4fdd\u6301", "\u8fde\u7eed $days \u5929\u6709\u65b0\u7167\u7247\uff0c\u8fd9\u80a1\u72b6\u6001\u5f88\u68d2\u3002")
        )
        AppLanguage.Japanese -> listOf(
            GreetingCandidate("streak_ja_1", "\u304a\u304b\u3048\u308a\u306a\u3055\u3044", "${days}\u65e5\u9023\u7d9a\u64ae\u5f71\u4e2d\u3002\u7d20\u6674\u3089\u3057\u3044\uff01"),
            GreetingCandidate("streak_ja_2", "\u7d9a\u7d9a\u3057\u3066", "\u9023\u7d9a${days}\u65e5\u3001\u6bcf\u65e5\u65b0\u3057\u3044\u5199\u771f\u3002\u3053\u306e\u8abf\u5b50\u306f\u7d20\u6674\u3089\u3057\u3044\u3002")
        )
        AppLanguage.Korean -> listOf(
            GreetingCandidate("streak_ko_1", "\ub3cc\uc544\uc628\uac83\uc744\ud658\uc601\ud569\ub2c8\ub2e4", "${days}\uc77c \uc5f0\uc18d \ucc45\uc601 \uc911\uc774\uc5d0\uc694. \ub300\ub2e8\ud574\uc694!"),
            GreetingCandidate("streak_ko_2", "\uacc4\uc18d \uc720\uc9c0", "\uc5f0\uc18d ${days}\uc77c, \ub9e4\uc77c \uc0c8\ub85c\uc6b4 \uc0ac\uc9c4. \uba40\uc84c\uc5b4\uc694!")
        )
        AppLanguage.French -> listOf(
            GreetingCandidate("streak_fr_1", "Bienvenue", "Vous êtes sur une série de $days jours de prise de vue."),
            GreetingCandidate("streak_fr_2", "Continuez", "$days jours consécutifs avec de nouvelles photos. Bravo !")
        )
        AppLanguage.German -> listOf(
            GreetingCandidate("streak_de_1", "Willkommen zurück", "Sie sind seit $days Tagen am Stück am Fotografieren."),
            GreetingCandidate("streak_de_2", "Machen Sie weiter so", "$days Tage in Folge mit neuen Fotos. Super!")
        )
        AppLanguage.Italian -> listOf(
            GreetingCandidate("streak_it_1", "Bentornato", "Sei su una serie di $days giorni di scatto."),
            GreetingCandidate("streak_it_2", "Continua così", "$days giorni di fila con nuove foto. Ottimo!")
        )
        AppLanguage.Spanish -> listOf(
            GreetingCandidate("streak_es_1", "Bienvenido de vuelta", "Llevas $days días seguidos tomando fotos."),
            GreetingCandidate("streak_es_2", "Sigue así", "$days días seguidos con nuevas fotos. ¡Genial!")
        )
        AppLanguage.Portuguese -> listOf(
            GreetingCandidate("streak_pt_1", "Bem-vindo de volta", "Você está em uma sequência de $days dias de fotografia."),
            GreetingCandidate("streak_pt_2", "Continue assim", "$days dias seguidos com novas fotos. Excelente!")
        )
        AppLanguage.Russian -> listOf(
            GreetingCandidate("streak_ru_1", "С возвращением", "Вы фотографируете уже $days дней подряд."),
            GreetingCandidate("streak_ru_2", "Продолжайте в том же духе", "$days дней подряд с новыми фото. Отлично!")
        )
        AppLanguage.Thai -> listOf(
            GreetingCandidate("streak_th_1", "ยินดีต้อนรับกลับมา", "คุณถ่ายภาพติดต่อกัน ${days} วันแล้ว"),
            GreetingCandidate("streak_th_2", "ทำต่อไปเลย", "${days} วันติดต่อกันด้วยรูปภาพใหม่ เยี่ยม!")
        )
        AppLanguage.Vietnamese -> listOf(
            GreetingCandidate("streak_vi_1", "Chào mừng quay lại", "Bạn đã chụp ảnh liên tục $days ngày."),
            GreetingCandidate("streak_vi_2", "Tiếp tục nhé", "$days ngày liên tiếp với ảnh mới. Tuyệt vời!")
        )
        AppLanguage.Indonesian -> listOf(
            GreetingCandidate("streak_id_1", "Selamat datang kembali", "Anda sudah berfoto secara berturut-turut selama $days hari."),
            GreetingCandidate("streak_id_2", "Teruskan", "$days hari berturut-turut dengan foto baru. Hebat!")
        )
        AppLanguage.Arabic -> listOf(
            GreetingCandidate("streak_ar_1", "مرحبًا بعودتك", "أنت تصور منذ $days يوم متتالي."),
            GreetingCandidate("streak_ar_2", "استمر", "$days يوم متتالي مع صور جديدة. ممتاز!")
        )
        AppLanguage.Ukrainian -> listOf(
            GreetingCandidate("streak_uk_1", "З поверненням", "Ви знімаєте вже днів поспіль: ${days}."),
            GreetingCandidate("streak_uk_2", "Так тримати", "Днів поспіль із новими фото: ${days}.")
        )
        AppLanguage.Polish -> listOf(
            GreetingCandidate("streak_pl_1", "Witaj ponownie", "Robisz zdjęcia już ${days} dni z rzędu."),
            GreetingCandidate("streak_pl_2", "Tak trzymaj", "Nowe zdjęcia ${days} dni z rzędu.")
        )
        AppLanguage.Czech -> listOf(
            GreetingCandidate("streak_cs_1", "Vítejte zpět", "Fotíte už ${days} ${csPlural(days, "den", "dny", "dní")} v řadě."),
            GreetingCandidate("streak_cs_2", "Jen tak dál", "${days} ${csPlural(days, "den", "dny", "dní")} v řadě s novými fotkami.")
        )
        AppLanguage.Dutch -> listOf(
            GreetingCandidate("streak_nl_1", "Welkom terug", "Je fotografeert al ${days} dagen op rij."),
            GreetingCandidate("streak_nl_2", "Ga zo door", "${days} dagen op rij nieuwe foto's.")
        )
        AppLanguage.Swedish -> listOf(
            GreetingCandidate("streak_sv_1", "Välkommen tillbaka", "Du har fotat ${days} dagar i rad."),
            GreetingCandidate("streak_sv_2", "Fortsätt så", "${days} dagar i rad med nya bilder.")
        )
        AppLanguage.Danish -> listOf(
            GreetingCandidate("streak_da_1", "Velkommen tilbage", "Du har fotograferet ${days} dage i træk."),
            GreetingCandidate("streak_da_2", "Bliv ved", "${days} dage i træk med nye billeder.")
        )
        AppLanguage.Norwegian -> listOf(
            GreetingCandidate("streak_nb_1", "Velkommen tilbake", "Du har tatt bilder ${days} dager på rad."),
            GreetingCandidate("streak_nb_2", "Stå på", "${days} dager på rad med nye bilder.")
        )
        AppLanguage.Finnish -> listOf(
            GreetingCandidate("streak_fi_1", "Tervetuloa takaisin", "Olet kuvannut ${days} päivää putkeen."),
            GreetingCandidate("streak_fi_2", "Hyvin menee", "${days} päivää peräkkäin uusia kuvia.")
        )
        AppLanguage.Greek -> listOf(
            GreetingCandidate("streak_el_1", "Καλώς ήρθατε ξανά", "Φωτογραφίζετε ${days} ημέρες στη σειρά."),
            GreetingCandidate("streak_el_2", "Συνεχίστε έτσι", "${days} ημέρες στη σειρά με νέες φωτογραφίες.")
        )
        AppLanguage.Romanian -> listOf(
            GreetingCandidate("streak_ro_1", "Bine ați revenit", "Fotografiați de ${days} zile la rând."),
            GreetingCandidate("streak_ro_2", "Continuați tot așa", "${days} zile la rând cu poze noi.")
        )
        AppLanguage.Hungarian -> listOf(
            GreetingCandidate("streak_hu_1", "Üdv újra", "Már ${days} napja fotózik megszakítás nélkül."),
            GreetingCandidate("streak_hu_2", "Így tovább", "${days} egymást követő nap új fotókkal.")
        )
        AppLanguage.Turkish -> listOf(
            GreetingCandidate("streak_tr_1", "Tekrar hoş geldin", "${days} gündür üst üste fotoğraf çekiyorsun."),
            GreetingCandidate("streak_tr_2", "Böyle devam", "${days} gün üst üste yeni fotoğraflar.")
        )
        AppLanguage.Malay -> listOf(
            GreetingCandidate("streak_ms_1", "Selamat kembali", "Anda merakam foto ${days} hari berturut-turut."),
            GreetingCandidate("streak_ms_2", "Teruskan", "${days} hari berturut-turut dengan foto baharu.")
        )
        AppLanguage.Hebrew -> listOf(
            GreetingCandidate("streak_he_1", "טוב שחזרת", "${days} ימים ברצף של צילומים."),
            GreetingCandidate("streak_he_2", "ממשיכים ככה", "${days} ימים ברצף עם תמונות חדשות.")
        )
        else -> listOf(
            GreetingCandidate("streak_en_1", "Welcome back", "You're on a $days-day shooting streak."),
            GreetingCandidate("streak_en_2", "Keep it up", "$days days in a row with new photos.")
        )
    }
}

private fun processedCandidates(language: AppLanguage, count: Int): List<GreetingCandidate> {
    return when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
            GreetingCandidate("processed_1", "\u6b22\u8fce\u56de\u6765", "\u4f60\u5df2\u7d2f\u8ba1\u6574\u7406 $count \u5f20\uff0c\u8282\u594f\u5f88\u7a33\u3002"),
            GreetingCandidate("processed_2", "\u8fdb\u5ea6\u5f88\u68d2", "\u5df2\u7ecf\u6574\u7406 $count \u5f20\u4e86\uff0c\u7ee7\u7eed\u4fdd\u6301\u3002")
        )
        AppLanguage.Japanese -> listOf(
            GreetingCandidate("processed_ja_1", "\u304a\u304b\u3048\u308a\u306a\u3055\u3044", "\u3059\u3067\u306b $count \u679a\u6574\u7406\u6e08\u307f\u3002\u30ea\u30ba\u30e0\u304c\u7d20\u6670\u3067\u3059\u3002"),
            GreetingCandidate("processed_ja_2", "\u9032\u6357\u304c\u7d20\u6670", "$count \u679a\u6574\u7406\u6e08\u307f\u3002\u3053\u306e\u8abf\u5b50\u3092\u4fdd\u3063\u3066\u3002")
        )
        AppLanguage.Korean -> listOf(
            GreetingCandidate("processed_ko_1", "\ub3cc\uc544\uc628\uac83\uc744\ud658\uc601\ud569\ub2c8\ub2e4", "\uc774\ubbaf $count\uc778\uac00 \uc815\ub9ac\ub418\uc5c8\uc5b4\uc694. \uc21c\ud488\ud788 \uac04\ub2c8!"),
            GreetingCandidate("processed_ko_2", "\ucc9c\ucc9c\ud788 \uc9c4\ud589", "\uc774\ubbaf $count\uc778\uac00 \uc815\ub9ac\ub418\uc5c8\uc2b5\ub2c8\ub2e4. \uacc4\uc18d \uc720\uc9c0!")
        )
        AppLanguage.French -> listOf(
            GreetingCandidate("processed_fr_1", "Bienvenue", "Vous avez déjà organisé $count éléments."),
            GreetingCandidate("processed_fr_2", "Bon progrès", "$count éléments déjà organisés.")
        )
        AppLanguage.German -> listOf(
            GreetingCandidate("processed_de_1", "Willkommen zurück", "Sie haben bereits $count Elemente organisiert."),
            GreetingCandidate("processed_de_2", "Großer Fortschritt", "$count Elemente bereits organisiert.")
        )
        AppLanguage.Italian -> listOf(
            GreetingCandidate("processed_it_1", "Bentornato", "Hai già organizzato $count elementi."),
            GreetingCandidate("processed_it_2", "Ottimo progresso", "$count elementi già organizzati.")
        )
        AppLanguage.Spanish -> listOf(
            GreetingCandidate("processed_es_1", "Bienvenido de vuelta", "Ya has organizado $count elementos."),
            GreetingCandidate("processed_es_2", "Gran progreso", "$count elementos ya organizados.")
        )
        AppLanguage.Portuguese -> listOf(
            GreetingCandidate("processed_pt_1", "Bem-vindo de volta", "Você já organizou $count itens."),
            GreetingCandidate("processed_pt_2", "Ótimo progresso", "$count itens já organizados.")
        )
        AppLanguage.Russian -> listOf(
            GreetingCandidate("processed_ru_1", "С возвращением", "Вы уже организовали $count элементов."),
            GreetingCandidate("processed_ru_2", "Отличный прогресс", "$count элементов уже организовано.")
        )
        AppLanguage.Thai -> listOf(
            GreetingCandidate("processed_th_1", "ยินดีต้อนรับกลับมา", "คุณจัดระเบียบ ${count} รายการแล้ว"),
            GreetingCandidate("processed_th_2", "ความก้าวหน้าที่ดี", "${count} รายการจัดระเบียบแล้ว")
        )
        AppLanguage.Vietnamese -> listOf(
            GreetingCandidate("processed_vi_1", "Chào mừng quay lại", "Bạn đã sắp xếp $count mục."),
            GreetingCandidate("processed_vi_2", "Tiến triển tốt", "$count mục đã được sắp xếp.")
        )
        AppLanguage.Indonesian -> listOf(
            GreetingCandidate("processed_id_1", "Selamat datang kembali", "Anda sudah mengatur $count item."),
            GreetingCandidate("processed_id_2", "Kemajuan bagus", "$count item sudah diatur.")
        )
        AppLanguage.Arabic -> listOf(
            GreetingCandidate("processed_ar_1", "مرحبًا بعودتك", "لقد نظمت بالفعل $count عنصر."),
            GreetingCandidate("processed_ar_2", "تقدم ممتاز", "$count عنصر منظم بالفعل.")
        )
        AppLanguage.Ukrainian -> listOf(
            GreetingCandidate("processed_uk_1", "З поверненням", "Ви вже розібрали ${count} фото."),
            GreetingCandidate("processed_uk_2", "Чудовий прогрес", "Уже розібрано: ${count} фото.")
        )
        AppLanguage.Polish -> listOf(
            GreetingCandidate("processed_pl_1", "Witaj ponownie", "Uporządkowane elementy: ${count}."),
            GreetingCandidate("processed_pl_2", "Świetne postępy", "Uporządkowano już: ${count}.")
        )
        AppLanguage.Czech -> listOf(
            GreetingCandidate("processed_cs_1", "Vítejte zpět", "Máte za sebou ${count} ${csPlural(count, "roztříděnou položku", "roztříděné položky", "roztříděných položek")}."),
            GreetingCandidate("processed_cs_2", "Skvělý pokrok", "Už ${count} ${csPlural(count, "roztříděná položka", "roztříděné položky", "roztříděných položek")}.")
        )
        AppLanguage.Dutch -> listOf(
            GreetingCandidate("processed_nl_1", "Welkom terug", "Je hebt al ${count} onderdelen geordend."),
            GreetingCandidate("processed_nl_2", "Mooi bezig", "Al ${count} onderdelen geordend.")
        )
        AppLanguage.Swedish -> listOf(
            GreetingCandidate("processed_sv_1", "Välkommen tillbaka", "Du har redan sorterat ${count} objekt."),
            GreetingCandidate("processed_sv_2", "Bra jobbat", "${count} objekt sorterade redan.")
        )
        AppLanguage.Danish -> listOf(
            GreetingCandidate("processed_da_1", "Velkommen tilbage", "Du har allerede sorteret ${count} emner."),
            GreetingCandidate("processed_da_2", "Flot fremskridt", "${count} emner er allerede sorteret.")
        )
        AppLanguage.Norwegian -> listOf(
            GreetingCandidate("processed_nb_1", "Velkommen tilbake", "Du har allerede ryddet ${count} bilder."),
            GreetingCandidate("processed_nb_2", "Godt jobbet", "${count} bilder er allerede ryddet.")
        )
        AppLanguage.Finnish -> listOf(
            GreetingCandidate("processed_fi_1", "Tervetuloa takaisin", "Olet jo järjestänyt ${count} kohdetta."),
            GreetingCandidate("processed_fi_2", "Hienoa edistystä", "${count} kohdetta jo järjestetty.")
        )
        AppLanguage.Greek -> listOf(
            GreetingCandidate("processed_el_1", "Καλώς ήρθατε ξανά", "Έχετε ήδη ταξινομήσει ${count} στοιχεία."),
            GreetingCandidate("processed_el_2", "Εξαιρετική πρόοδος", "${count} στοιχεία ταξινομήθηκαν ήδη.")
        )
        AppLanguage.Romanian -> listOf(
            GreetingCandidate("processed_ro_1", "Bine ați revenit", "Ați organizat deja ${count} elemente."),
            GreetingCandidate("processed_ro_2", "Progres frumos", "${count} elemente organizate deja.")
        )
        AppLanguage.Hungarian -> listOf(
            GreetingCandidate("processed_hu_1", "Üdv újra", "Már ${count} elemet rendezett."),
            GreetingCandidate("processed_hu_2", "Szép haladás", "Már ${count} elem rendezve.")
        )
        AppLanguage.Turkish -> listOf(
            GreetingCandidate("processed_tr_1", "Tekrar hoş geldin", "Şimdiden ${count} öğeyi düzenledin."),
            GreetingCandidate("processed_tr_2", "Harika ilerleme", "${count} öğe zaten düzenlendi.")
        )
        AppLanguage.Malay -> listOf(
            GreetingCandidate("processed_ms_1", "Selamat kembali", "Anda sudah menyusun ${count} item."),
            GreetingCandidate("processed_ms_2", "Kemajuan hebat", "${count} item sudah disusun.")
        )
        AppLanguage.Hebrew -> listOf(
            GreetingCandidate("processed_he_1", "טוב שחזרת", "כבר סידרת ${count} פריטים."),
            GreetingCandidate("processed_he_2", "התקדמות יפה", "${count} פריטים כבר מסודרים.")
        )
        else -> listOf(
            GreetingCandidate("processed_en_1", "Welcome back", "You've already organized $count items."),
            GreetingCandidate("processed_en_2", "Great progress", "$count items organized already.")
        )
    }
}

private fun savedCandidates(language: AppLanguage, saved: String): List<GreetingCandidate> {
    return when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
            GreetingCandidate("saved_1", "\u6b22\u8fce\u56de\u6765", "\u4f60\u5df2\u7701\u4e0b $saved \u7a7a\u95f4\uff0c\u518d\u6765\u4e00\u8f6e\u66f4\u6e05\u723d\u3002"),
            GreetingCandidate("saved_2", "\u66f4\u8f7b\u4e86", "\u76ee\u524d\u5df2\u7ecf\u8282\u7701 $saved\uff0c\u7ee7\u7eed\u4fdd\u6301\u3002")
        )
        AppLanguage.Japanese -> listOf(
            GreetingCandidate("saved_ja_1", "\u304a\u304b\u3048\u308a\u306a\u3055\u3044", "\u3059\u3067\u306b $saved \u306e\u7a7a\u9593\u3092\u7bc0\u7d04\u3002\u3055\u3089\u308b\u8efd\u304f\u306a\u308b\u3002"),
            GreetingCandidate("saved_ja_2", "\u8efd\u304f\u306a\u3063\u305f", "\u73fe\u6642\u70b9\u3067 $saved \u7bc0\u7d04\u6e08\u307f\u3002\u3053\u306e\u8abf\u5b50\u3092\u4fdd\u3063\u3066\u3002")
        )
        AppLanguage.Korean -> listOf(
            GreetingCandidate("saved_ko_1", "\ub3cc\uc544\uc628\uac83\uc744\ud658\uc601\ud569\ub2c8\ub2e4", "\uc774\ubbaf ${saved}\ub9cc\ud07c \uc80a\uc744 \uc800\uc721\ud588\uc5b4\uc694. \ub354 \uac00\ubcbc\uc6cc\uc84c\uc5b4\uc694!"),
            GreetingCandidate("saved_ko_2", "\ub354 \uac00\ubcbc\uc6cc\uc84c\uc5b4\uc694", "\uba54\ubc14 ${saved} \uc800\uc721\ub418\uc5c8\uc2b5\ub2c8\ub2e4. \uacc4\uc18d \uc720\uc9c0!")
        )
        AppLanguage.French -> listOf(
            GreetingCandidate("saved_fr_1", "Bienvenue", "Vous avez déjà économisé $saved."),
            GreetingCandidate("saved_fr_2", "Plus léger", "$saved économisé déjà.")
        )
        AppLanguage.German -> listOf(
            GreetingCandidate("saved_de_1", "Willkommen zurück", "Sie haben bereits $saved gespeichert."),
            GreetingCandidate("saved_de_2", "Leichter jetzt", "$saved bereits gespeichert.")
        )
        AppLanguage.Italian -> listOf(
            GreetingCandidate("saved_it_1", "Bentornato", "Hai già risparmiato $saved."),
            GreetingCandidate("saved_it_2", "Più leggero ora", "$saved già risparmiato.")
        )
        AppLanguage.Spanish -> listOf(
            GreetingCandidate("saved_es_1", "Bienvenido de vuelta", "Ya has ahorrado $saved."),
            GreetingCandidate("saved_es_2", "Más ligero ahora", "$saved ahorrado ya.")
        )
        AppLanguage.Portuguese -> listOf(
            GreetingCandidate("saved_pt_1", "Bem-vindo de volta", "Você já economizou $saved."),
            GreetingCandidate("saved_pt_2", "Mais leve agora", "$saved economizado já.")
        )
        AppLanguage.Russian -> listOf(
            GreetingCandidate("saved_ru_1", "С возвращением", "Вы уже сэкономили $saved."),
            GreetingCandidate("saved_ru_2", "Легче теперь", "$saved уже сохранено.")
        )
        AppLanguage.Thai -> listOf(
            GreetingCandidate("saved_th_1", "ยินดีต้อนรับกลับมา", "คุณประหยัดพื้นที่ ${saved} แล้ว"),
            GreetingCandidate("saved_th_2", "เบาลงแล้ว", "ประหยัด ${saved} แล้ว")
        )
        AppLanguage.Vietnamese -> listOf(
            GreetingCandidate("saved_vi_1", "Chào mừng quay lại", "Bạn đã tiết kiệm $saved."),
            GreetingCandidate("saved_vi_2", "Nhẹ hơn rồi", "$saved đã được tiết kiệm.")
        )
        AppLanguage.Indonesian -> listOf(
            GreetingCandidate("saved_id_1", "Selamat datang kembali", "Anda sudah menghemat $saved."),
            GreetingCandidate("saved_id_2", "Lebih ringan sekarang", "$saved sudah dihemat.")
        )
        AppLanguage.Arabic -> listOf(
            GreetingCandidate("saved_ar_1", "مرحبًا بعودتك", "لقد وفرت بالفعل $saved."),
            GreetingCandidate("saved_ar_2", "أخف الآن", "$saved تم حفظه بالفعل.")
        )
        AppLanguage.Ukrainian -> listOf(
            GreetingCandidate("saved_uk_1", "З поверненням", "Ви вже звільнили ${saved}."),
            GreetingCandidate("saved_uk_2", "Тепер легше", "Уже звільнено: ${saved}.")
        )
        AppLanguage.Polish -> listOf(
            GreetingCandidate("saved_pl_1", "Witaj ponownie", "Zaoszczędzono już ${saved}."),
            GreetingCandidate("saved_pl_2", "Teraz lżej", "Odzyskano już ${saved}.")
        )
        AppLanguage.Czech -> listOf(
            GreetingCandidate("saved_cs_1", "Vítejte zpět", "Zatím máte ušetřeno ${saved}."),
            GreetingCandidate("saved_cs_2", "Lehčí knihovna", "Ušetřeno už ${saved}.")
        )
        AppLanguage.Dutch -> listOf(
            GreetingCandidate("saved_nl_1", "Welkom terug", "Je hebt tot nu toe ${saved} bespaard."),
            GreetingCandidate("saved_nl_2", "Een stuk lichter", "Al ${saved} bespaard.")
        )
        AppLanguage.Swedish -> listOf(
            GreetingCandidate("saved_sv_1", "Välkommen tillbaka", "Du har frigjort ${saved} hittills."),
            GreetingCandidate("saved_sv_2", "Lättare nu", "${saved} frigjort redan.")
        )
        AppLanguage.Danish -> listOf(
            GreetingCandidate("saved_da_1", "Velkommen tilbage", "Du har sparet ${saved} indtil nu."),
            GreetingCandidate("saved_da_2", "Lettere nu", "${saved} sparet allerede.")
        )
        AppLanguage.Norwegian -> listOf(
            GreetingCandidate("saved_nb_1", "Velkommen tilbake", "Du har spart ${saved} så langt."),
            GreetingCandidate("saved_nb_2", "Lettere nå", "${saved} spart allerede.")
        )
        AppLanguage.Finnish -> listOf(
            GreetingCandidate("saved_fi_1", "Tervetuloa takaisin", "Olet säästänyt tähän mennessä ${saved}."),
            GreetingCandidate("saved_fi_2", "Nyt on kevyempää", "${saved} jo säästetty.")
        )
        AppLanguage.Greek -> listOf(
            GreetingCandidate("saved_el_1", "Καλώς ήρθατε ξανά", "Έχετε εξοικονομήσει ${saved} μέχρι τώρα."),
            GreetingCandidate("saved_el_2", "Πιο ελαφριά τώρα", "${saved} εξοικονομήθηκαν ήδη.")
        )
        AppLanguage.Romanian -> listOf(
            GreetingCandidate("saved_ro_1", "Bine ați revenit", "Ați economisit până acum ${saved}."),
            GreetingCandidate("saved_ro_2", "Mai ușor acum", "${saved} economisiți deja.")
        )
        AppLanguage.Hungarian -> listOf(
            GreetingCandidate("saved_hu_1", "Üdv újra", "Eddig ${saved} helyet takarított meg."),
            GreetingCandidate("saved_hu_2", "Már könnyebb", "Már ${saved} megtakarítva.")
        )
        AppLanguage.Turkish -> listOf(
            GreetingCandidate("saved_tr_1", "Tekrar hoş geldin", "Şimdiye kadar ${saved} yer açtın."),
            GreetingCandidate("saved_tr_2", "Artık daha hafif", "${saved} zaten kazanıldı.")
        )
        AppLanguage.Malay -> listOf(
            GreetingCandidate("saved_ms_1", "Selamat kembali", "Anda telah menjimatkan ${saved} setakat ini."),
            GreetingCandidate("saved_ms_2", "Lebih ringan kini", "${saved} sudah dijimatkan.")
        )
        AppLanguage.Hebrew -> listOf(
            GreetingCandidate("saved_he_1", "טוב שחזרת", "חסכת עד עכשיו ${saved}."),
            GreetingCandidate("saved_he_2", "קליל יותר", "כבר נחסכו ${saved}.")
        )
        else -> listOf(
            GreetingCandidate("saved_en_1", "Welcome back", "You've saved $saved so far."),
            GreetingCandidate("saved_en_2", "Lighter now", "$saved saved already.")
        )
    }
}

private fun pendingCandidates(language: AppLanguage, count: Int): List<GreetingCandidate> {
    return when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
            GreetingCandidate("pending_1", "\u6b22\u8fce\u56de\u6765", "\u5f53\u524d\u5f85\u6574\u7406 $count \u5f20\uff0c\u5148\u5feb\u5237\u4e00\u7ec4\u3002"),
            GreetingCandidate("pending_2", "\u5f00\u59cb\u6574\u7406", "\u8fd8\u6709 $count \u5f20\u5f85\u6574\u7406\uff0c\u4eca\u5929\u5148\u6d88\u5316\u4e00\u6279\u3002")
        )
        AppLanguage.Japanese -> listOf(
            GreetingCandidate("pending_ja_1", "\u304a\u304b\u3048\u308a\u306a\u3055\u3044", "\u73fe\u5728 $count \u679a\u304c\u6574\u7406\u5f85\u3061\u3002\u307e\u305a\u4e00\u62ac\u3057\u3066\u304a\u3053\u3046\u3002"),
            GreetingCandidate("pending_ja_2", "\u6574\u7406\u3092\u59cb\u3081\u3088\u3046", "\u307e\u3060 $count \u679a\u304c\u6b8b\u3063\u3066\u3044\u307e\u3059\u3002\u4eca\u65e5\u306f\u305d\u308c\u3092\u51e6\u7406\u3002")
        )
        AppLanguage.Korean -> listOf(
            GreetingCandidate("pending_ko_1", "\ub3cc\uc544\uc628\uac83\uc744\ud658\uc601\ud569\ub2c8\ub2e4", "\uc9c0\uae08 $count\uc778\uac00 \uc815\ub9ac \ub300\uae30\uc911. \uba39\uc800 \ube60\ub974\uac8c \ud55c \ubc29\uc73c\ub85c!"),
            GreetingCandidate("pending_ko_2", "\uc815\ub9ac \uc2dc\uc791", "\uc544\uc9c1 $count\uc778\uac00 \ub0a8\uc558\uc5b4\uc694. \uc624\ub298 \uba87 \uac1c\ub97c \ucc98\ub9ac!")
        )
        AppLanguage.French -> listOf(
            GreetingCandidate("pending_fr_1", "Bienvenue", "$count éléments en attente. Effacez un lot rapide."),
            GreetingCandidate("pending_fr_2", "Trieons", "$count éléments en attente. Commencez par un passage rapide.")
        )
        AppLanguage.German -> listOf(
            GreetingCandidate("pending_de_1", "Willkommen zurück", "$count Elemente warten. Löschen Sie schnell eine Charge."),
            GreetingCandidate("pending_de_2", "Lassen Sie uns sortieren", "$count Elemente ausstehend. Starten Sie mit einem schnellen Durchlauf.")
        )
        AppLanguage.Italian -> listOf(
            GreetingCandidate("pending_it_1", "Bentornato", "$count elementi in attesa. Cancella un lotto rapido."),
            GreetingCandidate("pending_it_2", "Ordiniamo", "$count elementi in attesa. Inizia con una passata veloce.")
        )
        AppLanguage.Spanish -> listOf(
            GreetingCandidate("pending_es_1", "Bienvenido de vuelta", "$count elementos en espera. Borra un lote rápido."),
            GreetingCandidate("pending_es_2", "Ordenemos", "$count elementos pendientes. Empieza con un paseo rápido.")
        )
        AppLanguage.Portuguese -> listOf(
            GreetingCandidate("pending_pt_1", "Bem-vindo de volta", "$count itens aguardando. Limpe um lote rápido."),
            GreetingCandidate("pending_pt_2", "Vamos organizar", "$count itens pendentes. Comece com uma passada rápida.")
        )
        AppLanguage.Russian -> listOf(
            GreetingCandidate("pending_ru_1", "С возвращением", "$count элементов в ожидании. Быстро очистите партию."),
            GreetingCandidate("pending_ru_2", "Давайте разберем", "$count элементов в ожидании. Начните с быстрого прохода.")
        )
        AppLanguage.Thai -> listOf(
            GreetingCandidate("pending_th_1", "ยินดีต้อนรับกลับมา", "${count} รายการรออยู่ ลบชุดเร็วๆ"),
            GreetingCandidate("pending_th_2", "จัดเรียงกันเถอะ", "${count} รายการค้างอยู่ เริ่มจากการผ่านเร็ว")
        )
        AppLanguage.Vietnamese -> listOf(
            GreetingCandidate("pending_vi_1", "Chào mừng quay lại", "$count mục đang chờ. Xóa một lô nhanh."),
            GreetingCandidate("pending_vi_2", "Hãy sắp xếp", "$count mục đang chờ xử lý. Bắt đầu với một lượt nhanh.")
        )
        AppLanguage.Indonesian -> listOf(
            GreetingCandidate("pending_id_1", "Selamat datang kembali", "$count item menunggu. Hapus satu batch cepat."),
            GreetingCandidate("pending_id_2", "Mari kita atur", "$count item tertunda. Mulai dengan pass cepat.")
        )
        AppLanguage.Arabic -> listOf(
            GreetingCandidate("pending_ar_1", "مرحبًا بعودتك", "$count عنصر في الانتظار. امسح دفعة سريعة."),
            GreetingCandidate("pending_ar_2", "لنرتب", "$count عنصر معلق. ابدأ بتمرير سريع.")
        )
        AppLanguage.Ukrainian -> listOf(
            GreetingCandidate("pending_uk_1", "З поверненням", "На вас чекають фото: ${count}. Розберімо одну швидку партію."),
            GreetingCandidate("pending_uk_2", "До розбору", "Фото до розбору: ${count}. Почніть зі швидкого проходу.")
        )
        AppLanguage.Polish -> listOf(
            GreetingCandidate("pending_pl_1", "Witaj ponownie", "Do przejrzenia czeka: ${count}. Zrób jedną szybką partię."),
            GreetingCandidate("pending_pl_2", "Porządkujemy", "Do przejrzenia: ${count}. Zacznij od szybkiego przeglądu.")
        )
        AppLanguage.Czech -> listOf(
            GreetingCandidate("pending_cs_1", "Vítejte zpět", "${csPlural(count, "Čeká", "Čekají", "Čeká")} na vás ${count} ${csPlural(count, "položka", "položky", "položek")}. Pojďme na jednu rychlou dávku."),
            GreetingCandidate("pending_cs_2", "Pojďme třídit", "K roztřídění: ${count}. Začněte rychlým projitím.")
        )
        AppLanguage.Dutch -> listOf(
            GreetingCandidate("pending_nl_1", "Welkom terug", "Er wachten ${count} onderdelen. Werk snel één reeks weg."),
            GreetingCandidate("pending_nl_2", "Aan de slag", "${count} onderdelen te sorteren. Begin met een snelle ronde.")
        )
        AppLanguage.Swedish -> listOf(
            GreetingCandidate("pending_sv_1", "Välkommen tillbaka", "${count} objekt väntar. Ta en snabb omgång."),
            GreetingCandidate("pending_sv_2", "Dags att sortera", "${count} objekt väntar. Börja med en snabb runda.")
        )
        AppLanguage.Danish -> listOf(
            GreetingCandidate("pending_da_1", "Velkommen tilbage", "${count} emner venter. Lad os tage en hurtig omgang."),
            GreetingCandidate("pending_da_2", "Lad os sortere", "${count} emner er usorterede. Start med en hurtig runde.")
        )
        AppLanguage.Norwegian -> listOf(
            GreetingCandidate("pending_nb_1", "Velkommen tilbake", "${count} bilder venter. Ta en rask runde."),
            GreetingCandidate("pending_nb_2", "La oss sortere", "${count} bilder er usortert. Start med en rask runde.")
        )
        AppLanguage.Finnish -> listOf(
            GreetingCandidate("pending_fi_1", "Tervetuloa takaisin", "${count} kohdetta odottaa. Käydään läpi yksi nopea erä."),
            GreetingCandidate("pending_fi_2", "Lajitellaan", "${count} kohdetta on lajittelematta. Aloita nopealla kierroksella.")
        )
        AppLanguage.Greek -> listOf(
            GreetingCandidate("pending_el_1", "Καλώς ήρθατε ξανά", "${count} στοιχεία περιμένουν. Ας ξεκαθαρίσουμε μια γρήγορη παρτίδα."),
            GreetingCandidate("pending_el_2", "Ας ταξινομήσουμε", "${count} στοιχεία είναι σε αναμονή. Ξεκινήστε με μια γρήγορη ματιά.")
        )
        AppLanguage.Romanian -> listOf(
            GreetingCandidate("pending_ro_1", "Bine ați revenit", "${count} elemente vă așteaptă. Să terminăm un lot rapid."),
            GreetingCandidate("pending_ro_2", "Să sortăm", "${count} elemente sunt de sortat. Începeți cu o trecere rapidă.")
        )
        AppLanguage.Hungarian -> listOf(
            GreetingCandidate("pending_hu_1", "Üdv újra", "${count} elem vár. Intézzünk el egy gyors csomagot."),
            GreetingCandidate("pending_hu_2", "Rendezzünk", "${count} elem vár rendezésre. Kezdje egy gyors menettel.")
        )
        AppLanguage.Turkish -> listOf(
            GreetingCandidate("pending_tr_1", "Tekrar hoş geldin", "${count} öğe bekliyor. Hızlı bir grubu temizleyelim."),
            GreetingCandidate("pending_tr_2", "Düzenleyelim", "${count} öğe bekliyor. Hızlı bir turla başla.")
        )
        AppLanguage.Malay -> listOf(
            GreetingCandidate("pending_ms_1", "Selamat kembali", "${count} item sedang menunggu. Mari selesaikan satu kelompok pantas."),
            GreetingCandidate("pending_ms_2", "Jom susun", "${count} item belum disusun. Mulakan dengan satu pusingan pantas.")
        )
        AppLanguage.Hebrew -> listOf(
            GreetingCandidate("pending_he_1", "טוב שחזרת", "${count} פריטים מחכים. בואו נסיים סבב קצר אחד."),
            GreetingCandidate("pending_he_2", "בואו נסדר", "${count} פריטים ממתינים. מתחילים במעבר קצר.")
        )
        else -> listOf(
            GreetingCandidate("pending_en_1", "Welcome back", "$count items are waiting. Let's clear one quick batch."),
            GreetingCandidate("pending_en_2", "Let's sort", "$count items are pending. Start with a quick pass.")
        )
    }
}

private fun recentCandidates(language: AppLanguage, count: Int): List<GreetingCandidate> {
    return when (language) {
        AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
            GreetingCandidate("recent_1", "\u6b22\u8fce\u56de\u6765", "\u6700\u8fd1\u4e24\u5468\u62cd\u4e86 $count \u5f20\uff0c\u6765\u5feb\u901f\u6574\u7406\u4e00\u4e0b\u3002"),
            GreetingCandidate("recent_2", "\u6709\u65b0\u5185\u5bb9", "\u8fd1 14 \u5929\u65b0\u589e $count \u5f20\uff0c\u73b0\u5728\u5f00\u59cb\u6574\u7406\u6b63\u597d\u3002")
        )
        AppLanguage.Japanese -> listOf(
            GreetingCandidate("recent_ja_1", "\u304a\u304b\u3048\u308a\u306a\u3055\u3044", "\u6700\u8fd1 2 \u9031\u9593\u3067 $count \u679a\u64ae\u5f71\u3002\u307e\u305a\u901f\u3081\u306b\u6574\u7406\u3002"),
            GreetingCandidate("recent_ja_2", "\u65b0\u3057\u3044\u30b3\u30f3\u30c6\u30f3\u30c4", "\u6700\u8fd1 14 \u65e5\u9593\u306b $count \u679a\u65b0\u7740\u3002\u4eca\u3059\u3050\u6574\u7406\u3092\u59cb\u3081\u3088\u3046\u3002")
        )
        AppLanguage.Korean -> listOf(
            GreetingCandidate("recent_ko_1", "\ub3cc\uc544\uc628\uac83\uc744\ud658\uc601\ud569\ub2c8\ub2e4", "\ucd5c\uadfc 2\uc8fc\uac04 $count\uc778\ucc45\uc601. \uba39\uc800 \ube60\ub978 \uc815\ub9ac!"),
            GreetingCandidate("recent_ko_2", "\uc0c8\ub85c\uc6b4 \uce58\ub9bd", "\ucd5c\uadfc 14\uc77c ${count}uc778 \uc0c8\ub85c\uc6b4 \uc0ac\uc9c4. \uc9c0\uae08 \uc815\ub9ac \uc2dc\uc791!")
        )
        AppLanguage.French -> listOf(
            GreetingCandidate("recent_fr_1", "Bienvenue", "Vous avez pris $count photos au cours des 2 dernières semaines."),
            GreetingCandidate("recent_fr_2", "Nouvelles captures", "$count nouvelles photos au cours des 14 derniers jours.")
        )
        AppLanguage.German -> listOf(
            GreetingCandidate("recent_de_1", "Willkommen zurück", "Sie haben in den letzten 2 Wochen $count Fotos aufgenommen."),
            GreetingCandidate("recent_de_2", "Frische Aufnahmen", "$count neue Fotos in den letzten 14 Tagen.")
        )
        AppLanguage.Italian -> listOf(
            GreetingCandidate("recent_it_1", "Bentornato", "Hai scattato $count foto nelle ultime 2 settimane."),
            GreetingCandidate("recent_it_2", "Scatti freschi", "$count nuove foto negli ultimi 14 giorni.")
        )
        AppLanguage.Spanish -> listOf(
            GreetingCandidate("recent_es_1", "Bienvenido de vuelta", "Tomaste $count fotos en las últimas 2 semanas."),
            GreetingCandidate("recent_es_2", "Capturas frescas", "$count fotos nuevas en los últimos 14 días.")
        )
        AppLanguage.Portuguese -> listOf(
            GreetingCandidate("recent_pt_1", "Bem-vindo de volta", "Você capturou $count fotos nas últimas 2 semanas."),
            GreetingCandidate("recent_pt_2", "Capturas frescas", "$count novas fotos nos últimos 14 dias.")
        )
        AppLanguage.Russian -> listOf(
            GreetingCandidate("recent_ru_1", "С возвращением", "Вы сделали $count фото за последние 2 недели."),
            GreetingCandidate("recent_ru_2", "Свежие снимки", "$count новых фото за последние 14 дней.")
        )
        AppLanguage.Thai -> listOf(
            GreetingCandidate("recent_th_1", "ยินดีต้อนรับกลับมา", "คุณถ่าย ${count} รูปใน 2 สัปดาห์ที่ผ่านมา"),
            GreetingCandidate("recent_th_2", "รูปภาพใหม่", "${count} รูปใหม่ใน 14 วันที่ผ่านมา")
        )
        AppLanguage.Vietnamese -> listOf(
            GreetingCandidate("recent_vi_1", "Chào mừng quay lại", "Bạn đã chụp $count ảnh trong 2 tuần qua."),
            GreetingCandidate("recent_vi_2", "Ảnh mới", "$count ảnh mới trong 14 ngày qua.")
        )
        AppLanguage.Indonesian -> listOf(
            GreetingCandidate("recent_id_1", "Selamat datang kembali", "Anda mengambil $count foto dalam 2 minggu terakhir."),
            GreetingCandidate("recent_id_2", "Tangkapan baru", "$count foto baru dalam 14 hari terakhir.")
        )
        AppLanguage.Arabic -> listOf(
            GreetingCandidate("recent_ar_1", "مرحبًا بعودتك", "التقطت $count صورة خلال الأسبوعين الماضيين."),
            GreetingCandidate("recent_ar_2", "لقطات جديدة", "$count صورة جديدة خلال آخر 14 يومًا.")
        )
        AppLanguage.Ukrainian -> listOf(
            GreetingCandidate("recent_uk_1", "З поверненням", "За останні 2 тижні ви зробили ${count} фото."),
            GreetingCandidate("recent_uk_2", "Свіжі кадри", "Нових фото за останні 14 днів: ${count}.")
        )
        AppLanguage.Polish -> listOf(
            GreetingCandidate("recent_pl_1", "Witaj ponownie", "Nowe zdjęcia z ostatnich 2 tygodni: ${count}."),
            GreetingCandidate("recent_pl_2", "Świeże ujęcia", "${count} zdj. z ostatnich 14 dni.")
        )
        AppLanguage.Czech -> listOf(
            GreetingCandidate("recent_cs_1", "Vítejte zpět", "Za poslední 2 týdny máte ${count} ${csPlural(count, "novou fotku", "nové fotky", "nových fotek")}."),
            GreetingCandidate("recent_cs_2", "Čerstvé snímky", "${count} ${csPlural(count, "nová fotka", "nové fotky", "nových fotek")} za posledních 14 dní.")
        )
        AppLanguage.Dutch -> listOf(
            GreetingCandidate("recent_nl_1", "Welkom terug", "Je maakte ${count} foto's in de afgelopen 2 weken."),
            GreetingCandidate("recent_nl_2", "Vers vastgelegd", "${count} nieuwe foto's in de afgelopen 14 dagen.")
        )
        AppLanguage.Swedish -> listOf(
            GreetingCandidate("recent_sv_1", "Välkommen tillbaka", "Du tog ${count} bilder de senaste 2 veckorna."),
            GreetingCandidate("recent_sv_2", "Nya bilder", "${count} nya bilder de senaste 14 dagarna.")
        )
        AppLanguage.Danish -> listOf(
            GreetingCandidate("recent_da_1", "Velkommen tilbage", "Du har taget ${count} billeder de sidste 2 uger."),
            GreetingCandidate("recent_da_2", "Friske billeder", "${count} nye billeder de sidste 14 dage.")
        )
        AppLanguage.Norwegian -> listOf(
            GreetingCandidate("recent_nb_1", "Velkommen tilbake", "Du har tatt ${count} bilder de siste 2 ukene."),
            GreetingCandidate("recent_nb_2", "Ferske bilder", "${count} nye bilder de siste 14 dagene.")
        )
        AppLanguage.Finnish -> listOf(
            GreetingCandidate("recent_fi_1", "Tervetuloa takaisin", "Otit ${count} kuvaa viimeisten 2 viikon aikana."),
            GreetingCandidate("recent_fi_2", "Tuoreita kuvia", "${count} uutta kuvaa viimeisten 14 päivän aikana.")
        )
        AppLanguage.Greek -> listOf(
            GreetingCandidate("recent_el_1", "Καλώς ήρθατε ξανά", "Τραβήξατε ${count} φωτογραφίες τις τελευταίες 2 εβδομάδες."),
            GreetingCandidate("recent_el_2", "Φρέσκες λήψεις", "${count} νέες φωτογραφίες τις τελευταίες 14 ημέρες.")
        )
        AppLanguage.Romanian -> listOf(
            GreetingCandidate("recent_ro_1", "Bine ați revenit", "Ați făcut ${count} poze în ultimele 2 săptămâni."),
            GreetingCandidate("recent_ro_2", "Poze proaspete", "${count} poze noi în ultimele 14 zile.")
        )
        AppLanguage.Hungarian -> listOf(
            GreetingCandidate("recent_hu_1", "Üdv újra", "Az elmúlt 2 hétben ${count} fotót készített."),
            GreetingCandidate("recent_hu_2", "Friss felvételek", "${count} új fotó az elmúlt 14 napban.")
        )
        AppLanguage.Turkish -> listOf(
            GreetingCandidate("recent_tr_1", "Tekrar hoş geldin", "Son 2 haftada ${count} fotoğraf çektin."),
            GreetingCandidate("recent_tr_2", "Taze kareler", "Son 14 günde ${count} yeni fotoğraf.")
        )
        AppLanguage.Malay -> listOf(
            GreetingCandidate("recent_ms_1", "Selamat kembali", "Anda merakam ${count} foto dalam 2 minggu lepas."),
            GreetingCandidate("recent_ms_2", "Rakaman baharu", "${count} foto baharu dalam 14 hari lepas.")
        )
        AppLanguage.Hebrew -> listOf(
            GreetingCandidate("recent_he_1", "טוב שחזרת", "צילמת ${count} תמונות בשבועיים האחרונים."),
            GreetingCandidate("recent_he_2", "צילומים טריים", "${count} תמונות חדשות ב-14 הימים האחרונים.")
        )
        else -> listOf(
            GreetingCandidate("recent_en_1", "Welcome back", "You captured $count photos in the last 2 weeks."),
            GreetingCandidate("recent_en_2", "Fresh captures", "$count new photos in the last 14 days.")
        )
    }
}

private fun mondayCandidates(language: AppLanguage): List<GreetingCandidate> = when (language) {
    AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
        GreetingCandidate("monday_1", "\u5468\u4e00\u597d", "\u5468\u4e00\u8f7b\u88c5\u4e0a\u9635\uff0c\u5148\u6574\u7406 20 \u5f20\u518d\u5f00\u5de5\u3002"),
        GreetingCandidate("monday_2", "\u6b22\u8fce\u56de\u6765", "\u5468\u4e00\u91cd\u7f6e\u4e00\u4e0b\uff0c\u5148\u5feb\u901f\u8fc7\u4e00\u8f6e\u5361\u7247\u3002")
    )
    AppLanguage.Japanese -> listOf(
        GreetingCandidate("monday_ja_1", "月曜日おはよう", "月曜リセット：まず20枚を片付けましょう。"),
        GreetingCandidate("monday_ja_2", "おかえりなさい", "月曜日のリフレッシュ：まず軽く1回カードを整理。")
    )
    AppLanguage.Korean -> listOf(
        GreetingCandidate("monday_ko_1", "월요일 좋은 아침입니다", "월요일 리셋트: 먼저 20장을 정리하고 시작하세요."),
        GreetingCandidate("monday_ko_2", "돌아오신 것을 환영합니다", "월요일 리프레시: 먼저 빠르게 한 번 카드 정리.")
    )
    AppLanguage.French -> listOf(
        GreetingCandidate("monday_fr_1", "Bon lundi", "Lundi reset : effacez 20 cartes avant de commencer."),
        GreetingCandidate("monday_fr_2", "Bienvenue", "Refresh du lundi : un passage rapide sur les cartes d'abord.")
    )
    AppLanguage.German -> listOf(
        GreetingCandidate("monday_de_1", "Guten Montag", "Montag-Reset : Löschen Sie zuerst 20 Karten."),
        GreetingCandidate("monday_de_2", "Willkommen zurück", "Montag-Refresh : Schnell einmal die Karten durchgehen.")
    )
    AppLanguage.Italian -> listOf(
        GreetingCandidate("monday_it_1", "Buon lunedì", "Reset del lunedì : cancella 20 carte prima di iniziare."),
        GreetingCandidate("monday_it_2", "Bentornato", "Refresh del lunedì : una rapida passata sulle carte.")
    )
    AppLanguage.Spanish -> listOf(
        GreetingCandidate("monday_es_1", "Buen lunes", "Reinicio del lunes : borra 20 cartas antes de empezar."),
        GreetingCandidate("monday_es_2", "Bienvenido de vuelta", "Refresco del lunes : un paseo rápido por las cartas primero.")
    )
    AppLanguage.Portuguese -> listOf(
        GreetingCandidate("monday_pt_1", "Boa segunda-feira", "Reset da segunda : limpe 20 cartões antes de começar."),
        GreetingCandidate("monday_pt_2", "Bem-vindo de volta", "Refresh da segunda : uma passada rápida nos cartões primeiro.")
    )
    AppLanguage.Russian -> listOf(
        GreetingCandidate("monday_ru_1", "Добрый понедельник", "Понедельничный сброс : очистите 20 карт перед началом."),
        GreetingCandidate("monday_ru_2", "С возвращением", "Обновление понедельника : быстрый проход по картам.")
    )
    AppLanguage.Thai -> listOf(
        GreetingCandidate("monday_th_1", "สวัสดีวันจันทร์", "รีเซ็ตวันจันทร์ : ลบ 20 การ์ดก่อนเริ่มต้น"),
        GreetingCandidate("monday_th_2", "ยินดีต้อนรับกลับมา", "รีเฟรชวันจันทร์ : ผ่านการ์ดเร็วๆ ก่อน")
    )
    AppLanguage.Vietnamese -> listOf(
        GreetingCandidate("monday_vi_1", "Chào thứ Hai", "Đặt lại thứ Hai : xóa 20 thẻ trước khi bắt đầu."),
        GreetingCandidate("monday_vi_2", "Chào mừng quay lại", "Làm mới thứ Hai : lướt nhanh qua các thẻ trước.")
    )
    AppLanguage.Indonesian -> listOf(
        GreetingCandidate("monday_id_1", "Selamat Senin", "Reset Senin : hapus 20 kartu sebelum memulai."),
        GreetingCandidate("monday_id_2", "Selamat datang kembali", "Refresh Senin : lewatkan kartu dengan cepat dulu.")
    )
    AppLanguage.Arabic -> listOf(
        GreetingCandidate("monday_ar_1", "يوم الاثنين سعيد", "إعادة تع يوم الاثنين : امسح 20 بطاقة قبل البدء."),
        GreetingCandidate("monday_ar_2", "مرحبًا بعودتك", "تحديث الاثنين : مرور سريع على البطاقات أولًا.")
    )
    AppLanguage.Ukrainian -> listOf(
        GreetingCandidate("monday_uk_1", "Понеділок з чистого аркуша", "Розберіть 20 карток, перш ніж почати."),
        GreetingCandidate("monday_uk_2", "З поверненням", "Понеділкове оновлення: спершу швидкий розбір карток.")
    )
    AppLanguage.Polish -> listOf(
        GreetingCandidate("monday_pl_1", "Poniedziałkowy reset", "Przejrzyj 20 kart na dobry początek."),
        GreetingCandidate("monday_pl_2", "Witaj ponownie", "Poniedziałkowe odświeżenie: najpierw jeden szybki przegląd kart.")
    )
    AppLanguage.Czech -> listOf(
        GreetingCandidate("monday_cs_1", "Pondělní restart", "Než začnete, projděte 20 karet."),
        GreetingCandidate("monday_cs_2", "Vítejte zpět", "Pondělní osvěžení: nejdřív jedno rychlé kolo karet.")
    )
    AppLanguage.Dutch -> listOf(
        GreetingCandidate("monday_nl_1", "Frisse maandag", "Werk 20 kaarten weg voordat je begint."),
        GreetingCandidate("monday_nl_2", "Welkom terug", "Maandagstart: eerst één snelle ronde kaarten.")
    )
    AppLanguage.Swedish -> listOf(
        GreetingCandidate("monday_sv_1", "Måndagsstart", "Rensa 20 kort innan du sätter igång."),
        GreetingCandidate("monday_sv_2", "Välkommen tillbaka", "Ny vecka: ta en snabb kortrunda först.")
    )
    AppLanguage.Danish -> listOf(
        GreetingCandidate("monday_da_1", "Mandagsstart", "Klar 20 kort, før du går i gang."),
        GreetingCandidate("monday_da_2", "Velkommen tilbage", "Frisk mandag: tag først en hurtig runde kort.")
    )
    AppLanguage.Norwegian -> listOf(
        GreetingCandidate("monday_nb_1", "Mandagsstart", "Rydd 20 kort før du begynner."),
        GreetingCandidate("monday_nb_2", "Velkommen tilbake", "Ny uke: en rask kortrunde først.")
    )
    AppLanguage.Finnish -> listOf(
        GreetingCandidate("monday_fi_1", "Maanantain nollaus", "Käy läpi 20 korttia ennen kuin aloitat."),
        GreetingCandidate("monday_fi_2", "Tervetuloa takaisin", "Maanantain piristys: ensin yksi nopea korttikierros.")
    )
    AppLanguage.Greek -> listOf(
        GreetingCandidate("monday_el_1", "Νέα αρχή Δευτέρας", "Ξεκαθαρίστε 20 κάρτες πριν ξεκινήσετε."),
        GreetingCandidate("monday_el_2", "Καλώς ήρθατε ξανά", "Ανανέωση Δευτέρας: πρώτα μια γρήγορη ματιά στις κάρτες.")
    )
    AppLanguage.Romanian -> listOf(
        GreetingCandidate("monday_ro_1", "Început de săptămână", "Treceți prin 20 de carduri înainte să începeți."),
        GreetingCandidate("monday_ro_2", "Bine ați revenit", "Luni proaspăt: mai întâi o trecere rapidă prin carduri.")
    )
    AppLanguage.Hungarian -> listOf(
        GreetingCandidate("monday_hu_1", "Hétfői újrakezdés", "Végezzen 20 kártyával, mielőtt belevág."),
        GreetingCandidate("monday_hu_2", "Üdv újra", "Hétfői frissítés: előbb egy gyors kártyamenet.")
    )
    AppLanguage.Turkish -> listOf(
        GreetingCandidate("monday_tr_1", "Pazartesi yenilenmesi", "Başlamadan önce 20 kartı temizle."),
        GreetingCandidate("monday_tr_2", "Tekrar hoş geldin", "Pazartesi tazelemesi: önce hızlı bir kart turu.")
    )
    AppLanguage.Malay -> listOf(
        GreetingCandidate("monday_ms_1", "Mula baharu Isnin", "Selesaikan 20 kad sebelum anda bermula."),
        GreetingCandidate("monday_ms_2", "Selamat kembali", "Segarkan hari Isnin: satu pusingan kad pantas dahulu.")
    )
    AppLanguage.Hebrew -> listOf(
        GreetingCandidate("monday_he_1", "יום שני, דף חדש", "20 כרטיסים לפני שמתחילים."),
        GreetingCandidate("monday_he_2", "טוב שחזרת", "רענון של יום שני: קודם סבב כרטיסים קצר.")
    )
    else -> listOf(
        GreetingCandidate("monday_en_1", "Monday reset", "Clear 20 cards before you start."),
        GreetingCandidate("monday_en_2", "Welcome back", "Monday refresh: one quick card pass first.")
    )
}

private fun weekendCandidates(language: AppLanguage): List<GreetingCandidate> = when (language) {
    AppLanguage.SimplifiedChinese, AppLanguage.TraditionalChinese -> listOf(
        GreetingCandidate("weekend_1", "\u5468\u672b\u597d", "\u5468\u672b\u56de\u987e\u6a21\u5f0f\uff0c\u7ffb\u7ffb\u8fd9\u5468\u7684\u9ad8\u5149\u7167\u7247\u3002"),
        GreetingCandidate("weekend_2", "\u6b22\u8fce\u56de\u6765", "\u5468\u672b\u4e86\uff0c\u628a\u672c\u5468\u7684\u56de\u5fc6\u5f52\u7eb3\u4e00\u4e0b\u3002")
    )
    AppLanguage.Japanese -> listOf(
        GreetingCandidate("weekend_ja_1", "週末おはようございます", "週末の振り返り：今週のハライトをもう一度。"),
        GreetingCandidate("weekend_ja_2", "おかえりなさい", "週末レビュー：今週のベストショットを整理。")
    )
    AppLanguage.Korean -> listOf(
        GreetingCandidate("weekend_ko_1", "좋은 주말입니다", "주말 회고: 이번 주의 하이라이트를 다시 보세요."),
        GreetingCandidate("weekend_ko_2", "돌아오신 것을 환영합니다", "주말 리뷰: 이번 주의 베스트 샷들을 정리하세요.")
    )
    AppLanguage.French -> listOf(
        GreetingCandidate("weekend_fr_1", "Bon week-end", "Bilan du week-end : revisitez les temps forts de la semaine."),
        GreetingCandidate("weekend_fr_2", "Bienvenue", "Revue du week-end : rangez les meilleures photos de la semaine.")
    )
    AppLanguage.German -> listOf(
        GreetingCandidate("weekend_de_1", "Schönes Wochenende", "Wochenenden-Rückblick : Schauen Sie sich die Highlights der Woche nochmal an."),
        GreetingCandidate("weekend_de_2", "Willkommen zurück", "Wochenenden-Review : Ordnen Sie die besten Fotos der Woche.")
    )
    AppLanguage.Italian -> listOf(
        GreetingCandidate("weekend_it_1", "Buon fine settimana", "Riepilogo del weekend : rivedi i momenti salienti della settimana."),
        GreetingCandidate("weekend_it_2", "Bentornato", "Review del weekend : organizza le migliori foto della settimana.")
    )
    AppLanguage.Spanish -> listOf(
        GreetingCandidate("weekend_es_1", "Buen fin de semana", "Resumen del fin de semana : revisita los momentos destacados de la semana."),
        GreetingCandidate("weekend_es_2", "Bienvenido de vuelta", "Revisión del fin de semana : organiza las mejores fotos de la semana.")
    )
    AppLanguage.Portuguese -> listOf(
        GreetingCandidate("weekend_pt_1", "Bom fim de semana", "Resumo do fim de semana : revise os destaques da semana."),
        GreetingCandidate("weekend_pt_2", "Bem-vindo de volta", "Revisão do fim de semana : organize as melhores fotos da semana.")
    )
    AppLanguage.Russian -> listOf(
        GreetingCandidate("weekend_ru_1", "Хорошего выходного", "Итоги выходных : пересмотрите лучшие моменты недели."),
        GreetingCandidate("weekend_ru_2", "С возвращением", "Обзор выходных : уберите лучшие фото недели.")
    )
    AppLanguage.Thai -> listOf(
        GreetingCandidate("weekend_th_1", "สวัสดีสุดสัปดาห์", "สรุปสุดสัปดาห์ : ทบทวนไฮไลท์ของสัปดาห์"),
        GreetingCandidate("weekend_th_2", "ยินดีต้อนรับกลับมา", "รีวิวสุดสัปดาห์ : จัดรูปภาพที่ดีที่สุดของสัปดาห์")
    )
    AppLanguage.Vietnamese -> listOf(
        GreetingCandidate("weekend_vi_1", "Chào cuối tuần", "Tổng kết cuối tuần : xem lại những khoảnh khắc nổi bật của tuần."),
        GreetingCandidate("weekend_vi_2", "Chào mừng quay lại", "Đánh giá cuối tuần : sắp xếp những bức ảnh đẹp nhất tuần.")
    )
    AppLanguage.Indonesian -> listOf(
        GreetingCandidate("weekend_id_1", "Selamat akhir pekan", "Ringkasan akhir pekan : tinjau kembali momen minggu ini."),
        GreetingCandidate("weekend_id_2", "Selamat datang kembali", "Ulasan akhir pekan : atur foto terbaik minggu ini.")
    )
    AppLanguage.Arabic -> listOf(
        GreetingCandidate("weekend_ar_1", "عطلة نهاية أسبوع سعيدة", "ملخص نهاية الأسبوع : استعرض أفضل لحظات الأسبوع."),
        GreetingCandidate("weekend_ar_2", "مرحبًا بعودتك", "مراجعة نهاية الأسبوع : رتب أفضل صور الأسبوع.")
    )
    AppLanguage.Ukrainian -> listOf(
        GreetingCandidate("weekend_uk_1", "Підсумки тижня", "Згадайте найкращі моменти цього тижня."),
        GreetingCandidate("weekend_uk_2", "З поверненням", "Огляд вихідних: розберіть найкращі кадри тижня.")
    )
    AppLanguage.Polish -> listOf(
        GreetingCandidate("weekend_pl_1", "Weekendowe podsumowanie", "Wróć do najlepszych chwil tego tygodnia."),
        GreetingCandidate("weekend_pl_2", "Witaj ponownie", "Weekendowy przegląd: uporządkuj najlepsze zdjęcia tygodnia.")
    )
    AppLanguage.Czech -> listOf(
        GreetingCandidate("weekend_cs_1", "Víkendové ohlédnutí", "Vraťte se k tomu nejlepšímu z týdne."),
        GreetingCandidate("weekend_cs_2", "Vítejte zpět", "Víkendová revize: ukliďte nejlepší snímky tohoto týdne.")
    )
    AppLanguage.Dutch -> listOf(
        GreetingCandidate("weekend_nl_1", "Weekendterugblik", "Bekijk de hoogtepunten van deze week nog eens."),
        GreetingCandidate("weekend_nl_2", "Welkom terug", "Weekendronde: orden de mooiste foto's van deze week.")
    )
    AppLanguage.Swedish -> listOf(
        GreetingCandidate("weekend_sv_1", "Helgens återblick", "Återupplev veckans höjdpunkter."),
        GreetingCandidate("weekend_sv_2", "Välkommen tillbaka", "Helgkoll: städa bland veckans bästa bilder.")
    )
    AppLanguage.Danish -> listOf(
        GreetingCandidate("weekend_da_1", "Ugens tilbageblik", "Gense ugens højdepunkter."),
        GreetingCandidate("weekend_da_2", "Velkommen tilbage", "Weekendrunde: ryd op i ugens bedste billeder.")
    )
    AppLanguage.Norwegian -> listOf(
        GreetingCandidate("weekend_nb_1", "Helgeoppsummering", "Se igjen ukas høydepunkter."),
        GreetingCandidate("weekend_nb_2", "Velkommen tilbake", "Helgegjennomgang: rydd i ukas beste bilder.")
    )
    AppLanguage.Finnish -> listOf(
        GreetingCandidate("weekend_fi_1", "Viikon kooste", "Palaa tämän viikon parhaisiin hetkiin."),
        GreetingCandidate("weekend_fi_2", "Tervetuloa takaisin", "Viikonlopun katsaus: järjestä viikon parhaat kuvat.")
    )
    AppLanguage.Greek -> listOf(
        GreetingCandidate("weekend_el_1", "Ανασκόπηση Σαββατοκύριακου", "Ξαναδείτε τα highlights της εβδομάδας."),
        GreetingCandidate("weekend_el_2", "Καλώς ήρθατε ξανά", "Ανασκόπηση Σαββατοκύριακου: τακτοποιήστε τις καλύτερες λήψεις της εβδομάδας.")
    )
    AppLanguage.Romanian -> listOf(
        GreetingCandidate("weekend_ro_1", "Recapitularea săptămânii", "Revedeți momentele bune ale săptămânii."),
        GreetingCandidate("weekend_ro_2", "Bine ați revenit", "Recapitulare de weekend: ordonați cele mai bune poze ale săptămânii.")
    )
    AppLanguage.Hungarian -> listOf(
        GreetingCandidate("weekend_hu_1", "Hétvégi összefoglaló", "Nézze újra a hét legjobb pillanatait."),
        GreetingCandidate("weekend_hu_2", "Üdv újra", "Hétvégi áttekintés: rendezze a hét legjobb felvételeit.")
    )
    AppLanguage.Turkish -> listOf(
        GreetingCandidate("weekend_tr_1", "Hafta sonu özeti", "Bu haftanın en güzel anlarına tekrar göz at."),
        GreetingCandidate("weekend_tr_2", "Tekrar hoş geldin", "Hafta sonu gözden geçirmesi: haftanın en iyi karelerini düzenle.")
    )
    AppLanguage.Malay -> listOf(
        GreetingCandidate("weekend_ms_1", "Imbas hujung minggu", "Lihat semula sorotan minggu ini."),
        GreetingCandidate("weekend_ms_2", "Selamat kembali", "Semakan hujung minggu: kemas foto terbaik minggu ini.")
    )
    AppLanguage.Hebrew -> listOf(
        GreetingCandidate("weekend_he_1", "סיכום סוף השבוע", "הרגעים הבולטים של השבוע, שוב מול העיניים."),
        GreetingCandidate("weekend_he_2", "טוב שחזרת", "סיכום סוף שבוע: מסדרים את הצילומים הכי טובים של השבוע.")
    )
    else -> listOf(
        GreetingCandidate("weekend_en_1", "Weekend recap", "Revisit this week's highlights."),
        GreetingCandidate("weekend_en_2", "Welcome back", "Weekend review: tidy this week's best captures.")
    )
}

private fun formatReadableSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    if (gb >= 1.0) {
        val rounded = (gb * 10.0).roundToInt() / 10.0
        return "$rounded GB"
    }
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    val rounded = mb.roundToInt()
    return "$rounded MB"
}
