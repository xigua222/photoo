package com.voicelike.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val campfireXhsUrl = "https://www.xiaohongshu.com/user/profile/61ecf3ca000000001000b54a"
private const val campfireXhsHandle = "@超级西瓜"
private const val campfireEmail = "photoo@superwatermelon.cn"
private const val campfireQrAssetPath = "file:///android_asset/campfire_supporters/alipay_qr.webp"
private const val campfireRewardQrAssetPath = "file:///android_asset/campfire_supporters/reward_qr.webp"
private const val campfireFeedbackFormUrl = "https://itlueqqx8t.feishu.cn/share/base/form/shrcnciUs1aGv7F64DbkABIuLjg"
private const val campfireSupportersPerRow = 7
private const val campfireSponsorAmount = "29.90"
private const val campfireKofiUrl = "https://ko-fi.com/s/250cf79768"
private const val campfireKofiPriceUsd = 9.99
private const val campfireQrExportImageFolder = "Pictures/Photoo/Campfire"
private const val startupMineProfilePrefsName = "startup_mine_profile_prefs"
private const val startupMineUserDisplayNameKey = "user_display_name"
private const val startupMineDefaultUserName = "\u795E\u79D8\u7528\u6237"

private fun campfireText(
    language: AppLanguage,
    simplified: String,
    traditional: String,
    english: String
): String = when (language) {
    AppLanguage.SimplifiedChinese -> simplified
    AppLanguage.TraditionalChinese -> traditional
    else -> english
}

private fun getKofiPriceText(language: AppLanguage): String {
    val usdPrice = campfireKofiPriceUsd
    return when (language) {
        AppLanguage.Japanese -> "USD $usdPrice (約${(usdPrice * 160).toInt()}円)"
        AppLanguage.Korean -> "USD $usdPrice (약 ${(usdPrice * 1400).toInt()}원)"
        AppLanguage.French, AppLanguage.German, AppLanguage.Italian, AppLanguage.Spanish, AppLanguage.Portuguese -> 
            "USD $usdPrice (approx. €${String.format("%.2f", usdPrice * 0.92)})"
        AppLanguage.Russian -> "USD $usdPrice (примерно ${(usdPrice * 95).toInt()} ₽)"
        AppLanguage.Thai -> "USD $usdPrice (ประมาณ ${(usdPrice * 36).toInt()} ฿)"
        AppLanguage.Vietnamese -> "USD $usdPrice (khoảng ${(usdPrice * 25000).toInt()} ₫)"
        AppLanguage.Indonesian -> "USD $usdPrice (sekitar ${(usdPrice * 16000).toInt()} Rp)"
        AppLanguage.Arabic -> "USD $usdPrice (حوالي ${(usdPrice * 3.75).toInt()} ر.س)"
        AppLanguage.Ukrainian -> "USD $usdPrice (приблизно ${(usdPrice * 41).toInt()} ₴)"
        AppLanguage.Polish -> "USD $usdPrice (ok. ${(usdPrice * 4).toInt()} zł)"
        AppLanguage.Czech -> "USD $usdPrice (přibližně ${(usdPrice * 23).toInt()} Kč)"
        AppLanguage.Dutch -> "USD $usdPrice (ca. €${String.format("%.2f", usdPrice * 0.92)})"
        AppLanguage.Swedish -> "USD $usdPrice (ca ${(usdPrice * 10.5).toInt()} kr)"
        AppLanguage.Danish -> "USD $usdPrice (ca. ${(usdPrice * 6.9).toInt()} kr.)"
        AppLanguage.Norwegian -> "USD $usdPrice (ca. ${(usdPrice * 10.5).toInt()} kr)"
        AppLanguage.Finnish -> "USD $usdPrice (noin ${String.format("%.2f", usdPrice * 0.92)} €)"
        AppLanguage.Greek -> "USD $usdPrice (περίπου ${String.format("%.2f", usdPrice * 0.92)} €)"
        AppLanguage.Romanian -> "USD $usdPrice (aprox. ${(usdPrice * 4.6).toInt()} lei)"
        AppLanguage.Hungarian -> "USD $usdPrice (kb. ${(usdPrice * 360).toInt()} Ft)"
        AppLanguage.Turkish -> "USD $usdPrice (yaklaşık ${(usdPrice * 34).toInt()} ₺)"
        AppLanguage.Malay -> "USD $usdPrice (kira-kira RM${(usdPrice * 4.4).toInt()})"
        AppLanguage.Hebrew -> "USD $usdPrice (כ-${(usdPrice * 3.7).toInt()} ₪)"
        AppLanguage.TraditionalChinese -> "USD $usdPrice (約 NT$${(usdPrice * 32).toInt()})"
        else -> "USD $usdPrice"
    }
}

private data class CampfireStrings(
    val supportersTitle: String,
    val supportersNote: String,
    val sponsorTitle: String,
    val shareTitle: String,
    val sponsorSummary: String,
    val shareSummary: String,
    val feedbackSummary: String,
    val sponsorBody: String,
    val sponsorButtonLabel: String,
    val sponsorButtonHint: String,
    val sponsorStatusChecking: String,
    val sponsorStatusPaidPrefix: String,
    val sponsorStatusPending: String,
    val sponsorStatusFailed: String,
    val sponsorStatusUnconfigured: String,
    val sponsorLaunchFailed: String,
    val sponsorRefreshLabel: String
)

private fun getCampfireStrings(language: AppLanguage): CampfireStrings {
    val kofiPriceText = getKofiPriceText(language)
    return when (language) {
        AppLanguage.SimplifiedChinese -> CampfireStrings(
            supportersTitle = "感谢这些用户/媒体",
            supportersNote = "（排名不分先后，不区分赞助/分享/反馈计划）",
            sponsorTitle = "赞助 photoo",
            shareTitle = "分享 photoo",
            sponsorSummary = "请开发者喝一杯奶茶，支持维护与开发。",
            shareSummary = "把 photoo 分享给更多人，帮助它走得更远。",
            feedbackSummary = "有哪里好用，哪里还不够顺手，直接告诉开发者。",
            sponsorBody = """
                photoo是一款纯粹的图片管理工具，承诺永久无广告，核心功能永远免费。

                如果这个小工具为你带来了一点便利、回忆或快乐。欢迎请开发者喝一杯奶茶，你的支持将全部用于软件维护与功能开发，帮助photoo走得更远。

                若您的赞助金额大于等于29.9元，您将在未来版本被列入photoo篝火计划致谢墙，如希望名字出现在致谢名单，请在转账备注中留下称呼；未备注将默认以「匿名」致谢。
            """.trimIndent(),
            sponsorButtonLabel = "赞助 ¥29.9",
            sponsorButtonHint = "将跳转支付宝收银台完成固定金额赞助。",
            sponsorStatusChecking = "正在确认支付结果…",
            sponsorStatusPaidPrefix = "赞助成功，感谢支持。",
            sponsorStatusPending = "订单已创建，请在支付宝完成支付。",
            sponsorStatusFailed = "支付尚未完成，你可以稍后再试一次。",
            sponsorStatusUnconfigured = "尚未配置支付服务地址，请先在构建配置中填写 campfirePayBaseUrl。",
            sponsorLaunchFailed = "拉起支付失败，请稍后重试。",
            sponsorRefreshLabel = "刷新支付状态"
        )
        AppLanguage.TraditionalChinese -> CampfireStrings(
            supportersTitle = "感謝這些用戶/媒體",
            supportersNote = "（排名不分先後，不區分贊助/分享/回饋計畫）",
            sponsorTitle = "贊助 photoo",
            shareTitle = "分享 photoo",
            sponsorSummary = "請開發者喝一杯奶茶，支援維護與開發。",
            shareSummary = "把 photoo 分享給更多人，幫助它走得更遠。",
            feedbackSummary = "有哪裡好用，哪裡還不夠順手，直接告訴開發者。",
            sponsorBody = """
                photoo是一款純粹的圖片管理工具，承諾永久無廣告。

                如果這個小工具為你帶來了一點便利、回憶或快樂。歡迎請開發者喝一杯奶茶，你的支持將全部用於軟體維護與功能開發，幫助photoo走得更遠。

                若您的贊助金額大於等於29.9元，您將在未來版本被列入photoo篝火計畫致謝牆，如希望名字出現在致謝名單，請在轉帳備註中留下稱呼；未備註將預設以「匿名」致謝。
            """.trimIndent(),
            sponsorButtonLabel = "贊助 ¥29.9",
            sponsorButtonHint = "將跳轉支付寶收銀台完成固定金額贊助。",
            sponsorStatusChecking = "正在確認支付結果…",
            sponsorStatusPaidPrefix = "贊助成功，感謝支持。",
            sponsorStatusPending = "訂單已建立，請在支付寶完成支付。",
            sponsorStatusFailed = "支付尚未完成，你可以稍後再試一次。",
            sponsorStatusUnconfigured = "尚未配置支付服務位址，請先在建構設定中填寫 campfirePayBaseUrl。",
            sponsorLaunchFailed = "拉起支付失敗，請稍後重試。",
            sponsorRefreshLabel = "重新整理支付狀態"
        )
        AppLanguage.Japanese -> CampfireStrings(
            supportersTitle = "スペシャルサンクス / メディア",
            supportersNote = "順不同。スポンサー、シェア支援者、フィードバック貢献者がまとめて表示されます。",
            sponsorTitle = "photooを支援",
            shareTitle = "photooをシェア",
            sponsorSummary = "開発者にミルクティーを奢って、開発とメンテナンスを応援しましょう。",
            shareSummary = "photooをより多くの人にシェアして、さらに広めましょう。",
            feedbackSummary = "使いやすい点や改善点を、開発者に直接お知らせください。",
            sponsorBody = """
                photooは、シンプルで広告のない写真管理ツールです。

                この小さなツールが少しでも便利で、思い出や喜びをもたらしたなら、開発者にミルクティーを奢ってみてください。あなたの支援はメンテナンスと新機能開発に直接活かされ、photooの未来を支えます。

                ${kofiPriceText}以上のご支援で、今後のバージョンでphotooキャンプファイヤー感謝ウォールに掲載されます。お名前を掲載希望の場合は、送金時の備考欄にお名前をご記入ください。未記入の場合は「匿名」として感謝いたします。
            """.trimIndent(),
            sponsorButtonLabel = "¥29.9で支援",
            sponsorButtonHint = "Alipayの決済ページを開き、固定金額で支援します。",
            sponsorStatusChecking = "支払い結果を確認中…",
            sponsorStatusPaidPrefix = "ご支援ありがとうございます。",
            sponsorStatusPending = "注文が作成されました。Alipayで支払いを完了してください。",
            sponsorStatusFailed = "支払いがまだ完了していません。後でもう一度お試しください。",
            sponsorStatusUnconfigured = "決済サービスURLが設定されていません。campfirePayBaseUrlを設定してください。",
            sponsorLaunchFailed = "支払いを開始できませんでした。後でもう一度お試しください。",
            sponsorRefreshLabel = "支払い状況を更新"
        )
        AppLanguage.Korean -> CampfireStrings(
            supportersTitle = "특별한 감사 / 미디어",
            supportersNote = "순서 무관. 스폰서, 공유 지지자, 피드백 기여자가 함께 표시됩니다.",
            sponsorTitle = "photoo 후원하기",
            shareTitle = "photoo 공유하기",
            sponsorSummary = "개발자에게 밀크티를 사주고, 개발과 유지보수를 응원하세요.",
            shareSummary = "photoo를 더 많은 사람에게 공유하고, 더 널리 퍼뜨리세요.",
            feedbackSummary = "편리한 점이나 개선할 점을 개발자에게 직접 알려주세요.",
            sponsorBody = """
                photoo는 광고 없는 깔끔한 사진 관리 도구입니다.

                이 작은 도구가 조금이라도 편리하고, 추억이나 기쁨을 주었다면 개발자에게 밀크티를 사주세요. 여러분의 지원은 유지보수와 새로운 기능 개발에 직접 사용되어 photoo가 더 멀리 나아갈 수 있도록 도와줍니다.

                ${kofiPriceText} 이상 후원하시면 향후 버전에서 photoo 캠프파이어 감사의 벽에 이름이 올라갑니다. 이름을 표시하고 싶으시다면 이체 메모에 이름을 남겨주세요. 미기재 시 '익명'으로 감사드립니다.
            """.trimIndent(),
            sponsorButtonLabel = "¥29.9 후원",
            sponsorButtonHint = "Alipay 결제 페이지를 열어 고정 금액으로 후원합니다.",
            sponsorStatusChecking = "결제 결과 확인 중…",
            sponsorStatusPaidPrefix = "후원해 주셔서 감사합니다.",
            sponsorStatusPending = "주문이 생성되었습니다. Alipay에서 결제를 완료해 주세요.",
            sponsorStatusFailed = "아직 결제가 완료되지 않았습니다. 나중에 다시 시도해 주세요.",
            sponsorStatusUnconfigured = "결제 서비스 URL이 설정되지 않았습니다. campfirePayBaseUrl을 설정해 주세요.",
            sponsorLaunchFailed = "결제를 시작할 수 없습니다. 나중에 다시 시도해 주세요.",
            sponsorRefreshLabel = "결제 상태 새로고침"
        )
        AppLanguage.French -> CampfireStrings(
            supportersTitle = "Remerciements spéciaux / Médias",
            supportersNote = "Sans ordre particulier. Sponsors, partageurs et contributeurs sont listés ensemble.",
            sponsorTitle = "Soutenir photoo",
            shareTitle = "Partager photoo",
            sponsorSummary = "Offrez un thé au développeur et soutenez le développement.",
            shareSummary = "Partagez photoo à plus de personnes pour l'aider à aller plus loin.",
            feedbackSummary = "Dites directement au développeur ce qui fonctionne bien ou ce qui peut être amélioré.",
            sponsorBody = """
                photoo est un outil de gestion de photos simple et sans publicité.

                Si ce petit outil vous a apporté un peu de commodité, de souvenirs ou de joie, n'hésitez pas à offrir un thé au développeur. Votre soutien contribue directement à la maintenance et au développement de nouvelles fonctionnalités.

                Pour un soutien de ${kofiPriceText} ou plus, votre nom apparaîtra sur le mur des remerciements Campfire dans une future version. Si vous souhaitez voir votre nom affiché, veuillez l'indiquer dans la note de transfert ; sinon, vous serez remercié comme « Anonyme ».
            """.trimIndent(),
            sponsorButtonLabel = "Soutenir ¥29.9",
            sponsorButtonHint = "Ouvre la page de paiement Alipay pour un soutien d'un montant fixe.",
            sponsorStatusChecking = "Vérification du paiement…",
            sponsorStatusPaidPrefix = "Merci pour votre soutien.",
            sponsorStatusPending = "Commande créée. Veuillez compléter le paiement dans Alipay.",
            sponsorStatusFailed = "Le paiement n'est pas encore terminé. Réessayez plus tard.",
            sponsorStatusUnconfigured = "L'URL du service de paiement n'est pas configurée. Veuillez définir campfirePayBaseUrl.",
            sponsorLaunchFailed = "Impossible de lancer le paiement. Réessayez plus tard.",
            sponsorRefreshLabel = "Actualiser le statut"
        )
        AppLanguage.German -> CampfireStrings(
            supportersTitle = "Besonderer Dank / Medien",
            supportersNote = "Ohne bestimmte Reihenfolge. Sponsoren, Unterstützer und Feedback-Geber sind gemeinsam aufgeführt.",
            sponsorTitle = "photoo unterstützen",
            shareTitle = "photoo teilen",
            sponsorSummary = "Laden Sie den Entwickler auf einen Tee ein und unterstützen Sie die Entwicklung.",
            shareSummary = "Teilen Sie photoo mit mehr Menschen, um ihm zu helfen, weiterzukommen.",
            feedbackSummary = "Sagen Sie dem Entwickler direkt, was gut funktioniert und was verbessert werden kann.",
            sponsorBody = """
                photoo ist ein einfaches, werbefreies Fotoverwaltungstool.

                Wenn dieses kleine Tool Ihnen etwas Komfort, Erinnerungen oder Freude gebracht hat, laden Sie den Entwickler auf einen Tee ein. Ihre Unterstützung fließt direkt in Wartung und neue Funktionen.

                Bei einer Unterstützung ab ${kofiPriceText} wird Ihr Name in einer zukünftigen Version auf der Campfire-Dankeswand erscheinen. Wenn Sie Ihren Namen dort sehen möchten, hinterlassen Sie ihn in der Überweisungsnachricht; andernfalls werden Sie als „Anonym" gedankt.
            """.trimIndent(),
            sponsorButtonLabel = "Mit ¥29.9 unterstützen",
            sponsorButtonHint = "Öffnet die Alipay-Zahlungsseite für einen festen Unterstützungsbetrag.",
            sponsorStatusChecking = "Zahlung wird überprüft…",
            sponsorStatusPaidPrefix = "Vielen Dank für Ihre Unterstützung.",
            sponsorStatusPending = "Bestellung erstellt. Bitte schließen Sie die Zahlung in Alipay ab.",
            sponsorStatusFailed = "Die Zahlung ist noch nicht abgeschlossen. Bitte versuchen Sie es später erneut.",
            sponsorStatusUnconfigured = "Die Zahlungs-URL ist nicht konfiguriert. Bitte setzen Sie campfirePayBaseUrl.",
            sponsorLaunchFailed = "Zahlung konnte nicht gestartet werden. Bitte versuchen Sie es später erneut.",
            sponsorRefreshLabel = "Status aktualisieren"
        )
        AppLanguage.Italian -> CampfireStrings(
            supportersTitle = "Ringraziamenti speciali / Media",
            supportersNote = "In nessun ordine particolare. Sponsor, sostenitori e contributori sono elencati insieme.",
            sponsorTitle = "Sostieni photoo",
            shareTitle = "Condividi photoo",
            sponsorSummary = "Offri un tè allo sviluppatore e sostieni lo sviluppo.",
            shareSummary = "Condividi photoo con più persone per aiutarlo a crescere.",
            feedbackSummary = "Comunica direttamente allo sviluppatore cosa funziona bene e cosa può essere migliorato.",
            sponsorBody = """
                photoo è uno strumento semplice e senza pubblicità per la gestione delle foto.

                Se questo piccolo strumento ti ha portato un po' di comodità, ricordi o gioia, offri un tè allo sviluppatore. Il tuo sostegno va direttamente alla manutenzione e allo sviluppo di nuove funzionalità.

                Con un sostegno di ${kofiPriceText} o più, il tuo nome apparirà sulla bacheca dei ringraziamenti Campfire in una futura versione. Se vuoi che il tuo nome sia mostrato, lascialo nella nota del bonifico; altrimenti sarai ringraziato come «Anonimo».
            """.trimIndent(),
            sponsorButtonLabel = "Sostieni con ¥29.9",
            sponsorButtonHint = "Apre la pagina di pagamento Alipay per un importo fisso.",
            sponsorStatusChecking = "Verifica del pagamento…",
            sponsorStatusPaidPrefix = "Grazie per il tuo sostegno.",
            sponsorStatusPending = "Ordine creato. Completa il pagamento in Alipay.",
            sponsorStatusFailed = "Il pagamento non è ancora completato. Riprova più tardi.",
            sponsorStatusUnconfigured = "L'URL del servizio di pagamento non è configurato. Imposta campfirePayBaseUrl.",
            sponsorLaunchFailed = "Impossibile avviare il pagamento. Riprova più tardi.",
            sponsorRefreshLabel = "Aggiorna stato"
        )
        AppLanguage.Spanish -> CampfireStrings(
            supportersTitle = "Agradecimientos especiales / Medios",
            supportersNote = "Sin orden particular. Patrocinadores, compartidores y contribuidores están listados juntos.",
            sponsorTitle = "Apoyar photoo",
            shareTitle = "Compartir photoo",
            sponsorSummary = "Invita al desarrollador a un té y apoya el desarrollo.",
            shareSummary = "Comparte photoo con más personas para ayudarle a crecer.",
            feedbackSummary = "Dile directamente al desarrollador qué funciona bien y qué se puede mejorar.",
            sponsorBody = """
                photoo es una herramienta sencilla y sin publicidad para gestionar fotos.

                Si esta pequeña herramienta te ha aportado algo de comodidad, recuerdos o alegría, invita al desarrollador a un té. Tu apoyo va directamente al mantenimiento y desarrollo de nuevas funciones.

                Con un apoyo de ${kofiPriceText} o más, tu nombre aparecerá en el muro de agradecimientos Campfire en una futura versión. Si quieres que se muestre tu nombre, déjalo en la nota de la transferencia; de lo contrario, se te agradecerá como «Anónimo».
            """.trimIndent(),
            sponsorButtonLabel = "Apoyar con ¥29.9",
            sponsorButtonHint = "Abre la página de pago de Alipay para un monto fijo.",
            sponsorStatusChecking = "Verificando el pago…",
            sponsorStatusPaidPrefix = "Gracias por tu apoyo.",
            sponsorStatusPending = "Pedido creado. Completa el pago en Alipay.",
            sponsorStatusFailed = "El pago aún no se ha completado. Intenta de nuevo más tarde.",
            sponsorStatusUnconfigured = "La URL del servicio de pago no está configurada. Establece campfirePayBaseUrl.",
            sponsorLaunchFailed = "No se pudo iniciar el pago. Intenta de nuevo más tarde.",
            sponsorRefreshLabel = "Actualizar estado"
        )
        AppLanguage.Portuguese -> CampfireStrings(
            supportersTitle = "Agradecimentos especiais / Mídia",
            supportersNote = "Sem ordem particular. Patrocinadores, compartilhadores e contribuidores estão listados juntos.",
            sponsorTitle = "Apoiar photoo",
            shareTitle = "Compartilhar photoo",
            sponsorSummary = "Convide o desenvolvedor para um chá e apoie o desenvolvimento.",
            shareSummary = "Compartilhe photoo com mais pessoas para ajudá-lo a crescer.",
            feedbackSummary = "Diga diretamente ao desenvolvedor o que funciona bem e o que pode ser melhorado.",
            sponsorBody = """
                photoo é uma ferramenta simples e sem anúncios para gerenciar fotos.

                Se esta pequena ferramenta trouxe um pouco de conveniência, memórias ou alegria, convide o desenvolvedor para um chá. Seu apoio vai diretamente para manutenção e desenvolvimento de novos recursos.

                Com um apoio de ${kofiPriceText} ou mais, seu nome aparecerá no mural de agradecimentos Campfire em uma versão futura. Se quiser que seu nome seja exibido, deixe-o na nota da transferência; caso contrário, você será agradecido como «Anônimo».
            """.trimIndent(),
            sponsorButtonLabel = "Apoiar com ¥29.9",
            sponsorButtonHint = "Abre a página de pagamento do Alipay para um valor fixo.",
            sponsorStatusChecking = "Verificando o pagamento…",
            sponsorStatusPaidPrefix = "Obrigado pelo seu apoio.",
            sponsorStatusPending = "Pedido criado. Complete o pagamento no Alipay.",
            sponsorStatusFailed = "O pagamento ainda não foi concluído. Tente novamente mais tarde.",
            sponsorStatusUnconfigured = "A URL do serviço de pagamento não está configurada. Defina campfirePayBaseUrl.",
            sponsorLaunchFailed = "Não foi possível iniciar o pagamento. Tente novamente mais tarde.",
            sponsorRefreshLabel = "Atualizar status"
        )
        AppLanguage.Russian -> CampfireStrings(
            supportersTitle = "Особая благодарность / Медиа",
            supportersNote = "В произвольном порядке. Спонсоры, распространители и авторы отзывов перечислены вместе.",
            sponsorTitle = "Поддержать photoo",
            shareTitle = "Поделиться photoo",
            sponsorSummary = "Угостите разработчика чаем и поддержите развитие.",
            shareSummary = "Поделитесь photoo с большим количеством людей, чтобы помочь ему развиваться.",
            feedbackSummary = "Сообщите разработчику напрямую, что работает хорошо, а что можно улучшить.",
            sponsorBody = """
                photoo — это простой инструмент для управления фотографиями без рекламы.

                Если этот небольшой инструмент принёс вам немного удобства, воспоминаний или радости, угостите разработчика чаем. Ваша поддержка идёт напрямую на техническое обслуживание и разработку новых функций.

                При поддержке от ${kofiPriceText} и выше ваше имя появится на стене благодарностей Campfire в будущей версии. Если хотите, чтобы ваше имя было указано, оставьте его в примечании к переводу; иначе вы будете отмечены как «Аноним».
            """.trimIndent(),
            sponsorButtonLabel = "Поддержать ¥29.9",
            sponsorButtonHint = "Открывает страницу оплаты Alipay для фиксированной суммы поддержки.",
            sponsorStatusChecking = "Проверка платежа…",
            sponsorStatusPaidPrefix = "Спасибо за вашу поддержку.",
            sponsorStatusPending = "Заказ создан. Завершите оплату в Alipay.",
            sponsorStatusFailed = "Платёж ещё не завершён. Попробуйте позже.",
            sponsorStatusUnconfigured = "URL платёжного сервиса не настроен. Установите campfirePayBaseUrl.",
            sponsorLaunchFailed = "Не удалось запустить платёж. Попробуйте позже.",
            sponsorRefreshLabel = "Обновить статус"
        )
        AppLanguage.Thai -> CampfireStrings(
            supportersTitle = "ขอบคุณเป็นพิเศษ / สื่อ",
            supportersNote = "ไม่เรียงลำดับ ผู้สนับสนุน ผู้แชร์ และผู้ให้ข้อเสนอแนะแสดงรวมกัน",
            sponsorTitle = "สนับสนุน photoo",
            shareTitle = "แชร์ photoo",
            sponsorSummary = "เลี้ยงนักพัฒนาชานมและสนับสนุนการพัฒนา",
            shareSummary = "แชร์ photoo ให้คนมากขึ้นเพื่อช่วยให้เติบโต",
            feedbackSummary = "บอกนักพัฒนาโดยตรงว่าอะไรใช้ดีและอะไรควรปรับปรุง",
            sponsorBody = """
                photoo เป็นเครื่องมือจัดการรูปภาพที่เรียบง่ายและไม่มีโฆษณา

                หากเครื่องมือเล็กๆ นี้มอบความสะดวก ความทรงจำ หรือความสุขให้คุณบ้าง เชิญเลี้ยงนักพัฒนาชานมสักถ้วย การสนับสนุนของคุณจะไปที่การดูแลและพัฒนาฟีเจอร์ใหม่โดยตรง

                สนับสนุนตั้งแต่ ${kofiPriceText} ขึ้นไป ชื่อของคุณจะปรากฏบนกำแพงขอบคุณ Campfire ในเวอร์ชันถัดไป หากต้องการให้แสดงชื่อ โปรดระบุในหมายเหตุการโอน มิฉะนั้นจะขอบคุณในนาม «ไม่ระบุชื่อ»
            """.trimIndent(),
            sponsorButtonLabel = "สนับสนุน ¥29.9",
            sponsorButtonHint = "เปิดหน้าชำระเงิน Alipay สำหรับยอดสนับสนุนคงที่",
            sponsorStatusChecking = "กำลังตรวจสอบการชำระเงิน…",
            sponsorStatusPaidPrefix = "ขอบคุณสำหรับการสนับสนุน",
            sponsorStatusPending = "สร้างคำสั่งแล้ว กรุณาชำระเงินใน Alipay",
            sponsorStatusFailed = "ยังไม่ได้ชำระเงิน โปรดลองอีกครั้งในภายหลัง",
            sponsorStatusUnconfigured = "ยังไม่ได้ตั้งค่า URL บริการชำระเงิน กรุณาตั้งค่า campfirePayBaseUrl",
            sponsorLaunchFailed = "ไม่สามารถเริ่มการชำระเงิน โปรดลองอีกครั้งในภายหลัง",
            sponsorRefreshLabel = "รีเฟรชสถานะ"
        )
        AppLanguage.Vietnamese -> CampfireStrings(
            supportersTitle = "Cảm ơn đặc biệt / Truyền thông",
            supportersNote = "Không theo thứ tự. Nhà tài trợ, người chia sẻ và người đóng góp được liệt kê cùng nhau.",
            sponsorTitle = "Ủng hộ photoo",
            shareTitle = "Chia sẻ photoo",
            sponsorSummary = "Mời nhà phát triển một ly trà sữa và hỗ trợ việc phát triển.",
            shareSummary = "Chia sẻ photoo cho nhiều người hơn để giúp nó phát triển.",
            feedbackSummary = "Cho nhà phát triển biết điều gì hoạt động tốt và điều gì cần cải thiện.",
            sponsorBody = """
                photoo là công cụ quản lý ảnh đơn giản và không có quảng cáo.

                Nếu công cụ nhỏ này mang lại cho bạn chút tiện lợi, kỷ niệm hoặc niềm vui, hãy mời nhà phát triển một ly trà sữa. Sự hỗ trợ của bạn sẽ đi trực tiếp vào việc bảo trì và phát triển tính năng mới.

                Với sự hỗ trợ từ ${kofiPriceText} trở lên, tên của bạn sẽ xuất hiện trên tường cảm ơn Campfire trong phiên bản tương lai. Nếu muốn tên được hiển thị, hãy để lại trong ghi chú chuyển khoản; nếu không, bạn sẽ được cảm ơn dưới tên « Ẩn danh ».
            """.trimIndent(),
            sponsorButtonLabel = "Ủng hộ ¥29.9",
            sponsorButtonHint = "Mở trang thanh toán Alipay cho số tiền cố định.",
            sponsorStatusChecking = "Đang kiểm tra thanh toán…",
            sponsorStatusPaidPrefix = "Cảm ơn bạn đã hỗ trợ.",
            sponsorStatusPending = "Đơn hàng đã tạo. Vui lòng hoàn tất thanh toán trong Alipay.",
            sponsorStatusFailed = "Thanh toán chưa hoàn tất. Vui lòng thử lại sau.",
            sponsorStatusUnconfigured = "URL dịch vụ thanh toán chưa được cấu hình. Vui lòng đặt campfirePayBaseUrl.",
            sponsorLaunchFailed = "Không thể khởi động thanh toán. Vui lòng thử lại sau.",
            sponsorRefreshLabel = "Làm mới trạng thái"
        )
        AppLanguage.Indonesian -> CampfireStrings(
            supportersTitle = "Terima kasih khusus / Media",
            supportersNote = "Tanpa urutan tertentu. Sponsor, pembagi, dan kontributor terdaftar bersama.",
            sponsorTitle = "Dukung photoo",
            shareTitle = "Bagikan photoo",
            sponsorSummary = "Traktir pengembang teh susu dan dukung pengembangan.",
            shareSummary = "Bagikan photoo ke lebih banyak orang untuk membantunya berkembang.",
            feedbackSummary = "Beri tahu pengembang langsung apa yang berfungsi baik dan apa yang bisa diperbaiki.",
            sponsorBody = """
                photoo adalah alat manajemen foto sederhana tanpa iklan.

                Jika alat kecil ini memberi Anda sedikit kenyamanan, kenangan, atau kegembiraan, traktir pengembang teh susu. Dukungan Anda langsung digunakan untuk pemeliharaan dan pengembangan fitur baru.

                Dengan dukungan dari ${kofiPriceText} ke atas, nama Anda akan muncul di dinding ucapan terima kasih Campfire di versi mendatang. Jika ingin nama Anda ditampilkan, tinggalkan di catatan transfer; jika tidak, Anda akan disebut sebagai « Anonim ».
            """.trimIndent(),
            sponsorButtonLabel = "Dukung dengan ¥29.9",
            sponsorButtonHint = "Membuka halaman pembayaran Alipay untuk jumlah tetap.",
            sponsorStatusChecking = "Memeriksa pembayaran…",
            sponsorStatusPaidPrefix = "Terima kasih atas dukungan Anda.",
            sponsorStatusPending = "Pesanan dibuat. Selesaikan pembayaran di Alipay.",
            sponsorStatusFailed = "Pembayaran belum selesai. Coba lagi nanti.",
            sponsorStatusUnconfigured = "URL layanan pembayaran belum dikonfigurasi. Atur campfirePayBaseUrl.",
            sponsorLaunchFailed = "Tidak dapat memulai pembayaran. Coba lagi nanti.",
            sponsorRefreshLabel = "Segarkan status"
        )
        AppLanguage.Arabic -> CampfireStrings(
            supportersTitle = "شكر خاص / وسائل الإعلام",
            supportersNote = "بدون ترتيب معين. الرعاة والمشاركون والمساهمون مدرجون معًا.",
            sponsorTitle = "ادعم photoo",
            shareTitle = "شارك photoo",
            sponsorSummary = "ادع المطور لشاي وادعم التطوير.",
            shareSummary = "شارك photoo مع المزيد من الناس لمساعدته على النمو.",
            feedbackSummary = "أخبر المطور مباشرة ما يعمل جيدًا وما يمكن تحسينه.",
            sponsorBody = """
                photoo هي أداة بسيطة لإدارة الصور بدون إعلانات.

                إذا جلبت لك هذه الأداة الصغيرة بعض الراحة أو الذكريات أو الفرح، ادع المطور لشاي. دعمك يذهب مباشرة إلى الصيانة وتطوير ميزات جديدة.

                بدعم من ${kofiPriceText} فأكثر، سيظهر اسمك على جدار الشكر Campfire في إصدار مستقبلي. إذا كنت تريد عرض اسمك، اتركه في ملاحظة التحويل؛ وإلا سيتم شكرك كـ «مجهول».
            """.trimIndent(),
            sponsorButtonLabel = "ادعم بـ ¥29.9",
            sponsorButtonHint = "يفتح صفحة دفع Alipay لمبلغ ثابت.",
            sponsorStatusChecking = "جاري التحقق من الدفع…",
            sponsorStatusPaidPrefix = "شكرًا لدعمك.",
            sponsorStatusPending = "تم إنشاء الطلب. أكمل الدفع في Alipay.",
            sponsorStatusFailed = "لم يكتمل الدفع بعد. حاول مرة أخرى لاحقًا.",
            sponsorStatusUnconfigured = "لم يتم تكوين عنوان URL لخدمة الدفع. قم بتعيين campfirePayBaseUrl.",
            sponsorLaunchFailed = "تعذر بدء الدفع. حاول مرة أخرى لاحقًا.",
            sponsorRefreshLabel = "تحديث الحالة"
        )
        AppLanguage.Ukrainian -> CampfireStrings(
            supportersTitle = "Особлива подяка / Медіа",
            supportersNote = "У довільному порядку. Спонсори, ті, хто поширює, і автори відгуків перелічені разом.",
            sponsorTitle = "Підтримати photoo",
            shareTitle = "Поділитися photoo",
            sponsorSummary = "Пригостіть розробника молочним чаєм і підтримайте подальшу розробку.",
            shareSummary = "Розкажіть про photoo більшій кількості людей і допоможіть йому розвиватися.",
            feedbackSummary = "Надішліть відгуки та ідеї функцій безпосередньо розробнику для майбутніх версій.",
            sponsorBody = """
                photoo — це зосереджений інструмент для керування фото. Він назавжди залишиться без реклами.

                Якщо цей невеликий інструмент приніс вам трохи зручності, спогадів чи радості, можете пригостити розробника молочним чаєм. Ваша підтримка йде безпосередньо на супровід і розробку нових функцій, допомагаючи photoo розвиватися.

                Якщо ваш внесок становить ${kofiPriceText} або більше, вас буде додано на стіну подяк Campfire у photoo в одній із майбутніх версій. Щоб ваше ім'я там відображалося, вкажіть його в примітці до переказу; інакше вам подякують як «Анонім».
            """.trimIndent(),
            sponsorButtonLabel = "Підтримати за ¥29.9",
            sponsorButtonHint = "Відкриває сторінку оплати Alipay із фіксованою сумою внеску.",
            sponsorStatusChecking = "Перевіряємо статус оплати…",
            sponsorStatusPaidPrefix = "Оплату отримано. Дякуємо за підтримку.",
            sponsorStatusPending = "Замовлення створено. Завершіть оплату в Alipay.",
            sponsorStatusFailed = "Оплату ще не завершено. Спробуйте пізніше.",
            sponsorStatusUnconfigured = "URL сервісу оплати ще не налаштовано. Спершу задайте campfirePayBaseUrl.",
            sponsorLaunchFailed = "Зараз не вдається почати оплату. Спробуйте пізніше.",
            sponsorRefreshLabel = "Оновити статус оплати"
        )
        AppLanguage.Polish -> CampfireStrings(
            supportersTitle = "Podziękowania / Media",
            supportersNote = "W dowolnej kolejności. Sponsorzy, osoby polecające aplikację i autorzy opinii są wymienieni razem.",
            sponsorTitle = "Wesprzyj photoo",
            shareTitle = "Udostępnij photoo",
            sponsorSummary = "Postaw deweloperowi mleczną herbatę i wesprzyj dalszy rozwój.",
            shareSummary = "Pokaż photoo większej liczbie osób i pomóż mu zajść dalej.",
            feedbackSummary = "Wyślij opinie i pomysły na funkcje bezpośrednio do dewelopera na potrzeby kolejnych wersji.",
            sponsorBody = """
                photoo to skoncentrowane narzędzie do zarządzania zdjęciami. Na zawsze pozostanie bez reklam.

                Jeśli to małe narzędzie dało Ci odrobinę wygody, wspomnień lub radości, możesz postawić deweloperowi mleczną herbatę. Twoje wsparcie trafia bezpośrednio na utrzymanie i rozwój nowych funkcji, pomagając photoo zajść dalej.

                Jeśli Twoje wsparcie wynosi ${kofiPriceText} lub więcej, w przyszłej wersji zostaniesz dodany do tablicy podziękowań Campfire w photoo. Jeśli chcesz, aby Twoje imię tam widniało, wpisz je w uwagach do przelewu; w przeciwnym razie podziękujemy Ci jako „Anonim”.
            """.trimIndent(),
            sponsorButtonLabel = "Wesprzyj za ¥29.9",
            sponsorButtonHint = "Otwiera stronę płatności Alipay ze stałą kwotą wsparcia.",
            sponsorStatusChecking = "Sprawdzanie statusu płatności…",
            sponsorStatusPaidPrefix = "Płatność otrzymana. Dziękujemy za wsparcie.",
            sponsorStatusPending = "Zamówienie utworzone. Dokończ płatność w Alipay.",
            sponsorStatusFailed = "Płatność nie została jeszcze zakończona. Spróbuj ponownie później.",
            sponsorStatusUnconfigured = "Adres URL usługi płatności nie jest jeszcze skonfigurowany. Najpierw ustaw campfirePayBaseUrl.",
            sponsorLaunchFailed = "Nie można teraz rozpocząć płatności. Spróbuj ponownie później.",
            sponsorRefreshLabel = "Odśwież status płatności"
        )
        AppLanguage.Czech -> CampfireStrings(
            supportersTitle = "Zvláštní poděkování / Média",
            supportersNote = "V libovolném pořadí. Sponzoři, lidé šířící aplikaci a autoři zpětné vazby jsou uvedeni společně.",
            sponsorTitle = "Podpořit photoo",
            shareTitle = "Sdílet photoo",
            sponsorSummary = "Pozvěte vývojáře na mléčný čaj a podpořte další vývoj.",
            shareSummary = "Sdílejte photoo s dalšími lidmi a pomozte mu dostat se dál.",
            feedbackSummary = "Pošlete zpětnou vazbu a nápady na funkce přímo vývojáři pro připravované verze.",
            sponsorBody = """
                photoo je zaměřený nástroj pro správu fotografií. Navždy zůstane bez reklam.

                Pokud vám tento malý nástroj přinesl trochu pohodlí, vzpomínek nebo radosti, můžete vývojáře pozvat na mléčný čaj. Vaše podpora jde přímo na údržbu a vývoj nových funkcí a pomáhá photoo dostat se dál.

                Pokud vaše podpora činí ${kofiPriceText} nebo více, budete v budoucí verzi přidáni na zeď poděkování Campfire v photoo. Chcete-li, aby se tam zobrazilo vaše jméno, uveďte ho v poznámce k převodu; jinak vám poděkujeme jako „Anonym“.
            """.trimIndent(),
            sponsorButtonLabel = "Podpořit za ¥29.9",
            sponsorButtonHint = "Otevře platební stránku Alipay s pevnou částkou podpory.",
            sponsorStatusChecking = "Kontrola stavu platby…",
            sponsorStatusPaidPrefix = "Platba přijata. Děkujeme za podporu.",
            sponsorStatusPending = "Objednávka vytvořena. Dokončete platbu v Alipay.",
            sponsorStatusFailed = "Platba ještě nebyla dokončena. Zkuste to znovu později.",
            sponsorStatusUnconfigured = "URL platební služby ještě není nastaveno. Nejprve nastavte campfirePayBaseUrl.",
            sponsorLaunchFailed = "Platbu se teď nepodařilo zahájit. Zkuste to znovu později.",
            sponsorRefreshLabel = "Obnovit stav platby"
        )
        AppLanguage.Dutch -> CampfireStrings(
            supportersTitle = "Speciale dank / Media",
            supportersNote = "In willekeurige volgorde. Sponsors, mensen die delen en feedbackgevers worden samen vermeld.",
            sponsorTitle = "Steun photoo",
            shareTitle = "Deel photoo",
            sponsorSummary = "Trakteer de ontwikkelaar op een melkthee en steun de verdere ontwikkeling.",
            shareSummary = "Deel photoo met meer mensen en help het verder.",
            feedbackSummary = "Stuur feedback en ideeën voor functies rechtstreeks naar de ontwikkelaar voor toekomstige versies.",
            sponsorBody = """
                photoo is een gerichte tool voor fotobeheer. Het blijft voor altijd vrij van advertenties.

                Als deze kleine tool je wat gemak, herinneringen of plezier heeft gebracht, mag je de ontwikkelaar trakteren op een melkthee. Je steun gaat rechtstreeks naar onderhoud en de ontwikkeling van nieuwe functies, zodat photoo verder kan komen.

                Als je steun ${kofiPriceText} of meer bedraagt, word je in een toekomstige versie toegevoegd aan de Campfire-dankwand van photoo. Wil je dat je naam daar staat, vermeld die dan in de overboekingsnotitie; anders word je bedankt als „Anoniem”.
            """.trimIndent(),
            sponsorButtonLabel = "Steun met ¥29.9",
            sponsorButtonHint = "Opent de Alipay-betaalpagina voor het vaste steunbedrag.",
            sponsorStatusChecking = "Betaalstatus controleren…",
            sponsorStatusPaidPrefix = "Betaling ontvangen. Bedankt voor je steun.",
            sponsorStatusPending = "Bestelling aangemaakt. Rond de betaling af in Alipay.",
            sponsorStatusFailed = "De betaling is nog niet voltooid. Probeer het later opnieuw.",
            sponsorStatusUnconfigured = "De URL van de betaalservice is nog niet ingesteld. Stel eerst campfirePayBaseUrl in.",
            sponsorLaunchFailed = "Betaling starten lukt nu niet. Probeer het later opnieuw.",
            sponsorRefreshLabel = "Betaalstatus vernieuwen"
        )
        AppLanguage.Swedish -> CampfireStrings(
            supportersTitle = "Särskilt tack / Media",
            supportersNote = "I valfri ordning. Sponsorer, de som delar och de som ger feedback listas tillsammans.",
            sponsorTitle = "Stöd photoo",
            shareTitle = "Dela photoo",
            sponsorSummary = "Bjud utvecklaren på ett mjölkte och stöd den fortsatta utvecklingen.",
            shareSummary = "Dela photoo med fler och hjälp det att nå längre.",
            feedbackSummary = "Skicka feedback och funktionsidéer direkt till utvecklaren inför kommande versioner.",
            sponsorBody = """
                photoo är ett fokuserat verktyg för fotohantering. Det förblir reklamfritt för alltid.

                Om det här lilla verktyget har gett dig lite bekvämlighet, minnen eller glädje är du välkommen att bjuda utvecklaren på ett mjölkte. Ditt stöd går direkt till underhåll och utveckling av nya funktioner och hjälper photoo att nå längre.

                Om ditt stöd är ${kofiPriceText} eller mer läggs du till på Campfire-tackväggen i photoo i en framtida version. Vill du att ditt namn visas där, skriv det i överföringsmeddelandet; annars tackar vi dig som ”Anonym”.
            """.trimIndent(),
            sponsorButtonLabel = "Stöd med ¥29.9",
            sponsorButtonHint = "Öppnar Alipays betalsida för det fasta stödbeloppet.",
            sponsorStatusChecking = "Kontrollerar betalstatus…",
            sponsorStatusPaidPrefix = "Betalningen är mottagen. Tack för ditt stöd.",
            sponsorStatusPending = "Beställningen är skapad. Slutför betalningen i Alipay.",
            sponsorStatusFailed = "Betalningen är inte slutförd än. Försök igen senare.",
            sponsorStatusUnconfigured = "Betaltjänstens URL är inte konfigurerad än. Ange campfirePayBaseUrl först.",
            sponsorLaunchFailed = "Det går inte att starta betalningen just nu. Försök igen senare.",
            sponsorRefreshLabel = "Uppdatera betalstatus"
        )
        AppLanguage.Danish -> CampfireStrings(
            supportersTitle = "Særlig tak / Medier",
            supportersNote = "I vilkårlig rækkefølge. Sponsorer, dem der deler, og dem der giver feedback, er nævnt sammen.",
            sponsorTitle = "Støt photoo",
            shareTitle = "Del photoo",
            sponsorSummary = "Giv udvikleren en mælkete og støt den videre udvikling.",
            shareSummary = "Del photoo med flere og hjælp det videre.",
            feedbackSummary = "Send feedback og idéer til funktioner direkte til udvikleren til kommende versioner.",
            sponsorBody = """
                photoo er et fokuseret værktøj til fotohåndtering. Det forbliver reklamefrit for altid.

                Hvis dette lille værktøj har givet dig lidt bekvemmelighed, minder eller glæde, er du velkommen til at give udvikleren en mælkete. Din støtte går direkte til vedligeholdelse og udvikling af nye funktioner og hjælper photoo videre.

                Hvis din støtte er ${kofiPriceText} eller mere, bliver du føjet til Campfires takkevæg i photoo i en kommende version. Hvis du vil have dit navn vist der, så skriv det i overførselsnoten; ellers takker vi dig som „Anonym”.
            """.trimIndent(),
            sponsorButtonLabel = "Støt med ¥29.9",
            sponsorButtonHint = "Åbner Alipays betalingsside med det faste støttebeløb.",
            sponsorStatusChecking = "Kontrollerer betalingsstatus…",
            sponsorStatusPaidPrefix = "Betalingen er modtaget. Tak for din støtte.",
            sponsorStatusPending = "Ordren er oprettet. Gennemfør betalingen i Alipay.",
            sponsorStatusFailed = "Betalingen er ikke gennemført endnu. Prøv igen senere.",
            sponsorStatusUnconfigured = "Betalingstjenestens URL er ikke konfigureret endnu. Angiv først campfirePayBaseUrl.",
            sponsorLaunchFailed = "Betalingen kan ikke startes lige nu. Prøv igen senere.",
            sponsorRefreshLabel = "Opdatér betalingsstatus"
        )
        AppLanguage.Norwegian -> CampfireStrings(
            supportersTitle = "Spesiell takk / Medier",
            supportersNote = "I tilfeldig rekkefølge. Sponsorer, de som deler og de som gir tilbakemeldinger, er oppført sammen.",
            sponsorTitle = "Støtt photoo",
            shareTitle = "Del photoo",
            sponsorSummary = "Spander en melkete på utvikleren og støtt videre utvikling.",
            shareSummary = "Del photoo med flere og hjelp det videre.",
            feedbackSummary = "Send tilbakemeldinger og funksjonsideer direkte til utvikleren for kommende versjoner.",
            sponsorBody = """
                photoo er et fokusert verktøy for bildehåndtering. Det forblir reklamefritt for alltid.

                Hvis dette lille verktøyet har gitt deg litt bekvemmelighet, minner eller glede, er du velkommen til å spandere en melkete på utvikleren. Støtten din går direkte til vedlikehold og utvikling av nye funksjoner og hjelper photoo videre.

                Hvis støtten din er ${kofiPriceText} eller mer, blir du lagt til på Campfire-takkeveggen i photoo i en kommende versjon. Vil du ha navnet ditt vist der, skriv det i overføringsnotatet; ellers takker vi deg som «Anonym».
            """.trimIndent(),
            sponsorButtonLabel = "Støtt med ¥29.9",
            sponsorButtonHint = "Åpner Alipays betalingsside for det faste støttebeløpet.",
            sponsorStatusChecking = "Sjekker betalingsstatus…",
            sponsorStatusPaidPrefix = "Betalingen er mottatt. Takk for støtten.",
            sponsorStatusPending = "Bestillingen er opprettet. Fullfør betalingen i Alipay.",
            sponsorStatusFailed = "Betalingen er ikke fullført ennå. Prøv igjen senere.",
            sponsorStatusUnconfigured = "URL-en til betalingstjenesten er ikke konfigurert ennå. Angi campfirePayBaseUrl først.",
            sponsorLaunchFailed = "Kan ikke starte betalingen akkurat nå. Prøv igjen senere.",
            sponsorRefreshLabel = "Oppdater betalingsstatus"
        )
        AppLanguage.Finnish -> CampfireStrings(
            supportersTitle = "Erityiskiitokset / Media",
            supportersNote = "Ei erityisessä järjestyksessä. Tukijat, jakajat ja palautteen antajat on lueteltu yhdessä.",
            sponsorTitle = "Tue photoota",
            shareTitle = "Jaa photoo",
            sponsorSummary = "Tarjoa kehittäjälle maitoteetä ja tue jatkokehitystä.",
            shareSummary = "Jaa photoo useammille ja auta sitä menemään pidemmälle.",
            feedbackSummary = "Lähetä palautetta ja ominaisuusideoita suoraan kehittäjälle tulevia versioita varten.",
            sponsorBody = """
                photoo on keskittynyt valokuvien hallintatyökalu. Se pysyy mainoksettomana ikuisesti.

                Jos tämä pieni työkalu on tuonut sinulle vähän helppoutta, muistoja tai iloa, voit tarjota kehittäjälle maitoteetä. Tukesi menee suoraan ylläpitoon ja uusien ominaisuuksien kehittämiseen ja auttaa photoota menemään pidemmälle.

                Jos tukesi on ${kofiPriceText} tai enemmän, sinut lisätään photoon Campfire-kiitosseinälle tulevassa versiossa. Jos haluat nimesi näkyviin, kirjoita se siirron viestikenttään; muuten sinua kiitetään nimellä ”Nimetön”.
            """.trimIndent(),
            sponsorButtonLabel = "Tue summalla ¥29.9",
            sponsorButtonHint = "Avaa Alipayn maksusivun kiinteälle tukisummalle.",
            sponsorStatusChecking = "Tarkistetaan maksun tilaa…",
            sponsorStatusPaidPrefix = "Maksu vastaanotettu. Kiitos tuestasi.",
            sponsorStatusPending = "Tilaus luotu. Viimeistele maksu Alipayssa.",
            sponsorStatusFailed = "Maksua ei ole vielä suoritettu loppuun. Yritä myöhemmin uudelleen.",
            sponsorStatusUnconfigured = "Maksupalvelun URL-osoitetta ei ole vielä määritetty. Aseta ensin campfirePayBaseUrl.",
            sponsorLaunchFailed = "Maksua ei voi aloittaa juuri nyt. Yritä myöhemmin uudelleen.",
            sponsorRefreshLabel = "Päivitä maksun tila"
        )
        AppLanguage.Greek -> CampfireStrings(
            supportersTitle = "Ιδιαίτερες ευχαριστίες / Μέσα",
            supportersNote = "Χωρίς συγκεκριμένη σειρά. Χορηγοί, άτομα που μοιράζονται και όσοι δίνουν σχόλια αναφέρονται μαζί.",
            sponsorTitle = "Στηρίξτε το photoo",
            shareTitle = "Μοιραστείτε το photoo",
            sponsorSummary = "Κεράστε τον προγραμματιστή ένα τσάι με γάλα και στηρίξτε τη μελλοντική ανάπτυξη.",
            shareSummary = "Μοιραστείτε το photoo με περισσότερους ανθρώπους και βοηθήστε το να προχωρήσει.",
            feedbackSummary = "Στείλτε σχόλια και ιδέες για λειτουργίες απευθείας στον προγραμματιστή για τις επόμενες εκδόσεις.",
            sponsorBody = """
                Το photoo είναι ένα εστιασμένο εργαλείο διαχείρισης φωτογραφιών. Θα παραμείνει για πάντα χωρίς διαφημίσεις.

                Αν αυτό το μικρό εργαλείο σάς έφερε λίγη ευκολία, αναμνήσεις ή χαρά, μπορείτε να κεράσετε τον προγραμματιστή ένα τσάι με γάλα. Η στήριξή σας πηγαίνει απευθείας στη συντήρηση και στην ανάπτυξη νέων λειτουργιών, βοηθώντας το photoo να προχωρήσει.

                Αν η στήριξή σας είναι ${kofiPriceText} ή περισσότερο, θα προστεθείτε στον τοίχο ευχαριστιών Campfire του photoo σε μελλοντική έκδοση. Αν θέλετε να εμφανίζεται το όνομά σας, αφήστε το στη σημείωση της μεταφοράς, διαφορετικά θα σας ευχαριστήσουμε ως «Ανώνυμος».
            """.trimIndent(),
            sponsorButtonLabel = "Στήριξη με ¥29.9",
            sponsorButtonHint = "Ανοίγει τη σελίδα πληρωμής Alipay για το σταθερό ποσό στήριξης.",
            sponsorStatusChecking = "Έλεγχος κατάστασης πληρωμής…",
            sponsorStatusPaidPrefix = "Η πληρωμή ελήφθη. Σας ευχαριστούμε για τη στήριξη.",
            sponsorStatusPending = "Η παραγγελία δημιουργήθηκε. Ολοκληρώστε την πληρωμή στο Alipay.",
            sponsorStatusFailed = "Η πληρωμή δεν έχει ολοκληρωθεί ακόμη. Δοκιμάστε ξανά αργότερα.",
            sponsorStatusUnconfigured = "Το URL της υπηρεσίας πληρωμών δεν έχει ρυθμιστεί ακόμη. Ορίστε πρώτα το campfirePayBaseUrl.",
            sponsorLaunchFailed = "Δεν είναι δυνατή η έναρξη πληρωμής αυτή τη στιγμή. Δοκιμάστε ξανά αργότερα.",
            sponsorRefreshLabel = "Ανανέωση κατάστασης πληρωμής"
        )
        AppLanguage.Romanian -> CampfireStrings(
            supportersTitle = "Mulțumiri speciale / Media",
            supportersNote = "În ordine aleatorie. Sponsorii, cei care distribuie aplicația și cei care oferă feedback sunt listați împreună.",
            sponsorTitle = "Susțineți photoo",
            shareTitle = "Distribuiți photoo",
            sponsorSummary = "Cumpărați dezvoltatorului un ceai cu lapte și susțineți dezvoltarea viitoare.",
            shareSummary = "Distribuiți photoo mai multor oameni și ajutați-l să meargă mai departe.",
            feedbackSummary = "Trimiteți feedback și idei de funcții direct dezvoltatorului pentru versiunile viitoare.",
            sponsorBody = """
                photoo este un instrument concentrat pentru gestionarea fotografiilor. Va rămâne fără reclame pentru totdeauna.

                Dacă acest mic instrument v-a adus puțină comoditate, amintiri sau bucurie, puteți cumpăra dezvoltatorului un ceai cu lapte. Sprijinul dumneavoastră merge direct către întreținere și dezvoltarea de funcții noi, ajutând photoo să meargă mai departe.

                Dacă sprijinul dumneavoastră este de ${kofiPriceText} sau mai mult, veți fi adăugat pe peretele de mulțumiri Campfire din photoo într-o versiune viitoare. Dacă doriți ca numele dumneavoastră să apară acolo, lăsați-l în nota transferului; altfel vă vom mulțumi ca „Anonim”.
            """.trimIndent(),
            sponsorButtonLabel = "Susțineți cu ¥29.9",
            sponsorButtonHint = "Deschide pagina de plată Alipay pentru suma fixă de sprijin.",
            sponsorStatusChecking = "Se verifică starea plății…",
            sponsorStatusPaidPrefix = "Plata a fost primită. Vă mulțumim pentru sprijin.",
            sponsorStatusPending = "Comanda a fost creată. Finalizați plata în Alipay.",
            sponsorStatusFailed = "Plata nu este încă finalizată. Încercați din nou mai târziu.",
            sponsorStatusUnconfigured = "URL-ul serviciului de plată nu este încă configurat. Setați mai întâi campfirePayBaseUrl.",
            sponsorLaunchFailed = "Plata nu poate fi pornită acum. Încercați din nou mai târziu.",
            sponsorRefreshLabel = "Reîmprospătați starea plății"
        )
        AppLanguage.Hungarian -> CampfireStrings(
            supportersTitle = "Külön köszönet / Média",
            supportersNote = "Nem meghatározott sorrendben. A támogatók, a megosztók és a visszajelzők együtt szerepelnek.",
            sponsorTitle = "Támogassa a photoót",
            shareTitle = "Ossza meg a photoót",
            sponsorSummary = "Hívja meg a fejlesztőt egy tejes teára, és támogassa a további fejlesztést.",
            shareSummary = "Ossza meg a photoót többekkel, és segítsen neki messzebbre jutni.",
            feedbackSummary = "Küldjön visszajelzést és funkcióötleteket közvetlenül a fejlesztőnek a későbbi verziókhoz.",
            sponsorBody = """
                A photoo egy célzott fotókezelő eszköz. Örökre reklámmentes marad.

                Ha ez a kis eszköz némi kényelmet, emléket vagy örömet hozott Önnek, meghívhatja a fejlesztőt egy tejes teára. Támogatása közvetlenül a karbantartásra és az új funkciók fejlesztésére megy, így a photoo messzebbre juthat.

                Ha támogatása eléri a ${kofiPriceText} összeget vagy meghaladja azt, egy későbbi verzióban felkerül a photoo Campfire köszönőfalára. Ha szeretné, hogy a neve megjelenjen, adja meg az átutalás megjegyzésében; egyébként „Névtelen” néven köszönjük meg.
            """.trimIndent(),
            sponsorButtonLabel = "Támogatás ¥29.9 összeggel",
            sponsorButtonHint = "Megnyitja az Alipay fizetési oldalát a rögzített támogatási összeghez.",
            sponsorStatusChecking = "Fizetési állapot ellenőrzése…",
            sponsorStatusPaidPrefix = "A fizetés megérkezett. Köszönjük a támogatást.",
            sponsorStatusPending = "A rendelés létrejött. Fejezze be a fizetést az Alipayben.",
            sponsorStatusFailed = "A fizetés még nem fejeződött be. Próbálja újra később.",
            sponsorStatusUnconfigured = "A fizetési szolgáltatás URL-je még nincs beállítva. Először állítsa be a campfirePayBaseUrl értékét.",
            sponsorLaunchFailed = "A fizetés most nem indítható el. Próbálja újra később.",
            sponsorRefreshLabel = "Fizetési állapot frissítése"
        )
        AppLanguage.Turkish -> CampfireStrings(
            supportersTitle = "Özel Teşekkürler / Medya",
            supportersNote = "Belirli bir sıra olmadan. Sponsorlar, paylaşanlar ve geri bildirim verenler birlikte listelenir.",
            sponsorTitle = "photoo'yu Destekle",
            shareTitle = "photoo'yu Paylaş",
            sponsorSummary = "Geliştiriciye bir sütlü çay ısmarlayın ve gelecekteki geliştirmeyi destekleyin.",
            shareSummary = "photoo'yu daha çok kişiyle paylaşın ve daha ileri gitmesine yardım edin.",
            feedbackSummary = "Gelecek sürümler için geri bildirimlerinizi ve özellik fikirlerinizi doğrudan geliştiriciye gönderin.",
            sponsorBody = """
                photoo, odaklı bir fotoğraf yönetim aracıdır. Sonsuza dek reklamsız kalacaktır.

                Bu küçük araç size biraz kolaylık, anı ya da mutluluk getirdiyse geliştiriciye bir sütlü çay ısmarlayabilirsiniz. Desteğiniz doğrudan bakıma ve yeni özelliklerin geliştirilmesine gider, photoo'nun daha ileri gitmesine yardımcı olur.

                Desteğiniz ${kofiPriceText} veya üzerindeyse, gelecek bir sürümde photoo Campfire teşekkür duvarına eklenirsiniz. Adınızın görünmesini istiyorsanız transfer notuna yazın; aksi halde size “Anonim” olarak teşekkür edilir.
            """.trimIndent(),
            sponsorButtonLabel = "¥29.9 ile destekle",
            sponsorButtonHint = "Sabit destek tutarı için Alipay ödeme sayfasını açar.",
            sponsorStatusChecking = "Ödeme durumu denetleniyor…",
            sponsorStatusPaidPrefix = "Ödeme alındı. Desteğiniz için teşekkürler.",
            sponsorStatusPending = "Sipariş oluşturuldu. Lütfen ödemeyi Alipay'de tamamlayın.",
            sponsorStatusFailed = "Ödeme henüz tamamlanmadı. Daha sonra tekrar deneyebilirsiniz.",
            sponsorStatusUnconfigured = "Ödeme hizmeti URL'si henüz yapılandırılmadı. Önce campfirePayBaseUrl değerini ayarlayın.",
            sponsorLaunchFailed = "Ödeme şu anda başlatılamıyor. Lütfen daha sonra tekrar deneyin.",
            sponsorRefreshLabel = "Ödeme durumunu yenile"
        )
        AppLanguage.Malay -> CampfireStrings(
            supportersTitle = "Terima Kasih Khas / Media",
            supportersNote = "Tanpa susunan tertentu. Penaja, penyokong perkongsian dan penyumbang maklum balas disenaraikan bersama.",
            sponsorTitle = "Sokong photoo",
            shareTitle = "Kongsi photoo",
            sponsorSummary = "Belanja pembangun teh susu dan sokong pembangunan akan datang.",
            shareSummary = "Kongsi photoo dengan lebih ramai orang dan bantu ia pergi lebih jauh.",
            feedbackSummary = "Hantar maklum balas dan idea ciri terus kepada pembangun untuk versi akan datang.",
            sponsorBody = """
                photoo ialah alat pengurusan foto yang fokus. Ia akan kekal bebas iklan selama-lamanya.

                Jika alat kecil ini telah membawa sedikit kemudahan, kenangan atau kegembiraan kepada anda, anda dialu-alukan untuk membelanja pembangun teh susu. Sokongan anda disalurkan terus kepada penyelenggaraan dan pembangunan ciri baharu, membantu photoo pergi lebih jauh.

                Jika sokongan anda ${kofiPriceText} atau lebih, anda akan ditambah pada dinding penghargaan Campfire photoo dalam versi akan datang. Jika anda mahu nama anda dipaparkan di situ, sila tinggalkan nama dalam nota pemindahan; jika tidak, anda akan diucapkan terima kasih sebagai “Awanama”.
            """.trimIndent(),
            sponsorButtonLabel = "Sokong dengan ¥29.9",
            sponsorButtonHint = "Membuka halaman pembayaran Alipay untuk jumlah sokongan tetap.",
            sponsorStatusChecking = "Menyemak status pembayaran…",
            sponsorStatusPaidPrefix = "Pembayaran diterima. Terima kasih atas sokongan anda.",
            sponsorStatusPending = "Pesanan dibuat. Sila lengkapkan pembayaran dalam Alipay.",
            sponsorStatusFailed = "Pembayaran belum selesai. Anda boleh cuba lagi nanti.",
            sponsorStatusUnconfigured = "URL perkhidmatan pembayaran belum dikonfigurasikan. Tetapkan campfirePayBaseUrl dahulu.",
            sponsorLaunchFailed = "Tidak dapat memulakan pembayaran sekarang. Sila cuba lagi nanti.",
            sponsorRefreshLabel = "Muat semula status pembayaran"
        )
        AppLanguage.Hebrew -> CampfireStrings(
            supportersTitle = "תודה מיוחדת / תקשורת",
            supportersNote = "ללא סדר מסוים. תומכים, משתפים ומשיבים מופיעים יחד.",
            sponsorTitle = "תמכו ב-photoo",
            shareTitle = "שתפו את photoo",
            sponsorSummary = "הזמינו את המפתח לתה חלב ותמכו בפיתוח העתידי.",
            shareSummary = "שתפו את photoo עם עוד אנשים ועזרו לו להגיע רחוק יותר.",
            feedbackSummary = "שלחו משוב ורעיונות לתכונות ישירות למפתח עבור גרסאות עתידיות.",
            sponsorBody = """
                photoo הוא כלי ממוקד לניהול תמונות. הוא יישאר נטול פרסומות לנצח.

                אם הכלי הקטן הזה הביא לכם מעט נוחות, זיכרונות או שמחה, אתם מוזמנים להזמין את המפתח לתה חלב. התמיכה שלכם הולכת ישירות לתחזוקה ולפיתוח תכונות חדשות, ועוזרת ל-photoo להגיע רחוק יותר.

                אם התמיכה שלכם היא ${kofiPriceText} ומעלה, תתווספו בגרסה עתידית לקיר התודות Campfire של photoo. אם תרצו שהשם שלכם יוצג שם, כתבו אותו בהערת ההעברה; אחרת נודה לכם כ״אנונימי״.
            """.trimIndent(),
            sponsorButtonLabel = "תמכו ב-¥29.9",
            sponsorButtonHint = "פותח את דף התשלום של Alipay לסכום תמיכה קבוע.",
            sponsorStatusChecking = "בודק את סטטוס התשלום…",
            sponsorStatusPaidPrefix = "התשלום התקבל. תודה על התמיכה.",
            sponsorStatusPending = "ההזמנה נוצרה. השלימו את התשלום ב-Alipay.",
            sponsorStatusFailed = "התשלום עדיין לא הושלם. נסו שוב מאוחר יותר.",
            sponsorStatusUnconfigured = "כתובת שירות התשלום עדיין לא הוגדרה. הגדירו קודם את campfirePayBaseUrl.",
            sponsorLaunchFailed = "לא ניתן להתחיל תשלום כרגע. נסו שוב מאוחר יותר.",
            sponsorRefreshLabel = "רענון סטטוס התשלום"
        )
        AppLanguage.English -> CampfireStrings(
            supportersTitle = "Special Thanks / Media",
            supportersNote = "In no particular order. Sponsors, sharing supporters, and feedback contributors are listed together.",
            sponsorTitle = "Support photoo",
            shareTitle = "Share photoo",
            sponsorSummary = "Buy the developer a milk tea and support future development.",
            shareSummary = "Share photoo with more people and help it go further.",
            feedbackSummary = "Send feedback and feature ideas directly to the developer for upcoming versions.",
            sponsorBody = """
                photoo is a focused photo management tool. It will stay ad-free forever.

                If this small tool has brought you a bit of convenience, memory, or joy, you're welcome to buy the developer a milk tea. Your support goes directly to maintenance and new feature development, helping photoo go further.

                If your sponsorship is ${kofiPriceText} or above, you will be added to the photoo Campfire acknowledgement wall in a future version. If you want your name shown there, please leave a name in the transfer note; otherwise you'll be thanked as "Anonymous".
            """.trimIndent(),
            sponsorButtonLabel = "Support with ¥29.9",
            sponsorButtonHint = "This opens the Alipay checkout page for the fixed sponsorship amount.",
            sponsorStatusChecking = "Checking payment status...",
            sponsorStatusPaidPrefix = "Payment received. Thank you for the support.",
            sponsorStatusPending = "Order created. Please complete the payment in Alipay.",
            sponsorStatusFailed = "The payment is not completed yet. You can try again later.",
            sponsorStatusUnconfigured = "Payment service URL is not configured yet. Set campfirePayBaseUrl first.",
            sponsorLaunchFailed = "Unable to start payment right now. Please try again later.",
            sponsorRefreshLabel = "Refresh payment status"
        )
    }
}

private enum class CampfireSection {
    Home,
    Sponsor,
    Share
}

fun getCampfireActionTitle(language: AppLanguage): String {
    return getNewPageStrings(language).campfireTitle
}

@Composable
fun CampfirePlanView(
    openVersion: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember(context) { PreferencesManager(context) }
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val campfirePayEnabled = BuildConfig.CAMPFIRE_PAY_ENABLED
    val appLanguage = LocalAppLanguage.current
    val pageStrings = remember(appLanguage) { getNewPageStrings(appLanguage) }
    val isChinese = appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese
    val ways = remember(pageStrings) {
        listOf(pageStrings.campfireWay1, pageStrings.campfireWay2, pageStrings.campfireWay3)
    }
    val shuffledSupporters = remember(openVersion) { campfireSupporters.shuffled() }
    val startupMinePrefs = remember(context) {
        context.getSharedPreferences(startupMineProfilePrefsName, Context.MODE_PRIVATE)
    }
    val userDisplayName = remember(startupMinePrefs, openVersion) {
        startupMinePrefs
            .getString(startupMineUserDisplayNameKey, startupMineDefaultUserName)
            .orEmpty()
            .trim()
            .ifBlank { startupMineDefaultUserName }
    }
    val isCurrentUserInCampfireSupporters = remember(userDisplayName) {
        campfireSupporters.any { supporter ->
            supporter.idLabel.trim().equals(userDisplayName, ignoreCase = true)
        }
    }
    val campfireStrings = remember(appLanguage) { getCampfireStrings(appLanguage) }
    val supportersTitle = campfireStrings.supportersTitle
    val supportersNote = campfireStrings.supportersNote
    val sponsorTitle = campfireStrings.sponsorTitle
    val shareTitle = campfireStrings.shareTitle
    val sponsorSummary = campfireStrings.sponsorSummary
    val shareSummary = campfireStrings.shareSummary
    val feedbackTitle = pageStrings.feedbackTitle
    val feedbackSummary = campfireStrings.feedbackSummary
    val sponsorBody = campfireStrings.sponsorBody
    val sponsorButtonLabel = campfireStrings.sponsorButtonLabel
    val sponsorButtonHint = campfireStrings.sponsorButtonHint
    val sponsorStatusChecking = campfireStrings.sponsorStatusChecking
    val sponsorStatusPaidPrefix = campfireStrings.sponsorStatusPaidPrefix
    val sponsorStatusPending = campfireStrings.sponsorStatusPending
    val sponsorStatusFailed = campfireStrings.sponsorStatusFailed
    val sponsorStatusUnconfigured = campfireStrings.sponsorStatusUnconfigured
    val sponsorLaunchFailed = campfireStrings.sponsorLaunchFailed
    val sponsorRefreshLabel = campfireStrings.sponsorRefreshLabel
    val participationClaimLabel = campfireText(
        language = appLanguage,
        simplified = "我已通过赞助或分享参与篝火计划",
        traditional = "我已透過贊助或分享參與篝火計畫",
        english = "I have joined the Campfire plan via sponsorship or sharing"
    )
    var currentSection by rememberSaveable { mutableStateOf(CampfireSection.Home) }
    var sponsorStatusMessage by remember { mutableStateOf<String?>(null) }
    var sponsorOrderId by remember { mutableStateOf<String?>(null) }
    var sponsorLaunching by remember { mutableStateOf(false) }
    var participationClaimChecked by rememberSaveable {
        mutableStateOf(appPrefs.campfireSelfParticipationChecked)
    }
    var participationConfettiTriggerToken by rememberSaveable { mutableStateOf(0L) }
    var participationConfettiInitialBurstProgress by rememberSaveable { mutableStateOf(0f) }
    var participationConfettiCenterInContainer by remember { mutableStateOf<Offset?>(null) }
    var campfireContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var campfireContainerOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val participationClaimEffectiveChecked = participationClaimChecked || isCurrentUserInCampfireSupporters

    LaunchedEffect(isCurrentUserInCampfireSupporters) {
        if (isCurrentUserInCampfireSupporters) {
            participationClaimChecked = true
            if (!appPrefs.campfireSelfParticipationChecked) {
                appPrefs.campfireSelfParticipationChecked = true
            }
        }
    }

    fun performHaptic() {
        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun openSection(section: CampfireSection) {
        performHaptic()
        currentSection = section
    }

    fun updateParticipationClaim(next: Boolean, buttonCenterInWindow: Offset?) {
        val target = if (isCurrentUserInCampfireSupporters) true else next
        val previous = participationClaimEffectiveChecked
        if (target == previous) return
        val turnsOn = !previous && target
        if (turnsOn) {
            if (buttonCenterInWindow != null && campfireContainerSize.width > 0 && campfireContainerSize.height > 0) {
                participationConfettiCenterInContainer = Offset(
                    x = (buttonCenterInWindow.x - campfireContainerOriginInWindow.x)
                        .coerceIn(0f, campfireContainerSize.width.toFloat()),
                    y = (buttonCenterInWindow.y - campfireContainerOriginInWindow.y)
                        .coerceIn(0f, campfireContainerSize.height.toFloat())
                )
            }
            participationConfettiInitialBurstProgress = 0.08f
            participationConfettiTriggerToken += 1L
        } else {
            performHaptic()
        }
        participationClaimChecked = target
        appPrefs.campfireSelfParticipationChecked = target
    }

    val pageTitle = when (currentSection) {
        CampfireSection.Home -> pageStrings.campfireTitle
        CampfireSection.Sponsor -> sponsorTitle
        CampfireSection.Share -> shareTitle
    }

    BackHandler(enabled = currentSection != CampfireSection.Home) {
        currentSection = CampfireSection.Home
    }

    if (campfirePayEnabled) {
        LaunchedEffect(Unit) {
            CampfirePaymentReturnCenter.events.collect { event ->
                currentSection = CampfireSection.Sponsor
                val orderId = event.orderId
                if (orderId.isNullOrBlank()) {
                    sponsorStatusMessage = sponsorStatusFailed
                    return@collect
                }
                sponsorOrderId = orderId
                sponsorStatusMessage = sponsorStatusChecking
                runCatching {
                    CampfirePaymentApi.queryStatus(orderId)
                }.onSuccess { status ->
                    sponsorStatusMessage = if (status.paid) {
                        sponsorStatusPaidPrefix
                    } else {
                        sponsorStatusFailed
                    }
                }.onFailure {
                    sponsorStatusMessage = sponsorStatusFailed
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { coordinates ->
                campfireContainerOriginInWindow = coordinates.positionInWindow()
                campfireContainerSize = coordinates.size
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        UnifiedSubpageTopBar(
            title = pageTitle,
            onBack = {
                performHaptic()
                if (currentSection == CampfireSection.Home) {
                    onBack()
                } else {
                    currentSection = CampfireSection.Home
                }
            }
        )

        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = currentSection,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = if (forward) {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn()
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn()
                }
                val exit = if (forward) {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut()
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut()
                }
                ContentTransform(enter, exit)
            },
            label = "campfire_section"
        ) { section ->
            when (section) {
                CampfireSection.Home -> CampfireHomeContent(
                    supporters = shuffledSupporters,
                    supportersTitle = supportersTitle,
                    supportersNote = supportersNote,
                    sponsorTitle = sponsorTitle,
                    sponsorSummary = sponsorSummary,
                    shareTitle = shareTitle,
                    shareSummary = shareSummary,
                    feedbackTitle = feedbackTitle,
                    feedbackSummary = feedbackSummary,
                    participationClaimLabel = participationClaimLabel,
                    participationClaimChecked = participationClaimEffectiveChecked,
                    participationClaimAutoChecked = isCurrentUserInCampfireSupporters,
                    onParticipationClaimChanged = ::updateParticipationClaim,
                    onSponsorClick = { openSection(CampfireSection.Sponsor) },
                    onShareClick = { openSection(CampfireSection.Share) },
                    onFeedbackClick = {
                        performHaptic()
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(campfireFeedbackFormUrl)))
                        }.onFailure {
                            Toast.makeText(context, "暂时无法打开反馈表单", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                CampfireSection.Sponsor -> CampfireSponsorContent(
                    sponsorBody = sponsorBody,
                    sponsorButtonLabel = sponsorButtonLabel,
                    sponsorButtonHint = sponsorButtonHint,
                    sponsorStatusMessage = sponsorStatusMessage,
                    sponsorLaunching = sponsorLaunching,
                    campfirePayEnabled = campfirePayEnabled,
                    isChinese = isChinese,
                    appLanguage = appLanguage,
                    participationClaimLabel = participationClaimLabel,
                    participationClaimChecked = participationClaimEffectiveChecked,
                    participationClaimAutoChecked = isCurrentUserInCampfireSupporters,
                    onParticipationClaimChanged = ::updateParticipationClaim,
                    onSponsorClick = {
                        performHaptic()
                        if (!CampfirePaymentApi.isConfigured()) {
                            sponsorStatusMessage = sponsorStatusUnconfigured
                        } else {
                            scope.launch {
                                sponsorLaunching = true
                                sponsorStatusMessage = null
                                runCatching {
                                    CampfirePaymentApi.createOrder(
                                        sponsorName = null,
                                        language = appLanguage.name
                                    )
                                }.onSuccess { order ->
                                    sponsorOrderId = order.orderId
                                    sponsorStatusMessage = sponsorStatusPending
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.checkoutUrl)))
                                }.onFailure {
                                    sponsorStatusMessage = sponsorLaunchFailed
                                }
                                sponsorLaunching = false
                            }
                        }
                    },
                    onCheckStatusClick = {
                        performHaptic()
                        sponsorOrderId?.let { orderId ->
                            scope.launch {
                                sponsorLaunching = true
                                sponsorStatusMessage = sponsorStatusChecking
                                runCatching {
                                    CampfirePaymentApi.queryStatus(orderId)
                                }.onSuccess { status ->
                                    sponsorStatusMessage = if (status.paid) {
                                        sponsorStatusPaidPrefix
                                    } else {
                                        sponsorStatusFailed
                                    }
                                }.onFailure {
                                    sponsorStatusMessage = sponsorStatusFailed
                                }
                                sponsorLaunching = false
                            }
                        }
                    },
                    sponsorRefreshLabel = sponsorRefreshLabel,
                    showCheckStatusButton = sponsorOrderId != null
                )

                CampfireSection.Share -> CampfireShareContent(
                    context = context,
                    clipboard = clipboard,
                    performHaptic = ::performHaptic,
                    appLanguage = appLanguage,
                    pageStrings = pageStrings,
                    ways = ways,
                    participationClaimLabel = participationClaimLabel,
                    participationClaimChecked = participationClaimEffectiveChecked,
                    participationClaimAutoChecked = isCurrentUserInCampfireSupporters,
                    onParticipationClaimChanged = ::updateParticipationClaim
                )
            }
        }
        }

        if (participationConfettiTriggerToken > 0L) {
            CelebrationConfetti(
                intensity = 100,
                triggerToken = participationConfettiTriggerToken,
                initialBurstProgress = participationConfettiInitialBurstProgress,
                burstCenterPx = participationConfettiCenterInContainer
            )
        }
    }
}

@Composable
private fun CampfireHomeContent(
    supporters: List<CampfireSupporter>,
    supportersTitle: String,
    supportersNote: String,
    sponsorTitle: String,
    sponsorSummary: String,
    shareTitle: String,
    shareSummary: String,
    feedbackTitle: String,
    feedbackSummary: String,
    participationClaimLabel: String,
    participationClaimChecked: Boolean,
    participationClaimAutoChecked: Boolean,
    onParticipationClaimChanged: (Boolean, Offset?) -> Unit,
    onSponsorClick: () -> Unit,
    onShareClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.campfire_top),
                contentDescription = "Campfire",
                modifier = Modifier
                    .width(148.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampfireEntryCard(
                title = sponsorTitle,
                body = sponsorSummary,
                modifier = Modifier.weight(1f),
                onClick = onSponsorClick
            )
            CampfireEntryCard(
                title = shareTitle,
                body = shareSummary,
                modifier = Modifier.weight(1f),
                onClick = onShareClick
            )
        }

        CampfireEntryCard(
            title = feedbackTitle,
            body = feedbackSummary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onFeedbackClick
        )

        CampfireSelfParticipationCard(
            label = participationClaimLabel,
            checked = participationClaimChecked,
            onCheckedChange = onParticipationClaimChanged,
            lockedBySupportersList = participationClaimAutoChecked
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .advancedTextureCard(shape = RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = supportersTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = supportersNote,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )

                supporters.chunked(campfireSupportersPerRow).forEach { supporterRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        supporterRow.forEach { supporter ->
                            CampfireSupporterAvatar(
                                supporter = supporter,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(campfireSupportersPerRow - supporterRow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun CampfireSponsorContent(
    sponsorBody: String,
    sponsorButtonLabel: String,
    sponsorButtonHint: String,
    sponsorStatusMessage: String?,
    sponsorLaunching: Boolean,
    campfirePayEnabled: Boolean,
    isChinese: Boolean,
    appLanguage: AppLanguage,
    participationClaimLabel: String,
    participationClaimChecked: Boolean,
    participationClaimAutoChecked: Boolean,
    onParticipationClaimChanged: (Boolean, Offset?) -> Unit,
    onSponsorClick: () -> Unit,
    onCheckStatusClick: () -> Unit,
    sponsorRefreshLabel: String,
    showCheckStatusButton: Boolean
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val kofiPriceText = remember(appLanguage) { getKofiPriceText(appLanguage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isChinese) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampfireQrCard(
                    assetPath = campfireQrAssetPath,
                    contentDescription = "Alipay QR",
                    isDark = isDark,
                    modifier = Modifier.weight(1f, fill = false)
                )
                CampfireQrCard(
                    assetPath = campfireRewardQrAssetPath,
                    contentDescription = "Reward QR",
                    isDark = isDark,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.support_me_on_kofi_badge_beige),
                    contentDescription = "Support on Ko-fi",
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .bounceClick(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(campfireKofiUrl)))
                            }
                        }),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = kofiPriceText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        CampfireSelfParticipationCard(
            label = participationClaimLabel,
            checked = participationClaimChecked,
            onCheckedChange = onParticipationClaimChanged,
            lockedBySupportersList = participationClaimAutoChecked
        )

        if (campfirePayEnabled && isChinese) {
            Button(
                onClick = onSponsorClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !sponsorLaunching,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (sponsorLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = sponsorButtonLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Text(
                text = sponsorButtonHint,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (campfirePayEnabled && isChinese) sponsorStatusMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .advancedTextureCard(shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                    )
                    if (showCheckStatusButton) {
                        Button(
                            onClick = onCheckStatusClick,
                            enabled = !sponsorLaunching,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(if (sponsorLaunching) "..." else sponsorRefreshLabel)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .advancedTextureCard(shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = sponsorBody,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 25.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun CampfireShareContent(
    context: android.content.Context,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    performHaptic: () -> Unit,
    appLanguage: AppLanguage,
    pageStrings: NewPageStrings,
    ways: List<String>,
    participationClaimLabel: String,
    participationClaimChecked: Boolean,
    participationClaimAutoChecked: Boolean,
    onParticipationClaimChanged: (Boolean, Offset?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .advancedTextureCard(shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = pageStrings.campfireIntroTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = pageStrings.campfireIntroBody,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        CampfireSelfParticipationCard(
            label = participationClaimLabel,
            checked = participationClaimChecked,
            onCheckedChange = onParticipationClaimChanged,
            lockedBySupportersList = participationClaimAutoChecked
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .advancedTextureCard(shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = pageStrings.campfireWaysTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                ways.forEachIndexed { index, line ->
                    val isChinese = appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese
                    val hasClickableHandle = isChinese && line.contains(campfireXhsHandle)
                    val hasClickableEmail = isChinese && line.contains(campfireEmail)

                    if (hasClickableHandle || hasClickableEmail) {
                        val annotated = buildAnnotatedString {
                            append(line)
                            if (hasClickableHandle) {
                                val start = line.indexOf(campfireXhsHandle)
                                val end = start + campfireXhsHandle.length
                                addStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF1E88E5),
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    start = start,
                                    end = end
                                )
                                addStringAnnotation(
                                    tag = "XHS",
                                    annotation = campfireXhsUrl,
                                    start = start,
                                    end = end
                                )
                            }
                            if (hasClickableEmail) {
                                val emailStart = line.indexOf(campfireEmail)
                                val emailEnd = emailStart + campfireEmail.length
                                addStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF1E88E5),
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    start = emailStart,
                                    end = emailEnd
                                )
                                addStringAnnotation(
                                    tag = "EMAIL",
                                    annotation = campfireEmail,
                                    start = emailStart,
                                    end = emailEnd
                                )
                            }
                        }
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 23.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations("XHS", offset, offset).firstOrNull()?.let {
                                    performHaptic()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                                    return@ClickableText
                                }
                                annotated.getStringAnnotations("EMAIL", offset, offset).firstOrNull()?.let {
                                    performHaptic()
                                    clipboard.setText(AnnotatedString(it.item))
                                    Toast.makeText(context, "邮箱已复制", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                        )
                    }
                    if (index != ways.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun CampfireSelfParticipationCard(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean, Offset?) -> Unit,
    lockedBySupportersList: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uncheckedContainer = Color(0xFFF3F5F7)
    val uncheckedIcon = Color(0xFF9CA6B2)
    val uncheckedBorder = Color(0xFFDCE2E8)
    val checkedContainer = Color(0xFF2FB86F)
    var buttonCenterInWindow by remember { mutableStateOf<Offset?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .advancedTextureCard(shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (checked) checkedContainer else uncheckedContainer)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        buttonCenterInWindow = Offset(
                            x = position.x + coordinates.size.width / 2f,
                            y = position.y + coordinates.size.height / 2f
                        )
                    }
                    .border(
                        width = 1.dp,
                        color = if (checked) checkedContainer else uncheckedBorder,
                        shape = CircleShape
                    )
                    .then(
                        if (lockedBySupportersList) {
                            Modifier
                        } else {
                            Modifier.bounceClick(onClick = { onCheckedChange(!checked, buttonCenterInWindow) })
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Campfire Participation Checked",
                    tint = if (checked) Color.White else uncheckedIcon,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CampfireQrCard(
    assetPath: String,
    contentDescription: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember(assetPath) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .widthIn(max = 248.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.24f else 0.12f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(assetPath)
                    .crossfade(false)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = CircleShape
                )
                .then(
                    if (isSaving) {
                        Modifier
                    } else {
                        Modifier.bounceClick {
                            scope.launch {
                                isSaving = true
                                val savedUri = saveCampfireQrAssetToGallery(
                                    context = context,
                                    assetPath = assetPath
                                )
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    if (savedUri != null) "二维码已保存到相册" else "保存失败，请稍后再试",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.6.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "保存二维码",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private suspend fun saveCampfireQrAssetToGallery(
    context: Context,
    assetPath: String
): Uri? = withContext(Dispatchers.IO) {
    val assetRelativePath = assetPath.substringAfter("file:///android_asset/", missingDelimiterValue = "")
    if (assetRelativePath.isBlank()) return@withContext null

    val extension = assetRelativePath.substringAfterLast('.', "webp")
    val mimeType = when (extension.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "image/webp"
    }
    val fileName = "photoo_campfire_qr_${System.currentTimeMillis()}.$extension"
    val resolver = context.contentResolver
    val nowSeconds = System.currentTimeMillis() / 1000L
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, campfireQrExportImageFolder)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null

    runCatching {
        context.assets.open(assetRelativePath).use { input ->
            resolver.openOutputStream(savedUri)?.use { output ->
                input.copyTo(output)
            } ?: throw IllegalStateException("Failed to open output stream")
        }
    }.onFailure {
        runCatching { resolver.delete(savedUri, null, null) }
        return@withContext null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.update(
            savedUri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )
    }
    savedUri
}

@Composable
private fun CampfireEntryCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(
        alpha = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.24f else 0.12f
    )

    Card(
        modifier = modifier
            .fillMaxHeight()
            .advancedTextureCard(shape = RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .bounceClick(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
private fun CampfireSupporterAvatar(
    supporter: CampfireSupporter,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/campfire_supporters/${supporter.assetName}")
                .crossfade(false)
                .build(),
            contentDescription = supporter.idLabel,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentScale = ContentScale.Crop
        )
        Text(
            text = supporter.idLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.1.sp, lineHeight = 8.6.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 0.5.dp)
        )
    }
}
