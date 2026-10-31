# MusicApp 🎵

Кроссплатформенный музыкальный плеер в стиле **Spotify** на базе **Kotlin Multiplatform (Compose Multiplatform)** для **Desktop (Windows)** и **Android**.

---

## ✨ Возможности
* 🎧 **Интерфейс в стиле Spotify (Dark Theme)**: боковое меню, подборки, градиентные обложки, аккуратная таблица треков и нижний плеер (Now Playing Bar).
* 🔊 **Управление звуком**: плавный слайдер громкости (0–100%) и кнопка быстрого отключения звука (Mute/Unmute).
* 🔀 **Режимы воспроизведения**: случайный порядок (Shuffle) и зацикливание (Repeat Off / All / One).
* 🔍 **Удобный поиск**: мгновенная фильтрация по названию и автору прямо в шапке.
* 📁 **Плейлисты и библиотека**: создание и удаление плейлистов, добавление и удаление треков, локальное сохранение в JSON.
* ➕ **Импорт треков**: поддержка добавления локальных файлов (`.mp3`, `.wav`, `.m4a`, `.aac`) с автоматическим считыванием ID3-тегов.

---

## 🛠️ Стек технологий
* **Kotlin Multiplatform 2.1.0**
* **Compose Multiplatform 1.7.3**
* **Material 3**
* **JavaFX Media & jaudiotagger** (Desktop)
* **Android MediaPlayer & MediaMetadataRetriever** (Android)
* **Kotlinx Serialization & Coroutines**

---

## 🚀 Сборка и запуск

### Desktop (Windows)
```bash
# Запуск приложения
./gradlew :desktopApp:run

# Сборка автономного дистрибутива
./gradlew :desktopApp:createDistributable
```

### Android
```bash
# Сборка debug APK
./gradlew :androidApp:assembleDebug
```
Готовый APK появится в `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
