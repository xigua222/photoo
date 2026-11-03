package com.voicelike.app

// Copy for the delete queue (tap marks a photo to keep) and the full-card option in Settings.
internal data class OrganizeOptionsStrings(
    // Delete queue
    val keepHint: String,
    val deleteAllFmt: String,
    val deleteRestFmt: String,
    val keepOnlyFmt: String,
    // Settings > Organize
    val fullCardTitle: String,
    val fullCardSubtitle: String,
)

internal fun organizeOptionsStrings(language: AppLanguage): OrganizeOptionsStrings =
    when (language) {
        AppLanguage.SimplifiedChinese -> OrganizeOptionsStrings("点选要保留的照片", "删除全部 %d 张", "删除其余 %d 张", "只移出这 %d 张", "全卡模式", "卡片统一大小，底部显示拍摄信息（旧版样式）")
        AppLanguage.TraditionalChinese -> OrganizeOptionsStrings("點選要保留的照片", "刪除全部 %d 張", "刪除其餘 %d 張", "只移出這 %d 張", "全卡模式", "卡片統一大小，底部顯示拍攝資訊（舊版樣式）")
        AppLanguage.Japanese -> OrganizeOptionsStrings("残したい写真をタップ", "すべて削除（%d枚）", "残りを削除（%d枚）", "この%d枚だけ外す", "フルカード表示", "カードを同じサイズにし、下部に撮影情報を表示（従来のスタイル）")
        AppLanguage.Korean -> OrganizeOptionsStrings("남길 사진을 탭하세요", "모두 삭제 (%d장)", "나머지 삭제 (%d장)", "이 %d장만 빼기", "전체 카드 모드", "카드를 같은 크기로 표시하고 하단에 촬영 정보를 보여줘요 (이전 스타일)")
        AppLanguage.French -> OrganizeOptionsStrings("Touchez les photos à garder", "Tout supprimer (%d)", "Supprimer le reste (%d)", "Garder la sélection (%d)", "Mode carte entière", "Cartes de même taille, infos de prise de vue en bas (style classique)")
        AppLanguage.German -> OrganizeOptionsStrings("Tippe auf die Fotos, die du behalten willst", "Alle löschen (%d)", "Rest löschen (%d)", "Auswahl behalten (%d)", "Vollkarten-Modus", "Gleich große Karten mit Aufnahmedaten unten (klassischer Stil)")
        AppLanguage.Italian -> OrganizeOptionsStrings("Tocca le foto da tenere", "Elimina tutto (%d)", "Elimina il resto (%d)", "Tieni la selezione (%d)", "Modalità scheda intera", "Schede della stessa dimensione con i dati di scatto in basso (stile classico)")
        AppLanguage.Portuguese -> OrganizeOptionsStrings("Toque nas fotos que quer manter", "Apagar tudo (%d)", "Apagar o resto (%d)", "Manter seleção (%d)", "Modo cartão inteiro", "Cartões do mesmo tamanho com os dados da foto embaixo (estilo clássico)")
        AppLanguage.Spanish -> OrganizeOptionsStrings("Toca las fotos que quieras conservar", "Eliminar todo (%d)", "Eliminar el resto (%d)", "Conservar selección (%d)", "Modo tarjeta completa", "Tarjetas del mismo tamaño con los datos de la foto abajo (estilo clásico)")
        AppLanguage.Russian -> OrganizeOptionsStrings("Отметьте фото, которые нужно оставить", "Удалить все (%d)", "Удалить остальные (%d)", "Оставить выбранные (%d)", "Полные карточки", "Карточки одного размера с данными съёмки внизу (классический стиль)")
        AppLanguage.Ukrainian -> OrganizeOptionsStrings("Позначте фото, які треба залишити", "Видалити все (%d)", "Видалити решту (%d)", "Залишити вибрані (%d)", "Повні картки", "Картки однакового розміру з даними зйомки внизу (класичний стиль)")
        AppLanguage.Polish -> OrganizeOptionsStrings("Dotknij zdjęć, które chcesz zachować", "Usuń wszystkie (%d)", "Usuń pozostałe (%d)", "Zachowaj zaznaczone (%d)", "Pełne karty", "Karty tej samej wielkości z danymi zdjęcia na dole (klasyczny styl)")
        AppLanguage.Czech -> OrganizeOptionsStrings("Klepněte na fotky, které chcete ponechat", "Smazat vše (%d)", "Smazat zbytek (%d)", "Ponechat vybrané (%d)", "Celé karty", "Karty stejné velikosti s údaji o snímku dole (klasický styl)")
        AppLanguage.Dutch -> OrganizeOptionsStrings("Tik op de foto's die je wilt houden", "Alles verwijderen (%d)", "Rest verwijderen (%d)", "Selectie houden (%d)", "Volledige kaarten", "Kaarten van gelijke grootte met opname-info onderaan (klassieke stijl)")
        AppLanguage.Swedish -> OrganizeOptionsStrings("Tryck på bilderna du vill behålla", "Radera alla (%d)", "Radera resten (%d)", "Behåll markerade (%d)", "Helkortsläge", "Lika stora kort med bildinfo längst ner (klassisk stil)")
        AppLanguage.Danish -> OrganizeOptionsStrings("Tryk på de billeder, du vil beholde", "Slet alle (%d)", "Slet resten (%d)", "Behold valgte (%d)", "Helkortstilstand", "Lige store kort med billedinfo nederst (klassisk stil)")
        AppLanguage.Norwegian -> OrganizeOptionsStrings("Trykk på bildene du vil beholde", "Slett alle (%d)", "Slett resten (%d)", "Behold valgte (%d)", "Helkortmodus", "Like store kort med bildeinfo nederst (klassisk stil)")
        AppLanguage.Finnish -> OrganizeOptionsStrings("Napauta kuvat, jotka haluat säilyttää", "Poista kaikki (%d)", "Poista loput (%d)", "Säilytä valitut (%d)", "Kokokorttitila", "Samankokoiset kortit, kuvaustiedot alareunassa (klassinen tyyli)")
        AppLanguage.Greek -> OrganizeOptionsStrings("Πατήστε τις φωτογραφίες που θέλετε να κρατήσετε", "Διαγραφή όλων (%d)", "Διαγραφή των υπολοίπων (%d)", "Διατήρηση επιλεγμένων (%d)", "Πλήρεις κάρτες", "Κάρτες ίδιου μεγέθους με τα στοιχεία λήψης κάτω (κλασικό στυλ)")
        AppLanguage.Romanian -> OrganizeOptionsStrings("Atingeți fotografiile pe care vreți să le păstrați", "Ștergeți tot (%d)", "Ștergeți restul (%d)", "Păstrați selecția (%d)", "Mod card complet", "Carduri de aceeași mărime, cu datele fotografiei jos (stil clasic)")
        AppLanguage.Hungarian -> OrganizeOptionsStrings("Koppints a megtartandó fotókra", "Összes törlése (%d)", "A többi törlése (%d)", "Kijelöltek megtartása (%d)", "Teljes kártya mód", "Egyforma méretű kártyák, alul a felvétel adataival (klasszikus stílus)")
        AppLanguage.Turkish -> OrganizeOptionsStrings("Saklamak istediğiniz fotoğraflara dokunun", "Tümünü sil (%d)", "Kalanları sil (%d)", "Seçilenleri sakla (%d)", "Tam kart modu", "Aynı boyutta kartlar, çekim bilgileri altta (klasik stil)")
        AppLanguage.Thai -> OrganizeOptionsStrings("แตะรูปที่ต้องการเก็บไว้", "ลบทั้งหมด (%d)", "ลบที่เหลือ (%d)", "เก็บที่เลือกไว้ (%d)", "โหมดการ์ดเต็ม", "การ์ดขนาดเท่ากัน แสดงข้อมูลการถ่ายด้านล่าง (แบบเดิม)")
        AppLanguage.Vietnamese -> OrganizeOptionsStrings("Chạm vào ảnh bạn muốn giữ", "Xóa tất cả (%d)", "Xóa phần còn lại (%d)", "Giữ ảnh đã chọn (%d)", "Chế độ thẻ đầy đủ", "Thẻ cùng kích thước, thông tin chụp ở dưới (kiểu cũ)")
        AppLanguage.Indonesian -> OrganizeOptionsStrings("Ketuk foto yang ingin disimpan", "Hapus semua (%d)", "Hapus sisanya (%d)", "Simpan yang dipilih (%d)", "Mode kartu penuh", "Kartu berukuran sama dengan info foto di bawah (gaya klasik)")
        AppLanguage.Malay -> OrganizeOptionsStrings("Ketik foto yang ingin disimpan", "Padam semua (%d)", "Padam selebihnya (%d)", "Simpan yang dipilih (%d)", "Mod kad penuh", "Kad bersaiz sama dengan maklumat foto di bawah (gaya klasik)")
        AppLanguage.Arabic -> OrganizeOptionsStrings("اضغط على الصور التي تريد الاحتفاظ بها", "حذف الكل (%d)", "حذف الباقي (%d)", "الاحتفاظ بالمحدد (%d)", "وضع البطاقة الكاملة", "بطاقات بالحجم نفسه مع معلومات التصوير في الأسفل (النمط الكلاسيكي)")
        AppLanguage.Hebrew -> OrganizeOptionsStrings("הקישו על התמונות שרוצים לשמור", "מחיקת הכול (%d)", "מחיקת השאר (%d)", "שמירת הנבחרות (%d)", "מצב כרטיס מלא", "כרטיסים בגודל אחיד עם פרטי הצילום למטה (סגנון קלאסי)")
        AppLanguage.English -> OrganizeOptionsStrings("Tap the photos you want to keep", "Delete all (%d)", "Delete the rest (%d)", "Keep selected (%d)", "Full card mode", "Same-size cards with shooting info at the bottom (classic style)")
    }
