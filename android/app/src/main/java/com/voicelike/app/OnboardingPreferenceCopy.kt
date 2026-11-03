package com.voicelike.app

internal data class OnboardingPreferenceCopy(
    val usagePreferenceTitle: String,
    val usagePreferenceSubtitle: String,
    val organizePreferredTitle: String,
    val organizePreferredSubtitle: String,
    val creativePreferredTitle: String,
    val creativePreferredSubtitle: String,
    val organizeSwipeTitle: String,
    val organizeSwipeSubtitle: String,
    val organizeDeleteTitle: String
)

internal fun onboardingPreferenceCopy(language: AppLanguage): OnboardingPreferenceCopy = when (language) {
    AppLanguage.SimplifiedChinese -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "使用偏好",
        usagePreferenceSubtitle = "先帮你决定欢迎页默认打开哪类功能，之后也可以在设置里修改。",
        organizePreferredTitle = "整理优先",
        organizePreferredSubtitle = "高效整理、回顾和分类相册",
        creativePreferredTitle = "创意功能优先",
        creativePreferredSubtitle = "调色盘相机、创意、水印边框、集邮等创意功能",
        organizeSwipeTitle = "整理·滑动偏好",
        organizeSwipeSubtitle = "设置整理时左滑、右滑和上滑分别执行什么操作。",
        organizeDeleteTitle = "整理·删除模式"
    )
    AppLanguage.TraditionalChinese -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "使用偏好",
        usagePreferenceSubtitle = "先幫你決定歡迎頁預設打開哪類功能，之後也可以在設定裡修改。",
        organizePreferredTitle = "整理優先",
        organizePreferredSubtitle = "高效整理、回顧和分類相簿",
        creativePreferredTitle = "創意功能優先",
        creativePreferredSubtitle = "調色盤相機、創意、水印邊框、集郵等創意功能",
        organizeSwipeTitle = "整理·滑動偏好",
        organizeSwipeSubtitle = "設定整理時左滑、右滑和上滑分別執行什麼操作。",
        organizeDeleteTitle = "整理·刪除模式"
    )
    AppLanguage.Japanese -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "利用設定",
        usagePreferenceSubtitle = "ようこそ画面で最初に開くエリアを選びます。後から設定で変更できます。",
        organizePreferredTitle = "整理を優先",
        organizePreferredSubtitle = "アルバムをより効率よく整理、見返し、分類できます",
        creativePreferredTitle = "クリエイティブを優先",
        creativePreferredSubtitle = "パレットカメラ、クリエイティブ、透かしフレーム、スタンプなどの機能",
        organizeSwipeTitle = "整理・スワイプ設定",
        organizeSwipeSubtitle = "整理中の左、右、上スワイプ動作を選びます。",
        organizeDeleteTitle = "整理・削除モード"
    )
    AppLanguage.Korean -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "사용 설정",
        usagePreferenceSubtitle = "시작 시 먼저 열릴 영역을 선택하세요. 나중에 설정에서 변경할 수 있습니다.",
        organizePreferredTitle = "정리 우선",
        organizePreferredSubtitle = "앨범을 더 효율적으로 정리하고 다시 보고 분류합니다",
        creativePreferredTitle = "크리에이티브 우선",
        creativePreferredSubtitle = "팔레트 카메라, 크리에이티브, 워터마크 프레임, 스탬프 등의 기능",
        organizeSwipeTitle = "정리 · 스와이프 설정",
        organizeSwipeSubtitle = "정리 중 왼쪽, 오른쪽, 위로 스와이프 동작을 선택하세요.",
        organizeDeleteTitle = "정리 · 삭제 모드"
    )
    AppLanguage.French -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Préférence d'utilisation",
        usagePreferenceSubtitle = "Choisissez la section ouverte en premier sur l'accueil. Vous pourrez la modifier plus tard dans Réglages.",
        organizePreferredTitle = "Organiser d'abord",
        organizePreferredSubtitle = "Organisez, revoyez et classez votre album plus efficacement",
        creativePreferredTitle = "Créatif d'abord",
        creativePreferredSubtitle = "Caméra palette, espace créatif, cadres filigrane, outils timbre et autres fonctions créatives",
        organizeSwipeTitle = "Organiser · Préférences de balayage",
        organizeSwipeSubtitle = "Choisissez l'action des balayages gauche, droite et haut pendant l'organisation.",
        organizeDeleteTitle = "Organiser · Mode de suppression"
    )
    AppLanguage.German -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Nutzungspräferenz",
        usagePreferenceSubtitle = "Wähle, welcher Bereich der Startseite zuerst geöffnet wird. Später kannst du das in den Einstellungen ändern.",
        organizePreferredTitle = "Zuerst organisieren",
        organizePreferredSubtitle = "Album effizienter ordnen, erneut ansehen und sortieren",
        creativePreferredTitle = "Zuerst kreativ",
        creativePreferredSubtitle = "Palettenkamera, Kreativbereich, Wasserzeichenrahmen, Stempelwerkzeuge und weitere kreative Funktionen",
        organizeSwipeTitle = "Organisieren · Wisch-Einstellungen",
        organizeSwipeSubtitle = "Lege fest, was Wischen nach links, rechts und oben beim Organisieren macht.",
        organizeDeleteTitle = "Organisieren · Löschmodus"
    )
    AppLanguage.Italian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Preferenza d'uso",
        usagePreferenceSubtitle = "Scegli quale area aprire per prima nella schermata iniziale. Puoi cambiarla più tardi nelle Impostazioni.",
        organizePreferredTitle = "Prima organizza",
        organizePreferredSubtitle = "Organizza, rivedi e ordina l'album in modo più efficiente",
        creativePreferredTitle = "Prima creativo",
        creativePreferredSubtitle = "Fotocamera palette, area creativa, cornici filigrana, strumenti francobollo e altre funzioni creative",
        organizeSwipeTitle = "Organizza · Preferenze di scorrimento",
        organizeSwipeSubtitle = "Scegli cosa fanno gli swipe a sinistra, destra e in alto durante l'organizzazione.",
        organizeDeleteTitle = "Organizza · Modalità eliminazione"
    )
    AppLanguage.Spanish -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Preferencia de uso",
        usagePreferenceSubtitle = "Elige qué área se abre primero en la pantalla de bienvenida. Puedes cambiarlo después en Ajustes.",
        organizePreferredTitle = "Organizar primero",
        organizePreferredSubtitle = "Organiza, revisa y clasifica tu álbum con más eficiencia",
        creativePreferredTitle = "Creativo primero",
        creativePreferredSubtitle = "Cámara de paleta, espacio creativo, marcos de marca de agua, herramientas de sellos y otras funciones creativas",
        organizeSwipeTitle = "Organizar · Preferencias de deslizamiento",
        organizeSwipeSubtitle = "Elige qué hacen los deslizamientos a la izquierda, derecha y arriba mientras organizas.",
        organizeDeleteTitle = "Organizar · Modo de eliminación"
    )
    AppLanguage.Portuguese -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Preferência de uso",
        usagePreferenceSubtitle = "Escolha qual área abre primeiro na tela inicial. Você pode mudar isso depois em Configurações.",
        organizePreferredTitle = "Organizar primeiro",
        organizePreferredSubtitle = "Organize, reveja e classifique seu álbum com mais eficiência",
        creativePreferredTitle = "Criativo primeiro",
        creativePreferredSubtitle = "Câmera de paleta, espaço criativo, molduras com marca-d'água, ferramentas de selos e outros recursos criativos",
        organizeSwipeTitle = "Organizar · Preferências de deslize",
        organizeSwipeSubtitle = "Escolha o que os gestos para esquerda, direita e cima fazem durante a organização.",
        organizeDeleteTitle = "Organizar · Modo de exclusão"
    )
    AppLanguage.Russian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Предпочтение использования",
        usagePreferenceSubtitle = "Выберите, какой раздел открывать первым на стартовой странице. Позже это можно изменить в настройках.",
        organizePreferredTitle = "Сначала организация",
        organizePreferredSubtitle = "Упорядочивайте, пересматривайте и сортируйте альбом эффективнее",
        creativePreferredTitle = "Сначала творчество",
        creativePreferredSubtitle = "Камера палитры, творческий раздел, рамки водяных знаков, инструменты штампов и другие творческие функции",
        organizeSwipeTitle = "Организация · Настройки свайпов",
        organizeSwipeSubtitle = "Выберите, что делают свайпы влево, вправо и вверх во время организации.",
        organizeDeleteTitle = "Организация · Режим удаления"
    )
    AppLanguage.Thai -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "การตั้งค่าการใช้งาน",
        usagePreferenceSubtitle = "เลือกว่าจะเปิดส่วนใดก่อนบนหน้าเริ่มต้น คุณเปลี่ยนได้ภายหลังในตั้งค่า",
        organizePreferredTitle = "จัดระเบียบก่อน",
        organizePreferredSubtitle = "จัดระเบียบ ทบทวน และแยกอัลบั้มได้อย่างมีประสิทธิภาพมากขึ้น",
        creativePreferredTitle = "สร้างสรรค์ก่อน",
        creativePreferredSubtitle = "กล้องพาเลต พื้นที่สร้างสรรค์ กรอบลายน้ำ เครื่องมือแสตมป์ และฟังก์ชันสร้างสรรค์อื่น ๆ",
        organizeSwipeTitle = "จัดระเบียบ · การตั้งค่าการปัด",
        organizeSwipeSubtitle = "เลือกว่าการปัดซ้าย ขวา และขึ้นจะทำอะไรระหว่างการจัดระเบียบ",
        organizeDeleteTitle = "จัดระเบียบ · โหมดการลบ"
    )
    AppLanguage.Vietnamese -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Tùy chọn sử dụng",
        usagePreferenceSubtitle = "Chọn khu vực mở đầu tiên trên trang chào mừng. Bạn có thể đổi sau trong Cài đặt.",
        organizePreferredTitle = "Ưu tiên sắp xếp",
        organizePreferredSubtitle = "Sắp xếp, xem lại và phân loại album hiệu quả hơn",
        creativePreferredTitle = "Ưu tiên sáng tạo",
        creativePreferredSubtitle = "Máy ảnh bảng màu, không gian sáng tạo, khung watermark, công cụ tem và các tính năng sáng tạo khác",
        organizeSwipeTitle = "Sắp xếp · Tùy chọn vuốt",
        organizeSwipeSubtitle = "Chọn thao tác của vuốt trái, phải và lên khi sắp xếp.",
        organizeDeleteTitle = "Sắp xếp · Chế độ xóa"
    )
    AppLanguage.Indonesian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Preferensi penggunaan",
        usagePreferenceSubtitle = "Pilih area yang dibuka lebih dulu di halaman sambutan. Anda bisa mengubahnya nanti di Pengaturan.",
        organizePreferredTitle = "Atur lebih dulu",
        organizePreferredSubtitle = "Atur, tinjau ulang, dan sortir album dengan lebih efisien",
        creativePreferredTitle = "Kreatif lebih dulu",
        creativePreferredSubtitle = "Kamera palet, ruang kreatif, bingkai watermark, alat perangko, dan fitur kreatif lainnya",
        organizeSwipeTitle = "Atur · Preferensi geser",
        organizeSwipeSubtitle = "Pilih fungsi geser kiri, kanan, dan atas saat mengatur.",
        organizeDeleteTitle = "Atur · Mode penghapusan"
    )
    AppLanguage.Arabic -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "تفضيل الاستخدام",
        usagePreferenceSubtitle = "اختر القسم الذي يفتح أولاً في صفحة الترحيب. يمكنك تغييره لاحقًا من الإعدادات.",
        organizePreferredTitle = "التنظيم أولاً",
        organizePreferredSubtitle = "نظّم الألبوم وراجعه وصنّفه بكفاءة أكبر",
        creativePreferredTitle = "الإبداع أولاً",
        creativePreferredSubtitle = "كاميرا اللوحات، قسم الإبداع، إطارات العلامة المائية، أدوات الطوابع، وميزات إبداعية أخرى",
        organizeSwipeTitle = "التنظيم · تفضيلات السحب",
        organizeSwipeSubtitle = "اختر ما تفعله السحبات لليسار واليمين والأعلى أثناء التنظيم.",
        organizeDeleteTitle = "التنظيم · وضع الحذف"
    )
    AppLanguage.Ukrainian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Спосіб використання",
        usagePreferenceSubtitle = "Виберіть, який розділ відкриватиметься першим на головній сторінці. Це можна змінити пізніше в налаштуваннях.",
        organizePreferredTitle = "Спершу розбір",
        organizePreferredSubtitle = "Упорядковуйте, переглядайте й сортуйте альбом ефективніше",
        creativePreferredTitle = "Спершу творчість",
        creativePreferredSubtitle = "Камера палітри, творчість, водяні знаки, марки та інші творчі функції",
        organizeSwipeTitle = "Розбір: налаштування свайпів",
        organizeSwipeSubtitle = "Виберіть, що робитимуть свайпи ліворуч, праворуч і вгору під час розбору.",
        organizeDeleteTitle = "Розбір: режим видалення"
    )
    AppLanguage.Polish -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Preferowany sposób użycia",
        usagePreferenceSubtitle = "Wybierz, który obszar ma się otwierać jako pierwszy na ekranie powitalnym. Możesz to później zmienić w Ustawieniach.",
        organizePreferredTitle = "Najpierw porządki",
        organizePreferredSubtitle = "Porządkuj, przeglądaj i sortuj album sprawniej",
        creativePreferredTitle = "Najpierw kreatywność",
        creativePreferredSubtitle = "Aparat z paletą, kreatywność, ramki ze znakiem wodnym, narzędzia pieczątek i inne funkcje kreatywne",
        organizeSwipeTitle = "Porządkowanie: preferencje przesuwania",
        organizeSwipeSubtitle = "Zdecyduj, co robią przesunięcia w lewo, w prawo i w górę podczas porządków.",
        organizeDeleteTitle = "Porządkowanie: tryb usuwania"
    )
    AppLanguage.Czech -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Způsob používání",
        usagePreferenceSubtitle = "Vyberte, která oblast se na úvodní obrazovce otevře jako první. Později to můžete změnit v Nastavení.",
        organizePreferredTitle = "Nejdřív třídění",
        organizePreferredSubtitle = "Třiďte, procházejte a řaďte album efektivněji",
        creativePreferredTitle = "Nejdřív tvorba",
        creativePreferredSubtitle = "Fotoaparát s paletou, tvorba, vodoznakové rámečky, nástroje razítek a další tvůrčí funkce",
        organizeSwipeTitle = "Třídění: předvolby gest",
        organizeSwipeSubtitle = "Vyberte, co se při třídění stane po přejetí doleva, doprava a nahoru.",
        organizeDeleteTitle = "Třídění: režim mazání"
    )
    AppLanguage.Dutch -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Gebruiksvoorkeur",
        usagePreferenceSubtitle = "Kies welk onderdeel het welkomstscherm als eerste opent. Je kunt dit later wijzigen in Instellingen.",
        organizePreferredTitle = "Eerst ordenen",
        organizePreferredSubtitle = "Orden, bekijk opnieuw en sorteer je album efficiënter",
        creativePreferredTitle = "Eerst creatief",
        creativePreferredSubtitle = "Paletcamera, Creatief, watermerkframes, stempeltools en andere creatieve functies",
        organizeSwipeTitle = "Ordenen: veegvoorkeuren",
        organizeSwipeSubtitle = "Kies wat vegen naar links, rechts en omhoog doet tijdens het ordenen.",
        organizeDeleteTitle = "Ordenen: verwijdermodus"
    )
    AppLanguage.Swedish -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Användningsval",
        usagePreferenceSubtitle = "Välj vilket område som öppnas först på startsidan. Du kan ändra det senare i Inställningar.",
        organizePreferredTitle = "Sortera först",
        organizePreferredSubtitle = "Sortera, gå igenom och ordna ditt album mer effektivt",
        creativePreferredTitle = "Kreativt först",
        creativePreferredSubtitle = "Palettkamera, Kreativt, vattenstämpelramar, stämpelverktyg och andra kreativa funktioner",
        organizeSwipeTitle = "Sortering: svepval",
        organizeSwipeSubtitle = "Välj vad svep åt vänster, höger och uppåt gör när du sorterar.",
        organizeDeleteTitle = "Sortering: raderingsläge"
    )
    AppLanguage.Danish -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Brugspræference",
        usagePreferenceSubtitle = "Vælg, hvilket område startsiden åbner først. Du kan ændre det senere i Indstillinger.",
        organizePreferredTitle = "Sortér først",
        organizePreferredSubtitle = "Sortér, gense og organisér dit album mere effektivt",
        creativePreferredTitle = "Kreativt først",
        creativePreferredSubtitle = "Paletkamera, Kreativt, vandmærkerammer, stempelværktøjer og andre kreative funktioner",
        organizeSwipeTitle = "Sortering: stryg-valg",
        organizeSwipeSubtitle = "Vælg, hvad strøg til venstre, højre og op gør under sorteringen.",
        organizeDeleteTitle = "Sortering: sletningstilstand"
    )
    AppLanguage.Norwegian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Bruksvalg",
        usagePreferenceSubtitle = "Velg hvilket område startsiden åpner først. Du kan endre det senere i Innstillinger.",
        organizePreferredTitle = "Rydd først",
        organizePreferredSubtitle = "Rydd, se gjennom og sorter albumet mer effektivt",
        creativePreferredTitle = "Kreativt først",
        creativePreferredSubtitle = "Palettkamera, Kreativt, vannmerkerammer, stempelverktøy og andre kreative funksjoner",
        organizeSwipeTitle = "Rydding: sveipvalg",
        organizeSwipeSubtitle = "Velg hva sveip til venstre, høyre og opp gjør mens du rydder.",
        organizeDeleteTitle = "Rydding: slettemodus"
    )
    AppLanguage.Finnish -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Käyttötapa",
        usagePreferenceSubtitle = "Valitse, mikä osio avautuu ensimmäisenä aloitusnäkymässä. Voit muuttaa tätä myöhemmin Asetuksissa.",
        organizePreferredTitle = "Järjestäminen ensin",
        organizePreferredSubtitle = "Järjestä, käy läpi ja lajittele albumisi tehokkaammin",
        creativePreferredTitle = "Luovuus ensin",
        creativePreferredSubtitle = "Palettikamera, Luova, vesileimakehykset, leimasintyökalut ja muut luovat toiminnot",
        organizeSwipeTitle = "Järjestäminen: pyyhkäisyasetukset",
        organizeSwipeSubtitle = "Valitse, mitä pyyhkäisy vasemmalle, oikealle ja ylös tekee järjestämisen aikana.",
        organizeDeleteTitle = "Järjestäminen: poistotila"
    )
    AppLanguage.Greek -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Προτίμηση χρήσης",
        usagePreferenceSubtitle = "Επιλέξτε ποια ενότητα θα ανοίγει πρώτη στην αρχική οθόνη. Μπορείτε να το αλλάξετε αργότερα στις Ρυθμίσεις.",
        organizePreferredTitle = "Πρώτα η ταξινόμηση",
        organizePreferredSubtitle = "Ταξινομήστε, ξαναδείτε και οργανώστε το άλμπουμ σας πιο αποτελεσματικά",
        creativePreferredTitle = "Πρώτα η δημιουργικότητα",
        creativePreferredSubtitle = "Κάμερα παλέτας, Δημιουργικότητα, πλαίσια υδατογραφήματος, εργαλεία σφραγίδων και άλλες δημιουργικές λειτουργίες",
        organizeSwipeTitle = "Ταξινόμηση: προτιμήσεις σύρσιμου",
        organizeSwipeSubtitle = "Επιλέξτε τι κάνουν τα σύρσιμα αριστερά, δεξιά και πάνω κατά την ταξινόμηση.",
        organizeDeleteTitle = "Ταξινόμηση: λειτουργία διαγραφής"
    )
    AppLanguage.Romanian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Preferință de utilizare",
        usagePreferenceSubtitle = "Alegeți ce secțiune se deschide prima pe ecranul de întâmpinare. Puteți schimba oricând din Setări.",
        organizePreferredTitle = "Mai întâi organizarea",
        organizePreferredSubtitle = "Organizați, revedeți și sortați albumul mai eficient",
        creativePreferredTitle = "Mai întâi creativitatea",
        creativePreferredSubtitle = "Cameră cu paletă, Creativ, rame cu filigran, instrumente de ștampile și alte funcții creative",
        organizeSwipeTitle = "Organizare: preferințe de glisare",
        organizeSwipeSubtitle = "Alegeți ce fac glisările la stânga, la dreapta și în sus în timpul organizării.",
        organizeDeleteTitle = "Organizare: modul de ștergere"
    )
    AppLanguage.Hungarian -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Használati preferencia",
        usagePreferenceSubtitle = "Válassza ki, melyik terület nyíljon meg először a kezdőlapon. Később a Beállításokban módosíthatja.",
        organizePreferredTitle = "Először a rendezés",
        organizePreferredSubtitle = "Rendezze, tekintse át és csoportosítsa albumát hatékonyabban",
        creativePreferredTitle = "Először a kreatív",
        creativePreferredSubtitle = "Palettakamera, Kreatív, vízjeles keretek, bélyegzőeszközök és más kreatív funkciók",
        organizeSwipeTitle = "Rendezés: csúsztatási beállítások",
        organizeSwipeSubtitle = "Válassza ki, mit tegyen a balra, jobbra és felfelé legyintés rendezés közben.",
        organizeDeleteTitle = "Rendezés: törlési mód"
    )
    AppLanguage.Turkish -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Kullanım Tercihi",
        usagePreferenceSubtitle = "Karşılama ekranında önce hangi alanın açılacağını seç. Bunu daha sonra Ayarlar'dan değiştirebilirsin.",
        organizePreferredTitle = "Önce Düzenleme",
        organizePreferredSubtitle = "Albümünü daha verimli düzenle, gözden geçir ve sınıflandır",
        creativePreferredTitle = "Önce Yaratıcılık",
        creativePreferredSubtitle = "Palet kamerası, Yaratıcılık, filigran çerçeveleri, damga araçları ve diğer yaratıcı özellikler",
        organizeSwipeTitle = "Düzenleme: Kaydırma Tercihleri",
        organizeSwipeSubtitle = "Düzenleme sırasında sola, sağa ve yukarı kaydırmanın ne yapacağını seç.",
        organizeDeleteTitle = "Düzenleme: Silme Modu"
    )
    AppLanguage.Malay -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Keutamaan Penggunaan",
        usagePreferenceSubtitle = "Pilih bahagian yang dibuka dahulu pada skrin utama. Anda boleh menukarnya kemudian dalam Tetapan.",
        organizePreferredTitle = "Susun Dahulu",
        organizePreferredSubtitle = "Susun, semak semula dan isih album anda dengan lebih cekap",
        creativePreferredTitle = "Kreatif Dahulu",
        creativePreferredSubtitle = "Kamera palet, Kreatif, bingkai tera air, alat setem dan ciri kreatif lain",
        organizeSwipeTitle = "Susun: Pilihan Leret",
        organizeSwipeSubtitle = "Pilih tindakan bagi leretan ke kiri, kanan dan atas semasa menyusun.",
        organizeDeleteTitle = "Susun: Mod Pemadaman"
    )
    AppLanguage.Hebrew -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "העדפת שימוש",
        usagePreferenceSubtitle = "בחרו איזה אזור ייפתח ראשון במסך הבית. אפשר לשנות זאת מאוחר יותר בהגדרות.",
        organizePreferredTitle = "קודם סידור",
        organizePreferredSubtitle = "סדרו, עברו שוב וסווגו את האלבום ביעילות רבה יותר",
        creativePreferredTitle = "קודם יצירה",
        creativePreferredSubtitle = "מצלמת פלטה, יצירה, מסגרות סימן מים, כלי חותמות ותכונות יצירתיות נוספות",
        organizeSwipeTitle = "סידור: העדפות החלקה",
        organizeSwipeSubtitle = "מה קורה בהחלקה שמאלה, ימינה ולמעלה בזמן הסידור.",
        organizeDeleteTitle = "סידור: מצב מחיקה"
    )
    AppLanguage.English -> OnboardingPreferenceCopy(
        usagePreferenceTitle = "Usage Preference",
        usagePreferenceSubtitle = "Choose which area the welcome home opens first. You can change it later in Settings.",
        organizePreferredTitle = "Organize First",
        organizePreferredSubtitle = "Organize, revisit, and sort your album more efficiently",
        creativePreferredTitle = "Creative First",
        creativePreferredSubtitle = "Palette camera, Creative, watermark frames, stamp tools, and other creative features",
        organizeSwipeTitle = "Organize · Swipe Preferences",
        organizeSwipeSubtitle = "Choose what left, right, and up swipes do while organizing.",
        organizeDeleteTitle = "Organize · Deletion Mode"
    )
}
