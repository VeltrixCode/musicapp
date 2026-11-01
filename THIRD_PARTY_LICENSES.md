# Сторонние компоненты и лицензии (Third-Party Licenses)

Приложение **JVXMusic** использует следующие библиотеки и компоненты с открытым исходным кодом:

---

### 1. vlcj
* **Лицензия:** GNU General Public License v3.0 or later (GPL-3.0-or-later)
* **Автор / Правообладатель:** Caprica Software Limited
* **Назначение:** Java-биндинги к библиотеке libVLC для воспроизведения мультимедиа на Desktop.
* **Исходный код:** [https://github.com/caprica/vlcj](https://github.com/caprica/vlcj)

### 2. VideoLAN VLC Media Player / libVLC
* **Версия в дистрибутиве:** 3.0.24 (Windows x86_64)
* **Лицензия:** GNU Lesser General Public License v2.1 or later (LGPL-2.1-or-later) / GNU General Public License v2.0 or later (GPL-2.0-or-later)
* **Автор / Правообладатель:** VideoLAN non-profit organization & VideoLAN Authors
* **Назначение:** Высокопроизводительный нативный движок декодирования и вывода аудио (`libvlc.dll`, `libvlccore.dll`, аудио-плагины и алгоритм сохранения высоты тона `scaletempo`).
* **Исходный код:** [https://www.videolan.org/vlc/download-sources.html](https://www.videolan.org/vlc/download-sources.html) и [https://code.videolan.org/videolan/vlc](https://code.videolan.org/videolan/vlc)
* **Примечание:** Установщик JVXMusic для Windows содержит автономные бинарные модули libVLC в соответствии с условиями лицензий LGPL/GPL.

### 3. OpenJFX (JavaFX)
* **Лицензия:** GNU General Public License v2.0 with the Classpath Exception (GPL-2.0-with-classpath-exception)
* **Автор / Правообладатель:** Oracle Corporation and/or its affiliates
* **Назначение:** Запасной (fallback) движок воспроизведения звука и системные графические утилиты на Desktop.
* **Исходный код:** [https://github.com/openjdk/jfx](https://github.com/openjdk/jfx)

### 4. JNA (Java Native Access) & JNA Platform
* **Лицензия:** GNU Lesser General Public License v2.1 or later (LGPL-2.1-or-later) / Apache License 2.0
* **Автор / Правообладатель:** Timothy Wall and JNA contributors
* **Назначение:** Вызов нативных C-библиотек (libVLC и Windows DPAPI / Crypt32).
* **Исходный код:** [https://github.com/java-native-access/jna](https://github.com/java-native-access/jna)

### 5. jaudiotagger
* **Лицензия:** GNU Lesser General Public License v2.1 or later (LGPL-2.1-or-later)
* **Автор / Правообладатель:** Paul Taylor
* **Назначение:** Чтение и парсинг аудиотегов ID3v1/ID3v2, FLAC, Vorbis Comment, MP4/M4A на Desktop.
* **Исходный код:** [https://www.jthink.net/jaudiotagger/](https://www.jthink.net/jaudiotagger/) и [https://bitbucket.org/ijabz/jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger)

### 6. Jetpack Media3 ExoPlayer
* **Лицензия:** Apache License, Version 2.0
* **Автор / Правообладатель:** The Android Open Source Project / Google LLC
* **Назначение:** Высокопроизводительный медиаплеер для Android с поддержкой встроенного DSP-процессора `SonicAudioProcessor`.
* **Исходный код:** [https://github.com/androidx/media](https://github.com/androidx/media)

### 7. Compose Multiplatform & Kotlin Multiplatform
* **Лицензия:** Apache License, Version 2.0
* **Автор / Правообладатель:** JetBrains s.r.o. and Google LLC
* **Назначение:** Декларативный UI-фреймворк и кроссплатформенная рантайм-среда.
* **Исходный код:** [https://github.com/JetBrains/compose-multiplatform](https://github.com/JetBrains/compose-multiplatform) и [https://github.com/JetBrains/kotlin](https://github.com/JetBrains/kotlin)

### 8. Ktor HTTP Client & Server
* **Лицензия:** Apache License, Version 2.0
* **Автор / Правообладатель:** JetBrains s.r.o.
* **Назначение:** Локальный потоковый HTTP-прокси для безопасного стриминга.
* **Исходный код:** [https://github.com/ktorio/ktor](https://github.com/ktorio/ktor)
