package com.voicelike.app

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale
import android.widget.Toast
import java.io.File
import com.voicelike.app.UpdateManager
import com.voicelike.app.GiteeRelease
import androidx.compose.ui.graphics.luminance
import androidx.core.content.FileProvider
import com.voicelike.app.helpcenter.getHelpCenterActionTitle
import com.voicelike.app.startup.StartupWelcomeTab
import com.voicelike.app.liquidglass.isLiquidGlassSupported

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class DebugFileStat(
    val name: String,
    val exists: Boolean,
    val sizeBytes: Long
)

enum class SettingsDestination {
    Home,
    Privacy,
    VersionUpdate
}

private data class SettingsHomeCopy(
    val appearanceSection: String,
    val organizeSection: String,
    val mediaSection: String,
    val intelligenceSection: String,
    val personalSection: String,
    val supportSection: String,
    val aboutSection: String,
    val labSubtitle: String,
    val helpSubtitle: String
)

private fun settingsHomeCopy(language: AppLanguage): SettingsHomeCopy = when (language) {
    AppLanguage.SimplifiedChinese -> SettingsHomeCopy(
        appearanceSection = "外观与入口",
        organizeSection = "整理体验",
        mediaSection = "声音与反馈",
        intelligenceSection = "AI 与识别",
        personalSection = "个人化",
        supportSection = "实验室与帮助",
        aboutSection = "关于、语言与隐私",
        labSubtitle = "电子相册、版权校验等仍在打磨的功能",
        helpSubtitle = "查看教程、常见问题和使用说明"
    )
    AppLanguage.TraditionalChinese -> SettingsHomeCopy(
        appearanceSection = "外觀與入口",
        organizeSection = "整理體驗",
        mediaSection = "聲音與回饋",
        intelligenceSection = "AI 與辨識",
        personalSection = "個人化",
        supportSection = "實驗室與幫助",
        aboutSection = "關於、語言與隱私",
        labSubtitle = "電子相簿、版權校驗等仍在打磨的功能",
        helpSubtitle = "查看教學、常見問題與使用說明"
    )
    else -> SettingsHomeCopy(
        appearanceSection = "Appearance and Entry",
        organizeSection = "Organizing",
        mediaSection = "Sound and Feedback",
        intelligenceSection = "AI and Recognition",
        personalSection = "Personalization",
        supportSection = "Lab and Help",
        aboutSection = "About, Language, and Privacy",
        labSubtitle = "Electronic album, copyright checks, and features still being tuned",
        helpSubtitle = "Tutorials, FAQs, and usage notes"
    )
}

private fun localizedSettingsHomeCopy(language: AppLanguage): SettingsHomeCopy = when (language) {
    AppLanguage.SimplifiedChinese -> SettingsHomeCopy(
        appearanceSection = "外观与入口",
        organizeSection = "整理体验",
        mediaSection = "声音与反馈",
        intelligenceSection = "AI 与识别",
        personalSection = "个人化",
        supportSection = "实验室与帮助",
        aboutSection = "关于、语言与隐私",
        labSubtitle = "电子相册、版权校验等仍在打磨的功能",
        helpSubtitle = "查看教程、常见问题和使用说明"
    )
    AppLanguage.TraditionalChinese -> SettingsHomeCopy(
        appearanceSection = "外觀與入口",
        organizeSection = "整理體驗",
        mediaSection = "聲音與回饋",
        intelligenceSection = "AI 與識別",
        personalSection = "個人化",
        supportSection = "實驗室與幫助",
        aboutSection = "關於、語言與隱私",
        labSubtitle = "電子相簿、版權校驗等仍在打磨的功能",
        helpSubtitle = "查看教學、常見問題和使用說明"
    )
    AppLanguage.Japanese -> SettingsHomeCopy(
        appearanceSection = "外観と入口",
        organizeSection = "整理体験",
        mediaSection = "サウンドとフィードバック",
        intelligenceSection = "AI と認識",
        personalSection = "パーソナル",
        supportSection = "ラボとヘルプ",
        aboutSection = "概要・言語・プライバシー",
        labSubtitle = "電子アルバムや著作権チェックなど、まだ調整中の機能",
        helpSubtitle = "チュートリアル、よくある質問、利用ガイド"
    )
    AppLanguage.Korean -> SettingsHomeCopy(
        appearanceSection = "외관과 진입",
        organizeSection = "정리 경험",
        mediaSection = "사운드와 피드백",
        intelligenceSection = "AI 및 인식",
        personalSection = "개인화",
        supportSection = "실험실과 도움말",
        aboutSection = "정보, 언어 및 개인정보",
        labSubtitle = "전자 앨범, 저작권 점검 등 아직 다듬는 중인 기능",
        helpSubtitle = "튜토리얼, FAQ, 사용 안내"
    )
    AppLanguage.French -> SettingsHomeCopy(
        appearanceSection = "Apparence et entrée",
        organizeSection = "Organisation",
        mediaSection = "Son et retours",
        intelligenceSection = "IA et reconnaissance",
        personalSection = "Personnalisation",
        supportSection = "Lab et aide",
        aboutSection = "À propos, langue et confidentialité",
        labSubtitle = "Album électronique, vérifications de droits d'auteur et fonctions encore en ajustement",
        helpSubtitle = "Tutoriels, FAQ et notes d'utilisation"
    )
    AppLanguage.German -> SettingsHomeCopy(
        appearanceSection = "Aussehen und Einstieg",
        organizeSection = "Organisation",
        mediaSection = "Ton und Feedback",
        intelligenceSection = "KI und Erkennung",
        personalSection = "Personalisierung",
        supportSection = "Labor und Hilfe",
        aboutSection = "Info, Sprache und Datenschutz",
        labSubtitle = "Elektronisches Album, Urheberrechtsprüfung und weitere noch abgestimmte Funktionen",
        helpSubtitle = "Anleitungen, FAQs und Nutzungshinweise"
    )
    AppLanguage.Italian -> SettingsHomeCopy(
        appearanceSection = "Aspetto e accesso",
        organizeSection = "Organizzazione",
        mediaSection = "Audio e feedback",
        intelligenceSection = "IA e riconoscimento",
        personalSection = "Personalizzazione",
        supportSection = "Lab e aiuto",
        aboutSection = "Info, lingua e privacy",
        labSubtitle = "Album elettronico, controlli copyright e funzioni ancora in fase di rifinitura",
        helpSubtitle = "Tutorial, FAQ e note d'uso"
    )
    AppLanguage.Spanish -> SettingsHomeCopy(
        appearanceSection = "Aspecto y entrada",
        organizeSection = "Organización",
        mediaSection = "Sonido y respuesta",
        intelligenceSection = "IA y reconocimiento",
        personalSection = "Personalización",
        supportSection = "Lab y ayuda",
        aboutSection = "Acerca de, idioma y privacidad",
        labSubtitle = "Álbum electrónico, comprobaciones de copyright y funciones aún en ajuste",
        helpSubtitle = "Tutoriales, preguntas frecuentes y notas de uso"
    )
    AppLanguage.Portuguese -> SettingsHomeCopy(
        appearanceSection = "Aparência e entrada",
        organizeSection = "Organização",
        mediaSection = "Som e feedback",
        intelligenceSection = "IA e reconhecimento",
        personalSection = "Personalização",
        supportSection = "Lab e ajuda",
        aboutSection = "Sobre, idioma e privacidade",
        labSubtitle = "Álbum eletrônico, verificações de direitos autorais e recursos ainda em ajuste",
        helpSubtitle = "Tutoriais, FAQ e notas de uso"
    )
    AppLanguage.Russian -> SettingsHomeCopy(
        appearanceSection = "Внешний вид и вход",
        organizeSection = "Организация",
        mediaSection = "Звук и отклик",
        intelligenceSection = "ИИ и распознавание",
        personalSection = "Персонализация",
        supportSection = "Лаборатория и помощь",
        aboutSection = "О приложении, язык и конфиденциальность",
        labSubtitle = "Электронный альбом, проверка авторских прав и функции, которые ещё дорабатываются",
        helpSubtitle = "Руководства, FAQ и заметки по использованию"
    )
    AppLanguage.Thai -> SettingsHomeCopy(
        appearanceSection = "รูปลักษณ์และทางเข้า",
        organizeSection = "การจัดระเบียบ",
        mediaSection = "เสียงและการตอบกลับ",
        intelligenceSection = "AI และการจดจำ",
        personalSection = "การปรับให้เป็นส่วนตัว",
        supportSection = "ห้องทดลองและช่วยเหลือ",
        aboutSection = "เกี่ยวกับ ภาษา และความเป็นส่วนตัว",
        labSubtitle = "อัลบั้มอิเล็กทรอนิกส์ การตรวจลิขสิทธิ์ และฟีเจอร์ที่ยังอยู่ระหว่างปรับแต่ง",
        helpSubtitle = "บทช่วยสอน คำถามที่พบบ่อย และคำแนะนำการใช้งาน"
    )
    AppLanguage.Vietnamese -> SettingsHomeCopy(
        appearanceSection = "Giao diện và lối vào",
        organizeSection = "Sắp xếp",
        mediaSection = "Âm thanh và phản hồi",
        intelligenceSection = "AI và nhận diện",
        personalSection = "Cá nhân hóa",
        supportSection = "Lab và trợ giúp",
        aboutSection = "Giới thiệu, ngôn ngữ và riêng tư",
        labSubtitle = "Album điện tử, kiểm tra bản quyền và các tính năng vẫn đang được tinh chỉnh",
        helpSubtitle = "Hướng dẫn, câu hỏi thường gặp và ghi chú sử dụng"
    )
    AppLanguage.Indonesian -> SettingsHomeCopy(
        appearanceSection = "Tampilan dan pintasan",
        organizeSection = "Penataan",
        mediaSection = "Suara dan umpan balik",
        intelligenceSection = "AI dan pengenalan",
        personalSection = "Personalisasi",
        supportSection = "Lab dan bantuan",
        aboutSection = "Tentang, bahasa, dan privasi",
        labSubtitle = "Album elektronik, pemeriksaan hak cipta, dan fitur yang masih disempurnakan",
        helpSubtitle = "Tutorial, FAQ, dan catatan penggunaan"
    )
    AppLanguage.Arabic -> SettingsHomeCopy(
        appearanceSection = "المظهر والمدخل",
        organizeSection = "التنظيم",
        mediaSection = "الصوت وردود الفعل",
        intelligenceSection = "الذكاء الاصطناعي والتعرّف",
        personalSection = "التخصيص",
        supportSection = "المختبر والمساعدة",
        aboutSection = "حول التطبيق واللغة والخصوصية",
        labSubtitle = "الألبوم الإلكتروني وفحص حقوق النشر والميزات التي لا تزال قيد الضبط",
        helpSubtitle = "الدروس والأسئلة الشائعة وملاحظات الاستخدام"
    )
    AppLanguage.Ukrainian -> SettingsHomeCopy(
        appearanceSection = "Вигляд і вхід",
        organizeSection = "Розбір",
        mediaSection = "Звук і відгук",
        intelligenceSection = "ШІ та розпізнавання",
        personalSection = "Персоналізація",
        supportSection = "Лабораторія і довідка",
        aboutSection = "Про застосунок, мова і приватність",
        labSubtitle = "Електронний альбом, перевірка авторських прав та функції, які ще доопрацьовуються",
        helpSubtitle = "Посібники, поширені запитання та поради"
    )
    AppLanguage.Polish -> SettingsHomeCopy(
        appearanceSection = "Wygląd i wejście",
        organizeSection = "Porządkowanie",
        mediaSection = "Dźwięk i reakcje",
        intelligenceSection = "AI i rozpoznawanie",
        personalSection = "Personalizacja",
        supportSection = "Laboratorium i pomoc",
        aboutSection = "O aplikacji, język i prywatność",
        labSubtitle = "Album elektroniczny, kontrola praw autorskich i funkcje, które wciąż są dopracowywane",
        helpSubtitle = "Poradniki, FAQ i wskazówki użytkowania"
    )
    AppLanguage.Czech -> SettingsHomeCopy(
        appearanceSection = "Vzhled a vstup",
        organizeSection = "Třídění",
        mediaSection = "Zvuk a odezva",
        intelligenceSection = "AI a rozpoznávání",
        personalSection = "Personalizace",
        supportSection = "Laboratoř a nápověda",
        aboutSection = "O aplikaci, jazyk a soukromí",
        labSubtitle = "Elektronické album, kontrola autorských práv a funkce, které se ještě ladí",
        helpSubtitle = "Návody, časté dotazy a poznámky k používání"
    )
    AppLanguage.Dutch -> SettingsHomeCopy(
        appearanceSection = "Uiterlijk en start",
        organizeSection = "Ordenen",
        mediaSection = "Geluid en feedback",
        intelligenceSection = "AI en herkenning",
        personalSection = "Personalisatie",
        supportSection = "Lab en hulp",
        aboutSection = "Over, taal en privacy",
        labSubtitle = "Elektronisch album, auteursrechtcontroles en functies die nog worden bijgesteld",
        helpSubtitle = "Handleidingen, veelgestelde vragen en gebruiksnotities"
    )
    AppLanguage.Swedish -> SettingsHomeCopy(
        appearanceSection = "Utseende och start",
        organizeSection = "Ordna",
        mediaSection = "Ljud och respons",
        intelligenceSection = "AI och igenkänning",
        personalSection = "Anpassning",
        supportSection = "Labb och hjälp",
        aboutSection = "Om, språk och integritet",
        labSubtitle = "Elektroniskt album, upphovsrättskontroller och funktioner som fortfarande finjusteras",
        helpSubtitle = "Guider, vanliga frågor och användningstips"
    )
    AppLanguage.Danish -> SettingsHomeCopy(
        appearanceSection = "Udseende og start",
        organizeSection = "Sortér",
        mediaSection = "Lyd og feedback",
        intelligenceSection = "AI og genkendelse",
        personalSection = "Tilpasning",
        supportSection = "Lab og hjælp",
        aboutSection = "Om, sprog og privatliv",
        labSubtitle = "Elektronisk album, ophavsretskontrol og funktioner, der stadig finjusteres",
        helpSubtitle = "Vejledninger, ofte stillede spørgsmål og brugsnoter"
    )
    AppLanguage.Norwegian -> SettingsHomeCopy(
        appearanceSection = "Utseende og start",
        organizeSection = "Rydd",
        mediaSection = "Lyd og respons",
        intelligenceSection = "AI og gjenkjenning",
        personalSection = "Tilpasning",
        supportSection = "Lab og hjelp",
        aboutSection = "Om, språk og personvern",
        labSubtitle = "Elektronisk album, opphavsrettskontroll og funksjoner som fortsatt finjusteres",
        helpSubtitle = "Veiledninger, vanlige spørsmål og brukstips"
    )
    AppLanguage.Finnish -> SettingsHomeCopy(
        appearanceSection = "Ulkoasu ja aloitus",
        organizeSection = "Järjestä",
        mediaSection = "Äänet ja palaute",
        intelligenceSection = "Tekoäly ja tunnistus",
        personalSection = "Mukauttaminen",
        supportSection = "Laboratorio ja ohje",
        aboutSection = "Tietoja, kieli ja tietosuoja",
        labSubtitle = "Sähköinen albumi, tekijänoikeustarkistukset ja vielä hiottavat toiminnot",
        helpSubtitle = "Oppaat, usein kysytyt kysymykset ja käyttövinkit"
    )
    AppLanguage.Greek -> SettingsHomeCopy(
        appearanceSection = "Εμφάνιση και είσοδος",
        organizeSection = "Ταξινόμηση",
        mediaSection = "Ήχος και ανάδραση",
        intelligenceSection = "AI και αναγνώριση",
        personalSection = "Εξατομίκευση",
        supportSection = "Εργαστήριο και βοήθεια",
        aboutSection = "Σχετικά, γλώσσα και απόρρητο",
        labSubtitle = "Ηλεκτρονικό άλμπουμ, έλεγχοι πνευματικών δικαιωμάτων και λειτουργίες που ακόμη βελτιώνονται",
        helpSubtitle = "Οδηγοί, συχνές ερωτήσεις και σημειώσεις χρήσης"
    )
    AppLanguage.Romanian -> SettingsHomeCopy(
        appearanceSection = "Aspect și intrare",
        organizeSection = "Organizare",
        mediaSection = "Sunet și feedback",
        intelligenceSection = "AI și recunoaștere",
        personalSection = "Personalizare",
        supportSection = "Laborator și ajutor",
        aboutSection = "Despre, limbă și confidențialitate",
        labSubtitle = "Album electronic, verificări de drepturi de autor și funcții încă în curs de ajustare",
        helpSubtitle = "Tutoriale, întrebări frecvente și note de utilizare"
    )
    AppLanguage.Hungarian -> SettingsHomeCopy(
        appearanceSection = "Megjelenés és belépés",
        organizeSection = "Rendezés",
        mediaSection = "Hang és visszajelzés",
        intelligenceSection = "MI és felismerés",
        personalSection = "Személyre szabás",
        supportSection = "Labor és súgó",
        aboutSection = "Névjegy, nyelv és adatvédelem",
        labSubtitle = "Elektronikus album, szerzői jogi ellenőrzések és még finomhangolás alatt álló funkciók",
        helpSubtitle = "Útmutatók, gyakori kérdések és használati jegyzetek"
    )
    AppLanguage.Turkish -> SettingsHomeCopy(
        appearanceSection = "Görünüm ve Giriş",
        organizeSection = "Düzenleme",
        mediaSection = "Ses ve Geri Bildirim",
        intelligenceSection = "AI ve Tanıma",
        personalSection = "Kişiselleştirme",
        supportSection = "Laboratuvar ve Yardım",
        aboutSection = "Hakkında, Dil ve Gizlilik",
        labSubtitle = "Elektronik albüm, telif hakkı kontrolleri ve hâlâ ince ayar yapılan özellikler",
        helpSubtitle = "Eğitimler, SSS ve kullanım notları"
    )
    AppLanguage.Malay -> SettingsHomeCopy(
        appearanceSection = "Rupa dan Kemasukan",
        organizeSection = "Penyusunan",
        mediaSection = "Bunyi dan Maklum Balas",
        intelligenceSection = "AI dan Pengecaman",
        personalSection = "Pemperibadian",
        supportSection = "Makmal dan Bantuan",
        aboutSection = "Perihal, Bahasa dan Privasi",
        labSubtitle = "Album elektronik, semakan hak cipta dan ciri yang masih diperhalusi",
        helpSubtitle = "Tutorial, Soalan Lazim dan nota penggunaan"
    )
    AppLanguage.Hebrew -> SettingsHomeCopy(
        appearanceSection = "מראה וכניסה",
        organizeSection = "סידור",
        mediaSection = "צלילים ומשוב",
        intelligenceSection = "AI וזיהוי",
        personalSection = "התאמה אישית",
        supportSection = "מעבדה ועזרה",
        aboutSection = "אודות, שפה ופרטיות",
        labSubtitle = "אלבום אלקטרוני, בדיקות זכויות יוצרים ותכונות שעדיין בכיוונון",
        helpSubtitle = "מדריכים, שאלות נפוצות והערות שימוש"
    )
    AppLanguage.English -> SettingsHomeCopy(
        appearanceSection = "Appearance and Entry",
        organizeSection = "Organizing",
        mediaSection = "Sound and Feedback",
        intelligenceSection = "AI and Recognition",
        personalSection = "Personalization",
        supportSection = "Lab and Help",
        aboutSection = "About, Language, and Privacy",
        labSubtitle = "Electronic album, copyright checks, and features still being tuned",
        helpSubtitle = "Tutorials, FAQs, and usage notes"
    )
}

private fun localizedSettingsModernHomeNavigationTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "新首页导航"
    AppLanguage.TraditionalChinese -> "新首頁導覽"
    AppLanguage.Japanese -> "新しいホームナビ"
    AppLanguage.Korean -> "새 홈 내비게이션"
    AppLanguage.French -> "Nouvelle navigation d'accueil"
    AppLanguage.German -> "Neue Startnavigation"
    AppLanguage.Italian -> "Nuova navigazione home"
    AppLanguage.Spanish -> "Nueva navegación de inicio"
    AppLanguage.Portuguese -> "Nova navegação inicial"
    AppLanguage.Russian -> "Новая навигация главной страницы"
    AppLanguage.Thai -> "การนำทางหน้าแรกแบบใหม่"
    AppLanguage.Vietnamese -> "Điều hướng trang chủ mới"
    AppLanguage.Indonesian -> "Navigasi beranda baru"
    AppLanguage.Arabic -> "تنقل الصفحة الرئيسية الجديد"
    AppLanguage.Ukrainian -> "Нова навігація головної"
    AppLanguage.Polish -> "Nowa nawigacja strony głównej"
    AppLanguage.Czech -> "Nová navigace domovské stránky"
    AppLanguage.Dutch -> "Nieuwe startnavigatie"
    AppLanguage.Swedish -> "Ny hemnavigering"
    AppLanguage.Danish -> "Ny hjemmenavigation"
    AppLanguage.Norwegian -> "Ny hjemmenavigasjon"
    AppLanguage.Finnish -> "Uusi kotinavigointi"
    AppLanguage.Greek -> "Νέα πλοήγηση αρχικής"
    AppLanguage.Romanian -> "Navigare nouă pe pagina principală"
    AppLanguage.Hungarian -> "Új főoldali navigáció"
    AppLanguage.Turkish -> "Yeni Ana Sayfa Gezintisi"
    AppLanguage.Malay -> "Navigasi Laman Utama Baharu"
    AppLanguage.Hebrew -> "ניווט חדש בדף הבית"
    AppLanguage.English -> "New Home Navigation"
}

private fun localizedSettingsModernHomeNavigationDesc(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "欢迎页底部显示整理、创意、水印边框和我的；关闭后恢复旧排布。"
    AppLanguage.TraditionalChinese -> "歡迎頁底部顯示整理、創意、水印邊框和我的；關閉後恢復舊排布。"
    AppLanguage.Japanese -> "ウェルカム画面下部に整理、クリエイティブ、透かしフレーム、マイを表示します。オフにすると以前の配置に戻ります。"
    AppLanguage.Korean -> "환영 화면 하단에 정리, 크리에이티브, 워터마크 프레임, 내 항목을 표시합니다. 끄면 이전 배치로 돌아갑니다."
    AppLanguage.French -> "Affiche Organiser, Créatif, Cadre filigrane et Moi en bas de l'accueil. Désactivez pour revenir à l'ancienne disposition."
    AppLanguage.German -> "Zeigt Organisieren, Kreativ, Wasserzeichenrahmen und Ich unten auf der Startseite. Deaktiviere es, um zum alten Layout zurückzukehren."
    AppLanguage.Italian -> "Mostra Organizza, Creativo, Cornice filigrana e Io in basso nella schermata iniziale. Disattivalo per tornare al vecchio layout."
    AppLanguage.Spanish -> "Muestra Organizar, Creativo, Marco de marca de agua y Yo en la parte inferior de la pantalla de bienvenida. Desactívalo para volver al diseño anterior."
    AppLanguage.Portuguese -> "Mostra Organizar, Criativo, Moldura com marca-d'água e Eu na parte inferior da tela inicial. Desative para voltar ao layout antigo."
    AppLanguage.Russian -> "Показывает Организацию, Творчество, Рамку водяного знака и Меня внизу приветственной страницы. Выключите, чтобы вернуть старую компоновку."
    AppLanguage.Thai -> "แสดง จัดระเบียบ, สร้างสรรค์, กรอบลายน้ำ และของฉัน ที่ด้านล่างของหน้าเริ่มต้น ปิดเพื่อกลับสู่เลย์เอาต์เดิม"
    AppLanguage.Vietnamese -> "Hiển thị Sắp xếp, Sáng tạo, Khung watermark và Tôi ở cuối trang chào mừng. Tắt để quay về bố cục cũ."
    AppLanguage.Indonesian -> "Tampilkan Atur, Kreatif, Bingkai watermark, dan Saya di bagian bawah halaman sambutan. Matikan untuk kembali ke tata letak lama."
    AppLanguage.Arabic -> "يعرض التنظيم والإبداع وإطار العلامة المائية وأنا أسفل صفحة الترحيب. عطّله للعودة إلى التخطيط القديم."
    AppLanguage.Ukrainian -> "Показує Розбір, Творчість, Рамку з водяним знаком і Я на екрані привітання. Вимкніть, щоб повернути старе розташування."
    AppLanguage.Polish -> "Pokazuje Porządkuj, Kreatywne, Ramkę ze znakiem wodnym i Ja na ekranie powitalnym. Wyłącz, aby przywrócić stary układ."
    AppLanguage.Czech -> "Zobrazí Třídění, Tvorbu, Rámeček s vodoznakem a Já na uvítací stránce. Vypnutím obnovíte původní rozvržení."
    AppLanguage.Dutch -> "Toont Ordenen, Creatief, Watermerkkader en Ik op de welkomstpagina. Zet uit om de oude indeling te herstellen."
    AppLanguage.Swedish -> "Visar Ordna, Kreativt, Vattenstämpelram och Jag på välkomstsidan. Stäng av för att återgå till den gamla layouten."
    AppLanguage.Danish -> "Viser Sortér, Kreativt, Vandmærkeramme og Mig på velkomstsiden. Slå fra for at gendanne det gamle layout."
    AppLanguage.Norwegian -> "Viser Rydd, Kreativt, Vannmerkeramme og Meg på velkomstsiden. Slå av for å gjenopprette den gamle layouten."
    AppLanguage.Finnish -> "Näyttää Järjestä, Luova, Vesileimakehys ja Minä aloitussivulla. Poista käytöstä palauttaaksesi vanhan asettelun."
    AppLanguage.Greek -> "Εμφανίζει Ταξινόμηση, Δημιουργικά, Πλαίσιο υδατογραφήματος και Εγώ στην αρχική οθόνη. Απενεργοποιήστε το για να επαναφέρετε την παλιά διάταξη."
    AppLanguage.Romanian -> "Afișează Organizare, Creativ, Ramă cu filigran și Eu pe pagina de întâmpinare. Dezactivați pentru a reveni la aspectul vechi."
    AppLanguage.Hungarian -> "Megjeleníti a Rendezés, Kreatív, Vízjeles keret és Én elemeket az üdvözlőoldalon. Kapcsolja ki a régi elrendezés visszaállításához."
    AppLanguage.Turkish -> "Karşılama sayfasında Düzenle, Yaratıcı, Filigran Çerçevesi ve Ben öğelerini gösterir. Eski düzene dönmek için kapatın."
    AppLanguage.Malay -> "Paparkan Susun, Kreatif, Bingkai Tera Air dan Saya pada halaman alu-aluan. Matikan untuk memulihkan reka letak lama."
    AppLanguage.Hebrew -> "מציג את סידור, יצירתי, מסגרת סימן מים ואני בדף הפתיחה. כבו כדי לשחזר את הפריסה הישנה."
    AppLanguage.English -> "Show Organize, Creative, Watermark Frame, and Me on the welcome page. Turn it off to restore the old layout."
}

private fun localizedSettingsLiquidGlassTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "液态玻璃"
    AppLanguage.TraditionalChinese -> "液態玻璃"
    AppLanguage.Japanese -> "リキッドガラス"
    AppLanguage.Korean -> "리퀴드 글래스"
    AppLanguage.French -> "Liquid Glass"
    AppLanguage.German -> "Liquid Glass"
    AppLanguage.Italian -> "Liquid Glass"
    AppLanguage.Spanish -> "Liquid Glass"
    AppLanguage.Portuguese -> "Liquid Glass"
    AppLanguage.Russian -> "Liquid Glass"
    AppLanguage.Thai -> "Liquid Glass"
    AppLanguage.Vietnamese -> "Liquid Glass"
    AppLanguage.Indonesian -> "Liquid Glass"
    AppLanguage.Arabic -> "Liquid Glass"
    AppLanguage.Ukrainian -> "Liquid Glass"
    AppLanguage.Polish -> "Liquid Glass"
    AppLanguage.Czech -> "Liquid Glass"
    AppLanguage.Dutch -> "Liquid Glass"
    AppLanguage.Swedish -> "Liquid Glass"
    AppLanguage.Danish -> "Liquid Glass"
    AppLanguage.Norwegian -> "Liquid Glass"
    AppLanguage.Finnish -> "Liquid Glass"
    AppLanguage.Greek -> "Liquid Glass"
    AppLanguage.Romanian -> "Liquid Glass"
    AppLanguage.Hungarian -> "Liquid Glass"
    AppLanguage.Turkish -> "Liquid Glass"
    AppLanguage.Malay -> "Liquid Glass"
    AppLanguage.Hebrew -> "Liquid Glass"
    AppLanguage.English -> "Liquid Glass"
}

private fun localizedSettingsLiquidGlassDesc(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "底部导航和悬浮按钮改用液态玻璃材质，底栏可按住左右拖动切换。会多耗一些电。"
    AppLanguage.TraditionalChinese -> "底部導覽和懸浮按鈕改用液態玻璃材質，底欄可按住左右拖動切換。會多耗一些電。"
    AppLanguage.Japanese -> "下部ナビとフローティングボタンをリキッドガラス素材にします。タブバーは長押しで左右にドラッグして切り替えられます。電池消費が少し増えます。"
    AppLanguage.Korean -> "하단 내비게이션과 플로팅 버튼을 리퀴드 글래스로 바꿉니다. 탭 바는 길게 눌러 좌우로 끌어 전환할 수 있습니다. 배터리를 조금 더 사용합니다."
    AppLanguage.French -> "La navigation du bas et les boutons flottants passent en Liquid Glass ; maintenez et glissez la barre pour changer d'onglet. Consomme un peu plus de batterie."
    AppLanguage.German -> "Untere Navigation und schwebende Buttons nutzen Liquid Glass; in der Tableiste gedrückt halten und ziehen, um zu wechseln. Verbraucht etwas mehr Akku."
    AppLanguage.Italian -> "Navigazione in basso e pulsanti flottanti usano Liquid Glass; tieni premuta la barra e trascina per cambiare scheda. Consuma un po' più di batteria."
    AppLanguage.Spanish -> "La navegación inferior y los botones flotantes usan Liquid Glass; mantén pulsada la barra y arrastra para cambiar de pestaña. Consume algo más de batería."
    AppLanguage.Portuguese -> "A navegação inferior e os botões flutuantes usam Liquid Glass; toque e arraste a barra para trocar de aba. Consome um pouco mais de bateria."
    AppLanguage.Russian -> "Нижняя навигация и плавающие кнопки в стиле Liquid Glass; удерживайте и тяните панель, чтобы переключать вкладки. Чуть больше расходует батарею."
    AppLanguage.Thai -> "แถบนำทางด้านล่างและปุ่มลอยเป็นวัสดุ Liquid Glass กดค้างที่แถบแล้วลากซ้ายขวาเพื่อสลับแท็บ ใช้แบตเตอรี่เพิ่มขึ้นเล็กน้อย"
    AppLanguage.Vietnamese -> "Thanh điều hướng dưới và các nút nổi dùng chất liệu Liquid Glass; nhấn giữ và kéo thanh tab để chuyển. Tốn pin hơn một chút."
    AppLanguage.Indonesian -> "Navigasi bawah dan tombol mengambang memakai Liquid Glass; tekan lama lalu geser bilah tab untuk berpindah. Sedikit lebih boros baterai."
    AppLanguage.Arabic -> "يستخدم شريط التنقل السفلي والأزرار العائمة مادة Liquid Glass؛ اضغط مطولًا على الشريط واسحب للتبديل. يستهلك بطارية أكثر قليلًا."
    AppLanguage.Ukrainian -> "Нижня навігація та плаваючі кнопки в стилі Liquid Glass; натисніть і потягніть панель вкладок, щоб перемикатися. Трохи більше витрачає батарею."
    AppLanguage.Polish -> "Dolna nawigacja i pływające przyciski w stylu Liquid Glass; przytrzymaj i przeciągnij pasek kart, aby przełączać zakładki. Zużywa trochę więcej baterii."
    AppLanguage.Czech -> "Spodní navigace a plovoucí tlačítka ve stylu Liquid Glass; podržením a přetažením lišty karet přepínáte mezi kartami. Mírně zvyšuje spotřebu baterie."
    AppLanguage.Dutch -> "Gebruik Liquid Glass voor de onderste navigatie en zwevende knoppen; houd de tabbalk ingedrukt en sleep om van tabblad te wisselen. Gebruikt iets meer batterij."
    AppLanguage.Swedish -> "Använd Liquid Glass för den nedre navigeringen och flytande knappar; tryck och dra i flikfältet för att byta flik. Drar lite mer batteri."
    AppLanguage.Danish -> "Brug Liquid Glass til den nederste navigation og de svævende knapper; tryk og træk i fanelinjen for at skifte fane. Bruger lidt mere batteri."
    AppLanguage.Norwegian -> "Bruk Liquid Glass for den nedre navigasjonen og flytende knapper; trykk og dra i fanelinjen for å bytte fane. Bruker litt mer batteri."
    AppLanguage.Finnish -> "Käytä Liquid Glass -ulkoasua alanavigoinnissa ja kelluvissa painikkeissa; paina ja vedä välilehtipalkkia vaihtaaksesi välilehteä. Kuluttaa hieman enemmän akkua."
    AppLanguage.Greek -> "Χρησιμοποιεί Liquid Glass για την κάτω πλοήγηση και τα αιωρούμενα κουμπιά, πατήστε παρατεταμένα και σύρετε τη γραμμή καρτελών για εναλλαγή. Καταναλώνει λίγο περισσότερη μπαταρία."
    AppLanguage.Romanian -> "Folosește Liquid Glass pentru navigarea de jos și butoanele plutitoare; țineți apăsat și trageți bara de file pentru a comuta filele. Consumă puțin mai multă baterie."
    AppLanguage.Hungarian -> "Liquid Glass az alsó navigációhoz és a lebegő gombokhoz; nyomja meg és húzza a fülsávot a fülek váltásához. Kicsit több akkumulátort fogyaszt."
    AppLanguage.Turkish -> "Alt gezinti ve yüzen düğmeler için Liquid Glass kullanır; sekmeleri değiştirmek için sekme çubuğuna basılı tutup sürükleyin. Biraz daha fazla pil harcar."
    AppLanguage.Malay -> "Gunakan Liquid Glass untuk navigasi bawah dan butang terapung; tekan dan seret bar tab untuk menukar tab. Menggunakan sedikit lebih banyak bateri."
    AppLanguage.Hebrew -> "משתמש ב-Liquid Glass בניווט התחתון ובכפתורים הצפים; לחצו והחזיקו וגררו את סרגל הכרטיסיות כדי להחליף כרטיסייה. צורך קצת יותר סוללה."
    AppLanguage.English -> "Use Liquid Glass for the bottom navigation and floating buttons; press and drag the tab bar to switch tabs. Uses a little more battery."
}

private fun localizedSettingsStartupDefaultPageTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "默认打开页面"
    AppLanguage.TraditionalChinese -> "預設開啟頁面"
    AppLanguage.Japanese -> "既定の開始ページ"
    AppLanguage.Korean -> "기본 시작 페이지"
    AppLanguage.French -> "Page de démarrage par défaut"
    AppLanguage.German -> "Standard-Startseite"
    AppLanguage.Italian -> "Pagina iniziale predefinita"
    AppLanguage.Spanish -> "Página de inicio predeterminada"
    AppLanguage.Portuguese -> "Página inicial padrão"
    AppLanguage.Russian -> "Страница запуска по умолчанию"
    AppLanguage.Thai -> "หน้าที่เปิดเริ่มต้น"
    AppLanguage.Vietnamese -> "Trang mở mặc định"
    AppLanguage.Indonesian -> "Halaman awal default"
    AppLanguage.Arabic -> "صفحة البدء الافتراضية"
    AppLanguage.Ukrainian -> "Початкова сторінка за замовчуванням"
    AppLanguage.Polish -> "Domyślna strona startowa"
    AppLanguage.Czech -> "Výchozí úvodní stránka"
    AppLanguage.Dutch -> "Standaard startpagina"
    AppLanguage.Swedish -> "Standardstartsida"
    AppLanguage.Danish -> "Standardstartside"
    AppLanguage.Norwegian -> "Standard startside"
    AppLanguage.Finnish -> "Oletusaloitussivu"
    AppLanguage.Greek -> "Προεπιλεγμένη αρχική σελίδα"
    AppLanguage.Romanian -> "Pagina de pornire implicită"
    AppLanguage.Hungarian -> "Alapértelmezett kezdőlap"
    AppLanguage.Turkish -> "Varsayılan Başlangıç Sayfası"
    AppLanguage.Malay -> "Halaman Mula Lalai"
    AppLanguage.Hebrew -> "דף פתיחה ברירת מחדל"
    AppLanguage.English -> "Default Start Page"
}

private fun localizedSettingsStartupDefaultPageDesc(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "冷启动欢迎页默认打开“整理”或“创意”。"
    AppLanguage.TraditionalChinese -> "冷啟動歡迎頁預設打開「整理」或「創意」。"
    AppLanguage.Japanese -> "コールドスタート時のウェルカム画面で「整理」または「クリエイティブ」を最初に開きます。"
    AppLanguage.Korean -> "앱을 새로 시작할 때 환영 화면에서 정리 또는 크리에이티브를 먼저 엽니다."
    AppLanguage.French -> "Choisissez si le démarrage à froid ouvre d'abord Organiser ou Créatif."
    AppLanguage.German -> "Lege fest, ob beim Kaltstart zuerst Organisieren oder Kreativ geöffnet wird."
    AppLanguage.Italian -> "Scegli se all'avvio a freddo si apre prima Organizza o Creativo."
    AppLanguage.Spanish -> "Elige si al inicio en frío se abre primero Organizar o Creativo."
    AppLanguage.Portuguese -> "Escolha se a inicialização a frio abre primeiro Organizar ou Criativo."
    AppLanguage.Russian -> "Выберите, что открывать первым при холодном запуске: Организацию или Творчество."
    AppLanguage.Thai -> "เลือกว่าจะเปิด “จัดระเบียบ” หรือ “สร้างสรรค์” ก่อนเมื่อเริ่มต้นใหม่"
    AppLanguage.Vietnamese -> "Chọn mở Sắp xếp hay Sáng tạo trước khi khởi động lạnh."
    AppLanguage.Indonesian -> "Pilih apakah saat cold start membuka Atur atau Kreatif terlebih dahulu."
    AppLanguage.Arabic -> "اختر ما إذا كان التشغيل البارد يفتح التنظيم أو الإبداع أولاً."
    AppLanguage.Ukrainian -> "Оберіть, що відкривати першим під час холодного запуску: Розбір чи Творчість."
    AppLanguage.Polish -> "Wybierz, czy po zimnym starcie ma się najpierw otwierać Porządkuj, czy Kreatywne."
    AppLanguage.Czech -> "Vyberte, zda se při studeném startu nejdřív otevře Třídění, nebo Tvorba."
    AppLanguage.Dutch -> "Kies of een koude start eerst Ordenen of Creatief opent."
    AppLanguage.Swedish -> "Välj om en kallstart ska öppna Ordna eller Kreativt först."
    AppLanguage.Danish -> "Vælg, om en koldstart først åbner Sortér eller Kreativt."
    AppLanguage.Norwegian -> "Velg om en kaldstart skal åpne Rydd eller Kreativt først."
    AppLanguage.Finnish -> "Valitse, avautuuko kylmäkäynnistyksessä ensin Järjestä vai Luova."
    AppLanguage.Greek -> "Επιλέξτε αν η ψυχρή εκκίνηση θα ανοίγει πρώτα Ταξινόμηση ή Δημιουργικά."
    AppLanguage.Romanian -> "Alegeți dacă la pornirea la rece se deschide mai întâi Organizare sau Creativ."
    AppLanguage.Hungarian -> "Válassza ki, hogy hidegindításkor először a Rendezés vagy a Kreatív nyíljon meg."
    AppLanguage.Turkish -> "Soğuk başlatmada önce Düzenle'nin mi yoksa Yaratıcı'nın mı açılacağını seçin."
    AppLanguage.Malay -> "Pilih sama ada pelancaran sejuk membuka Susun atau Kreatif dahulu."
    AppLanguage.Hebrew -> "בחרו אם בהפעלה קרה ייפתח קודם סידור או יצירתי."
    AppLanguage.English -> "Choose whether cold start opens Organize or Creative first."
}

private fun localizedSettingsOrganizeLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "整理"
    AppLanguage.TraditionalChinese -> "整理"
    AppLanguage.Japanese -> "整理"
    AppLanguage.Korean -> "정리"
    AppLanguage.French -> "Organiser"
    AppLanguage.German -> "Organisieren"
    AppLanguage.Italian -> "Organizza"
    AppLanguage.Spanish -> "Organizar"
    AppLanguage.Portuguese -> "Organizar"
    AppLanguage.Russian -> "Организация"
    AppLanguage.Thai -> "จัดระเบียบ"
    AppLanguage.Vietnamese -> "Sắp xếp"
    AppLanguage.Indonesian -> "Atur"
    AppLanguage.Arabic -> "التنظيم"
    AppLanguage.Ukrainian -> "Розбір"
    AppLanguage.Polish -> "Porządkuj"
    AppLanguage.Czech -> "Třídění"
    AppLanguage.Dutch -> "Ordenen"
    AppLanguage.Swedish -> "Ordna"
    AppLanguage.Danish -> "Sortér"
    AppLanguage.Norwegian -> "Rydd"
    AppLanguage.Finnish -> "Järjestä"
    AppLanguage.Greek -> "Ταξινόμηση"
    AppLanguage.Romanian -> "Organizare"
    AppLanguage.Hungarian -> "Rendezés"
    AppLanguage.Turkish -> "Düzenle"
    AppLanguage.Malay -> "Susun"
    AppLanguage.Hebrew -> "סידור"
    AppLanguage.English -> "Organize"
}

private fun localizedVersionUpdateTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7248\u672C\u66F4\u65B0"
    AppLanguage.TraditionalChinese -> "\u7248\u672C\u66F4\u65B0"
    else -> "Version Update"
}

private fun localizedVersionUpdateNow(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u7ACB\u523B\u66F4\u65B0"
    AppLanguage.TraditionalChinese -> "\u7ACB\u523B\u66F4\u65B0"
    else -> "Update Now"
}

private fun localizedVersionChecking(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u8054\u7F51\u68C0\u6D4B\u4E2D..."
    AppLanguage.TraditionalChinese -> "\u9023\u7DB2\u6AA2\u6E2C\u4E2D..."
    else -> "Checking for updates..."
}

private fun localizedVersionLatest(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u5DF2\u662F\u6700\u65B0\u7248\u672C"
    AppLanguage.TraditionalChinese -> "\u5DF2\u662F\u6700\u65B0\u7248\u672C"
    else -> "Already up to date"
}

private fun localizedVersionHasUpdate(language: AppLanguage, version: String): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u68C0\u6D4B\u5230\u65B0\u7248\u672C $version"
    AppLanguage.TraditionalChinese -> "\u5075\u6E2C\u5230\u65B0\u7248\u672C $version"
    else -> "New version detected: $version"
}

private fun localizedVersionCheckFailed(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u68C0\u6D4B\u5931\u8D25\uff0c\u8BF7\u7A0D\u540E\u91CD\u8BD5"
    AppLanguage.TraditionalChinese -> "\u6AA2\u6E2C\u5931\u6557\uff0c\u8ACB\u7A0D\u5F8C\u91CD\u8A66"
    else -> "Update check failed, please retry later"
}

private fun localizedVersionLogTitle(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u66F4\u65B0\u65E5\u5FD7"
    AppLanguage.TraditionalChinese -> "\u66F4\u65B0\u65E5\u8A8C"
    else -> "Release Notes"
}

private fun localizedVersionNoLog(language: AppLanguage): String = when (language) {
    AppLanguage.SimplifiedChinese -> "\u6682\u65E0\u66F4\u65B0\u65E5\u5FD7"
    AppLanguage.TraditionalChinese -> "\u66AB\u7121\u66F4\u65B0\u65E5\u8A8C"
    else -> "No release notes available"
}

private fun readDebugFileStat(context: android.content.Context, name: String): DebugFileStat {
    val file = File(context.filesDir, name)
    return DebugFileStat(
        name = name,
        exists = file.exists(),
        sizeBytes = if (file.exists()) file.length() else 0L
    )
}

class AppUpdateUiState(
    private val context: android.content.Context,
    private val language: AppLanguage,
    private val strings: LocalizedStrings,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    companion object {
        private const val UPDATE_CACHE_PREFS = "app_update_cache"
        private const val KEY_CURRENT_VERSION = "current_version"
        private const val KEY_CHANNEL = "channel"
        private const val KEY_RELEASE_TAG = "release_tag"
        private const val KEY_RELEASE_BODY = "release_body"
        private const val KEY_LAST_CHECK_AT = "last_check_at_ms"
        private const val CURRENT_VERSION_LOG_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
    }

    private val cachePrefs by lazy {
        context.getSharedPreferences(UPDATE_CACHE_PREFS, android.content.Context.MODE_PRIVATE)
    }
    private val normalizedCurrentVersion = normalizeVersionTag(BuildConfig.VERSION_NAME)
    private val currentChannelTag = if (language == AppLanguage.SimplifiedChinese) "gitee" else "github"

    var isChecking by mutableStateOf(false)
    var hasChecked by mutableStateOf(false)
    var updateInfo by mutableStateOf<GiteeRelease?>(null)
    var lastCheckErrorMessage by mutableStateOf<String?>(null)
    var downloadProgress by mutableStateOf<Int?>(null)
    var showUpdateDialog by mutableStateOf(false)
    var updateMessage by mutableStateOf("")
    var currentVersionLogTag by mutableStateOf<String?>(null)
    var currentVersionLogBody by mutableStateOf<String?>(null)
    private var lastCheckedAtMs by mutableLongStateOf(0L)

    init {
        restoreCurrentVersionLogFromCache()
    }

    private fun normalizeVersionTag(version: String): String {
        return version.removePrefix("v").trim()
    }

    private fun restoreCurrentVersionLogFromCache() {
        val cachedVersion = cachePrefs.getString(KEY_CURRENT_VERSION, null)
        val cachedChannel = cachePrefs.getString(KEY_CHANNEL, null)
        val cachedTag = cachePrefs.getString(KEY_RELEASE_TAG, null)
        val cachedBody = cachePrefs.getString(KEY_RELEASE_BODY, null)
        lastCheckedAtMs = cachePrefs.getLong(KEY_LAST_CHECK_AT, 0L)

        val cacheMatches = cachedVersion == normalizedCurrentVersion &&
            cachedChannel == currentChannelTag &&
            !cachedBody.isNullOrBlank()
        if (cacheMatches) {
            currentVersionLogTag = cachedTag
            currentVersionLogBody = cachedBody
        } else {
            currentVersionLogTag = null
            currentVersionLogBody = null
        }
    }

    private fun saveCurrentVersionLogToCache(tag: String, body: String) {
        val now = System.currentTimeMillis()
        cachePrefs.edit()
            .putString(KEY_CURRENT_VERSION, normalizedCurrentVersion)
            .putString(KEY_CHANNEL, currentChannelTag)
            .putString(KEY_RELEASE_TAG, tag)
            .putString(KEY_RELEASE_BODY, body)
            .putLong(KEY_LAST_CHECK_AT, now)
            .apply()
        currentVersionLogTag = tag
        currentVersionLogBody = body
        lastCheckedAtMs = now
    }

    private fun markCheckTimestamp() {
        val now = System.currentTimeMillis()
        cachePrefs.edit()
            .putString(KEY_CURRENT_VERSION, normalizedCurrentVersion)
            .putString(KEY_CHANNEL, currentChannelTag)
            .putLong(KEY_LAST_CHECK_AT, now)
            .apply()
        lastCheckedAtMs = now
    }

    fun shouldAutoCheckOnVersionPage(): Boolean {
        if (isChecking || downloadProgress != null) return false
        if (!currentVersionLogBody.isNullOrBlank()) {
            val age = System.currentTimeMillis() - lastCheckedAtMs
            if (age in 0 until CURRENT_VERSION_LOG_CACHE_TTL_MS) {
                return false
            }
        }
        return true
    }

    fun checkForUpdates(manual: Boolean, showDialogOnUpdate: Boolean = true) {
        if (isChecking) return
        isChecking = true
        lastCheckErrorMessage = null
        scope.launch {
            val result = UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME, language)
            isChecking = false
            hasChecked = true
            when {
                result.errorMessage != null -> {
                    updateInfo = null
                    lastCheckErrorMessage = result.errorMessage
                    if (manual) {
                        Toast.makeText(
                            context,
                            String.format(strings.updateCheckFailed, result.errorMessage),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                result.release != null -> {
                    markCheckTimestamp()
                    updateInfo = result.release
                    updateMessage = String.format(strings.newVersionAvailable, result.release.tag_name)
                    if (showDialogOnUpdate) {
                        showUpdateDialog = true
                    }
                }
                else -> {
                    markCheckTimestamp()
                    updateInfo = null
                    val latestRelease = result.latestRelease
                    if (latestRelease != null) {
                        val latestVersion = normalizeVersionTag(latestRelease.tag_name)
                        if (latestVersion == normalizedCurrentVersion && latestRelease.body.isNotBlank()) {
                            saveCurrentVersionLogToCache(
                                tag = latestRelease.tag_name,
                                body = latestRelease.body
                            )
                        }
                    }
                    if (manual) {
                        Toast.makeText(context, strings.noUpdateAvailable, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun downloadAndInstall() {
        val asset = updateInfo?.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        if (asset == null) {
            Toast.makeText(
                context,
                getSettingsDebugCopy(language).noApkFound,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        scope.launch {
            downloadProgress = 0
            val file = File(context.externalCacheDir, "update.apk")
            val success = UpdateManager.downloadApk(asset.browser_download_url, file) { progress ->
                downloadProgress = progress
            }
            if (success) {
                UpdateManager.installApk(context, file)
                showUpdateDialog = false
                downloadProgress = null
            } else {
                Toast.makeText(context, strings.downloadFailed, Toast.LENGTH_SHORT).show()
                downloadProgress = null
            }
        }
    }
}

@Composable
fun rememberAppUpdateUiState(
    context: android.content.Context,
    language: AppLanguage,
    strings: LocalizedStrings,
    scope: kotlinx.coroutines.CoroutineScope
): AppUpdateUiState {
    return remember(context, language, strings, scope) {
        AppUpdateUiState(context, language, strings, scope)
    }
}

@Composable
fun AppUpdateDialogHost(updateUiState: AppUpdateUiState) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dialogContainerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    if (updateUiState.showUpdateDialog && updateUiState.updateInfo != null) {
        AlertDialog(
            onDismissRequest = {
                if (updateUiState.downloadProgress == null) {
                    updateUiState.showUpdateDialog = false
                }
            },
            title = { Text(strings.update, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(updateUiState.updateMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(updateUiState.updateInfo!!.body, style = MaterialTheme.typography.bodySmall)
                    }

                    if (updateUiState.downloadProgress != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = updateUiState.downloadProgress!! / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${strings.downloading} ${updateUiState.downloadProgress}%",
                            modifier = Modifier.align(Alignment.End),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                if (updateUiState.downloadProgress == null) {
                    TextButton(onClick = { updateUiState.downloadAndInstall() }) {
                        Text(strings.update, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (updateUiState.downloadProgress == null) {
                    TextButton(onClick = { updateUiState.showUpdateDialog = false }) {
                        Text(strings.cancel, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            },
            containerColor = dialogContainerColor,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun VersionUpdateProgressBar(
    progress: Int,
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0, 100)
    val shape = RoundedCornerShape(14.dp)
    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fillColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
    } else {
        Color.Black
    }
    val labelColor = if (isDark || clamped >= 48) {
        if (isDark) MaterialTheme.colorScheme.onBackground else Color.White
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth((clamped / 100f).coerceIn(0f, 1f))
                .background(fillColor)
        )
        Text(
            text = "$text $clamped%",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun VersionUpdatePage(
    currentLang: AppLanguage,
    strings: LocalizedStrings,
    updateUiState: AppUpdateUiState,
    autoCheckUpdatesEnabled: Boolean,
    isDark: Boolean,
    onAutoCheckUpdatesEnabledChange: (Boolean) -> Unit,
    onEnableAutoCheckRequest: () -> Unit
) {
    val hasUpdate = updateUiState.updateInfo != null
    val bannerRes = if (hasUpdate) {
        R.drawable.update_banner_2026_hot
    } else {
        R.drawable.update_banner_2026
    }
    val actionText = when {
        updateUiState.downloadProgress != null -> null
        updateUiState.isChecking -> localizedVersionChecking(currentLang)
        hasUpdate -> localizedVersionUpdateNow(currentLang)
        else -> localizedVersionLatest(currentLang)
    }
    val statusText: String? = when {
        updateUiState.downloadProgress != null -> "${strings.downloading} ${updateUiState.downloadProgress}%"
        updateUiState.isChecking -> localizedVersionChecking(currentLang)
        hasUpdate -> localizedVersionHasUpdate(currentLang, updateUiState.updateInfo?.tag_name.orEmpty())
        updateUiState.lastCheckErrorMessage != null -> localizedVersionCheckFailed(currentLang)
        else -> null
    }
    val releaseBody = remember(updateUiState.updateInfo?.id, updateUiState.updateInfo?.body) {
        updateUiState.updateInfo?.body?.trim().orEmpty()
    }
    val currentVersionLogBody = remember(updateUiState.currentVersionLogBody) {
        updateUiState.currentVersionLogBody?.trim().orEmpty()
    }
    val logBodyText = when {
        hasUpdate && releaseBody.isNotBlank() -> releaseBody
        !hasUpdate && currentVersionLogBody.isNotBlank() -> currentVersionLogBody
        releaseBody.isNotBlank() -> releaseBody
        else -> localizedVersionNoLog(currentLang)
    }
    val logVersionText = when {
        hasUpdate -> updateUiState.updateInfo?.tag_name.orEmpty()
        !updateUiState.currentVersionLogTag.isNullOrBlank() -> updateUiState.currentVersionLogTag.orEmpty()
        else -> "v${BuildConfig.VERSION_NAME.removePrefix("v")}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Crossfade(
            targetState = bannerRes,
            animationSpec = tween(durationMillis = 420),
            label = "version-update-banner"
        ) { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(208.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .advancedTextureCard(shape = RoundedCornerShape(28.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!statusText.isNullOrBlank()) {
            Text(
                text = statusText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = localizedVersionLogTitle(currentLang),
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${strings.version} $logVersionText",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
        )

        if (statusText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        val logCardShape = RoundedCornerShape(16.dp)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            shape = logCardShape,
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .advancedTextureCard(shape = logCardShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = logBodyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val progress = updateUiState.downloadProgress
        if (progress != null) {
            VersionUpdateProgressBar(
                progress = progress,
                text = strings.downloading,
                isDark = isDark
            )
        } else {
            Button(
                onClick = { updateUiState.downloadAndInstall() },
                enabled = hasUpdate && !updateUiState.isChecking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .advancedTextureButton(shape = RoundedCornerShape(14.dp))
            ) {
                Text(actionText ?: localizedVersionLatest(currentLang))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 2.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = strings.autoCheckUpdate,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
                )
                Text(
                    text = strings.autoCheckUpdateDesc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = autoCheckUpdatesEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        onEnableAutoCheckRequest()
                    } else {
                        onAutoCheckUpdatesEnabledChange(false)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    checkedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f) else Color.Black,
                    uncheckedThumbColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                    uncheckedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f) else Color.Gray.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
    }
    val shape = RoundedCornerShape(16.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = shape,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = shape)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            SettingsSectionTitle(text = title)
            content()
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    layoutDirection: LayoutDirection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = if (layoutDirection == LayoutDirection.Rtl) {
                Modifier.scale(scaleX = -1f, scaleY = 1f)
            } else {
                Modifier
            }
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.advancedTextureButton(shape = CircleShape),
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                checkedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f) else Color.Black,
                uncheckedThumbColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                uncheckedTrackColor = if (isDark) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsView(
    batchSize: Int,
    queueOrder: String,
    muteVideos: Boolean,
    soundEffectsEnabled: Boolean,
    hapticsEnabled: Boolean,
    lensyEnabled: Boolean,
    aiImageResultsEnabled: Boolean,
    autoCheckUpdatesEnabled: Boolean,
    appThemeMode: AppThemeMode,
    avatarType: String,
    modernHomeNavigationEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    startupDefaultTab: String,
    backtrackModeEnabled: Boolean,
    quickOrganizeMode: Boolean,
    onBatchSizeChange: (Int) -> Unit,
    onQueueOrderChange: (String) -> Unit,
    onMuteVideosChange: (Boolean) -> Unit,
    onSoundEffectsChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onLensyEnabledChange: (Boolean) -> Unit,
    onAiImageResultsEnabledChange: (Boolean) -> Unit,
    onAutoCheckUpdatesEnabledChange: (Boolean) -> Unit,
    onModernHomeNavigationEnabledChange: (Boolean) -> Unit,
    onLiquidGlassEnabledChange: (Boolean) -> Unit,
    onStartupDefaultTabChange: (String) -> Unit,
    onBacktrackModeEnabledChange: (Boolean) -> Unit,
    onQuickOrganizeModeChange: (Boolean) -> Unit,
    fullCardMode: Boolean = false,
    onFullCardModeChange: (Boolean) -> Unit = {},
    onLensyInfoClick: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onAvatarChange: (String) -> Unit,
    onReleaseKept: () -> Unit,
    onLanguageClick: () -> Unit,
    onDeletionModeClick: () -> Unit,
    onSwipePreferencesClick: () -> Unit,
    onLabClick: () -> Unit,
    onHelpCenterClick: (() -> Unit)?,
    onOpenSourceClick: () -> Unit,
    currentDestination: SettingsDestination,
    onDestinationChange: (SettingsDestination) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val debugCopy = remember(currentLang) { getSettingsDebugCopy(currentLang) }
    val newPageStrings = getNewPageStrings(currentLang)
    val preferenceCopy = remember(currentLang) { onboardingPreferenceCopy(currentLang) }
    val settingsCopy = remember(currentLang) { localizedSettingsHomeCopy(currentLang) }
    val queueOrderOldestFirstLabel = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "由旧到新"
        AppLanguage.TraditionalChinese -> "由舊到新"
        else -> "${strings.orderTime} ↑"
    }
    val queueOrderNewestFirstLabel = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "由新到旧"
        AppLanguage.TraditionalChinese -> "由新到舊"
        else -> "${strings.orderTime} ↓"
    }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val layoutDirection = LocalLayoutDirection.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val settingsPageBackground = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val settingsDialogContainer = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    var releasedMessageVisible by remember { mutableStateOf(false) }
    var showReleaseDialog by remember { mutableStateOf(false) }
    val themeTitle = newPageStrings.themeTitle
    val themeSystemText = newPageStrings.themeSystemText
    val themeLightText = newPageStrings.themeLightText
    val themeDarkText = newPageStrings.themeDarkText
    val unusedModernHomeNavigationTitle = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "新首页导航"
        AppLanguage.TraditionalChinese -> "新首頁導覽"
        else -> "New Home Navigation"
    }
    val unusedModernHomeNavigationDesc = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "欢迎页底部显示整理、色卡、水印边框和我的；关闭后恢复旧排布。"
        AppLanguage.TraditionalChinese -> "歡迎頁底部顯示整理、色卡、水印邊框和我的；關閉後恢復舊排布。"
        else -> "Show Organize, Color Card, Watermark Frame, and Me on the welcome page. Turn it off to restore the old layout."
    }
    val unusedStartupDefaultPageTitle = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "默认打开页面"
        AppLanguage.TraditionalChinese -> "預設開啟頁面"
        else -> "Default Start Page"
    }
    val unusedStartupDefaultPageDesc = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "冷启动欢迎页默认打开“整理”或“创意”。"
        AppLanguage.TraditionalChinese -> "冷啟動歡迎頁預設打開「整理」或「創意」。"
        else -> "Choose whether cold start opens Organize or Creative first."
    }
    val unusedStartupDefaultOrganizeLabel = when (currentLang) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> "整理"
        else -> "Organize"
    }
    val unusedStartupDefaultCreativeLabel = when (currentLang) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese -> "创意"
        else -> "Creative"
    }
    val unusedLanguageSettingTitle = when (currentLang) {
        AppLanguage.SimplifiedChinese -> "语言"
        AppLanguage.TraditionalChinese -> "語言"
        else -> "Language"
    }
    val modernHomeNavigationTitle = remember(currentLang) {
        localizedSettingsModernHomeNavigationTitle(currentLang)
    }
    val modernHomeNavigationDesc = remember(currentLang) {
        localizedSettingsModernHomeNavigationDesc(currentLang)
    }
    val startupDefaultPageTitle = remember(currentLang) {
        localizedSettingsStartupDefaultPageTitle(currentLang)
    }
    val liquidGlassTitle = remember(currentLang) {
        localizedSettingsLiquidGlassTitle(currentLang)
    }
    val liquidGlassDesc = remember(currentLang) {
        localizedSettingsLiquidGlassDesc(currentLang)
    }
    val startupDefaultPageDesc = remember(currentLang) {
        localizedSettingsStartupDefaultPageDesc(currentLang)
    }
    val startupDefaultOrganizeLabel = remember(currentLang) {
        localizedSettingsOrganizeLabel(currentLang)
    }
    val startupDefaultCreativeLabel = remember(currentLang) {
        getColorCardUiStrings(currentLang).pageTitle
    }
    val languageSettingTitle = strings.language
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val updateUiState = rememberAppUpdateUiState(context, currentLang, strings, scope)
    var showAutoUpdateConfirmDialog by remember { mutableStateOf(false) }
    var showIndexDebugDialog by remember { mutableStateOf(false) }
    var debugTapCount by remember { mutableStateOf(0) }
    var debugLastTapAtMs by remember { mutableStateOf(0L) }
    var debugLogRefreshTick by remember { mutableStateOf(0) }
    val settingsHomeScrollState = rememberScrollState()
    val handleBack = {
        if (currentDestination == SettingsDestination.Home) {
            onBack()
        } else {
            onDestinationChange(SettingsDestination.Home)
        }
    }
    val pageTitle = when (currentDestination) {
        SettingsDestination.Home -> strings.settings
        SettingsDestination.Privacy -> strings.privacyTitle
        SettingsDestination.VersionUpdate -> localizedVersionUpdateTitle(currentLang)
    }

    LaunchedEffect(currentDestination) {
        if (
            currentDestination == SettingsDestination.VersionUpdate &&
            updateUiState.shouldAutoCheckOnVersionPage()
        ) {
            updateUiState.checkForUpdates(manual = false, showDialogOnUpdate = false)
        }
    }

    val analysisScanning by MediaAnalysisManager.isScanning.collectAsState()
    val analysisProgress by MediaAnalysisManager.progress.collectAsState()
    val analysisRate by MediaAnalysisManager.scanRatePerMinute.collectAsState()
    val similarScanning by SimilarPhotoManager.isScanning.collectAsState()
    val similarProgress by SimilarPhotoManager.scanProgress.collectAsState()
    val lowQualityScanning by LowQualityManager.isScanning.collectAsState()
    val lowQualityProgress by LowQualityManager.scanProgress.collectAsState()

    BackHandler(enabled = currentDestination != SettingsDestination.Home) {
        onDestinationChange(SettingsDestination.Home)
    }

    if (showIndexDebugDialog) {
        val runtimeLogs = remember(debugLogRefreshTick, showIndexDebugDialog) {
            AppRuntimeLogStore.readRecentForIndexDebug(context, 100)
        }
        val debugFiles = listOf(
            readDebugFileStat(context, "media_analysis_cache.json"),
            readDebugFileStat(context, "media_prefetch_cache.json"),
            readDebugFileStat(context, "motion_photo_index_v1.json"),
            readDebugFileStat(context, "globe_heatmap_cache_v3.json"),
            readDebugFileStat(context, "media_catalog_snapshot_v1.json"),
            readDebugFileStat(context, "runtime_events_v1.jsonl")
        )
        AlertDialog(
            onDismissRequest = { showIndexDebugDialog = false },
            title = { Text(debugCopy.indexDebugTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${debugCopy.globalIndex}: ${if (analysisScanning) debugCopy.scanning else debugCopy.idle}  ${(analysisProgress * 100f).roundToInt()}%  (${analysisRate.roundToInt()}/min)")
                    Text("${debugCopy.similarIndex}: ${if (similarScanning) debugCopy.scanning else debugCopy.idle}  ${(similarProgress * 100f).roundToInt()}%")
                    Text("${debugCopy.lowQualityIndex}: ${if (lowQualityScanning) debugCopy.scanning else debugCopy.idle}  ${(lowQualityProgress * 100f).roundToInt()}%")
                    Text("${debugCopy.files}:", fontWeight = FontWeight.SemiBold)
                    debugFiles.forEach { stat ->
                        val sizeKb = stat.sizeBytes / 1024L
                        Text("${stat.name}: ${if (stat.exists) debugCopy.ok else debugCopy.missing} (${sizeKb}KB)")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${debugCopy.runtimeLogsLast24h}: ${runtimeLogs.size}", fontWeight = FontWeight.SemiBold)
                    if (runtimeLogs.isEmpty()) {
                        Text(debugCopy.noLogsLast24h)
                    } else {
                        runtimeLogs.take(30).forEach { entry ->
                            Text(
                                text = entry.formatLine(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        AppRuntimeLogStore.clear(context)
                        debugLogRefreshTick++
                    }) {
                        Text(debugCopy.clear)
                    }
                    TextButton(onClick = {
                        val text = buildIndexDebugCopyText(context, runtimeLogs, debugCopy)
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, debugCopy.copied, Toast.LENGTH_SHORT).show()
                    }) {
                        Text(debugCopy.copy)
                    }
                    TextButton(onClick = {
                        kotlin.runCatching {
                            val exportFile = AppRuntimeLogStore.exportLast24hToCache(context)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                exportFile
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, debugCopy.runtimeLogSubject)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, debugCopy.exportRuntimeLogs))
                        }.onFailure {
                            Toast.makeText(context, debugCopy.exportFailed, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(debugCopy.export)
                    }
                    TextButton(onClick = { showIndexDebugDialog = false }) {
                        Text(strings.done, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = settingsDialogContainer,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    AppUpdateDialogHost(updateUiState = updateUiState)

    if (showAutoUpdateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAutoUpdateConfirmDialog = false },
            title = { Text(strings.autoCheckUpdateDialogTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.autoCheckUpdateDialogContent) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAutoCheckUpdatesEnabledChange(true)
                        showAutoUpdateConfirmDialog = false
                    }
                ) {
                    Text(strings.confirm, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoUpdateConfirmDialog = false }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            },
            containerColor = settingsDialogContainer,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    if (showReleaseDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseDialog = false },
            title = { Text(strings.releaseDialogTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.releaseDialogContent) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReleaseKept()
                        scope.launch {
                            releasedMessageVisible = true
                            delay(2000)
                            releasedMessageVisible = false
                        }
                        showReleaseDialog = false
                    }
                ) {
                    Text(strings.confirm, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReleaseDialog = false }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            },
            containerColor = settingsDialogContainer,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsPageBackground)
    ) {
        UnifiedSubpageTopBar(
            title = pageTitle,
            onBack = handleBack
        )

        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = currentDestination,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = FluidTransitions.SheetEnter,
                    initialContentExit = FluidTransitions.SheetExit
                )
            },
            label = "settings-destination"
        ) { destination ->
            when (destination) {
            SettingsDestination.Home -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(settingsHomeScrollState)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
            SettingsSectionCard(
                title = settingsCopy.appearanceSection,
                modifier = Modifier.then(staggeredRevealModifier(0))
            ) {
            Text(
                text = themeTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = appThemeMode == AppThemeMode.System,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeModeChange(AppThemeMode.System)
                    },
                    label = { Text(themeSystemText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                )
                FilterChip(
                    selected = appThemeMode == AppThemeMode.Light,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeModeChange(AppThemeMode.Light)
                    },
                    label = { Text(themeLightText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                )
                FilterChip(
                    selected = appThemeMode == AppThemeMode.Dark,
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeModeChange(AppThemeMode.Dark)
                    },
                    label = { Text(themeDarkText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SettingsSwitchItem(
                title = modernHomeNavigationTitle,
                subtitle = modernHomeNavigationDesc,
                checked = modernHomeNavigationEnabled,
                isDark = isDark,
                modifier = Modifier.then(staggeredRevealModifier(2)),
                onCheckedChange = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onModernHomeNavigationEnabledChange(it)
                }
            )

            if (modernHomeNavigationEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = startupDefaultPageTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = startupDefaultPageDesc,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = startupDefaultTab == StartupWelcomeTab.Organize.name,
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartupDefaultTabChange(StartupWelcomeTab.Organize.name)
                        },
                        label = { Text(startupDefaultOrganizeLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                            selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                    )
                    FilterChip(
                        selected = startupDefaultTab == StartupWelcomeTab.ColorCard.name,
                        onClick = {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartupDefaultTabChange(StartupWelcomeTab.ColorCard.name)
                        },
                        label = { Text(startupDefaultCreativeLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                            selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                    )
                }
            }

            if (isLiquidGlassSupported()) {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsSwitchItem(
                    title = liquidGlassTitle,
                    subtitle = liquidGlassDesc,
                    checked = liquidGlassEnabled,
                    isDark = isDark,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLiquidGlassEnabledChange(it)
                    }
                )
            }

            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSectionCard(
                title = settingsCopy.organizeSection,
                modifier = Modifier.then(staggeredRevealModifier(2))
            ) {
            SettingsSwitchItem(
                title = newPageStrings.labBacktrackModeTitle,
                subtitle = newPageStrings.labBacktrackModeSubtitle,
                checked = backtrackModeEnabled,
                isDark = isDark,
                modifier = Modifier.then(staggeredRevealModifier(3)),
                onCheckedChange = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBacktrackModeEnabledChange(it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchItem(
                title = newPageStrings.labQuickOrganizeTitle,
                subtitle = newPageStrings.labQuickOrganizeSubtitle,
                checked = quickOrganizeMode,
                isDark = isDark,
                modifier = Modifier.then(staggeredRevealModifier(4)),
                onCheckedChange = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQuickOrganizeModeChange(it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            val organizeOptions = organizeOptionsStrings(LocalAppLanguage.current)
            SettingsSwitchItem(
                title = organizeOptions.fullCardTitle,
                subtitle = organizeOptions.fullCardSubtitle,
                checked = fullCardMode,
                isDark = isDark,
                modifier = Modifier.then(staggeredRevealModifier(4)),
                onCheckedChange = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFullCardModeChange(it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Deletion Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeletionModeClick()
                    })
                    .then(staggeredRevealModifier(2))
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preferenceCopy.organizeDeleteTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = if (layoutDirection == LayoutDirection.Rtl) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipePreferencesClick()
                    })
                    .then(staggeredRevealModifier(5))
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preferenceCopy.organizeSwipeTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = if (layoutDirection == LayoutDirection.Rtl) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier
                )
            }

            Spacer(modifier = Modifier.height(10.dp))


            // Batch Size
            Text(
                text = strings.batchSize,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                modifier = Modifier.then(staggeredRevealModifier(6))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .advancedTextureCard(shape = RoundedCornerShape(16.dp))
                    .then(staggeredRevealModifier(6))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "$ORGANIZE_BATCH_SIZE_MIN", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text(text = "$batchSize", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "$ORGANIZE_BATCH_SIZE_MAX", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    Slider(
                        value = batchSize.toFloat(),
                        onValueChange = { value ->
                            val rounded = value.roundToInt().coerceIn(ORGANIZE_BATCH_SIZE_MIN, ORGANIZE_BATCH_SIZE_MAX)
                            if (rounded != batchSize) {
                                if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBatchSizeChange(rounded)
                            }
                        },
                        valueRange = ORGANIZE_BATCH_SIZE_MIN.toFloat()..ORGANIZE_BATCH_SIZE_MAX.toFloat(),
                        steps = ORGANIZE_BATCH_SIZE_MAX - ORGANIZE_BATCH_SIZE_MIN - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                            activeTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                            inactiveTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Queue Order
            Text(
                text = strings.queueOrder,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                modifier = Modifier.then(staggeredRevealModifier(7))
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(7)),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = queueOrder == "random",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("random")
                    },
                    label = { Text(strings.orderRandom) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                        .bounceClick()
                )

                FilterChip(
                    selected = queueOrder == "time_asc",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("time_asc")
                    },
                    label = { Text(queueOrderOldestFirstLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                        .bounceClick()
                )

                FilterChip(
                    selected = queueOrder == "time_desc",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("time_desc")
                    },
                    label = { Text(queueOrderNewestFirstLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                        .bounceClick()
                )

                FilterChip(
                    selected = queueOrder == "size_desc",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("size_desc")
                    },
                    label = { Text(organizeSortingStrings(currentLang).orderLargestFirst) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                        .bounceClick()
                )

                FilterChip(
                    selected = queueOrder == "size_asc",
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueOrderChange("size_asc")
                    },
                    label = { Text(organizeSortingStrings(currentLang).orderSmallestFirst) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.Black,
                        selectedLabelColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .advancedTextureButton(shape = RoundedCornerShape(20.dp))
                        .bounceClick()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Organize Again: bring kept photos back by month, album or type
            val organizeAgainStrings = organizeSortingStrings(currentLang)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReleaseKept()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .then(staggeredRevealModifier(8)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = organizeAgainStrings.organizeAgain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = organizeAgainStrings.organizeAgainSubtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = if (layoutDirection == LayoutDirection.Rtl) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier
                )
            }

            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSectionCard(
                title = settingsCopy.mediaSection,
                modifier = Modifier.then(staggeredRevealModifier(6))
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(6)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.muteVideos,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = muteVideos,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMuteVideosChange(it)
                    },
                    modifier = Modifier.advancedTextureButton(shape = CircleShape),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                        checkedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f) else Color.Black,
                        uncheckedThumbColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        uncheckedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f) else Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(7)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.soundEffects,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = soundEffectsEnabled,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSoundEffectsChange(it)
                    },
                    modifier = Modifier.advancedTextureButton(shape = CircleShape),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                        checkedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f) else Color.Black,
                        uncheckedThumbColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        uncheckedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f) else Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(8)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.haptics,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHapticsEnabledChange(it)
                    },
                    modifier = Modifier.advancedTextureButton(shape = CircleShape),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                        checkedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f) else Color.Black,
                        uncheckedThumbColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        uncheckedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f) else Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }

            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSectionCard(
                title = settingsCopy.intelligenceSection,
                modifier = Modifier.then(staggeredRevealModifier(8))
            ) {
            AiCapabilitySettingsSection(
                lensyEnabled = lensyEnabled,
                imageResultsEnabled = aiImageResultsEnabled,
                onLensyEnabledChange = onLensyEnabledChange,
                onImageResultsEnabledChange = onAiImageResultsEnabledChange,
                onLensyInfoClick = onLensyInfoClick,
                modifier = Modifier.then(staggeredRevealModifier(8)),
                showTitle = false,
                useContainer = false
            )

            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSectionCard(
                title = settingsCopy.supportSection,
                modifier = Modifier.then(staggeredRevealModifier(18))
            ) {
            SettingsNavigationItem(
                title = newPageStrings.labEntryTitle,
                subtitle = settingsCopy.labSubtitle,
                icon = Icons.Outlined.Science,
                layoutDirection = layoutDirection,
                modifier = Modifier.then(staggeredRevealModifier(18)),
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLabClick()
                }
            )

            val openHelpCenter = onHelpCenterClick
            if (openHelpCenter != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsNavigationItem(
                    title = getHelpCenterActionTitle(currentLang),
                    subtitle = settingsCopy.helpSubtitle,
                    icon = Icons.Outlined.HelpOutline,
                    layoutDirection = layoutDirection,
                    modifier = Modifier.then(staggeredRevealModifier(18)),
                    onClick = {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        openHelpCenter()
                    }
                )
            }

            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSectionCard(
                title = settingsCopy.aboutSection,
                modifier = Modifier.then(staggeredRevealModifier(18))
            ) {
            SettingsNavigationItem(
                title = languageSettingTitle,
                subtitle = currentLang.displayName,
                layoutDirection = layoutDirection,
                modifier = Modifier.then(staggeredRevealModifier(18)),
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLanguageClick()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavigationItem(
                title = strings.privacyButton,
                layoutDirection = layoutDirection,
                modifier = Modifier.then(staggeredRevealModifier(18)),
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDestinationChange(SettingsDestination.Privacy)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavigationItem(
                title = localizedVersionUpdateTitle(currentLang),
                subtitle = "${strings.version} ${BuildConfig.VERSION_NAME}",
                layoutDirection = layoutDirection,
                modifier = Modifier.then(staggeredRevealModifier(19)),
                onClick = {
                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDestinationChange(SettingsDestination.VersionUpdate)
                }
            )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy Links
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            val termsUrl = remember(currentLang) { userAgreementUrlFor(currentLang) }
            val privacyUrl = remember(currentLang) { privacyPolicyUrlFor(currentLang) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .then(staggeredRevealModifier(19)),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { 
                    uriHandler.openUri(termsUrl)
                }) {
                    Text(strings.termsOfService, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                TextButton(onClick = { 
                    uriHandler.openUri(privacyUrl)
                }) {
                    Text(strings.privacyPolicy, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            // Open Source Licenses
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(21)),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onOpenSourceClick) {
                    Text(
                        text = strings.openSourceLicenses ?: "Open Source Licenses",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Version & Copyright
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(staggeredRevealModifier(22)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${strings.version} ${BuildConfig.VERSION_NAME}  ${strings.copyright} 2026",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.clickable {
                            val copy = strings.copyright
                            if (copy.contains("\u8D85\u7EA7\u897F\u74DC") || copy.contains("superwatermelon", ignoreCase = true)) {
                                val now = System.currentTimeMillis()
                                val nextTapCount = if (now - debugLastTapAtMs <= 450L) debugTapCount + 1 else 1
                                debugTapCount = nextTapCount
                                debugLastTapAtMs = now
                                if (nextTapCount >= 2) {
                                    showIndexDebugDialog = true
                                    debugTapCount = 0
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (currentLang == AppLanguage.SimplifiedChinese) {
                        Text(
                            text = "粤ICP备2025506225号-4A",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://beian.miit.gov.cn")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
            }
            SettingsDestination.Privacy -> {
                PrivacyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    topSpacing = 8.dp,
                    bottomSpacing = 48.dp
                )
            }
            SettingsDestination.VersionUpdate -> {
                VersionUpdatePage(
                    currentLang = currentLang,
                    strings = strings,
                    updateUiState = updateUiState,
                    autoCheckUpdatesEnabled = autoCheckUpdatesEnabled,
                    isDark = isDark,
                    onAutoCheckUpdatesEnabledChange = onAutoCheckUpdatesEnabledChange,
                    onEnableAutoCheckRequest = { showAutoUpdateConfirmDialog = true }
                )
            }
        }
        }
    }
}
