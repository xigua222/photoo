package com.voicelike.app

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import java.util.UUID
import kotlin.math.absoluteValue

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.voicelike.app.startup.StartupWelcomeTab

@Composable
fun WelcomeView(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    hasManageStorage: Boolean,
    onRequestManageStorage: () -> Unit,
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    upAction: SwipeAction,
    onLeftActionChange: (SwipeAction) -> Unit,
    onRightActionChange: (SwipeAction) -> Unit,
    onUpActionChange: (SwipeAction) -> Unit,
    startupDefaultTab: String,
    onStartupDefaultTabChange: (String) -> Unit,
    deletionMode: String,
    onDeletionModeChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    val strings = LocalAppStrings.current
    val preferenceCopy = onboardingPreferenceCopy(currentLanguage)
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val maxStep = 6 // Intro, Lang, Agreement, Permission, Usage Preference, Swipe Preferences, Deletion Mode
    var isAgreementChecked by remember { mutableStateOf(false) }
    var hasSkippedPermissionRequest by remember { mutableStateOf(false) }
    var hasSkippedManageStorageRequest by remember { mutableStateOf(false) }
    var showPermissionSkipDialog by remember { mutableStateOf(false) }
    val onboardingSkipLabel = welcomeSkipLabel(currentLanguage)
    val canContinuePastPermissionStep = when {
        !hasPermission -> hasSkippedPermissionRequest
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStorage -> hasSkippedManageStorageRequest
        else -> true
    }
    val permissionPrimaryActionLabel = when {
        step == 3 && !canContinuePastPermissionStep && !hasPermission -> strings.onboardingPermissionButton
        step == 3 && !canContinuePastPermissionStep && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStorage -> strings.manageFilesPermission
        step >= maxStep + 1 -> strings.onboardingStartButton
        else -> strings.onboardingNext
    }

    if (showPermissionSkipDialog) {
        val permissionSkipMessage = when (currentLanguage) {
            AppLanguage.SimplifiedChinese ->
                "暂未授权，基础功能可能无法正常使用，依然继续。后续当您使用照片整理、压缩、移动等需要访问媒体的功能时，我们会再说明用途并向您申请相关权限。"
            AppLanguage.TraditionalChinese ->
                "暫未授權，基礎功能可能無法正常使用，依然繼續。後續當您使用照片整理、壓縮、移動等需要存取媒體的功能時，我們會再說明用途並向您申請相關權限。"
            AppLanguage.Japanese ->
                "まだ許可されていません。一部のメディア機能が利用できない可能性がありますが、続けることができます。メディアへのアクセスが必要な機能を使用する際に、その理由を説明し、許可をお願いします。"
            AppLanguage.Korean ->
                "아직 권한이 부여되지 않았습니다. 일부 핵심 미디어 기능을 사용할 수 없지만 계속할 수 있습니다. 미디어 접근이 필요한 기능을 사용할 때 다시 설명하고 권한을 요청합니다."
            AppLanguage.French ->
                "Pas encore autorisé. Certaines fonctionnalités média peuvent ne pas être disponibles, mais vous pouvez continuer. Nous expliquerons pourquoi et demanderons uniquement lorsque vous utiliserez une fonction nécessitant l'accès aux médias."
            AppLanguage.German ->
                "Noch nicht erteilt. Einige Kern-Medienfunktionen sind möglicherweise nicht verfügbar, Sie können aber fortfahren. Wir erklären warum und fragen nur, wenn Sie eine Funktion verwenden, die Medienzugriff benötigt."
            AppLanguage.Italian ->
                "Non ancora concesso. Alcune funzionalità multimediali potrebbero non essere disponibili, ma puoi continuare. Spiegheremo il perché e chiederemo solo quando userai una funzione che richiede l'accesso ai media."
            AppLanguage.Spanish ->
                "Aún no concedido. Algunas funciones principales de medios pueden no estar disponibles, pero puede continuar. Explicaremos por qué y solicitaremos solo cuando use una función que necesite acceso a medios."
            AppLanguage.Portuguese ->
                "Ainda não concedido. Algumas funções principais de mídia podem não estar disponíveis, mas você pode continuar. Explicaremos o porquê e solicitaremos apenas quando você usar uma função que precise de acesso à mídia."
            AppLanguage.Russian ->
                "Еще не предоставлено. Некоторые основные медиа-функции могут быть недоступны, но вы можете продолжить. Мы объясним почему и попросим только когда вы будете использовать функцию, требующую доступа к медиа."
            AppLanguage.Thai ->
                "ยังไม่ได้รับอนุญาต บางฟีเจอร์หลักของสื่ออาจไม่พร้อมใช้งาน แต่คุณสามารถดำเนินการต่อได้ เราจะอธิบายเหตุผลและขออนุญาตเมื่อคุณใช้ฟีเจอร์ที่ต้องการเข้าถึงสื่อ"
            AppLanguage.Vietnamese ->
                "Chưa được cấp quyền. Một số tính năng phương tiện chính có thể không khả dụng, nhưng bạn có thể tiếp tục. Chúng tôi sẽ giải thích lý do và yêu cầu khi bạn sử dụng tính năng cần truy cập phương tiện."
            AppLanguage.Indonesian ->
                "Belum diberikan izin. Beberapa fitur media utama mungkin tidak tersedia, tetapi Anda bisa melanjutkan. Kami akan menjelaskan alasannya dan meminta izin hanya saat Anda menggunakan fitur yang memerlukan akses media."
            AppLanguage.Arabic ->
                "لم يتم منح الإذن بعد. قد لا تتوفر بعض الوظائف الأساسية للوسائط، ولكن يمكنك المتابعة. سنشرح السبب ونطلب الإذن فقط عند استخدام ميزة تتطلب الوصول إلى الوسائط."
            AppLanguage.Ukrainian ->
                "Доступ ще не надано. Деякі основні медіафункції можуть бути недоступні, але ви можете продовжити. Ми поясним причину й запитаємо дозвіл лише тоді, коли ви скористаєтеся функцією, якій потрібен доступ до медіа."
            AppLanguage.Polish ->
                "Dostęp nie został jeszcze przyznany. Niektóre podstawowe funkcje multimedialne mogą być niedostępne, ale możesz kontynuować. Wyjaśnimy dlaczego i poprosimy o zgodę dopiero wtedy, gdy użyjesz funkcji wymagającej dostępu do multimediów."
            AppLanguage.Czech ->
                "Přístup zatím nebyl udělen. Některé základní mediální funkce nemusí být dostupné, ale můžete pokračovat. Vysvětlíme proč a požádáme o oprávnění, až použijete funkci, která přístup k médiím potřebuje."
            AppLanguage.Dutch ->
                "Nog geen toegang verleend. Sommige belangrijke mediafuncties zijn mogelijk niet beschikbaar, maar je kunt doorgaan. We leggen uit waarom en vragen alleen toestemming wanneer je een functie gebruikt die toegang tot media nodig heeft."
            AppLanguage.Swedish ->
                "Åtkomst har inte beviljats ännu. Vissa centrala mediefunktioner kanske inte är tillgängliga, men du kan fortsätta. Vi förklarar varför och frågar först när du använder en funktion som behöver åtkomst till media."
            AppLanguage.Danish ->
                "Adgang er ikke givet endnu. Nogle centrale mediefunktioner er måske ikke tilgængelige, men du kan fortsætte. Vi forklarer hvorfor og spørger først, når du bruger en funktion, der kræver adgang til medier."
            AppLanguage.Norwegian ->
                "Tilgang er ikke gitt ennå. Noen sentrale mediefunksjoner kan være utilgjengelige, men du kan fortsette. Vi forklarer hvorfor og spør først når du bruker en funksjon som trenger tilgang til medier."
            AppLanguage.Finnish ->
                "Käyttöoikeutta ei ole vielä annettu. Jotkin median perustoiminnot eivät ehkä ole käytettävissä, mutta voit jatkaa. Kerromme syyn ja pyydämme lupaa vasta, kun käytät toimintoa, joka tarvitsee pääsyn mediaan."
            AppLanguage.Greek ->
                "Δεν έχει δοθεί ακόμη άδεια. Ορισμένες βασικές λειτουργίες πολυμέσων ενδέχεται να μην είναι διαθέσιμες, αλλά μπορείτε να συνεχίσετε. Θα εξηγήσουμε τον λόγο και θα ζητήσουμε άδεια μόνο όταν χρησιμοποιήσετε λειτουργία που χρειάζεται πρόσβαση στα πολυμέσα."
            AppLanguage.Romanian ->
                "Accesul nu a fost acordat încă. Unele funcții media de bază ar putea fi indisponibile, dar puteți continua. Vă vom explica motivul și vom cere permisiunea doar când folosiți o funcție care necesită acces la media."
            AppLanguage.Hungarian ->
                "A hozzáférés még nincs megadva. Néhány alapvető médiafunkció nem biztos, hogy elérhető, de folytathatja. Elmagyarázzuk az okot, és csak akkor kérünk engedélyt, ha olyan funkciót használ, amelyhez médiahozzáférés kell."
            AppLanguage.Turkish ->
                "Henüz izin verilmedi. Bazı temel medya özellikleri kullanılamayabilir ama devam edebilirsin. Nedenini açıklayacağız ve yalnızca medya erişimi gerektiren bir özelliği kullandığında izin isteyeceğiz."
            AppLanguage.Malay ->
                "Belum diberi akses. Beberapa ciri media teras mungkin tidak tersedia, tetapi anda boleh teruskan. Kami akan menerangkan sebabnya dan hanya meminta apabila anda menggunakan ciri yang memerlukan akses media."
            AppLanguage.Hebrew ->
                "ההרשאה עדיין לא ניתנה. ייתכן שחלק מתכונות המדיה הבסיסיות לא יהיו זמינות, אך אפשר להמשיך. נסביר מדוע ונבקש הרשאה רק כשמשתמשים בתכונה שזקוקה לגישה למדיה."
            else ->
                "Not granted yet. Some core media features may be unavailable, but you can continue. We'll explain why and ask only when you use a feature that needs media access."
        }
        val confirmSkipLabel = welcomeSkipConfirmLabel(currentLanguage)
        AlertDialog(
            onDismissRequest = { showPermissionSkipDialog = false },
            title = { Text(onboardingSkipLabel) },
            text = { Text(permissionSkipMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSkipDialog = false
                        if (!hasPermission) {
                            hasSkippedPermissionRequest = true
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStorage) {
                            hasSkippedManageStorageRequest = true
                        }
                        if (step < maxStep + 1) {
                            step++
                        }
                    }
                ) {
                    Text(confirmSkipLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSkipDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepIndicator(
                current = step,
                total = maxStep + 1,
                modifier = Modifier
            )
            if (step == 3 && !canContinuePastPermissionStep) {
                TextButton(
                    onClick = { showPermissionSkipDialog = true },
                    modifier = Modifier
                        .height(36.dp)
                ) {
                    Text(
                        text = onboardingSkipLabel,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * direction } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { target ->
            when (target) {
                0 -> WelcomeIntroCard(
                    title = strings.onboardingWelcomeTitle,
                    subtitle = strings.onboardingWelcomeSubtitle
                )
                1 -> WelcomeLanguageStep(
                    title = strings.onboardingLanguageTitle,
                    subtitle = strings.onboardingLanguageSubtitle,
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
                2 -> WelcomeAgreementStep(
                    currentLanguage = currentLanguage,
                    onAgreementChanged = { agreed -> 
                        isAgreementChecked = agreed
                    },
                    isChecked = isAgreementChecked
                )
                3 -> WelcomePermissionStep(
                    title = if (!hasPermission) strings.onboardingPermissionTitle else strings.manageFilesPermission,
                    subtitle = if (!hasPermission) strings.onboardingPermissionSubtitle else strings.manageFilesPermissionDesc,
                    hasPermission = hasPermission,
                    hasManageStorage = hasManageStorage,
                    canContinue = canContinuePastPermissionStep
                )
                4 -> WelcomeUsagePreferenceStep(
                    currentLanguage = currentLanguage,
                    startupDefaultTab = startupDefaultTab,
                    onStartupDefaultTabChange = onStartupDefaultTabChange
                )
                5 -> WelcomeSwipeStep(
                    title = preferenceCopy.organizeSwipeTitle,
                    subtitle = preferenceCopy.organizeSwipeSubtitle,
                    leftAction = leftAction,
                    rightAction = rightAction,
                    upAction = upAction,
                    onLeftActionChange = onLeftActionChange,
                    onRightActionChange = onRightActionChange,
                    onUpActionChange = onUpActionChange
                )
                6 -> WelcomeDeletionModeStep(
                    title = preferenceCopy.organizeDeleteTitle,
                    deletionMode = deletionMode,
                    onDeletionModeChange = onDeletionModeChange
                )
                else -> WelcomeStartCard(
                    title = strings.onboardingStartTitle,
                    subtitle = strings.onboardingStartSubtitle
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (step > 0) {
                TextButton(
                    onClick = { step = (step - 1).coerceAtLeast(0) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(44.dp)
                        .bounceClick(onClick = { step = (step - 1).coerceAtLeast(0) })
                ) {
                    Text(strings.onboardingBack, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Button(
                onClick = {
                    if (step == 3 && !canContinuePastPermissionStep) {
                        if (!hasPermission) {
                            onRequestPermission()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStorage) {
                            onRequestManageStorage()
                        }
                    } else if (step < maxStep + 1) {
                        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        step++
                    } else {
                        onFinish()
                    }
                },
                enabled = when (step) {
                    2 -> isAgreementChecked
                    else -> true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground else Color(0xFF111827),
                    contentColor = if (isSystemInDarkTheme()) Color.Black else Color.White,
                    disabledContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f) else Color(0xFFE5E7EB),
                    disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(48.dp)
                    .advancedTextureButton(shape = RoundedCornerShape(24.dp))
                    .bounceClick(onClick = {
                        if (step == 3 && !canContinuePastPermissionStep) {
                            if (!hasPermission) {
                                onRequestPermission()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStorage) {
                                onRequestManageStorage()
                            }
                        } else if (step < maxStep + 1) {
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            step++
                        } else {
                            onFinish()
                        }
                    })
            ) {
                Text(
                    text = permissionPrimaryActionLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            val width = if (isActive) 24.dp else 8.dp
            val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun WelcomeIntroCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start // Changed from implicit default (Start) to explicit Start for clarity, or just ensure Image aligns Start
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = (-24).dp) // Move logo left to compensate for built-in padding
                    .align(Alignment.Start)
                    .then(staggeredRevealModifier(0))
            )
            // No Spacer needed if we want it tighter, but keep spacing.
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.Start) // Explicitly align to start
                    .then(staggeredRevealModifier(1))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 20.sp,
                modifier = Modifier
                    .align(Alignment.Start) // Explicitly align to start
                    .then(staggeredRevealModifier(2))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WelcomeLanguageStep(
    title: String,
    subtitle: String,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    // Generate a unique ID for this view session to control animation reset
    val pageId = remember { UUID.randomUUID().toString() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.then(staggeredRevealModifier(0, key = pageId + "title"))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.then(staggeredRevealModifier(1, key = pageId + "subtitle"))
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val colors = listOf(
                Color(0xFFFFD700),
                Color(0xFFFF6B6B),
                Color(0xFF4ECDC4),
                Color(0xFF45B7D1),
                Color(0xFF96CEB4),
                Color(0xFFFFBE76),
                Color(0xFFDFF9FB)
            )
            val sortedLanguages = remember(currentLanguage) {
                 AppLanguage.values().sortedByDescending { it == currentLanguage }
            }
            val listState = rememberLazyListState()

            // Scroll to top when language changes (so the user sees the selected item at top)
            LaunchedEffect(currentLanguage) {
                listState.scrollToItem(0)
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(sortedLanguages, key = { _, lang -> lang.name }) { index, lang ->
                    val isSelected = lang == currentLanguage
                    // Use stable color based on language ordinal, not list index
                    val colorIndex = lang.ordinal.absoluteValue % colors.size
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors[colorIndex])
                            .bounceClick(onClick = { onLanguageSelected(lang) })
                            // Only apply reveal animation for the first few items to prevent "scroll reveal" effect
                            // Use pageId + lang.name as key to ensure animation resets on page re-entry but persists on scroll
                            .then(staggeredRevealModifier(
                                index = index + 2, 
                                key = pageId + lang.name,
                                skipAnimation = index > 8 // Only animate top items
                            ))
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = lang.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(26.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeAgreementStep(
    currentLanguage: AppLanguage,
    onAgreementChanged: (Boolean) -> Unit,
    isChecked: Boolean
) {
    val strings = LocalAppStrings.current
    val uriHandler = LocalUriHandler.current
    val termsUrl = remember(currentLanguage) { userAgreementUrlFor(currentLanguage) }
    val privacyUrl = remember(currentLanguage) { privacyPolicyUrlFor(currentLanguage) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = (-24).dp)
                    .align(Alignment.Start)
                    .then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = strings.termsOfService + " & " + strings.privacyPolicy,
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.then(staggeredRevealModifier(1))
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Links
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                 TextButton(onClick = { 
                    uriHandler.openUri(termsUrl)
                }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                   modifier = Modifier
                       .height(32.dp)
                       .then(staggeredRevealModifier(2))) {
                    Text(strings.termsOfService, fontSize = 14.sp, color = Color(0xFF3B82F6))
                }
                 
                TextButton(onClick = { 
                    uriHandler.openUri(privacyUrl)
                }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                   modifier = Modifier
                       .height(32.dp)
                       .then(staggeredRevealModifier(3))) {
                    Text(strings.privacyPolicy, fontSize = 14.sp, color = Color(0xFF3B82F6))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAgreementChanged(!isChecked) }
                    .then(staggeredRevealModifier(4))
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onAgreementChanged(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground else Color(0xFF111827),
                        uncheckedColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.agreeToTerms,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomePermissionStep(
    title: String,
    subtitle: String,
    hasPermission: Boolean,
    hasManageStorage: Boolean,
    canContinue: Boolean
) {
    val strings = LocalAppStrings.current
    val showManageStorage = hasPermission && !hasManageStorage
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.then(staggeredRevealModifier(0))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.then(staggeredRevealModifier(1))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!showManageStorage) {
                        PrivacySummaryRow(
                            title = strings.privacySection1Title,
                            content = strings.privacySection1Content,
                            icon = Icons.Outlined.Storage,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            iconColor = Color(0xFF4B5563),
                            modifier = Modifier.then(staggeredRevealModifier(2))
                        )
                        PrivacySummaryRow(
                            title = strings.privacySection2Title,
                            content = strings.privacySection2Content,
                            icon = Icons.Outlined.Security,
                            color = Color(0xFFDCFCE7),
                            iconColor = Color(0xFF16A34A),
                            modifier = Modifier.then(staggeredRevealModifier(3))
                        )
                        PrivacySummaryRow(
                            title = strings.privacySection3Title,
                            content = strings.privacySection3Content,
                            icon = Icons.Outlined.Lock,
                            color = Color(0xFFE0F2FE),
                            iconColor = Color(0xFF0284C7),
                            modifier = Modifier.then(staggeredRevealModifier(4))
                        )
                    } else {
                        PrivacySummaryRow(
                            title = strings.manageFilesPermission,
                            content = strings.manageFilesPermissionDesc,
                            icon = Icons.Outlined.Storage,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            iconColor = Color(0xFF4B5563),
                            modifier = Modifier.then(staggeredRevealModifier(2))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeUsagePreferenceStep(
    currentLanguage: AppLanguage,
    startupDefaultTab: String,
    onStartupDefaultTabChange: (String) -> Unit
) {
    val copy = onboardingPreferenceCopy(currentLanguage)
    val normalizedTab = when (startupDefaultTab) {
        StartupWelcomeTab.ColorCard.name -> StartupWelcomeTab.ColorCard.name
        else -> StartupWelcomeTab.Organize.name
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = copy.usagePreferenceTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = copy.usagePreferenceSubtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                lineHeight = 18.sp,
                modifier = Modifier.then(staggeredRevealModifier(1))
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                UsagePreferenceOptionCard(
                    title = copy.organizePreferredTitle,
                    subtitle = copy.organizePreferredSubtitle,
                    accentColor = Color(0xFF2563EB),
                    isSelected = normalizedTab == StartupWelcomeTab.Organize.name,
                    modifier = Modifier.then(staggeredRevealModifier(2)),
                    onClick = { onStartupDefaultTabChange(StartupWelcomeTab.Organize.name) }
                )
                UsagePreferenceOptionCard(
                    title = copy.creativePreferredTitle,
                    subtitle = copy.creativePreferredSubtitle,
                    accentColor = Color(0xFFDB2777),
                    isSelected = normalizedTab == StartupWelcomeTab.ColorCard.name,
                    modifier = Modifier.then(staggeredRevealModifier(3)),
                    onClick = { onStartupDefaultTabChange(StartupWelcomeTab.ColorCard.name) }
                )
            }
        }
    }
}

@Composable
private fun UsagePreferenceOptionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = when {
        isSelected && isDark -> MaterialTheme.colorScheme.surfaceVariant
        isSelected -> accentColor.copy(alpha = 0.10f)
        isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val borderColor = when {
        isSelected && isDark -> accentColor.copy(alpha = 0.7f)
        isSelected -> accentColor.copy(alpha = 0.35f)
        isDark -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(188.dp)
            .bounceClick(onClick = onClick)
            .advancedTextureCard(shape = RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = subtitle,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun WelcomeStartCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .offset(x = (-16).dp)
                    .align(Alignment.Start)
                    .then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.then(staggeredRevealModifier(1))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 20.sp,
                modifier = Modifier.then(staggeredRevealModifier(2))
            )
        }
    }
}

private fun welcomeSkipLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese -> "跳过"
        AppLanguage.TraditionalChinese -> "跳過"
        AppLanguage.Japanese -> "スキップ"
        AppLanguage.Korean -> "건너뛰기"
        AppLanguage.French -> "Passer"
        AppLanguage.German -> "Überspringen"
        AppLanguage.Spanish -> "Omitir"
        AppLanguage.Italian -> "Salta"
        AppLanguage.Portuguese -> "Pular"
        AppLanguage.Russian -> "Пропустить"
        AppLanguage.Thai -> "ข้าม"
        AppLanguage.Vietnamese -> "Bỏ qua"
        AppLanguage.Indonesian -> "Lewati"
        AppLanguage.Arabic -> "تخطي"
        AppLanguage.Ukrainian -> "Пропустити"
        AppLanguage.Polish -> "Pomiń"
        AppLanguage.Czech -> "Přeskočit"
        AppLanguage.Dutch -> "Overslaan"
        AppLanguage.Swedish -> "Hoppa över"
        AppLanguage.Danish -> "Spring over"
        AppLanguage.Norwegian -> "Hopp over"
        AppLanguage.Finnish -> "Ohita"
        AppLanguage.Greek -> "Παράλειψη"
        AppLanguage.Romanian -> "Omite"
        AppLanguage.Hungarian -> "Kihagyás"
        AppLanguage.Turkish -> "Atla"
        AppLanguage.Malay -> "Langkau"
        AppLanguage.Hebrew -> "דילוג"
        else -> "Skip"
    }
}

private fun welcomeSkipConfirmLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SimplifiedChinese -> "依然跳过"
        AppLanguage.TraditionalChinese -> "依然跳過"
        AppLanguage.Japanese -> "このままスキップ"
        AppLanguage.Korean -> "계속 건너뛰기"
        AppLanguage.French -> "Ignorer quand même"
        AppLanguage.German -> "Trotzdem überspringen"
        AppLanguage.Spanish -> "Omitir de todos modos"
        AppLanguage.Italian -> "Salta comunque"
        AppLanguage.Portuguese -> "Pular assim mesmo"
        AppLanguage.Russian -> "Все равно пропустить"
        AppLanguage.Thai -> "ข้ามต่อไป"
        AppLanguage.Vietnamese -> "Vẫn bỏ qua"
        AppLanguage.Indonesian -> "Tetap lewati"
        AppLanguage.Arabic -> "تخطَّ على أي حال"
        AppLanguage.Ukrainian -> "Усе одно пропустити"
        AppLanguage.Polish -> "Pomiń mimo to"
        AppLanguage.Czech -> "Přesto přeskočit"
        AppLanguage.Dutch -> "Toch overslaan"
        AppLanguage.Swedish -> "Hoppa över ändå"
        AppLanguage.Danish -> "Spring over alligevel"
        AppLanguage.Norwegian -> "Hopp over likevel"
        AppLanguage.Finnish -> "Ohita silti"
        AppLanguage.Greek -> "Παράλειψη παρ' όλα αυτά"
        AppLanguage.Romanian -> "Omite oricum"
        AppLanguage.Hungarian -> "Kihagyás mégis"
        AppLanguage.Turkish -> "Yine de atla"
        AppLanguage.Malay -> "Langkau juga"
        AppLanguage.Hebrew -> "לדלג בכל זאת"
        else -> "Skip anyway"
    }
}

@Composable
private fun PrivacySummaryRow(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val rowColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else color
    val rowIconColor = if (isDark) MaterialTheme.colorScheme.onBackground else iconColor
    val titleColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
    val contentColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = rowIconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)
            Text(
                text = content,
                fontSize = 11.sp,
                color = contentColor,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun WelcomeDeletionModeStep(
    title: String,
    deletionMode: String,
    onDeletionModeChange: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    val isDark = isSystemInDarkTheme()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize().advancedTextureCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.then(staggeredRevealModifier(0))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = newPageStrings.deletionModeReassurance,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.then(staggeredRevealModifier(1))
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .then(staggeredRevealModifier(2)),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recycle Bin Mode Card (Left)
                DeletionModeVerticalCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = strings.recycleBinModeTitle,
                    description = strings.recycleBinModeDesc,
                    icon = Icons.Outlined.RestoreFromTrash,
                    isSelected = deletionMode == "recycle_bin",
                    onClick = { onDeletionModeChange("recycle_bin") },
                    isDark = isDark,
                    accentColor = Color(0xFF2196F3), // Blue
                    isRecommended = true
                )

                // Direct Delete Mode Card (Right)
                DeletionModeVerticalCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = strings.directDeleteModeTitle,
                    description = strings.directDeleteModeDesc,
                    icon = Icons.Outlined.DeleteForever,
                    isSelected = deletionMode == "direct_delete",
                    onClick = { onDeletionModeChange("direct_delete") },
                    isDark = isDark,
                    accentColor = Color(0xFFEF4444) // Red
                )
            }
        }
    }
}

