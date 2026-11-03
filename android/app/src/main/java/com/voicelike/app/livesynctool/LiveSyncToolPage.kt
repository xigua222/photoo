package com.voicelike.app.livesynctool

import android.app.Activity
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.voicelike.app.AppLanguage
import com.voicelike.app.LocalAppLanguage
import com.voicelike.app.UnifiedSubpageTopBarIconButton
import kotlinx.coroutines.launch

private val LiveSyncAccent = Color(0xFFDB7C26)
private val LiveSyncAcceptanceAccent = Color(0xFF4F83CC)

private data class LiveSyncToolCopy(
    val title: String,
    val restorePrefix: String,
    val restoreSubtitle: String,
    val justEntered: String,
    val toRepair: String,
    val selected: String,
    val lastUpdated: String,
    val scanningTitle: String,
    val scanningSubtitle: String,
    val emptyTitle: String,
    val emptySubtitle: String,
    val converted: String,
    val failed: String,
    val resultsTitle: String,
    val reviewTitle: String,
    val reviewSubtitle: String,
    val newPhotos: String,
    val oldPendingDelete: String,
    val openAlbumFailed: String,
    val viewFirst: String,
    val deleteAllOld: String,
    val back: String,
    val refresh: String,
    val converting: String,
    val selectedItems: String,
    val scanning: String,
    val found: String,
    val unnamedPhoto: String,
    val clearAll: String,
    val selectAll: String,
    val startConvert: String,
    val oldDeleted: String,
    val deleteOldFailed: String,
    val pendingReview: String
)

private fun getLiveSyncToolCopy(language: AppLanguage): LiveSyncToolCopy = when (language) {
    AppLanguage.SimplifiedChinese -> LiveSyncToolCopy(
        title = "Live 修复工具",
        restorePrefix = "恢复为 ",
        restoreSubtitle = "将其他设备同步过来的 Live Photo 文件，转化为本机相册和三方应用可识别的格式。",
        justEntered = "刚进入",
        toRepair = "待修复",
        selected = "已选中",
        lastUpdated = "最近更新",
        scanningTitle = "正在扫描动态照片",
        scanningSubtitle = "会先恢复上次结果，再逐步刷新本次扫描。",
        emptyTitle = "没有发现待修复的动态照片",
        emptySubtitle = "如果刚导入过照片，可以点右上角重新扫描。",
        converted = "已转换",
        failed = "失败",
        resultsTitle = "修复结果验收",
        reviewTitle = "先验收新照片，再删除旧照片",
        reviewSubtitle = "点按或长按任意新图，都会直接跳到系统相册里确认 Live 是否已经生效。",
        newPhotos = "新照片",
        oldPendingDelete = "旧图待删",
        openAlbumFailed = "系统相册打开失败",
        viewFirst = "查看第一张",
        deleteAllOld = "删除全部旧图",
        back = "返回",
        refresh = "刷新",
        converting = "正在转换",
        selectedItems = "选中项目",
        scanning = "正在扫描",
        found = "已发现",
        unnamedPhoto = "未命名照片",
        clearAll = "全不选",
        selectAll = "全选",
        startConvert = "开始转换",
        oldDeleted = "旧图已删",
        deleteOldFailed = "删除旧照片失败",
        pendingReview = "待验收"
    )
    AppLanguage.TraditionalChinese -> LiveSyncToolCopy(
        title = "Live 修復工具",
        restorePrefix = "恢復為 ",
        restoreSubtitle = "將其他裝置同步過來的 Live Photo 檔案，轉化為本機相簿和第三方應用可識別的格式。",
        justEntered = "剛進入",
        toRepair = "待修復",
        selected = "已選中",
        lastUpdated = "最近更新",
        scanningTitle = "正在掃描動態照片",
        scanningSubtitle = "會先恢復上次結果，再逐步刷新本次掃描。",
        emptyTitle = "沒有發現待修復的動態照片",
        emptySubtitle = "如果剛匯入過照片，可以點右上角重新掃描。",
        converted = "已轉換",
        failed = "失敗",
        resultsTitle = "修復結果驗收",
        reviewTitle = "先驗收新照片，再刪除舊照片",
        reviewSubtitle = "點按或長按任意新圖，都會直接跳到系統相簿裡確認 Live 是否已經生效。",
        newPhotos = "新照片",
        oldPendingDelete = "舊圖待刪",
        openAlbumFailed = "系統相簿打開失敗",
        viewFirst = "查看第一張",
        deleteAllOld = "刪除全部舊圖",
        back = "返回",
        refresh = "重新整理",
        converting = "正在轉換",
        selectedItems = "選中項目",
        scanning = "正在掃描",
        found = "已發現",
        unnamedPhoto = "未命名照片",
        clearAll = "全不選",
        selectAll = "全選",
        startConvert = "開始轉換",
        oldDeleted = "舊圖已刪",
        deleteOldFailed = "刪除舊照片失敗",
        pendingReview = "待驗收"
    )
    AppLanguage.Korean -> LiveSyncToolCopy(
        title = "Live 복구 도구",
        restorePrefix = "다음으로 복구: ",
        restoreSubtitle = "다른 기기에서 동기화된 Live Photo 파일을 이 기기 앨범과 서드파티 앱이 인식할 수 있는 형식으로 변환합니다.",
        justEntered = "방금 들어옴",
        toRepair = "복구 대기",
        selected = "선택됨",
        lastUpdated = "최근 업데이트",
        scanningTitle = "동적 사진을 스캔하는 중",
        scanningSubtitle = "먼저 지난 결과를 복원한 뒤, 이번 스캔 결과를 단계적으로 새로고칩니다.",
        emptyTitle = "복구할 동적 사진을 찾지 못했습니다",
        emptySubtitle = "방금 사진을 가져왔다면 오른쪽 위에서 다시 스캔할 수 있습니다.",
        converted = "변환 완료",
        failed = "실패",
        resultsTitle = "복구 결과 확인",
        reviewTitle = "새 사진을 먼저 확인한 뒤 이전 사진을 삭제하세요",
        reviewSubtitle = "새 사진을 탭하거나 길게 누르면 시스템 앨범으로 바로 이동해 Live 적용 여부를 확인할 수 있습니다.",
        newPhotos = "새 사진",
        oldPendingDelete = "삭제 대기 기존 사진",
        openAlbumFailed = "시스템 앨범을 열지 못했습니다",
        viewFirst = "첫 번째 보기",
        deleteAllOld = "기존 사진 모두 삭제",
        back = "뒤로",
        refresh = "새로고침",
        converting = "변환 중",
        selectedItems = "선택한 항목",
        scanning = "스캔 중",
        found = "발견됨",
        unnamedPhoto = "이름 없는 사진",
        clearAll = "전체 해제",
        selectAll = "전체 선택",
        startConvert = "변환 시작",
        oldDeleted = "기존 사진 삭제됨",
        deleteOldFailed = "기존 사진 삭제 실패",
        pendingReview = "확인 대기"
    )
    AppLanguage.Japanese -> LiveSyncToolCopy(
        title = "Live 修復ツール",
        restorePrefix = "次として復元: ",
        restoreSubtitle = "他の端末から同期された Live Photo ファイルを、この端末のアルバムやサードパーティアプリが認識できる形式に変換します。",
        justEntered = "今入ったところ",
        toRepair = "修復待ち",
        selected = "選択済み",
        lastUpdated = "最近の更新",
        scanningTitle = "動的写真をスキャン中",
        scanningSubtitle = "まず前回の結果を復元し、その後今回のスキャン結果を順次更新します。",
        emptyTitle = "修復が必要な動的写真は見つかりませんでした",
        emptySubtitle = "写真を取り込んだばかりなら、右上から再スキャンできます。",
        converted = "変換済み",
        failed = "失敗",
        resultsTitle = "修復結果の確認",
        reviewTitle = "新しい写真を確認してから古い写真を削除してください",
        reviewSubtitle = "新しい画像をタップまたは長押しすると、システムアルバムに直接移動して Live が有効か確認できます。",
        newPhotos = "新しい写真",
        oldPendingDelete = "削除待ちの旧写真",
        openAlbumFailed = "システムアルバムを開けませんでした",
        viewFirst = "最初の1枚を見る",
        deleteAllOld = "古い写真をすべて削除",
        back = "戻る",
        refresh = "更新",
        converting = "変換中",
        selectedItems = "選択項目",
        scanning = "スキャン中",
        found = "検出済み",
        unnamedPhoto = "無題の写真",
        clearAll = "すべて解除",
        selectAll = "すべて選択",
        startConvert = "変換開始",
        oldDeleted = "旧写真を削除済み",
        deleteOldFailed = "旧写真の削除に失敗しました",
        pendingReview = "確認待ち"
    )
    AppLanguage.French -> LiveSyncToolCopy(
        title = "Outil de réparation Live",
        restorePrefix = "Restaurer en ",
        restoreSubtitle = "Convertir les fichiers Live Photo synchronisés depuis d'autres appareils dans un format reconnu par cet album et les applications tierces.",
        justEntered = "À l'instant",
        toRepair = "À réparer",
        selected = "Sélectionné",
        lastUpdated = "Dernière mise à jour",
        scanningTitle = "Analyse des photos dynamiques",
        scanningSubtitle = "Les résultats précédents seront restaurés d'abord, puis cette analyse sera actualisée progressivement.",
        emptyTitle = "Aucune photo dynamique à réparer",
        emptySubtitle = "Si vous venez d'importer des photos, relancez l'analyse depuis le coin supérieur droit.",
        converted = "Converti",
        failed = "Échec",
        resultsTitle = "Vérification des résultats",
        reviewTitle = "Vérifiez d'abord les nouvelles photos, puis supprimez les anciennes",
        reviewSubtitle = "Touchez ou maintenez une nouvelle photo pour ouvrir l'album système et vérifier que Live fonctionne.",
        newPhotos = "Nouvelles photos",
        oldPendingDelete = "Anciennes à supprimer",
        openAlbumFailed = "Impossible d'ouvrir l'album système",
        viewFirst = "Voir la première",
        deleteAllOld = "Supprimer toutes les anciennes",
        back = "Retour",
        refresh = "Actualiser",
        converting = "Conversion",
        selectedItems = "éléments sélectionnés",
        scanning = "Analyse",
        found = "Trouvé",
        unnamedPhoto = "Photo sans nom",
        clearAll = "Tout désélectionner",
        selectAll = "Tout sélectionner",
        startConvert = "Commencer la conversion",
        oldDeleted = "Ancienne supprimée",
        deleteOldFailed = "Échec de la suppression des anciennes photos",
        pendingReview = "À vérifier"
    )
    AppLanguage.German -> LiveSyncToolCopy(
        title = "Live-Reparaturwerkzeug",
        restorePrefix = "Wiederherstellen als ",
        restoreSubtitle = "Live-Photo-Dateien, die von anderen Geräten synchronisiert wurden, in ein Format umwandeln, das von diesem Album und Drittanbieter-Apps erkannt wird.",
        justEntered = "Gerade geöffnet",
        toRepair = "Zu reparieren",
        selected = "Ausgewählt",
        lastUpdated = "Zuletzt aktualisiert",
        scanningTitle = "Dynamische Fotos werden gescannt",
        scanningSubtitle = "Zuerst werden die letzten Ergebnisse wiederhergestellt, danach wird dieser Scan schrittweise aktualisiert.",
        emptyTitle = "Keine dynamischen Fotos zur Reparatur gefunden",
        emptySubtitle = "Wenn du gerade Fotos importiert hast, starte den Scan oben rechts erneut.",
        converted = "Konvertiert",
        failed = "Fehlgeschlagen",
        resultsTitle = "Ergebnisse prüfen",
        reviewTitle = "Prüfe zuerst die neuen Fotos und lösche danach die alten",
        reviewSubtitle = "Tippe oder halte ein neues Foto gedrückt, um direkt das Systemalbum zu öffnen und zu prüfen, ob Live funktioniert.",
        newPhotos = "Neue Fotos",
        oldPendingDelete = "Alte zum Löschen",
        openAlbumFailed = "Systemalbum konnte nicht geöffnet werden",
        viewFirst = "Erstes ansehen",
        deleteAllOld = "Alle alten löschen",
        back = "Zurück",
        refresh = "Aktualisieren",
        converting = "Konvertiere",
        selectedItems = "ausgewählte Elemente",
        scanning = "Scanne",
        found = "Gefunden",
        unnamedPhoto = "Unbenanntes Foto",
        clearAll = "Alle abwählen",
        selectAll = "Alle auswählen",
        startConvert = "Konvertierung starten",
        oldDeleted = "Altes Foto gelöscht",
        deleteOldFailed = "Alte Fotos konnten nicht gelöscht werden",
        pendingReview = "Prüfung ausstehend"
    )
    AppLanguage.Italian -> LiveSyncToolCopy(
        title = "Strumento di ripristino Live",
        restorePrefix = "Ripristina come ",
        restoreSubtitle = "Converti i file Live Photo sincronizzati da altri dispositivi in un formato riconosciuto da questo album e dalle app di terze parti.",
        justEntered = "Appena entrato",
        toRepair = "Da ripristinare",
        selected = "Selezionato",
        lastUpdated = "Ultimo aggiornamento",
        scanningTitle = "Scansione delle foto dinamiche",
        scanningSubtitle = "Prima verranno ripristinati i risultati precedenti, poi questa scansione verrà aggiornata gradualmente.",
        emptyTitle = "Nessuna foto dinamica da ripristinare trovata",
        emptySubtitle = "Se hai appena importato foto, puoi rieseguire la scansione dall'angolo in alto a destra.",
        converted = "Convertito",
        failed = "Fallito",
        resultsTitle = "Verifica risultati",
        reviewTitle = "Controlla prima le nuove foto, poi elimina quelle vecchie",
        reviewSubtitle = "Tocca o tieni premuta una nuova immagine per aprire direttamente l'album di sistema e verificare che Live sia attivo.",
        newPhotos = "Nuove foto",
        oldPendingDelete = "Vecchie da eliminare",
        openAlbumFailed = "Impossibile aprire l'album di sistema",
        viewFirst = "Vedi la prima",
        deleteAllOld = "Elimina tutte le vecchie",
        back = "Indietro",
        refresh = "Aggiorna",
        converting = "Conversione",
        selectedItems = "elementi selezionati",
        scanning = "Scansione",
        found = "Trovate",
        unnamedPhoto = "Foto senza nome",
        clearAll = "Deseleziona tutto",
        selectAll = "Seleziona tutto",
        startConvert = "Avvia conversione",
        oldDeleted = "Vecchia eliminata",
        deleteOldFailed = "Eliminazione delle vecchie foto non riuscita",
        pendingReview = "Da verificare"
    )
    AppLanguage.Portuguese -> LiveSyncToolCopy(
        title = "Ferramenta de reparo Live",
        restorePrefix = "Restaurar como ",
        restoreSubtitle = "Converter arquivos Live Photo sincronizados de outros dispositivos para um formato reconhecido por este álbum e por apps de terceiros.",
        justEntered = "Acabou de entrar",
        toRepair = "Para reparar",
        selected = "Selecionado",
        lastUpdated = "Atualizado recentemente",
        scanningTitle = "Escaneando fotos dinâmicas",
        scanningSubtitle = "Primeiro os resultados anteriores serão restaurados, depois esta varredura será atualizada gradualmente.",
        emptyTitle = "Nenhuma foto dinâmica para reparar encontrada",
        emptySubtitle = "Se você acabou de importar fotos, pode escanear novamente pelo canto superior direito.",
        converted = "Convertido",
        failed = "Falhou",
        resultsTitle = "Revisar resultados",
        reviewTitle = "Revise primeiro as novas fotos e depois exclua as antigas",
        reviewSubtitle = "Toque ou pressione uma nova foto para abrir diretamente o álbum do sistema e confirmar se o Live já está funcionando.",
        newPhotos = "Novas fotos",
        oldPendingDelete = "Antigas para excluir",
        openAlbumFailed = "Não foi possível abrir o álbum do sistema",
        viewFirst = "Ver a primeira",
        deleteAllOld = "Excluir todas as antigas",
        back = "Voltar",
        refresh = "Atualizar",
        converting = "Convertendo",
        selectedItems = "itens selecionados",
        scanning = "Escaneando",
        found = "Encontradas",
        unnamedPhoto = "Foto sem nome",
        clearAll = "Limpar tudo",
        selectAll = "Selecionar tudo",
        startConvert = "Iniciar conversão",
        oldDeleted = "Foto antiga excluída",
        deleteOldFailed = "Falha ao excluir fotos antigas",
        pendingReview = "Aguardando revisão"
    )
    AppLanguage.Spanish -> LiveSyncToolCopy(
        title = "Herramienta de reparación Live",
        restorePrefix = "Restaurar como ",
        restoreSubtitle = "Convierte los archivos Live Photo sincronizados desde otros dispositivos a un formato reconocido por este álbum y por aplicaciones de terceros.",
        justEntered = "Recién entrado",
        toRepair = "Por reparar",
        selected = "Seleccionado",
        lastUpdated = "Actualizado recientemente",
        scanningTitle = "Escaneando fotos dinámicas",
        scanningSubtitle = "Primero se restaurarán los resultados anteriores y luego este escaneo se actualizará poco a poco.",
        emptyTitle = "No se encontraron fotos dinámicas para reparar",
        emptySubtitle = "Si acabas de importar fotos, puedes volver a escanear desde la esquina superior derecha.",
        converted = "Convertido",
        failed = "Falló",
        resultsTitle = "Revisar resultados",
        reviewTitle = "Primero revisa las fotos nuevas y luego elimina las antiguas",
        reviewSubtitle = "Toca o mantén pulsada cualquier foto nueva para abrir directamente el álbum del sistema y confirmar si Live ya funciona.",
        newPhotos = "Fotos nuevas",
        oldPendingDelete = "Antiguas por borrar",
        openAlbumFailed = "No se pudo abrir el álbum del sistema",
        viewFirst = "Ver la primera",
        deleteAllOld = "Eliminar todas las antiguas",
        back = "Volver",
        refresh = "Actualizar",
        converting = "Convirtiendo",
        selectedItems = "elementos seleccionados",
        scanning = "Escaneando",
        found = "Encontradas",
        unnamedPhoto = "Foto sin nombre",
        clearAll = "Deseleccionar todo",
        selectAll = "Seleccionar todo",
        startConvert = "Empezar conversión",
        oldDeleted = "Foto antigua eliminada",
        deleteOldFailed = "No se pudieron eliminar las fotos antiguas",
        pendingReview = "Pendiente de revisar"
    )
    AppLanguage.Russian -> LiveSyncToolCopy(
        title = "Инструмент восстановления Live",
        restorePrefix = "Восстановить как ",
        restoreSubtitle = "Преобразует файлы Live Photo, синхронизированные с других устройств, в формат, распознаваемый этим альбомом и сторонними приложениями.",
        justEntered = "Только что открыто",
        toRepair = "К восстановлению",
        selected = "Выбрано",
        lastUpdated = "Последнее обновление",
        scanningTitle = "Идёт сканирование динамических фото",
        scanningSubtitle = "Сначала будут восстановлены предыдущие результаты, затем текущее сканирование будет постепенно обновляться.",
        emptyTitle = "Динамические фото для восстановления не найдены",
        emptySubtitle = "Если вы только что импортировали фото, попробуйте повторно запустить сканирование из правого верхнего угла.",
        converted = "Преобразовано",
        failed = "Не удалось",
        resultsTitle = "Проверка результатов",
        reviewTitle = "Сначала проверьте новые фото, затем удаляйте старые",
        reviewSubtitle = "Нажмите или удерживайте любое новое фото, чтобы открыть системный альбом и проверить, заработал ли Live.",
        newPhotos = "Новые фото",
        oldPendingDelete = "Старые к удалению",
        openAlbumFailed = "Не удалось открыть системный альбом",
        viewFirst = "Открыть первое",
        deleteAllOld = "Удалить все старые",
        back = "Назад",
        refresh = "Обновить",
        converting = "Преобразование",
        selectedItems = "выбранные элементы",
        scanning = "Сканирование",
        found = "Найдено",
        unnamedPhoto = "Фото без названия",
        clearAll = "Снять всё",
        selectAll = "Выбрать всё",
        startConvert = "Начать преобразование",
        oldDeleted = "Старое фото удалено",
        deleteOldFailed = "Не удалось удалить старые фото",
        pendingReview = "Ожидает проверки"
    )
    AppLanguage.Thai -> LiveSyncToolCopy(
        title = "เครื่องมือซ่อม Live",
        restorePrefix = "กู้คืนเป็น ",
        restoreSubtitle = "แปลงไฟล์ Live Photo ที่ซิงก์มาจากอุปกรณ์อื่นให้อยู่ในรูปแบบที่อัลบั้มเครื่องนี้และแอปภายนอกรู้จักได้",
        justEntered = "เพิ่งเข้า",
        toRepair = "รอซ่อม",
        selected = "เลือกแล้ว",
        lastUpdated = "อัปเดตล่าสุด",
        scanningTitle = "กำลังสแกนภาพเคลื่อนไหว",
        scanningSubtitle = "ระบบจะกู้คืนผลลัพธ์ครั้งก่อนก่อน แล้วค่อยอัปเดตผลการสแกนครั้งนี้ทีละส่วน",
        emptyTitle = "ไม่พบภาพเคลื่อนไหวที่ต้องซ่อม",
        emptySubtitle = "หากเพิ่งนำเข้ารูปภาพ สามารถกดสแกนใหม่จากมุมขวาบนได้",
        converted = "แปลงแล้ว",
        failed = "ล้มเหลว",
        resultsTitle = "ตรวจรับผลการซ่อม",
        reviewTitle = "ตรวจรูปใหม่ก่อน แล้วค่อยลบรูปเก่า",
        reviewSubtitle = "แตะหรือกดค้างที่รูปใหม่ใดก็ได้ ระบบจะพาไปยังอัลบั้มของระบบทันทีเพื่อตรวจว่า Live ใช้งานได้แล้วหรือไม่",
        newPhotos = "รูปใหม่",
        oldPendingDelete = "รูปรอการลบ",
        openAlbumFailed = "เปิดอัลบั้มของระบบไม่สำเร็จ",
        viewFirst = "ดูรูปแรก",
        deleteAllOld = "ลบรูปเก่าทั้งหมด",
        back = "ย้อนกลับ",
        refresh = "รีเฟรช",
        converting = "กำลังแปลง",
        selectedItems = "รายการที่เลือก",
        scanning = "กำลังสแกน",
        found = "พบแล้ว",
        unnamedPhoto = "รูปไม่มีชื่อ",
        clearAll = "ไม่เลือกทั้งหมด",
        selectAll = "เลือกทั้งหมด",
        startConvert = "เริ่มแปลง",
        oldDeleted = "ลบรูปเก่าแล้ว",
        deleteOldFailed = "ลบรูปเก่าไม่สำเร็จ",
        pendingReview = "รอตรวจรับ"
    )
    AppLanguage.Vietnamese -> LiveSyncToolCopy(
        title = "Công cụ sửa Live",
        restorePrefix = "Khôi phục thành ",
        restoreSubtitle = "Chuyển các tệp Live Photo được đồng bộ từ thiết bị khác sang định dạng mà album máy này và ứng dụng bên thứ ba có thể nhận diện.",
        justEntered = "Vừa vào",
        toRepair = "Chờ sửa",
        selected = "Đã chọn",
        lastUpdated = "Cập nhật gần đây",
        scanningTitle = "Đang quét ảnh động",
        scanningSubtitle = "Hệ thống sẽ khôi phục kết quả lần trước trước, sau đó làm mới dần kết quả quét lần này.",
        emptyTitle = "Không tìm thấy ảnh động cần sửa",
        emptySubtitle = "Nếu bạn vừa nhập ảnh, có thể quét lại ở góc trên bên phải.",
        converted = "Đã chuyển",
        failed = "Thất bại",
        resultsTitle = "Nghiệm thu kết quả sửa",
        reviewTitle = "Hãy kiểm tra ảnh mới trước rồi mới xóa ảnh cũ",
        reviewSubtitle = "Chạm hoặc nhấn giữ bất kỳ ảnh mới nào để mở ngay album hệ thống và xác nhận Live đã hoạt động hay chưa.",
        newPhotos = "Ảnh mới",
        oldPendingDelete = "Ảnh cũ chờ xóa",
        openAlbumFailed = "Không thể mở album hệ thống",
        viewFirst = "Xem ảnh đầu tiên",
        deleteAllOld = "Xóa toàn bộ ảnh cũ",
        back = "Quay lại",
        refresh = "Làm mới",
        converting = "Đang chuyển đổi",
        selectedItems = "mục đã chọn",
        scanning = "Đang quét",
        found = "Đã tìm thấy",
        unnamedPhoto = "Ảnh chưa đặt tên",
        clearAll = "Bỏ chọn hết",
        selectAll = "Chọn tất cả",
        startConvert = "Bắt đầu chuyển đổi",
        oldDeleted = "Ảnh cũ đã xóa",
        deleteOldFailed = "Xóa ảnh cũ thất bại",
        pendingReview = "Chờ nghiệm thu"
    )
    AppLanguage.Indonesian -> LiveSyncToolCopy(
        title = "Alat perbaikan Live",
        restorePrefix = "Pulihkan sebagai ",
        restoreSubtitle = "Ubah file Live Photo yang disinkronkan dari perangkat lain ke format yang dikenali album perangkat ini dan aplikasi pihak ketiga.",
        justEntered = "Baru masuk",
        toRepair = "Perlu diperbaiki",
        selected = "Dipilih",
        lastUpdated = "Terakhir diperbarui",
        scanningTitle = "Memindai foto dinamis",
        scanningSubtitle = "Hasil sebelumnya akan dipulihkan lebih dulu, lalu hasil pemindaian ini akan diperbarui secara bertahap.",
        emptyTitle = "Tidak ditemukan foto dinamis yang perlu diperbaiki",
        emptySubtitle = "Jika baru mengimpor foto, Anda bisa memindai ulang dari pojok kanan atas.",
        converted = "Sudah dikonversi",
        failed = "Gagal",
        resultsTitle = "Tinjau hasil perbaikan",
        reviewTitle = "Tinjau dulu foto baru, lalu hapus foto lama",
        reviewSubtitle = "Ketuk atau tekan lama foto baru mana pun untuk langsung membuka album sistem dan memastikan Live sudah aktif.",
        newPhotos = "Foto baru",
        oldPendingDelete = "Foto lama menunggu dihapus",
        openAlbumFailed = "Gagal membuka album sistem",
        viewFirst = "Lihat yang pertama",
        deleteAllOld = "Hapus semua yang lama",
        back = "Kembali",
        refresh = "Segarkan",
        converting = "Mengonversi",
        selectedItems = "item yang dipilih",
        scanning = "Memindai",
        found = "Ditemukan",
        unnamedPhoto = "Foto tanpa nama",
        clearAll = "Batalkan semua",
        selectAll = "Pilih semua",
        startConvert = "Mulai konversi",
        oldDeleted = "Foto lama dihapus",
        deleteOldFailed = "Gagal menghapus foto lama",
        pendingReview = "Menunggu ditinjau"
    )
    AppLanguage.Arabic -> LiveSyncToolCopy(
        title = "أداة إصلاح Live",
        restorePrefix = "استعادة كـ ",
        restoreSubtitle = "تحوّل ملفات Live Photo المتزامنة من أجهزة أخرى إلى صيغة يمكن لهذا الألبوم وتطبيقات الطرف الثالث التعرف عليها.",
        justEntered = "تم الدخول الآن",
        toRepair = "بانتظار الإصلاح",
        selected = "محدد",
        lastUpdated = "آخر تحديث",
        scanningTitle = "جارٍ فحص الصور المتحركة",
        scanningSubtitle = "سيتم أولاً استعادة نتائج المرة السابقة، ثم تحديث نتائج هذا الفحص تدريجيًا.",
        emptyTitle = "لم يتم العثور على صور متحركة تحتاج إلى إصلاح",
        emptySubtitle = "إذا كنت قد استوردت صورًا للتو، يمكنك إعادة الفحص من الزاوية اليمنى العليا.",
        converted = "تم التحويل",
        failed = "فشل",
        resultsTitle = "مراجعة نتائج الإصلاح",
        reviewTitle = "راجع الصور الجديدة أولاً ثم احذف الصور القديمة",
        reviewSubtitle = "انقر أو اضغط مطولاً على أي صورة جديدة للانتقال مباشرة إلى ألبوم النظام والتأكد من أن Live يعمل بالفعل.",
        newPhotos = "صور جديدة",
        oldPendingDelete = "صور قديمة بانتظار الحذف",
        openAlbumFailed = "تعذر فتح ألبوم النظام",
        viewFirst = "عرض الأولى",
        deleteAllOld = "حذف كل الصور القديمة",
        back = "رجوع",
        refresh = "تحديث",
        converting = "جارٍ التحويل",
        selectedItems = "العناصر المحددة",
        scanning = "جارٍ الفحص",
        found = "تم العثور على",
        unnamedPhoto = "صورة بلا اسم",
        clearAll = "إلغاء تحديد الكل",
        selectAll = "تحديد الكل",
        startConvert = "بدء التحويل",
        oldDeleted = "تم حذف الصورة القديمة",
        deleteOldFailed = "فشل حذف الصور القديمة",
        pendingReview = "بانتظار المراجعة"
    )
    AppLanguage.Ukrainian -> LiveSyncToolCopy(
        title = "Інструмент відновлення Live",
        restorePrefix = "Відновити як ",
        restoreSubtitle = "Перетворіть файли Live Photo, синхронізовані з інших пристроїв, на формат, який розпізнають ця галерея та сторонні застосунки.",
        justEntered = "Щойно відкрито",
        toRepair = "До відновлення",
        selected = "Вибрано",
        lastUpdated = "Останнє оновлення",
        scanningTitle = "Сканування динамічних фото",
        scanningSubtitle = "Спершу відновляться попередні результати, потім сканування оновлюватиметься поступово.",
        emptyTitle = "Динамічних фото для відновлення немає",
        emptySubtitle = "Якщо ви щойно імпортували фото, торкніться «Оновити» у верхньому правому куті.",
        converted = "Перетворено",
        failed = "Помилка",
        resultsTitle = "Перегляд результатів",
        reviewTitle = "Перегляньте нові фото, перш ніж видаляти старі",
        reviewSubtitle = "Торкніться або утримуйте будь-який новий елемент, щоб перейти до системної галереї та переконатися, що Live працює.",
        newPhotos = "Нові фото",
        oldPendingDelete = "Старі, що очікують видалення",
        openAlbumFailed = "Не вдалося відкрити системну галерею",
        viewFirst = "Переглянути спершу",
        deleteAllOld = "Видалити всі старі",
        back = "Назад",
        refresh = "Оновити",
        converting = "Перетворення",
        selectedItems = "вибраних елементів",
        scanning = "Сканування",
        found = "Знайдено",
        unnamedPhoto = "Фото без назви",
        clearAll = "Очистити все",
        selectAll = "Вибрати все",
        startConvert = "Перетворити",
        oldDeleted = "Старі видалено",
        deleteOldFailed = "Не вдалося видалити старі фото",
        pendingReview = "Очікує перегляду"
    )
    AppLanguage.Polish -> LiveSyncToolCopy(
        title = "Narzędzie naprawy Live",
        restorePrefix = "Przywróć jako ",
        restoreSubtitle = "Zamień pliki Live Photo zsynchronizowane z innych urządzeń na format rozpoznawany przez tę galerię i aplikacje innych firm.",
        justEntered = "Właśnie otwarto",
        toRepair = "Do naprawy",
        selected = "Wybrano",
        lastUpdated = "Ostatnia aktualizacja",
        scanningTitle = "Skanowanie zdjęć dynamicznych",
        scanningSubtitle = "Najpierw zostaną przywrócone poprzednie wyniki, a potem skanowanie będzie się stopniowo odświeżać.",
        emptyTitle = "Brak zdjęć dynamicznych do naprawy",
        emptySubtitle = "Jeśli właśnie zaimportowano zdjęcia, dotknij odświeżania w prawym górnym rogu.",
        converted = "Przekonwertowano",
        failed = "Niepowodzenie",
        resultsTitle = "Sprawdź wyniki",
        reviewTitle = "Sprawdź nowe zdjęcia przed usunięciem starych",
        reviewSubtitle = "Dotknij lub przytrzymaj dowolny nowy element, aby przejść do galerii systemowej i sprawdzić, czy Live działa.",
        newPhotos = "Nowe zdjęcia",
        oldPendingDelete = "Stare do usunięcia",
        openAlbumFailed = "Nie można otworzyć galerii systemowej",
        viewFirst = "Najpierw zobacz",
        deleteAllOld = "Usuń wszystkie stare",
        back = "Wstecz",
        refresh = "Odśwież",
        converting = "Konwertowanie",
        selectedItems = "wybranych elementów",
        scanning = "Skanowanie",
        found = "Znaleziono",
        unnamedPhoto = "Zdjęcie bez nazwy",
        clearAll = "Wyczyść wszystko",
        selectAll = "Zaznacz wszystko",
        startConvert = "Konwertuj",
        oldDeleted = "Stare usunięto",
        deleteOldFailed = "Nie udało się usunąć starych zdjęć",
        pendingReview = "Czeka na sprawdzenie"
    )
    AppLanguage.Czech -> LiveSyncToolCopy(
        title = "Nástroj pro opravu Live",
        restorePrefix = "Obnovit jako ",
        restoreSubtitle = "Převeďte soubory Live Photo synchronizované z jiných zařízení do formátu, který rozpozná tato galerie i aplikace třetích stran.",
        justEntered = "Právě otevřeno",
        toRepair = "K opravě",
        selected = "Vybráno",
        lastUpdated = "Naposledy aktualizováno",
        scanningTitle = "Skenování dynamických fotek",
        scanningSubtitle = "Nejprve se obnoví předchozí výsledky, poté se skenování postupně aktualizuje.",
        emptyTitle = "Žádné dynamické fotky k opravě",
        emptySubtitle = "Pokud jste právě importovali fotky, klepněte na obnovení vpravo nahoře.",
        converted = "Převedeno",
        failed = "Selhalo",
        resultsTitle = "Zkontrolovat výsledky",
        reviewTitle = "Zkontrolujte nové fotky před smazáním starých",
        reviewSubtitle = "Klepněte na libovolnou novou položku nebo ji podržte, přejdete do systémové galerie a ověříte, že Live funguje.",
        newPhotos = "Nové fotky",
        oldPendingDelete = "Staré čekající na smazání",
        openAlbumFailed = "Systémovou galerii se nepodařilo otevřít",
        viewFirst = "Nejdřív zobrazit",
        deleteAllOld = "Smazat všechny staré",
        back = "Zpět",
        refresh = "Obnovit",
        converting = "Převod",
        selectedItems = "vybraných položek",
        scanning = "Skenování",
        found = "Nalezeno",
        unnamedPhoto = "Fotka bez názvu",
        clearAll = "Zrušit výběr",
        selectAll = "Vybrat vše",
        startConvert = "Převést",
        oldDeleted = "Staré smazány",
        deleteOldFailed = "Staré fotky se nepodařilo smazat",
        pendingReview = "Čeká na kontrolu"
    )
    AppLanguage.Dutch -> LiveSyncToolCopy(
        title = "Live-reparatietool",
        restorePrefix = "Herstel als ",
        restoreSubtitle = "Zet Live Photo-bestanden die van andere apparaten zijn gesynchroniseerd om naar een indeling die deze galerij en apps van derden herkennen.",
        justEntered = "Net geopend",
        toRepair = "Te repareren",
        selected = "Geselecteerd",
        lastUpdated = "Laatst bijgewerkt",
        scanningTitle = "Dynamische foto's scannen",
        scanningSubtitle = "Eerdere resultaten worden eerst hersteld, daarna wordt de scan stapsgewijs vernieuwd.",
        emptyTitle = "Geen dynamische foto's om te repareren",
        emptySubtitle = "Als je net foto's hebt geïmporteerd, tik dan rechtsboven op vernieuwen.",
        converted = "Omgezet",
        failed = "Mislukt",
        resultsTitle = "Resultaten bekijken",
        reviewTitle = "Bekijk nieuwe foto's voordat je oude verwijdert",
        reviewSubtitle = "Tik op een nieuw item of houd het ingedrukt om naar de systeemgalerij te gaan en te controleren of Live werkt.",
        newPhotos = "Nieuwe foto's",
        oldPendingDelete = "Oude, wachten op verwijdering",
        openAlbumFailed = "Systeemgalerij kan niet worden geopend",
        viewFirst = "Eerst bekijken",
        deleteAllOld = "Alle oude verwijderen",
        back = "Terug",
        refresh = "Vernieuw",
        converting = "Omzetten",
        selectedItems = "geselecteerde items",
        scanning = "Scannen",
        found = "Gevonden",
        unnamedPhoto = "Naamloze foto",
        clearAll = "Alles wissen",
        selectAll = "Selecteer alles",
        startConvert = "Zet om",
        oldDeleted = "Oude verwijderd",
        deleteOldFailed = "Oude foto's verwijderen mislukt",
        pendingReview = "Wacht op controle"
    )
    AppLanguage.Swedish -> LiveSyncToolCopy(
        title = "Verktyg för Live-reparation",
        restorePrefix = "Återställ som ",
        restoreSubtitle = "Konvertera Live Photo-filer som synkats från andra enheter till ett format som det här galleriet och tredjepartsappar känner igen.",
        justEntered = "Nyss öppnat",
        toRepair = "Att reparera",
        selected = "Valda",
        lastUpdated = "Senast uppdaterad",
        scanningTitle = "Söker efter dynamiska foton",
        scanningSubtitle = "Tidigare resultat återställs först, sedan uppdateras sökningen stegvis.",
        emptyTitle = "Inga dynamiska foton behöver repareras",
        emptySubtitle = "Om du nyss importerade foton, tryck på uppdatera uppe till höger.",
        converted = "Konverterade",
        failed = "Misslyckades",
        resultsTitle = "Granska resultat",
        reviewTitle = "Granska nya foton innan du raderar gamla",
        reviewSubtitle = "Tryck eller håll nedtryckt på ett nytt objekt för att öppna systemgalleriet och bekräfta att Live fungerar.",
        newPhotos = "Nya foton",
        oldPendingDelete = "Gamla som väntar på radering",
        openAlbumFailed = "Det går inte att öppna systemgalleriet",
        viewFirst = "Visa först",
        deleteAllOld = "Radera alla gamla",
        back = "Tillbaka",
        refresh = "Uppdatera",
        converting = "Konverterar",
        selectedItems = "valda objekt",
        scanning = "Söker",
        found = "Hittade",
        unnamedPhoto = "Namnlöst foto",
        clearAll = "Rensa allt",
        selectAll = "Markera alla",
        startConvert = "Konvertera",
        oldDeleted = "Gamla raderade",
        deleteOldFailed = "Det gick inte att radera gamla foton",
        pendingReview = "Väntar på granskning"
    )
    AppLanguage.Danish -> LiveSyncToolCopy(
        title = "Live-reparationsværktøj",
        restorePrefix = "Gendan som ",
        restoreSubtitle = "Konvertér Live Photo-filer synkroniseret fra andre enheder til et format, som dette galleri og tredjepartsapps genkender.",
        justEntered = "Lige åbnet",
        toRepair = "Skal repareres",
        selected = "Valgt",
        lastUpdated = "Sidst opdateret",
        scanningTitle = "Scanner dynamiske fotos",
        scanningSubtitle = "Tidligere resultater gendannes først, derefter opdateres scanningen gradvist.",
        emptyTitle = "Ingen dynamiske fotos skal repareres",
        emptySubtitle = "Hvis du lige har importeret fotos, så tryk på opdater øverst til højre.",
        converted = "Konverteret",
        failed = "Mislykkedes",
        resultsTitle = "Gennemgå resultater",
        reviewTitle = "Gennemgå nye fotos, før du sletter de gamle",
        reviewSubtitle = "Tryk på eller hold et nyt element nede for at åbne systemgalleriet og bekræfte, at Live virker.",
        newPhotos = "Nye fotos",
        oldPendingDelete = "Gamle, der afventer sletning",
        openAlbumFailed = "Systemgalleriet kunne ikke åbnes",
        viewFirst = "Se først",
        deleteAllOld = "Slet alle gamle",
        back = "Tilbage",
        refresh = "Opdater",
        converting = "Konverterer",
        selectedItems = "valgte elementer",
        scanning = "Scanner",
        found = "Fundet",
        unnamedPhoto = "Unavngivet foto",
        clearAll = "Ryd alle",
        selectAll = "Vælg alle",
        startConvert = "Konvertér",
        oldDeleted = "Gamle slettet",
        deleteOldFailed = "Gamle fotos kunne ikke slettes",
        pendingReview = "Afventer gennemgang"
    )
    AppLanguage.Norwegian -> LiveSyncToolCopy(
        title = "Live-reparasjonsverktøy",
        restorePrefix = "Gjenopprett som ",
        restoreSubtitle = "Konverter Live Photo-filer synkronisert fra andre enheter til et format som dette galleriet og tredjepartsapper gjenkjenner.",
        justEntered = "Nettopp åpnet",
        toRepair = "Må repareres",
        selected = "Valgt",
        lastUpdated = "Sist oppdatert",
        scanningTitle = "Skanner dynamiske bilder",
        scanningSubtitle = "Tidligere resultater gjenopprettes først, deretter oppdateres skanningen gradvis.",
        emptyTitle = "Ingen dynamiske bilder må repareres",
        emptySubtitle = "Hvis du nettopp har importert bilder, trykk på oppdater øverst til høyre.",
        converted = "Konvertert",
        failed = "Mislyktes",
        resultsTitle = "Se gjennom resultater",
        reviewTitle = "Se gjennom nye bilder før du sletter de gamle",
        reviewSubtitle = "Trykk på eller hold inne et nytt element for å åpne systemgalleriet og bekrefte at Live fungerer.",
        newPhotos = "Nye bilder",
        oldPendingDelete = "Gamle som venter på sletting",
        openAlbumFailed = "Kunne ikke åpne systemgalleriet",
        viewFirst = "Vis først",
        deleteAllOld = "Slett alle gamle",
        back = "Tilbake",
        refresh = "Oppdater",
        converting = "Konverterer",
        selectedItems = "valgte elementer",
        scanning = "Skanner",
        found = "Fant",
        unnamedPhoto = "Bilde uten navn",
        clearAll = "Fjern alle",
        selectAll = "Velg alle",
        startConvert = "Konverter",
        oldDeleted = "Gamle slettet",
        deleteOldFailed = "Kunne ikke slette gamle bilder",
        pendingReview = "Venter på gjennomgang"
    )
    AppLanguage.Finnish -> LiveSyncToolCopy(
        title = "Live-korjaustyökalu",
        restorePrefix = "Palauta muodossa ",
        restoreSubtitle = "Muunna muilta laitteilta synkronoidut Live Photo -tiedostot muotoon, jonka tämä galleria ja kolmannen osapuolen sovellukset tunnistavat.",
        justEntered = "Juuri avattu",
        toRepair = "Korjattavat",
        selected = "Valittu",
        lastUpdated = "Päivitetty viimeksi",
        scanningTitle = "Skannataan dynaamisia kuvia",
        scanningSubtitle = "Aiemmat tulokset palautetaan ensin, minkä jälkeen skannaus päivittyy vähitellen.",
        emptyTitle = "Korjattavia dynaamisia kuvia ei ole",
        emptySubtitle = "Jos toit juuri kuvia, napauta päivitystä oikeassa yläkulmassa.",
        converted = "Muunnettu",
        failed = "Epäonnistui",
        resultsTitle = "Tarkista tulokset",
        reviewTitle = "Tarkista uudet kuvat ennen vanhojen poistamista",
        reviewSubtitle = "Napauta tai paina pitkään mitä tahansa uutta kohdetta avataksesi järjestelmän gallerian ja varmistaaksesi, että Live toimii.",
        newPhotos = "Uudet kuvat",
        oldPendingDelete = "Vanhat poistettavina",
        openAlbumFailed = "Järjestelmän galleriaa ei voi avata",
        viewFirst = "Katso ensin",
        deleteAllOld = "Poista kaikki vanhat",
        back = "Takaisin",
        refresh = "Päivitä",
        converting = "Muunnetaan",
        selectedItems = "valittua kohdetta",
        scanning = "Skannataan",
        found = "Löytyi",
        unnamedPhoto = "Nimetön kuva",
        clearAll = "Tyhjennä kaikki",
        selectAll = "Valitse kaikki",
        startConvert = "Muunna",
        oldDeleted = "Vanhat poistettu",
        deleteOldFailed = "Vanhojen kuvien poisto epäonnistui",
        pendingReview = "Odottaa tarkistusta"
    )
    AppLanguage.Greek -> LiveSyncToolCopy(
        title = "Εργαλείο επιδιόρθωσης Live",
        restorePrefix = "Επαναφορά ως ",
        restoreSubtitle = "Μετατρέψτε αρχεία Live Photo που συγχρονίστηκαν από άλλες συσκευές σε μορφή που αναγνωρίζουν αυτή η συλλογή και εφαρμογές τρίτων.",
        justEntered = "Μόλις ανοίχτηκε",
        toRepair = "Για επιδιόρθωση",
        selected = "Επιλεγμένα",
        lastUpdated = "Τελευταία ενημέρωση",
        scanningTitle = "Σάρωση δυναμικών φωτογραφιών",
        scanningSubtitle = "Πρώτα θα επαναφερθούν τα προηγούμενα αποτελέσματα και έπειτα η σάρωση θα ανανεώνεται σταδιακά.",
        emptyTitle = "Καμία δυναμική φωτογραφία δεν χρειάζεται επιδιόρθωση",
        emptySubtitle = "Αν μόλις εισαγάγατε φωτογραφίες, πατήστε την ανανέωση πάνω δεξιά.",
        converted = "Μετατράπηκαν",
        failed = "Απέτυχαν",
        resultsTitle = "Έλεγχος αποτελεσμάτων",
        reviewTitle = "Ελέγξτε τις νέες φωτογραφίες πριν διαγράψετε τις παλιές",
        reviewSubtitle = "Πατήστε ή κρατήστε πατημένο οποιοδήποτε νέο στοιχείο για να μεταβείτε στη συλλογή συστήματος και να βεβαιωθείτε ότι το Live λειτουργεί.",
        newPhotos = "Νέες φωτογραφίες",
        oldPendingDelete = "Παλιές προς διαγραφή",
        openAlbumFailed = "Δεν είναι δυνατό το άνοιγμα της συλλογής συστήματος",
        viewFirst = "Προβολή πρώτα",
        deleteAllOld = "Διαγραφή όλων των παλιών",
        back = "Πίσω",
        refresh = "Ανανέωση",
        converting = "Μετατροπή",
        selectedItems = "επιλεγμένα στοιχεία",
        scanning = "Σάρωση",
        found = "Βρέθηκαν",
        unnamedPhoto = "Φωτογραφία χωρίς όνομα",
        clearAll = "Εκκαθάριση όλων",
        selectAll = "Επιλογή όλων",
        startConvert = "Μετατροπή",
        oldDeleted = "Οι παλιές διαγράφηκαν",
        deleteOldFailed = "Αποτυχία διαγραφής παλιών φωτογραφιών",
        pendingReview = "Εκκρεμεί έλεγχος"
    )
    AppLanguage.Romanian -> LiveSyncToolCopy(
        title = "Instrument de reparare Live",
        restorePrefix = "Restaurează ca ",
        restoreSubtitle = "Convertește fișierele Live Photo sincronizate de pe alte dispozitive într-un format recunoscut de această galerie și de aplicațiile terțe.",
        justEntered = "Tocmai deschis",
        toRepair = "De reparat",
        selected = "Selectate",
        lastUpdated = "Ultima actualizare",
        scanningTitle = "Se scanează fotografiile dinamice",
        scanningSubtitle = "Mai întâi se restaurează rezultatele anterioare, apoi scanarea se reîmprospătează treptat.",
        emptyTitle = "Nicio fotografie dinamică nu necesită reparare",
        emptySubtitle = "Dacă tocmai ai importat fotografii, atinge reîmprospătarea din dreapta sus.",
        converted = "Convertite",
        failed = "Eșuate",
        resultsTitle = "Verifică rezultatele",
        reviewTitle = "Verifică fotografiile noi înainte de a le șterge pe cele vechi",
        reviewSubtitle = "Atinge sau ține apăsat pe un element nou pentru a deschide galeria sistemului și a confirma că Live funcționează.",
        newPhotos = "Fotografii noi",
        oldPendingDelete = "Vechi în așteptarea ștergerii",
        openAlbumFailed = "Nu se poate deschide galeria sistemului",
        viewFirst = "Vezi mai întâi",
        deleteAllOld = "Șterge toate cele vechi",
        back = "Înapoi",
        refresh = "Reîmprospătează",
        converting = "Se convertește",
        selectedItems = "elemente selectate",
        scanning = "Se scanează",
        found = "Găsite",
        unnamedPhoto = "Fotografie fără nume",
        clearAll = "Șterge tot",
        selectAll = "Selectează tot",
        startConvert = "Convertește",
        oldDeleted = "Cele vechi au fost șterse",
        deleteOldFailed = "Nu s-au putut șterge fotografiile vechi",
        pendingReview = "În așteptarea verificării"
    )
    AppLanguage.Hungarian -> LiveSyncToolCopy(
        title = "Live javítóeszköz",
        restorePrefix = "Visszaállítás mint ",
        restoreSubtitle = "Más eszközökről szinkronizált Live Photo fájlok átalakítása olyan formátumra, amelyet ez a galéria és a harmadik féltől származó alkalmazások felismernek.",
        justEntered = "Most megnyitva",
        toRepair = "Javítandó",
        selected = "Kijelölve",
        lastUpdated = "Utolsó frissítés",
        scanningTitle = "Dinamikus fotók keresése",
        scanningSubtitle = "Először az előző eredmények állnak vissza, majd a vizsgálat fokozatosan frissül.",
        emptyTitle = "Nincs javítandó dinamikus fotó",
        emptySubtitle = "Ha most importáltál fotókat, koppints a frissítésre jobb felül.",
        converted = "Átalakítva",
        failed = "Sikertelen",
        resultsTitle = "Eredmények áttekintése",
        reviewTitle = "Nézd át az új fotókat, mielőtt törölnéd a régieket",
        reviewSubtitle = "Koppints egy új elemre vagy tartsd lenyomva, hogy a rendszergalériában ellenőrizhesd, működik-e a Live.",
        newPhotos = "Új fotók",
        oldPendingDelete = "Törlésre váró régiek",
        openAlbumFailed = "A rendszergaléria nem nyitható meg",
        viewFirst = "Előbb megtekintés",
        deleteAllOld = "Összes régi törlése",
        back = "Vissza",
        refresh = "Frissítés",
        converting = "Átalakítás",
        selectedItems = "kijelölt elem",
        scanning = "Keresés",
        found = "Találat",
        unnamedPhoto = "Névtelen fotó",
        clearAll = "Összes törlése",
        selectAll = "Összes kijelölése",
        startConvert = "Átalakítás",
        oldDeleted = "Régiek törölve",
        deleteOldFailed = "A régi fotók törlése sikertelen",
        pendingReview = "Áttekintésre vár"
    )
    AppLanguage.Turkish -> LiveSyncToolCopy(
        title = "Live Onarım Aracı",
        restorePrefix = "Şu olarak geri yükle: ",
        restoreSubtitle = "Diğer cihazlardan eşitlenen Live Photo dosyalarını bu galerinin ve üçüncü taraf uygulamaların tanıdığı biçime dönüştür.",
        justEntered = "Az önce girildi",
        toRepair = "Onarılacak",
        selected = "Seçildi",
        lastUpdated = "Son güncelleme",
        scanningTitle = "Dinamik fotoğraflar taranıyor",
        scanningSubtitle = "Önce önceki sonuçlar geri yüklenir, ardından tarama adım adım yenilenir.",
        emptyTitle = "Onarılacak dinamik fotoğraf yok",
        emptySubtitle = "Az önce fotoğraf içe aktardıysan sağ üstteki yenile düğmesine dokun.",
        converted = "Dönüştürüldü",
        failed = "Başarısız",
        resultsTitle = "Sonuçları İncele",
        reviewTitle = "Eskileri silmeden önce yeni fotoğrafları incele",
        reviewSubtitle = "Sistem galerisine geçip Live'ın çalıştığını doğrulamak için herhangi bir yeni öğeye dokun veya basılı tut.",
        newPhotos = "Yeni fotoğraflar",
        oldPendingDelete = "Silinmeyi bekleyen eskiler",
        openAlbumFailed = "Sistem galerisi açılamadı",
        viewFirst = "Önce görüntüle",
        deleteAllOld = "Tüm eskileri sil",
        back = "Geri",
        refresh = "Yenile",
        converting = "Dönüştürülüyor",
        selectedItems = "öğe seçildi",
        scanning = "Taranıyor",
        found = "Bulunan",
        unnamedPhoto = "Adsız fotoğraf",
        clearAll = "Tümünü temizle",
        selectAll = "Tümünü seç",
        startConvert = "Dönüştür",
        oldDeleted = "Eskiler silindi",
        deleteOldFailed = "Eski fotoğraflar silinemedi",
        pendingReview = "İnceleme bekliyor"
    )
    AppLanguage.Malay -> LiveSyncToolCopy(
        title = "Alat Pembaikan Live",
        restorePrefix = "Pulihkan sebagai ",
        restoreSubtitle = "Tukarkan fail Live Photo yang disegerakkan daripada peranti lain kepada format yang dikenali oleh galeri ini dan apl pihak ketiga.",
        justEntered = "Baru dimasuki",
        toRepair = "Perlu dibaiki",
        selected = "Dipilih",
        lastUpdated = "Dikemas kini terakhir",
        scanningTitle = "Mengimbas foto dinamik",
        scanningSubtitle = "Hasil sebelumnya dipulihkan dahulu, kemudian imbasan ini disegarkan secara berperingkat.",
        emptyTitle = "Tiada foto dinamik perlu dibaiki",
        emptySubtitle = "Jika anda baru mengimport foto, ketik segar semula di penjuru kanan atas.",
        converted = "Ditukar",
        failed = "Gagal",
        resultsTitle = "Semak Hasil",
        reviewTitle = "Semak foto baharu sebelum memadam yang lama",
        reviewSubtitle = "Ketik atau tekan lama mana-mana item baharu untuk ke galeri sistem dan sahkan Live berfungsi.",
        newPhotos = "Foto baharu",
        oldPendingDelete = "Lama menunggu dipadam",
        openAlbumFailed = "Tidak dapat membuka galeri sistem",
        viewFirst = "Lihat dahulu",
        deleteAllOld = "Padam semua yang lama",
        back = "Kembali",
        refresh = "Segar semula",
        converting = "Menukar",
        selectedItems = "item dipilih",
        scanning = "Mengimbas",
        found = "Ditemui",
        unnamedPhoto = "Foto tanpa tajuk",
        clearAll = "Kosongkan semua",
        selectAll = "Pilih semua",
        startConvert = "Tukar",
        oldDeleted = "Yang lama dipadam",
        deleteOldFailed = "Gagal memadam foto lama",
        pendingReview = "Menunggu semakan"
    )
    AppLanguage.Hebrew -> LiveSyncToolCopy(
        title = "כלי לתיקון Live",
        restorePrefix = "שחזור בתור ",
        restoreSubtitle = "המרת קובצי Live Photo שסונכרנו ממכשירים אחרים לפורמט שהגלריה הזו ואפליקציות צד שלישי מזהות.",
        justEntered = "נפתח עכשיו",
        toRepair = "לתיקון",
        selected = "נבחרו",
        lastUpdated = "עודכן לאחרונה",
        scanningTitle = "סורק תמונות דינמיות",
        scanningSubtitle = "תוצאות קודמות ישוחזרו קודם, ואז הסריקה תתרענן בהדרגה.",
        emptyTitle = "אין תמונות דינמיות לתיקון",
        emptySubtitle = "אם ייבאתם תמונות הרגע, הקישו על רענון בפינה הימנית העליונה.",
        converted = "הומרו",
        failed = "נכשלו",
        resultsTitle = "סקירת תוצאות",
        reviewTitle = "סקרו את התמונות החדשות לפני מחיקת הישנות",
        reviewSubtitle = "הקישו או לחצו לחיצה ארוכה על פריט חדש כדי לעבור לגלריית המערכת ולוודא ש-Live עובד.",
        newPhotos = "תמונות חדשות",
        oldPendingDelete = "ישנות שממתינות למחיקה",
        openAlbumFailed = "לא ניתן לפתוח את גלריית המערכת",
        viewFirst = "צפייה קודם",
        deleteAllOld = "מחיקת כל הישנות",
        back = "חזרה",
        refresh = "רענון",
        converting = "ממיר",
        selectedItems = "פריטים שנבחרו",
        scanning = "סורק",
        found = "נמצאו",
        unnamedPhoto = "תמונה ללא שם",
        clearAll = "ניקוי הכול",
        selectAll = "בחירת הכול",
        startConvert = "המרה",
        oldDeleted = "הישנות נמחקו",
        deleteOldFailed = "מחיקת התמונות הישנות נכשלה",
        pendingReview = "ממתין לסקירה"
    )
    AppLanguage.English -> LiveSyncToolCopy(
        title = "Live Repair Tool",
        restorePrefix = "Restore as ",
        restoreSubtitle = "Convert Live Photo files synced from other devices into a format recognized by this gallery and third-party apps.",
        justEntered = "Just entered",
        toRepair = "To repair",
        selected = "Selected",
        lastUpdated = "Last updated",
        scanningTitle = "Scanning dynamic photos",
        scanningSubtitle = "Previous results will be restored first, then this scan will refresh progressively.",
        emptyTitle = "No dynamic photos need repair",
        emptySubtitle = "If you just imported photos, tap refresh in the top-right corner.",
        converted = "Converted",
        failed = "Failed",
        resultsTitle = "Review Results",
        reviewTitle = "Review new photos before deleting old ones",
        reviewSubtitle = "Tap or long-press any new item to jump into the system gallery and confirm that Live is working.",
        newPhotos = "New photos",
        oldPendingDelete = "Old pending delete",
        openAlbumFailed = "Unable to open the system gallery",
        viewFirst = "View first",
        deleteAllOld = "Delete all old",
        back = "Back",
        refresh = "Refresh",
        converting = "Converting",
        selectedItems = "selected items",
        scanning = "Scanning",
        found = "Found",
        unnamedPhoto = "Untitled photo",
        clearAll = "Clear all",
        selectAll = "Select all",
        startConvert = "Convert",
        oldDeleted = "Old deleted",
        deleteOldFailed = "Failed to delete old photos",
        pendingReview = "Review pending"
    )
}

@Composable
internal fun LiveSyncToolPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLanguage = LocalAppLanguage.current
    val copy = remember(appLanguage) { getLiveSyncToolCopy(appLanguage) }
    val targetFamily = remember { LiveSyncToolRouting.resolveTargetFamily() }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var scanResults by remember { mutableStateOf<List<LiveSyncToolScanCandidate>>(emptyList()) }
    var acceptanceItems by remember { mutableStateOf<List<LiveSyncToolConvertedItem>?>(null) }
    var scanProgress by remember { mutableStateOf(LiveSyncToolScanProgress(0, 0, 0)) }
    var scanning by remember { mutableStateOf(false) }
    var converting by remember { mutableStateOf(false) }
    var convertingLabel by remember { mutableStateOf("") }
    var lastUpdatedAtMs by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var resumeToken by remember { mutableStateOf<LiveSyncToolScanResumeToken?>(null) }

    fun persist(scanInFlight: Boolean) {
        LiveSyncToolSnapshotStore.save(
            context,
            LiveSyncToolSnapshotStore.toSnapshot(
                targetFamily = targetFamily,
                candidates = scanResults,
                selectedIds = selectedIds.toSet(),
                progress = scanProgress,
                scanInFlight = scanInFlight,
                resumeToken = if (scanInFlight) resumeToken else null
            )
        )
        lastUpdatedAtMs = System.currentTimeMillis()
    }

    fun startScan() {
        scope.launch {
            val restoredResults = scanResults
            scanning = true
            persist(true)
            val finalResults = LiveSyncToolScanner.scan(
                context = context,
                initialCandidates = if (resumeToken != null) restoredResults else emptyList(),
                resumeToken = resumeToken,
                onProgress = { scanProgress = it },
                onCheckpoint = { partial, progress, isFinal, nextResumeToken ->
                    if (partial.isNotEmpty() || restoredResults.isEmpty() || isFinal) {
                        scanResults = partial
                    }
                    scanProgress = progress
                    resumeToken = nextResumeToken
                    persist(!isFinal && (scanResults.isNotEmpty() || restoredResults.isNotEmpty()))
                }
            )
            scanResults = finalResults
            scanProgress = scanProgress.copy(matched = finalResults.count { !it.targetReady })
            scanning = false
            resumeToken = null
            persist(false)
        }
    }

    val deleteLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && pendingDeleteIds.isNotEmpty()) {
                acceptanceItems = acceptanceItems?.map {
                    if (it.sourceItem.id in pendingDeleteIds) it.copy(deletedOld = true) else it
                }
                Toast.makeText(context, copy.oldDeleted, Toast.LENGTH_SHORT).show()
            }
            pendingDeleteIds = emptySet()
        }

    fun requestDelete(sourceIds: Set<Long>) {
        val targets = acceptanceItems.orEmpty().filter { it.sourceItem.id in sourceIds && !it.deletedOld }
        if (targets.isEmpty()) return
        val uris = targets.flatMap { it.oldUris }.distinct()
        val intentSender = LiveSyncToolSystemActions.buildDeleteIntentSender(context, uris)
        if (intentSender != null) {
            pendingDeleteIds = sourceIds
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        } else {
            val deleted = LiveSyncToolSystemActions.deleteDirectly(context, uris)
            if (deleted) {
                acceptanceItems = acceptanceItems?.map {
                    if (it.sourceItem.id in pendingDeleteIds) it.copy(deletedOld = true) else it
                }
                Toast.makeText(context, copy.oldDeleted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, copy.deleteOldFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        LiveSyncToolSnapshotStore.load(context)
            ?.takeIf { it.targetFamily == targetFamily }
            ?.let { snapshot ->
                scanResults = LiveSyncToolSnapshotStore.restoreCandidates(snapshot)
                selectedIds.clear()
                selectedIds.addAll(snapshot.selectedIds)
                scanProgress = snapshot.progress
                lastUpdatedAtMs = snapshot.updatedAtMs
                resumeToken = snapshot.resumeToken.takeIf { snapshot.scanInFlight }
            }
        startScan()
    }

    if (acceptanceItems != null) {
        AcceptanceView(
            items = acceptanceItems.orEmpty(),
            copy = copy,
            onBack = { acceptanceItems = null },
            onDelete = ::requestDelete
        )
        return
    }

    val selectableIds = remember(scanResults) { scanResults.filter { it.selectable }.map { it.mediaItem.id } }
    val isAllSelected = selectableIds.isNotEmpty() && selectableIds.all { it in selectedIds }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ToolTopBar(
                title = copy.title,
                onBack = onBack,
                onRefresh = if (!scanning && !converting) ({ startScan() }) else null
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroCard(
                    accent = LiveSyncAccent,
                    title = copy.restorePrefix + LiveSyncToolRouting.targetLabel(targetFamily),
                    subtitle = copy.restoreSubtitle
                ) {
                    val updatedText = lastUpdatedAtMs?.let {
                        DateUtils.getRelativeTimeSpanString(
                            it,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    } ?: copy.justEntered
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadge(copy.toRepair, scanResults.count { !it.targetReady }.toString())
                        MetricBadge(copy.selected, selectedIds.size.toString())
                        MetricBadge(copy.lastUpdated, updatedText)
                    }
                }

                StatusStrip(
                    scanning = scanning,
                    converting = converting,
                    convertingLabel = convertingLabel,
                    scanProgress = scanProgress,
                    resultCount = scanResults.size
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    scanning && scanResults.isEmpty() -> {
                        EmptyState(copy.scanningTitle, copy.scanningSubtitle, true)
                    }

                    scanResults.isEmpty() -> {
                        EmptyState(copy.emptyTitle, copy.emptySubtitle, false)
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(scanResults, key = { it.mediaItem.id }) { candidate ->
                                CandidateGridItem(
                                    candidate = candidate,
                                    selected = candidate.mediaItem.id in selectedIds,
                                    onToggle = {
                                        if (!candidate.selectable) return@CandidateGridItem
                                        if (candidate.mediaItem.id in selectedIds) {
                                            selectedIds.remove(candidate.mediaItem.id)
                                        } else {
                                            selectedIds.add(candidate.mediaItem.id)
                                        }
                                        persist(scanning)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (scanResults.isNotEmpty()) {
                SelectionBar(
                    selectedCount = selectedIds.size,
                    isAllSelected = isAllSelected,
                    converting = converting,
                    onToggleAll = {
                        if (isAllSelected) {
                            selectedIds.clear()
                        } else {
                            selectedIds.clear()
                            selectedIds.addAll(selectableIds)
                        }
                        persist(scanning)
                    },
                    onConvert = {
                        val selected = scanResults.filter { it.mediaItem.id in selectedIds }
                        scope.launch {
                            converting = true
                            val report = LiveSyncToolEngine.convertSelected(context, selected) {
                                convertingLabel = it.currentName
                            }
                            converting = false
                            val successIds = report.successes.map { it.sourceItem.id }.toSet()
                            selectedIds.removeAll(successIds)
                            scanResults = scanResults.filterNot { it.mediaItem.id in successIds }
                            persist(scanning)
                            if (report.successes.isNotEmpty()) {
                                acceptanceItems = report.successes
                            }
                            Toast.makeText(
                                context,
                                buildString {
                                    append(copy.converted)
                                    append(' ')
                                    append(report.succeeded)
                                    append(if (appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese) " 张" else " items")
                                    if (report.failures.isNotEmpty()) {
                                        append(if (appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese) "，" else ", ")
                                        append(copy.failed)
                                        append(' ')
                                        append(report.failures.size)
                                        append(if (appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese) " 张" else " items")
                                    }
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AcceptanceView(
    items: List<LiveSyncToolConvertedItem>,
    copy: LiveSyncToolCopy,
    onBack: () -> Unit,
    onDelete: (Set<Long>) -> Unit
) {
    val context = LocalContext.current
    val undeleted = items.filterNot { it.deletedOld }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ToolTopBar(title = copy.resultsTitle, onBack = onBack, onRefresh = null)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroCard(
                    accent = LiveSyncAcceptanceAccent,
                    title = copy.reviewTitle,
                    subtitle = copy.reviewSubtitle
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadge(copy.newPhotos, items.size.toString())
                        MetricBadge(copy.oldPendingDelete, undeleted.size.toString())
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            items.firstOrNull()?.let {
                                if (!LiveSyncToolSystemActions.openInSystemGallery(context, it.newPhotoUri)) {
                                    Toast.makeText(context, copy.openAlbumFailed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = items.isNotEmpty()
                    ) {
                        Text(copy.viewFirst)
                    }
                    Button(
                        onClick = { onDelete(undeleted.map { it.sourceItem.id }.toSet()) },
                        enabled = undeleted.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(copy.deleteAllOld)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.sourceItem.id }) { item ->
                    AcceptanceGridItem(
                        item = item,
                        onOpen = {
                            if (!LiveSyncToolSystemActions.openInSystemGallery(context, item.newPhotoUri)) {
                                Toast.makeText(context, copy.openAlbumFailed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        copy = copy,
                        onDelete = { onDelete(setOf(item.sourceItem.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolTopBar(title: String, onBack: () -> Unit, onRefresh: (() -> Unit)?) {
    val appLanguage = LocalAppLanguage.current
    val copy = remember(appLanguage) { getLiveSyncToolCopy(appLanguage) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnifiedSubpageTopBarIconButton(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = copy.back,
            onClick = onBack,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
            iconTint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (onRefresh != null) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = copy.refresh,
                onClick = onRefresh,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
                iconTint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun HeroCard(
    accent: Color,
    title: String,
    subtitle: String,
    footer: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            footer()
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusStrip(
    scanning: Boolean,
    converting: Boolean,
    convertingLabel: String,
    scanProgress: LiveSyncToolScanProgress,
    resultCount: Int
) {
    val appLanguage = LocalAppLanguage.current
    val copy = remember(appLanguage) { getLiveSyncToolCopy(appLanguage) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = when {
                    converting -> "${copy.converting} ${convertingLabel.ifBlank { copy.selectedItems }}"
                    scanning -> "${copy.scanning} ${scanProgress.processed}/${scanProgress.total}"
                    else -> buildString {
                        append(copy.found)
                        append(' ')
                        append(resultCount)
                        append(if (appLanguage == AppLanguage.SimplifiedChinese || appLanguage == AppLanguage.TraditionalChinese) " 张候选动态照片" else " candidate dynamic photos")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (scanning || converting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LiveSyncAccent)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, loading: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = LiveSyncAccent, strokeWidth = 2.6.dp)
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CandidateGridItem(
    candidate: LiveSyncToolScanCandidate,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val copy = remember(appLanguage) { getLiveSyncToolCopy(appLanguage) }
    val selectable = candidate.selectable
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f))
            .combinedClickable(
                onClick = { if (selectable) onToggle() },
                onLongClick = { if (selectable) onToggle() }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(candidate.mediaItem.uri).crossfade(true).build(),
            contentDescription = candidate.mediaItem.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(LiveSyncAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else if (selectable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.38f))
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Text(
                text = candidate.mediaItem.displayName.ifBlank { copy.unnamedPhoto },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    converting: Boolean,
    onToggleAll: () -> Unit,
    onConvert: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val copy = remember(appLanguage) { getLiveSyncToolCopy(appLanguage) }
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onToggleAll,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isAllSelected) copy.clearAll else copy.selectAll)
            }
            Button(
                onClick = onConvert,
                enabled = selectedCount > 0 && !converting,
                colors = ButtonDefaults.buttonColors(containerColor = LiveSyncAccent),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (selectedCount > 0) "${copy.startConvert} ($selectedCount)" else copy.startConvert)
            }
        }
    }
}

@Composable
private fun AcceptanceGridItem(
    item: LiveSyncToolConvertedItem,
    copy: LiveSyncToolCopy,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)
        )
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(onClick = onOpen, onLongClick = onOpen)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.newPhotoUri).crossfade(true).build(),
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.46f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (item.deletedOld) copy.oldDeleted else copy.pendingReview,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = item.displayName.ifBlank { copy.unnamedPhoto },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniActionButton(
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    onClick = onOpen
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                MiniActionButton(
                    modifier = Modifier.weight(1f),
                    enabled = !item.deletedOld,
                    background = if (item.deletedOld) {
                        MaterialTheme.colorScheme.background.copy(alpha = 0.52f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    contentColor = if (item.deletedOld) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    onClick = onDelete
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    background: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            content()
        }
    }
}
