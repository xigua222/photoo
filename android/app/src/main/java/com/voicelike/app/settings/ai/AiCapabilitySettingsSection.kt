package com.voicelike.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class AiCapabilitySettingsCopy(
    val sectionTitle: String,
    val showLensyEntry: Boolean,
    val lensySubtitle: String,
    val imageResultsTitle: String,
    val imageResultsSubtitle: String,
    val lensyContentDescription: String,
    val infoContentDescription: String
)

@Composable
private fun rememberAiCapabilitySettingsCopy(): AiCapabilitySettingsCopy {
    val language = LocalAppLanguage.current
    return remember(language) {
        when (language) {
            AppLanguage.SimplifiedChinese -> AiCapabilitySettingsCopy(
                sectionTitle = "AI 能力设置",
                showLensyEntry = true,
                lensySubtitle = "智能助手 Lensy 会在浏览图片时提供轻量陪伴和点评。",
                imageResultsTitle = "AI 识图结果",
                imageResultsSubtitle = "控制首页卡片上的 ML Kit 标签展示。关闭后仅隐藏首页展示，不影响后台分析、持久化和词云。",
                lensyContentDescription = "Lensy",
                infoContentDescription = "了解 Lensy"
            )
            AppLanguage.TraditionalChinese -> AiCapabilitySettingsCopy(
                sectionTitle = "AI 功能設定",
                showLensyEntry = false,
                lensySubtitle = "智慧助手 Lensy 會在瀏覽圖片時提供即時陪伴與點評。",
                imageResultsTitle = "AI 識圖結果",
                imageResultsSubtitle = "控制首頁卡片上的 ML Kit 標籤展示。關閉後只會隱藏首頁顯示，不影響背景分析、持久化與詞雲。",
                lensyContentDescription = "Lensy",
                infoContentDescription = "瞭解 Lensy"
            )
            AppLanguage.Korean -> AiCapabilitySettingsCopy(
                sectionTitle = "AI 기능",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "AI 이미지 인식 결과",
                imageResultsSubtitle = "홈 카드의 ML Kit 라벨 표시를 제어합니다. 끄면 홈 표시만 숨겨지고 백그라운드 분석과 저장 결과는 유지됩니다.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensy info"
            )
            AppLanguage.Japanese -> AiCapabilitySettingsCopy(
                sectionTitle = "AI 機能",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "AI画像認識結果",
                imageResultsSubtitle = "ホームカード上の ML Kit ラベル表示を切り替えます。オフにするとホーム表示のみ非表示になり、バックグラウンド分析や保存結果には影響しません。",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensy info"
            )
            AppLanguage.French -> AiCapabilitySettingsCopy(
                sectionTitle = "Fonctions IA",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Résultats de reconnaissance IA",
                imageResultsSubtitle = "Contrôle l'affichage des libellés ML Kit sur les cartes d'accueil. La désactivation masque seulement l'affichage sur l'accueil.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Infos Lensy"
            )
            AppLanguage.German -> AiCapabilitySettingsCopy(
                sectionTitle = "KI-Funktionen",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "KI-Bilderkennung",
                imageResultsSubtitle = "Steuert die Anzeige von ML-Kit-Labels auf Startkarten. Beim Ausschalten wird nur die Anzeige auf der Startseite verborgen.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensy-Info"
            )
            AppLanguage.Italian -> AiCapabilitySettingsCopy(
                sectionTitle = "Funzioni AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Risultati riconoscimento AI",
                imageResultsSubtitle = "Controlla le etichette ML Kit nelle schede della home. Disattivandolo si nasconde solo la visualizzazione nella home.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info Lensy"
            )
            AppLanguage.Portuguese -> AiCapabilitySettingsCopy(
                sectionTitle = "Recursos de IA",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Resultados de reconhecimento por IA",
                imageResultsSubtitle = "Controla os rótulos do ML Kit nos cards da tela inicial. Desativar oculta apenas a exibição na tela inicial.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info do Lensy"
            )
            AppLanguage.Spanish -> AiCapabilitySettingsCopy(
                sectionTitle = "Funciones de IA",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Resultados de reconocimiento IA",
                imageResultsSubtitle = "Controla las etiquetas de ML Kit en las tarjetas de inicio. Al desactivarlo solo se oculta la vista en inicio.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Información de Lensy"
            )
            AppLanguage.Russian -> AiCapabilitySettingsCopy(
                sectionTitle = "Функции ИИ",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Результаты распознавания ИИ",
                imageResultsSubtitle = "Управляет показом меток ML Kit на карточках главной страницы. При отключении скрывается только показ на главной.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "О Lensy"
            )
            AppLanguage.Thai -> AiCapabilitySettingsCopy(
                sectionTitle = "ความสามารถ AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "ผลลัพธ์การรู้จำภาพ AI",
                imageResultsSubtitle = "ควบคุมการแสดงป้ายกำกับ ML Kit บนการ์ดหน้าแรก เมื่อปิดจะซ่อนเฉพาะการแสดงบนหน้าแรกเท่านั้น",
                lensyContentDescription = "Lensy",
                infoContentDescription = "ข้อมูล Lensy"
            )
            AppLanguage.Vietnamese -> AiCapabilitySettingsCopy(
                sectionTitle = "Tính năng AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Kết quả nhận diện ảnh AI",
                imageResultsSubtitle = "Điều khiển nhãn ML Kit trên thẻ trang chủ. Tắt chỉ ẩn phần hiển thị ở trang chủ.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Thông tin Lensy"
            )
            AppLanguage.Indonesian -> AiCapabilitySettingsCopy(
                sectionTitle = "Fitur AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Hasil pengenalan gambar AI",
                imageResultsSubtitle = "Mengontrol label ML Kit di kartu beranda. Jika dimatikan, hanya tampilan di beranda yang disembunyikan.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info Lensy"
            )
            AppLanguage.Arabic -> AiCapabilitySettingsCopy(
                sectionTitle = "ميزات الذكاء الاصطناعي",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "نتائج التعرف على الصور بالذكاء الاصطناعي",
                imageResultsSubtitle = "يتحكم في عرض تسميات ML Kit على بطاقات الصفحة الرئيسية. عند إيقافه يتم إخفاء العرض في الصفحة الرئيسية فقط.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "معلومات Lensy"
            )
            AppLanguage.Ukrainian -> AiCapabilitySettingsCopy(
                sectionTitle = "Функції ШІ",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Результати розпізнавання зображень ШІ",
                imageResultsSubtitle = "Керує мітками ML Kit на картках головної. Вимкнення лише ховає їх на головній і не впливає на фоновий аналіз.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Інфо про Lensy"
            )
            AppLanguage.Polish -> AiCapabilitySettingsCopy(
                sectionTitle = "Funkcje AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Wyniki rozpoznawania obrazów AI",
                imageResultsSubtitle = "Steruje etykietami ML Kit na kartach strony głównej. Wyłączenie ukrywa je tylko na stronie głównej i nie wpływa na analizę w tle.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Informacje o Lensy"
            )
            AppLanguage.Czech -> AiCapabilitySettingsCopy(
                sectionTitle = "Funkce AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Výsledky rozpoznávání obrázků AI",
                imageResultsSubtitle = "Řídí zobrazení štítků ML Kit na kartách domovské stránky. Vypnutí skryje pouze zobrazení na domovské stránce a neovlivní analýzu na pozadí.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Informace o Lensy"
            )
            AppLanguage.Dutch -> AiCapabilitySettingsCopy(
                sectionTitle = "AI-functies",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Resultaten van AI-beeldherkenning",
                imageResultsSubtitle = "Bepaalt ML Kit-labels op kaarten van het startscherm. Uitzetten verbergt alleen de weergave op het startscherm en heeft geen invloed op analyse op de achtergrond.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info over Lensy"
            )
            AppLanguage.Swedish -> AiCapabilitySettingsCopy(
                sectionTitle = "AI-funktioner",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Resultat från AI-bildigenkänning",
                imageResultsSubtitle = "Styr ML Kit-etiketter på startsidans kort. Att stänga av döljer bara visningen på startsidan och påverkar inte analysen i bakgrunden.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info om Lensy"
            )
            AppLanguage.Danish -> AiCapabilitySettingsCopy(
                sectionTitle = "AI-funktioner",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Resultater af AI-billedgenkendelse",
                imageResultsSubtitle = "Styrer ML Kit-etiketter på kortene på startsiden. Slås det fra, skjules kun visningen på startsiden, og analysen i baggrunden påvirkes ikke.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info om Lensy"
            )
            AppLanguage.Norwegian -> AiCapabilitySettingsCopy(
                sectionTitle = "AI-funksjoner",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Resultater fra AI-bildegjenkjenning",
                imageResultsSubtitle = "Styrer ML Kit-etiketter på kortene på startsiden. Å slå av skjuler bare visningen på startsiden og påvirker ikke analysen i bakgrunnen.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Info om Lensy"
            )
            AppLanguage.Finnish -> AiCapabilitySettingsCopy(
                sectionTitle = "Tekoälyominaisuudet",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Tekoälyn kuvantunnistuksen tulokset",
                imageResultsSubtitle = "Ohjaa ML Kit -tunnisteiden näyttämistä etusivun korteilla. Poistaminen käytöstä piilottaa vain etusivun näkymän eikä vaikuta taustalla tehtävään analyysiin.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensyn tiedot"
            )
            AppLanguage.Greek -> AiCapabilitySettingsCopy(
                sectionTitle = "Λειτουργίες AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Αποτελέσματα αναγνώρισης εικόνας με AI",
                imageResultsSubtitle = "Ελέγχει τις ετικέτες ML Kit στις κάρτες της αρχικής. Η απενεργοποίηση κρύβει μόνο την εμφάνιση στην αρχική και δεν επηρεάζει την ανάλυση στο παρασκήνιο.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Πληροφορίες Lensy"
            )
            AppLanguage.Romanian -> AiCapabilitySettingsCopy(
                sectionTitle = "Funcții AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Rezultatele recunoașterii imaginilor cu AI",
                imageResultsSubtitle = "Controlează etichetele ML Kit de pe cardurile paginii principale. Dezactivarea ascunde doar afișarea pe pagina principală și nu afectează analiza din fundal.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Informații despre Lensy"
            )
            AppLanguage.Hungarian -> AiCapabilitySettingsCopy(
                sectionTitle = "MI-funkciók",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "MI-képfelismerési eredmények",
                imageResultsSubtitle = "A főoldali kártyákon megjelenő ML Kit-címkéket szabályozza. Kikapcsolva csak a főoldali megjelenítés tűnik el, a háttérelemzést nem érinti.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensy infó"
            )
            AppLanguage.Turkish -> AiCapabilitySettingsCopy(
                sectionTitle = "AI Özellikleri",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "AI Görsel Tanıma Sonuçları",
                imageResultsSubtitle = "Ana sayfa kartlarındaki ML Kit etiketlerini denetler. Kapatmak yalnızca ana sayfadaki görünümü gizler, arka plan analizini etkilemez.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensy bilgisi"
            )
            AppLanguage.Malay -> AiCapabilitySettingsCopy(
                sectionTitle = "Ciri AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "Hasil Pengecaman Imej AI",
                imageResultsSubtitle = "Mengawal label ML Kit pada kad laman utama. Mematikannya hanya menyembunyikan paparan laman utama dan tidak menjejaskan analisis latar belakang.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Maklumat Lensy"
            )
            AppLanguage.Hebrew -> AiCapabilitySettingsCopy(
                sectionTitle = "תכונות AI",
                showLensyEntry = false,
                lensySubtitle = "Lensy provides lightweight AI companion copy while you browse photos.",
                imageResultsTitle = "תוצאות זיהוי תמונות ב-AI",
                imageResultsSubtitle = "שולט בתוויות ML Kit בכרטיסי דף הבית. כיבוי מסתיר רק את התצוגה בדף הבית ואינו משפיע על הניתוח ברקע.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "מידע על Lensy"
            )
            AppLanguage.English -> AiCapabilitySettingsCopy(
                sectionTitle = "AI Features",
                showLensyEntry = false,
                lensySubtitle = "Lensy, your intelligent assistant, stays with you in real time and adds commentary while you browse photos.",
                imageResultsTitle = "AI Image Recognition Results",
                imageResultsSubtitle = "Controls ML Kit labels on homepage cards. Turning it off only hides the homepage display and does not affect background analysis.",
                lensyContentDescription = "Lensy",
                infoContentDescription = "Lensy info"
            )
        }
    }
}

@Composable
private fun AiCapabilityToggleRow(
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleLogoResId: Int? = null,
    titleContentDescription: String? = null,
    infoContentDescription: String? = null,
    onInfoClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val toggle = {
        if (hapticsAllowed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onCheckedChange(!checked)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = toggle)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (titleLogoResId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = titleLogoResId),
                        contentDescription = titleContentDescription,
                        modifier = Modifier.height(28.dp)
                    )
                    if (onInfoClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onInfoClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = infoContentDescription,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                if (hapticsAllowed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onCheckedChange(value)
            },
            modifier = Modifier.advancedTextureButton(shape = CircleShape),
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDark) MaterialTheme.colorScheme.background else Color.White,
                checkedTrackColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black,
                uncheckedThumbColor = if (isDark) MaterialTheme.colorScheme.background else Color.White,
                uncheckedTrackColor = if (isDark) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
            )
        )
    }
}

@Composable
fun AiCapabilitySettingsSection(
    lensyEnabled: Boolean,
    imageResultsEnabled: Boolean,
    onLensyEnabledChange: (Boolean) -> Unit,
    onImageResultsEnabledChange: (Boolean) -> Unit,
    onLensyInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    useContainer: Boolean = true
) {
    val copy = rememberAiCapabilitySettingsCopy()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (showTitle) {
            Text(
                text = copy.sectionTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (useContainer) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .advancedTextureCard(shape = RoundedCornerShape(16.dp))
            ) {
                AiCapabilitySettingsRows(
                    copy = copy,
                    lensyEnabled = lensyEnabled,
                    imageResultsEnabled = imageResultsEnabled,
                    onLensyEnabledChange = onLensyEnabledChange,
                    onImageResultsEnabledChange = onImageResultsEnabledChange,
                    onLensyInfoClick = onLensyInfoClick,
                    isDark = isDark
                )
            }
        } else {
            AiCapabilitySettingsRows(
                copy = copy,
                lensyEnabled = lensyEnabled,
                imageResultsEnabled = imageResultsEnabled,
                onLensyEnabledChange = onLensyEnabledChange,
                onImageResultsEnabledChange = onImageResultsEnabledChange,
                onLensyInfoClick = onLensyInfoClick,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun AiCapabilitySettingsRows(
    copy: AiCapabilitySettingsCopy,
    lensyEnabled: Boolean,
    imageResultsEnabled: Boolean,
    onLensyEnabledChange: (Boolean) -> Unit,
    onImageResultsEnabledChange: (Boolean) -> Unit,
    onLensyInfoClick: () -> Unit,
    isDark: Boolean
) {
    Column {
        if (copy.showLensyEntry) {
            AiCapabilityToggleRow(
                titleLogoResId = R.drawable.lensy_wordmark,
                subtitle = copy.lensySubtitle,
                checked = lensyEnabled,
                onCheckedChange = onLensyEnabledChange,
                titleContentDescription = copy.lensyContentDescription,
                infoContentDescription = copy.infoContentDescription,
                onInfoClick = onLensyInfoClick,
                isDark = isDark
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            )
        }
        AiCapabilityToggleRow(
            title = copy.imageResultsTitle,
            subtitle = copy.imageResultsSubtitle,
            checked = imageResultsEnabled,
            onCheckedChange = onImageResultsEnabledChange,
            isDark = isDark
        )
    }
}
