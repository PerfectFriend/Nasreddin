package com.example.util

import com.example.model.AppLanguage
import com.example.ui.library.LibraryFilter

object AppStrings {

    fun appTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Ходжа"
        AppLanguage.SPANISH -> "Hodja"
        AppLanguage.ENGLISH -> "Hodja"
    }

    fun appSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Полноэкранная читалка комиксов"
        AppLanguage.SPANISH -> "Lector de cómics a pantalla completa"
        AppLanguage.ENGLISH -> "Fullscreen comic reader"
    }

    fun splashSermonTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Проповедь на вершине минарета"
        AppLanguage.SPANISH -> "El sermón en la cumbre del minarete"
        AppLanguage.ENGLISH -> "The Sermon on the Minaret Summit"
    }

    fun systemLangDetected(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Язык системы определен"
        AppLanguage.SPANISH -> "Idioma del sistema detectado"
        AppLanguage.ENGLISH -> "System language detected"
    }

    fun activeLangLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Язык приложения"
        AppLanguage.SPANISH -> "Idioma de la app"
        AppLanguage.ENGLISH -> "App language"
    }

    fun readComicAction(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Читать комикс"
        AppLanguage.SPANISH -> "Leer el cómic"
        AppLanguage.ENGLISH -> "Read comic"
    }

    fun goToLibraryAction(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Перейти в библиотеку"
        AppLanguage.SPANISH -> "Ir a la biblioteca"
        AppLanguage.ENGLISH -> "Go to library"
    }

    fun collectionHeader(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Коллекция комиксов ($count)"
        AppLanguage.SPANISH -> "Colección de cómics ($count)"
        AppLanguage.ENGLISH -> "Comic Collection ($count)"
    }

    fun loadFolderTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Загрузить папку"
        AppLanguage.SPANISH -> "Cargar carpeta"
        AppLanguage.ENGLISH -> "Load Folder"
    }

    fun loadFolderSub(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Авто-сортировка картинок"
        AppLanguage.SPANISH -> "Auto-orden natural"
        AppLanguage.ENGLISH -> "Auto-sort images"
    }

    fun selectFilesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Выбрать файлы"
        AppLanguage.SPANISH -> "Elegir imágenes"
        AppLanguage.ENGLISH -> "Select Files"
    }

    fun selectFilesSub(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Любые изображения"
        AppLanguage.SPANISH -> "Archivos de imagen"
        AppLanguage.ENGLISH -> "Image files"
    }

    fun readButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Читать"
        AppLanguage.SPANISH -> "Leer"
        AppLanguage.ENGLISH -> "Read"
    }

    fun continueReading(lang: AppLanguage, page: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Продолжить (стр. $page)"
        AppLanguage.SPANISH -> "Continuar (pág. $page)"
        AppLanguage.ENGLISH -> "Continue (page $page)"
    }

    fun pageSequence(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Порядок ($count)"
        AppLanguage.SPANISH -> "Orden ($count)"
        AppLanguage.ENGLISH -> "Sequence ($count)"
    }

    fun pageOrderButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Порядок"
        AppLanguage.SPANISH -> "Orden"
        AppLanguage.ENGLISH -> "Sequence"
    }

    fun officialEdition(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Официальное издание • $count стр."
        AppLanguage.SPANISH -> "Edición oficial • $count págs."
        AppLanguage.ENGLISH -> "Official Edition • $count pages"
    }

    fun userVolume(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Пользовательский том • $count стр."
        AppLanguage.SPANISH -> "Tomo de usuario • $count págs."
        AppLanguage.ENGLISH -> "User Volume • $count pages"
    }

    fun stoppedAtPage(lang: AppLanguage, page: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Остановлено на стр. $page"
        AppLanguage.SPANISH -> "Detenido en pág. $page"
        AppLanguage.ENGLISH -> "Stopped at page $page"
    }

    fun deleteComic(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить комикс"
        AppLanguage.SPANISH -> "Eliminar cómic"
        AppLanguage.ENGLISH -> "Delete comic"
    }

    fun wisdomCardTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Восточная мудрость Ходжи Насреддина"
        AppLanguage.SPANISH -> "La sabiduría de Nasreddin Hodja"
        AppLanguage.ENGLISH -> "The Wisdom of Hodja Nasreddin"
    }

    fun wisdomCardText(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "«Проповедь в мечети» и другие истории — бессмертное наследие Ходжи Насреддина. Мудрец испытывает людей парадоксальными вопросами, преподавая тонкий урок о готовности познавать и делиться знаниями."
        AppLanguage.SPANISH -> "«El sermón en la mezquita» y otras historias son el legado atemporal de Nasreddin Hodja. El sabio pone a prueba a las personas con preguntas paradójicas, ofreciendo lecciones sobre el conocimiento."
        AppLanguage.ENGLISH -> "'The Sermon in the Mosque' and other tales are the timeless legacy of Hodja Nasreddin. The sage challenges people with paradoxical questions, offering lessons on knowledge and sharing."
    }

    fun loadingImages(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Загрузка и сортировка изображений..."
        AppLanguage.SPANISH -> "Cargando y ordenando imágenes..."
        AppLanguage.ENGLISH -> "Loading and sorting images..."
    }

    fun pageCounter(lang: AppLanguage, current: Int, total: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Стр. $current из $total"
        AppLanguage.SPANISH -> "Pág. $current de $total"
        AppLanguage.ENGLISH -> "Page $current of $total"
    }

    fun backToLibrary(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Назад к библиотеке"
        AppLanguage.SPANISH -> "Volver a la biblioteca"
        AppLanguage.ENGLISH -> "Back to Library"
    }

    fun sermonTextTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Текст притчи"
        AppLanguage.SPANISH -> "Texto del sermón"
        AppLanguage.ENGLISH -> "Sermon text"
    }

    fun pageSequenceTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Порядок страниц"
        AppLanguage.SPANISH -> "Orden de páginas"
        AppLanguage.ENGLISH -> "Page sequence"
    }

    fun switchToScrollTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Переключить на вебтун-скролл"
        AppLanguage.SPANISH -> "Cambiar a scroll continuo"
        AppLanguage.ENGLISH -> "Switch to continuous scroll"
    }

    fun switchToPagerTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Переключить на постраничный режим"
        AppLanguage.SPANISH -> "Cambiar a modo página"
        AppLanguage.ENGLISH -> "Switch to page mode"
    }

    fun readerBackgroundTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Фон ридера"
        AppLanguage.SPANISH -> "Fondo del lector"
        AppLanguage.ENGLISH -> "Reader background"
    }

    fun bgCyberMidnight(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Кибер-полночь (тёмно-синий)"
        AppLanguage.SPANISH -> "Azul medianoche"
        AppLanguage.ENGLISH -> "Midnight Blue"
    }

    fun bgPureBlack(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "OLED Черный"
        AppLanguage.SPANISH -> "Negro puro OLED"
        AppLanguage.ENGLISH -> "OLED Pure Black"
    }

    fun bgSepia(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Сепия (пергамент)"
        AppLanguage.SPANISH -> "Sepia (pergamino)"
        AppLanguage.ENGLISH -> "Sepia (parchment)"
    }

    fun prevPage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Предыдущая страница"
        AppLanguage.SPANISH -> "Página anterior"
        AppLanguage.ENGLISH -> "Previous page"
    }

    fun nextPage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Следующая страница"
        AppLanguage.SPANISH -> "Página siguiente"
        AppLanguage.ENGLISH -> "Next page"
    }

    fun close(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Закрыть"
        AppLanguage.SPANISH -> "Cerrar"
        AppLanguage.ENGLISH -> "Close"
    }

    fun pageStripTitle(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Лента страниц ($count)"
        AppLanguage.SPANISH -> "Tira de páginas ($count)"
        AppLanguage.ENGLISH -> "Page Strip ($count)"
    }

    fun sequenceSheetTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Последовательность страниц"
        AppLanguage.SPANISH -> "Orden de páginas"
        AppLanguage.ENGLISH -> "Page Sequence"
    }

    fun sortAZ(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Имена (А-Я)"
        AppLanguage.SPANISH -> "Nombres (A-Z)"
        AppLanguage.ENGLISH -> "Names (A-Z)"
    }

    fun sortReverse(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Реверс"
        AppLanguage.SPANISH -> "Invertir"
        AppLanguage.ENGLISH -> "Reverse"
    }

    fun moveUp(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Поднять выше"
        AppLanguage.SPANISH -> "Mover arriba"
        AppLanguage.ENGLISH -> "Move up"
    }

    fun moveDown(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Опустить ниже"
        AppLanguage.SPANISH -> "Mover abajo"
        AppLanguage.ENGLISH -> "Move down"
    }

    fun deletePage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить страницу"
        AppLanguage.SPANISH -> "Eliminar página"
        AppLanguage.ENGLISH -> "Delete page"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Отмена"
        AppLanguage.SPANISH -> "Cancelar"
        AppLanguage.ENGLISH -> "Cancel"
    }

    fun apply(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Применить"
        AppLanguage.SPANISH -> "Aplicar"
        AppLanguage.ENGLISH -> "Apply"
    }

    fun comicDialoguesLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Реплики комикса:"
        AppLanguage.SPANISH -> "Diálogos del cómic:"
        AppLanguage.ENGLISH -> "Comic Dialogues:"
    }

    fun emptyComicNotice(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Комикс не содержит страниц"
        AppLanguage.SPANISH -> "El cómic no contiene páginas"
        AppLanguage.ENGLISH -> "The comic contains no pages"
    }

    fun splashTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Заставка"
        AppLanguage.SPANISH -> "Inicio"
        AppLanguage.ENGLISH -> "Splash"
    }

    // Hybrid Monetization & Paywall strings
    fun freeBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Бесплатно"
        AppLanguage.SPANISH -> "Gratis"
        AppLanguage.ENGLISH -> "Free"
    }

    fun adUnlockedBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "📺 Открыто с рекламой"
        AppLanguage.SPANISH -> "📺 Con anuncios"
        AppLanguage.ENGLISH -> "📺 Ad-Supported"
    }

    fun proUnlockedBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "👑 PRO без рекламы"
        AppLanguage.SPANISH -> "👑 PRO sin anuncios"
        AppLanguage.ENGLISH -> "👑 PRO Ad-Free"
    }

    fun premiumBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "🔒 Реклама или PRO"
        AppLanguage.SPANISH -> "🔒 Anuncio o PRO"
        AppLanguage.ENGLISH -> "🔒 Ad or PRO"
    }

    fun unlockCtaChip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Открыть"
        AppLanguage.SPANISH -> "Abrir"
        AppLanguage.ENGLISH -> "Unlock"
    }

    fun pagesCountSuffix(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "$count стр."
        AppLanguage.SPANISH -> "$count págs."
        AppLanguage.ENGLISH -> "$count pages"
    }

    fun unlockFullVaultBanner(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "👑 PRO без рекламы и вся коллекция ($4.99)"
        AppLanguage.SPANISH -> "👑 PRO sin anuncios y colección completa ($4.99)"
        AppLanguage.ENGLISH -> "👑 PRO Ad-Free Full Collection ($4.99)"
    }

    fun hybridBannerSubtext(lang: AppLanguage, unlockedCount: Int, totalCount: Int, isAdSupportedUnlocked: Boolean): String = when (lang) {
        AppLanguage.RUSSIAN -> if (isAdSupportedUnlocked) {
            "Открыто $unlockedCount из $totalCount (с рекламой) • Отключите рекламу в PRO"
        } else {
            "1–10 бесплатно • 11–$totalCount за рекламу, серию ($0.99) или в PRO"
        }
        AppLanguage.SPANISH -> if (isAdSupportedUnlocked) {
            "Desbloqueadas $unlockedCount de $totalCount (con anuncios) • Quítalos con PRO"
        } else {
            "1–10 gratis • 11–$totalCount con anuncio, serie ($0.99) o PRO"
        }
        AppLanguage.ENGLISH -> if (isAdSupportedUnlocked) {
            "Unlocked $unlockedCount of $totalCount (Ad-supported) • Go Ad-Free in PRO"
        } else {
            "1–10 Free • 11–$totalCount via Ad, Series ($0.99) or PRO"
        }
    }

    fun unlockedStatusBanner(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "👑 PRO активирован • Без рекламы"
        AppLanguage.SPANISH -> "👑 PRO activado • Sin anuncios"
        AppLanguage.ENGLISH -> "👑 PRO Active • Ad-Free"
    }

    fun unlockedStatusSubtext(lang: AppLanguage, totalCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Все $totalCount историй доступны офлайн и без рекламы"
        AppLanguage.SPANISH -> "Las $totalCount historias disponibles sin conexión y sin anuncios"
        AppLanguage.ENGLISH -> "All $totalCount stories unlocked offline & ad-free"
    }

    fun seriesUnlockedStatusBanner(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "✨ Серия открыта без рекламы"
        AppLanguage.SPANISH -> "✨ Serie desbloqueada sin anuncios"
        AppLanguage.ENGLISH -> "✨ Series Unlocked Ad-Free"
    }

    fun paywallTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите способ доступа"
        AppLanguage.SPANISH -> "Elige cómo desbloquear"
        AppLanguage.ENGLISH -> "Choose Access Mode"
    }

    fun paywallSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Первые 10 историй полностью бесплатны. Дальше можно читать бесплатно с короткой рекламой, купить серию за $0.99 или открыть PRO навсегда."
        AppLanguage.SPANISH -> "Las primeras 10 historias son gratis. Sigue leyendo gratis con un anuncio corto, compra la serie por $0.99 o desbloquea PRO para siempre."
        AppLanguage.ENGLISH -> "Stories 1–10 are completely free. Continue free with a short ad, unlock this series for $0.99, or go PRO forever."
    }

    fun paywallOriginalPrice(): String = "$9.99"
    fun paywallSalePrice(): String = "$4.99"
    fun seriesSalePrice(): String = "$0.99"

    fun paywallDiscountBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "-50% PRO • Без рекламы навсегда"
        AppLanguage.SPANISH -> "-50% PRO • Sin anuncios para siempre"
        AppLanguage.ENGLISH -> "-50% PRO • Ad-Free Forever"
    }

    fun paywallBenefit1(lang: AppLanguage, totalCount: Int = 100): String = when (lang) {
        AppLanguage.RUSSIAN -> "Все $totalCount+ историй (минимум 4 страницы в каждой)"
        AppLanguage.SPANISH -> "Las $totalCount+ historias (mínimo 4 páginas cada una)"
        AppLanguage.ENGLISH -> "All $totalCount+ stories (4+ fullscreen pages each)"
    }

    fun paywallBenefit2(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Полное отключение всей рекламы навсегда"
        AppLanguage.SPANISH -> "Cero anuncios para siempre"
        AppLanguage.ENGLISH -> "100% Ad-free reading forever"
    }

    fun paywallBenefit3(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Все языки: RU, ES, EN + будущие тома"
        AppLanguage.SPANISH -> "Todos los idiomas: RU, ES, EN + próximos tomos"
        AppLanguage.ENGLISH -> "All languages: RU, ES, EN + future volumes"
    }

    fun paywallBenefit4(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Офлайн-чтение в высоком качестве"
        AppLanguage.SPANISH -> "Lectura sin conexión en alta calidad"
        AppLanguage.ENGLISH -> "High-res offline reading"
    }

    fun buyButtonText(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "PRO без рекламы — $4.99"
        AppLanguage.SPANISH -> "PRO sin anuncios — $4.99"
        AppLanguage.ENGLISH -> "Go PRO Ad-Free — $4.99"
    }

    fun buySeriesButtonText(lang: AppLanguage, totalCount: Int = 100): String = when (lang) {
        AppLanguage.RUSSIAN -> "Открыть серию 11–$totalCount без рекламы — $0.99"
        AppLanguage.SPANISH -> "Abrir serie 11–$totalCount sin anuncios — $0.99"
        AppLanguage.ENGLISH -> "Unlock Series 11–$totalCount Ad-Free — $0.99"
    }

    fun watchAdButtonText(lang: AppLanguage, nextStart: Int, nextEnd: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Смотреть рекламу → открыть $nextStart–$nextEnd бесплатно"
        AppLanguage.SPANISH -> "Ver anuncio → abrir $nextStart–$nextEnd gratis"
        AppLanguage.ENGLISH -> "Watch Ad → Unlock $nextStart–$nextEnd Free"
    }

    fun restoreButtonText(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Восстановить покупку"
        AppLanguage.SPANISH -> "Restaurar compra"
        AppLanguage.ENGLISH -> "Restore Purchase"
    }

    fun purchaseSuccessMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "👑 PRO активирован! Все истории открыты без рекламы."
        AppLanguage.SPANISH -> "👑 ¡PRO activado! Todas las historias abiertas sin anuncios."
        AppLanguage.ENGLISH -> "👑 PRO unlocked! All stories available ad-free."
    }

    fun seriesPurchaseSuccessMessage(lang: AppLanguage, totalCount: Int = 100): String = when (lang) {
        AppLanguage.RUSSIAN -> "✨ Серия 11–$totalCount разблокирована без рекламы!"
        AppLanguage.SPANISH -> "✨ ¡Serie 11–$totalCount desbloqueada sin anuncios!"
        AppLanguage.ENGLISH -> "✨ Series 11–$totalCount unlocked ad-free!"
    }

    fun adBatchUnlockedMessage(lang: AppLanguage, maxUnlocked: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "🎁 Открыто до истории №$maxUnlocked бесплатно (режим с рекламой)!"
        AppLanguage.SPANISH -> "🎁 ¡Desbloqueado hasta la historia #$maxUnlocked gratis (con anuncios)!"
        AppLanguage.ENGLISH -> "🎁 Unlocked up to story #$maxUnlocked free (ad-supported)!"
    }

    fun adSlotTitle(lang: AppLanguage, isRewarded: Boolean): String = when (lang) {
        AppLanguage.RUSSIAN -> if (isRewarded) "Рекламная пауза • +5 историй" else "Спонсорская пауза"
        AppLanguage.SPANISH -> if (isRewarded) "Pausa publicitaria • +5 historias" else "Pausa de patrocinio"
        AppLanguage.ENGLISH -> if (isRewarded) "Ad Break • Unlock +5 Stories" else "Sponsored Break"
    }

    fun adSlotBody(lang: AppLanguage, isRewarded: Boolean): String = when (lang) {
        AppLanguage.RUSSIAN -> if (isRewarded) {
            "Подождите несколько секунд — сразу после показа откроются следующие 5 историй бесплатно."
        } else {
            "Вы читаете в бесплатном режиме с поддержкой рекламы. В PRO-версии рекламных пауз нет."
        }
        AppLanguage.SPANISH -> if (isRewarded) {
            "Espera unos segundos para desbloquear gratis las próximas 5 historias."
        } else {
            "Estás leyendo gratis con anuncios. En PRO no hay interrupciones."
        }
        AppLanguage.ENGLISH -> if (isRewarded) {
            "Please wait a moment — 5 more stories will unlock for free right after this break."
        } else {
            "You are reading in free ad-supported mode. Upgrade to PRO for zero interruptions."
        }
    }

    fun adSlotCountdown(lang: AppLanguage, secondsLeft: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Доступ откроется через $secondsLeft сек..."
        AppLanguage.SPANISH -> "Disponible en $secondsLeft s..."
        AppLanguage.ENGLISH -> "Ready in ${secondsLeft}s..."
    }

    fun adSlotContinueAction(lang: AppLanguage, isRewarded: Boolean): String = when (lang) {
        AppLanguage.RUSSIAN -> if (isRewarded) "Забрать 5 историй" else "Продолжить чтение"
        AppLanguage.SPANISH -> if (isRewarded) "Reclamar 5 historias" else "Seguir leyendo"
        AppLanguage.ENGLISH -> if (isRewarded) "Claim 5 Stories" else "Continue Reading"
    }

    fun panelStepLabel(lang: AppLanguage, panelNum: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Кадр $panelNum из 4"
        AppLanguage.SPANISH -> "Viñeta $panelNum de 4"
        AppLanguage.ENGLISH -> "Panel $panelNum of 4"
    }

    fun previousStory(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Пред."
        AppLanguage.SPANISH -> "Ant."
        AppLanguage.ENGLISH -> "Prev"
    }

    fun nextStory(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "След."
        AppLanguage.SPANISH -> "Sig."
        AppLanguage.ENGLISH -> "Next"
    }

    fun chooseStoryTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Выбор истории"
        AppLanguage.SPANISH -> "Elegir historia"
        AppLanguage.ENGLISH -> "Select Story"
    }

    fun storyCounter(lang: AppLanguage, current: Int, total: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "История $current из $total"
        AppLanguage.SPANISH -> "Historia $current de $total"
        AppLanguage.ENGLISH -> "Story $current of $total"
    }

    fun languageSwitchedMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.SPANISH -> "Idioma cambiado a Español (ES)"
        AppLanguage.ENGLISH -> "Language switched to English (EN)"
        AppLanguage.RUSSIAN -> "Язык переключен на Русский (RU)"
    }

    fun folderImportedMessage(lang: AppLanguage, title: String, pagesCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Папка «$title» успешно загружена: $pagesCount стр."
        AppLanguage.SPANISH -> "Carpeta «$title» importada: $pagesCount págs."
        AppLanguage.ENGLISH -> "Folder '$title' imported: $pagesCount pages"
    }

    fun folderEmptyMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "В выбранной папке не найдено подходящих изображений"
        AppLanguage.SPANISH -> "No se encontraron imágenes válidas en la carpeta"
        AppLanguage.ENGLISH -> "No supported images found in selected folder"
    }

    fun imagesImportedMessage(lang: AppLanguage, pagesCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Загружено изображений: $pagesCount"
        AppLanguage.SPANISH -> "Imágenes importadas: $pagesCount"
        AppLanguage.ENGLISH -> "Imported $pagesCount images"
    }

    fun imagesImportFailedMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось загрузить выбранные файлы"
        AppLanguage.SPANISH -> "No se pudieron importar los archivos"
        AppLanguage.ENGLISH -> "Failed to import selected files"
    }

    fun sequenceSavedMessage(lang: AppLanguage, pagesCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Порядок страниц сохранен ($pagesCount стр.)"
        AppLanguage.SPANISH -> "Orden de páginas guardado ($pagesCount págs.)"
        AppLanguage.ENGLISH -> "Page sequence saved ($pagesCount pages)"
    }

    fun comicDeletedMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Комикс удален"
        AppLanguage.SPANISH -> "Cómic eliminado"
        AppLanguage.ENGLISH -> "Comic deleted"
    }

    fun speechBubblesTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Речевые облачка"
        AppLanguage.SPANISH -> "Globos de texto"
        AppLanguage.ENGLISH -> "Speech bubbles"
    }

    fun importedComicFallbackTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Импортированный комикс"
        AppLanguage.SPANISH -> "Cómic importado"
        AppLanguage.ENGLISH -> "Imported Comic"
    }

    fun importedPageTitle(lang: AppLanguage, pageNumber: Int, fileName: String): String = when (lang) {
        AppLanguage.RUSSIAN -> "Стр. $pageNumber: $fileName"
        AppLanguage.SPANISH -> "Pág. $pageNumber: $fileName"
        AppLanguage.ENGLISH -> "Page $pageNumber: $fileName"
    }

    fun importedFolderSubtitle(lang: AppLanguage, pageCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Папка ($pageCount стр.)"
        AppLanguage.SPANISH -> "Carpeta ($pageCount págs.)"
        AppLanguage.ENGLISH -> "Folder ($pageCount pages)"
    }

    fun importedFolderDescription(lang: AppLanguage, folderName: String): String = when (lang) {
        AppLanguage.RUSSIAN -> "Импортировано из папки: $folderName"
        AppLanguage.SPANISH -> "Importado de la carpeta: $folderName"
        AppLanguage.ENGLISH -> "Imported from folder: $folderName"
    }

    fun newImportedComicTitle(lang: AppLanguage, pageCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Новый комикс ($pageCount стр.)"
        AppLanguage.SPANISH -> "Nuevo cómic ($pageCount págs.)"
        AppLanguage.ENGLISH -> "New Comic ($pageCount pages)"
    }

    fun customSelectionSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Пользовательский выбор файлов"
        AppLanguage.SPANISH -> "Selección de archivos"
        AppLanguage.ENGLISH -> "Custom File Selection"
    }

    fun customSelectionDescription(lang: AppLanguage, pageCount: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "Комикс сформирован из $pageCount выбранных изображений"
        AppLanguage.SPANISH -> "Cómic creado con $pageCount imágenes seleccionadas"
        AppLanguage.ENGLISH -> "Comic created from $pageCount selected images"
    }

    fun searchStoriesPlaceholder(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск притчи или номера (#42)..."
        AppLanguage.SPANISH -> "Buscar historia o número (#42)..."
        AppLanguage.ENGLISH -> "Search story or # (#42)..."
    }

    fun clearSearchTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить поиск"
        AppLanguage.SPANISH -> "Limpiar búsqueda"
        AppLanguage.ENGLISH -> "Clear search"
    }

    fun libraryFilterLabel(lang: AppLanguage, filter: LibraryFilter): String = when (filter) {
        LibraryFilter.ALL -> when (lang) {
            AppLanguage.RUSSIAN -> "Все"
            AppLanguage.SPANISH -> "Todas"
            AppLanguage.ENGLISH -> "All"
        }
        LibraryFilter.FREE -> when (lang) {
            AppLanguage.RUSSIAN -> "Бесплатные"
            AppLanguage.SPANISH -> "Gratis"
            AppLanguage.ENGLISH -> "Free"
        }
        LibraryFilter.UNLOCKED -> when (lang) {
            AppLanguage.RUSSIAN -> "Доступные"
            AppLanguage.SPANISH -> "Disponibles"
            AppLanguage.ENGLISH -> "Unlocked"
        }
        LibraryFilter.BAZAAR -> when (lang) {
            AppLanguage.RUSSIAN -> "Базар"
            AppLanguage.SPANISH -> "Bazar"
            AppLanguage.ENGLISH -> "Bazaar"
        }
        LibraryFilter.TEAHOUSE -> when (lang) {
            AppLanguage.RUSSIAN -> "Чайхана"
            AppLanguage.SPANISH -> "Tetería"
            AppLanguage.ENGLISH -> "Chaikhana"
        }
        LibraryFilter.NIGHT -> when (lang) {
            AppLanguage.RUSSIAN -> "Ночь"
            AppLanguage.SPANISH -> "Noche"
            AppLanguage.ENGLISH -> "Night"
        }
    }

    fun emptySearchTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Истории не найдены"
        AppLanguage.SPANISH -> "No se encontraron historias"
        AppLanguage.ENGLISH -> "No matching stories"
    }

    fun emptySearchSub(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Попробуйте другое слово или сбросьте фильтр"
        AppLanguage.SPANISH -> "Prueba otra palabra o restablece los filtros"
        AppLanguage.ENGLISH -> "Try another keyword or reset active filters"
    }

    fun resetFiltersAction(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Сбросить фильтры"
        AppLanguage.SPANISH -> "Restablecer filtros"
        AppLanguage.ENGLISH -> "Reset filters"
    }

    fun coverPageTitle(lang: AppLanguage, seq: Int, title: String): String = when (lang) {
        AppLanguage.RUSSIAN -> "Обложка • $seq. $title"
        AppLanguage.SPANISH -> "Portada • $seq. $title"
        AppLanguage.ENGLISH -> "Cover • $seq. $title"
    }

    fun parchmentEpisodeBadge(lang: AppLanguage, seq: Int): String = when (lang) {
        AppLanguage.RUSSIAN -> "ПРИТЧА №$seq • ХОДЖА НАСРЕДДИН"
        AppLanguage.SPANISH -> "RELATO #$seq • NASREDDIN HODJA"
        AppLanguage.ENGLISH -> "TALE #$seq • HODJA NASREDDIN"
    }

    fun parchmentButtonLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Текст на пергаменте"
        AppLanguage.SPANISH -> "Texto en pergamino"
        AppLanguage.ENGLISH -> "Story on Parchment"
    }

    fun parchmentShortLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Пергамент"
        AppLanguage.SPANISH -> "Pergamino"
        AppLanguage.ENGLISH -> "Parchment"
    }

    fun parchmentOpenTooltip(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Открыть историю на пергаменте"
        AppLanguage.SPANISH -> "Abrir historia en pergamino"
        AppLanguage.ENGLISH -> "Open story on parchment"
    }

    fun parchmentMoralHeader(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Мудрость притчи"
        AppLanguage.SPANISH -> "Moraleja de la historia"
        AppLanguage.ENGLISH -> "Moral of the Tale"
    }

    fun coverSwipeHint(lang: AppLanguage): String = when (lang) {
        AppLanguage.RUSSIAN -> "Листайте дальше, чтобы читать притчу"
        AppLanguage.SPANISH -> "Desliza para leer el cómic"
        AppLanguage.ENGLISH -> "Swipe to read the comic"
    }
}
