package com.voicelike.app

// Copy for the photo wall page and its entry on the organize home. Follows iOS L10nStrings.
internal data class PhotoWallStrings(
    val title: String,
    val subtitle: String,
    val hint: String,
)

internal fun photoWallStrings(language: AppLanguage): PhotoWallStrings =
    when (language) {
        AppLanguage.SimplifiedChinese ->
            PhotoWallStrings(
                title = "照片墙",
                subtitle = "铺开整面相册，甩走不要的",
                hint = "按住一张照片，甩出去就标记删除",
            )
        AppLanguage.TraditionalChinese ->
            PhotoWallStrings(
                title = "照片牆",
                subtitle = "鋪開整面相簿，甩走不要的",
                hint = "按住一張照片，甩出去就標記刪除",
            )
        AppLanguage.Japanese ->
            PhotoWallStrings(
                title = "フォトウォール",
                subtitle = "ライブラリを一面に広げて、いらない写真ははじき出す",
                hint = "写真を長押しして、はじき出すと削除候補になります",
            )
        AppLanguage.Korean ->
            PhotoWallStrings(
                title = "사진 벽",
                subtitle = "보관함을 한눈에 펼치고, 필요 없는 사진은 튕겨 내기",
                hint = "사진을 길게 누른 뒤 튕겨 내면 삭제 대상으로 표시돼요",
            )
        AppLanguage.French ->
            PhotoWallStrings(
                title = "Mur de photos",
                subtitle = "Étalez votre photothèque, écartez le superflu d'un geste",
                hint = "Maintenez une photo, puis lancez-la pour la marquer à supprimer",
            )
        AppLanguage.German ->
            PhotoWallStrings(
                title = "Fotowand",
                subtitle = "Die ganze Mediathek ausbreiten, Unnötiges wegschnippen",
                hint = "Foto gedrückt halten und wegschnippen, um es zum Löschen vorzumerken",
            )
        AppLanguage.Italian ->
            PhotoWallStrings(
                title = "Muro di foto",
                subtitle = "Stendi tutta la libreria, scarta il superfluo con un gesto",
                hint = "Tieni premuta una foto e lanciala via per segnarla da eliminare",
            )
        AppLanguage.Portuguese ->
            PhotoWallStrings(
                title = "Mural de fotos",
                subtitle = "Espalhe sua fototeca e descarte as ruins com um gesto",
                hint = "Toque e segure uma foto e jogue-a longe para marcá-la para apagar",
            )
        AppLanguage.Spanish ->
            PhotoWallStrings(
                title = "Muro de fotos",
                subtitle = "Extiende toda tu fototeca y descarta lo que sobra con un gesto",
                hint = "Mantén pulsada una foto y lánzala para marcarla para eliminar",
            )
        AppLanguage.Russian ->
            PhotoWallStrings(
                title = "Фотостена",
                subtitle = "Вся медиатека на одной стене, лишнее смахните прочь",
                hint = "Удерживайте фото и смахните его, чтобы отметить для удаления",
            )
        AppLanguage.Thai ->
            PhotoWallStrings(
                title = "กำแพงภาพ",
                subtitle = "กางคลังรูปภาพออก ปัดรูปที่พลาดทิ้งไป",
                hint = "แตะค้างที่รูป แล้วปัดออกไปเพื่อทำเครื่องหมายลบ",
            )
        AppLanguage.Vietnamese ->
            PhotoWallStrings(
                title = "Tường ảnh",
                subtitle = "Trải rộng thư viện, búng đi những tấm hỏng",
                hint = "Nhấn giữ một ảnh rồi búng đi để đánh dấu xóa",
            )
        AppLanguage.Indonesian ->
            PhotoWallStrings(
                title = "Dinding Foto",
                subtitle = "Hamparkan pustaka Anda, lempar yang gagal",
                hint = "Tekan dan tahan foto, lalu lempar untuk menandainya akan dihapus",
            )
        AppLanguage.Arabic ->
            PhotoWallStrings(
                title = "جدار الصور",
                subtitle = "انشر مكتبتك كاملة، واقذف الصور غير الموفقة",
                hint = "اضغط مطولًا على صورة، ثم اقذفها بعيدًا لتمييزها للحذف",
            )
        AppLanguage.Ukrainian ->
            PhotoWallStrings(
                title = "Фотостіна",
                subtitle = "Розгорніть усю медіатеку, змахніть невдалі",
                hint = "Утримуйте фото, а потім змахніть його, щоб позначити для видалення",
            )
        AppLanguage.Polish ->
            PhotoWallStrings(
                title = "Ściana zdjęć",
                subtitle = "Rozłóż całą bibliotekę i odrzuć nieudane",
                hint = "Przytrzymaj zdjęcie i odrzuć je szybkim ruchem, aby oznaczyć je do usunięcia",
            )
        AppLanguage.Czech ->
            PhotoWallStrings(
                title = "Stěna fotek",
                subtitle = "Rozložte si knihovnu a nepovedené odhoďte",
                hint = "Podržte fotku a švihnutím ji odhoďte, tím ji označíte ke smazání",
            )
        AppLanguage.Dutch ->
            PhotoWallStrings(
                title = "Fotomuur",
                subtitle = "Spreid je bibliotheek uit, gooi de missers weg",
                hint = "Houd een foto vast en gooi hem weg om hem te markeren voor verwijdering",
            )
        AppLanguage.Swedish ->
            PhotoWallStrings(
                title = "Fotovägg",
                subtitle = "Bred ut biblioteket, släng bort missarna",
                hint = "Håll in en bild och släng iväg den för att markera den för radering",
            )
        AppLanguage.Danish ->
            PhotoWallStrings(
                title = "Fotovæg",
                subtitle = "Spred dit bibliotek ud, og smid fejlskuddene væk",
                hint = "Hold fingeren på et billede, og svirp det væk for at markere det til sletning",
            )
        AppLanguage.Norwegian ->
            PhotoWallStrings(
                title = "Bildevegg",
                subtitle = "Spre ut biblioteket, kast bort bommene",
                hint = "Hold inne et bilde og kast det bort for å merke det for sletting",
            )
        AppLanguage.Finnish ->
            PhotoWallStrings(
                title = "Kuvaseinä",
                subtitle = "Levitä kirjasto auki ja sinkoa hudit pois",
                hint = "Pidä kuvaa painettuna ja sinkoa se pois, niin se merkitään poistettavaksi",
            )
        AppLanguage.Greek ->
            PhotoWallStrings(
                title = "Τοίχος φωτογραφιών",
                subtitle = "Απλώστε τη βιβλιοθήκη σας, πετάξτε μακριά τις αποτυχημένες",
                hint = "Πατήστε παρατεταμένα μια φωτογραφία και πετάξτε την για να τη σημειώσετε προς διαγραφή",
            )
        AppLanguage.Romanian ->
            PhotoWallStrings(
                title = "Perete foto",
                subtitle = "Întindeți biblioteca, aruncați ce nu vă place",
                hint = "Țineți apăsat pe o poză, apoi aruncați-o pentru a o marca pentru ștergere",
            )
        AppLanguage.Hungarian ->
            PhotoWallStrings(
                title = "Fotófal",
                subtitle = "Terítse ki a fotótárát, és pöccintse el a mellényúlásokat",
                hint = "Tartson nyomva egy fotót, majd pöccintse el a törlésre jelöléséhez",
            )
        AppLanguage.Turkish ->
            PhotoWallStrings(
                title = "Fotoğraf Duvarı",
                subtitle = "Arşivini yay, istemediklerini fırlatıp at",
                hint = "Bir fotoğrafa basılı tut ve silinecek olarak işaretlemek için fırlat",
            )
        AppLanguage.Malay ->
            PhotoWallStrings(
                title = "Dinding Foto",
                subtitle = "Bentangkan pustaka anda, lontar yang tidak menjadi",
                hint = "Tekan dan tahan foto, kemudian lontarkannya untuk menanda untuk dipadam",
            )
        AppLanguage.Hebrew ->
            PhotoWallStrings(
                title = "קיר תמונות",
                subtitle = "כל הספרייה על קיר אחד, ומה שלא מוצלח עף",
                hint = "לחיצה ארוכה על תמונה והעפה שלה הצידה מסמנות אותה למחיקה",
            )
        AppLanguage.English ->
            PhotoWallStrings(
                title = "Photo Wall",
                subtitle = "Spread out your library, flick away the misses",
                hint = "Press and hold a photo, then flick it away to mark it for deletion",
            )
    }
