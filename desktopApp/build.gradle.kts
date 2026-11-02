import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URL
import java.net.HttpURLConnection
import java.security.MessageDigest

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

kotlin { jvmToolchain(17) }

compose.desktop {
    application {
        mainClass = "com.example.musicapp.desktop.MainKt"
        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            licenseFile.set(rootProject.file("LICENSE"))
            packageName = "JVXMusic"
            packageVersion = "1.0.2"
            description = "JVXMusic Cloud Player"
            copyright = "© 2026 JVXMusic"
            vendor = "JVXMusic"
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.xml",
                "java.prefs",
                "java.management",
                "java.instrument",
                "java.naming",
                "java.net.http",
                "java.sql",
                "java.security.jgss",
                "jdk.unsupported",
                "jdk.crypto.ec",
                "jdk.jfr"
            )
            windows {
                console = false
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    val testDataDir = layout.buildDirectory.dir("test-jvx-data/$name").get().asFile
    testDataDir.mkdirs()
    systemProperty("jvx.data.dir", testDataDir.absolutePath)
    systemProperty("jvx.test.isolation", "true")
}

val copyLicensesToAppResources by tasks.registering {
    val commonResDir = project.layout.projectDirectory.dir("resources/common").asFile
    outputs.files(File(commonResDir, "LICENSE"), File(commonResDir, "THIRD_PARTY_LICENSES.md"))
    doLast {
        commonResDir.mkdirs()
        rootProject.file("LICENSE").copyTo(File(commonResDir, "LICENSE"), overwrite = true)
        rootProject.file("THIRD_PARTY_LICENSES.md").copyTo(File(commonResDir, "THIRD_PARTY_LICENSES.md"), overwrite = true)
    }
}

val prepareVlcNatives by tasks.registering {
    val legacyVlcDir = project.layout.projectDirectory.dir("resources/vlc").asFile
    val vlcDir = project.layout.projectDirectory.dir("resources/windows/vlc").asFile
    val libvlcDll = File(vlcDir, "libvlc.dll")
    val pluginsDat = File(vlcDir, "plugins/plugins.dat")
    outputs.files(libvlcDll, pluginsDat)

    doLast {
        // Удаляем устаревшую папку resources/vlc если она осталась
        if (legacyVlcDir.exists()) {
            val legacySize = legacyVlcDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            logger.lifecycle("Удаление устаревшей папки ${legacyVlcDir.absolutePath} (${legacySize / (1024 * 1024)} МБ)...")
            legacyVlcDir.deleteRecursively()
        }

        if (!libvlcDll.exists() || !pluginsDat.exists()) {
            val vlcVersion = project.findProperty("vlc.version")?.toString() ?: "3.0.21"
            val vlcUrl = project.findProperty("vlc.win64.url")?.toString()
                ?: "https://download.videolan.org/pub/videolan/vlc/$vlcVersion/win64/vlc-$vlcVersion-win64.zip"
            val expectedSha256 = project.findProperty("vlc.win64.sha256")?.toString()
                ?: "a0b7ec02b50adf6417eed014fb8df50af39690505a4225b85b3dc2ed17d14843"

            val downloadDir = layout.buildDirectory.dir("vlc-download").get().asFile
            downloadDir.mkdirs()
            val archiveFile = File(downloadDir, "vlc-$vlcVersion-win64.zip")

            if (!archiveFile.exists() || archiveFile.length() < 70000000L) {
                logger.lifecycle("Загрузка официального архива VLC $vlcVersion с $vlcUrl...")
                val connection = URL(vlcUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 JVXMusic-Build")
                connection.connect()
                connection.inputStream.use { input ->
                    archiveFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Проверка SHA-256 хеша
            logger.lifecycle("Проверка SHA-256 хеша для ${archiveFile.name}...")
            val digest = MessageDigest.getInstance("SHA-256")
            val computedHash = archiveFile.inputStream().use { input ->
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { b -> String.format("%02x", b) }
            }

            if (!computedHash.equals(expectedSha256, ignoreCase = true)) {
                archiveFile.delete()
                throw GradleException("Контрольная сумма SHA-256 для архива VLC не совпала!\nОжидалось: $expectedSha256\nПолучено:   $computedHash")
            }
            logger.lifecycle("Контрольная сумма SHA-256 подтверждена: $computedHash")

            // Распаковка нужных файлов
            logger.lifecycle("Распаковка нативных библиотек VLC...")
            val extractDir = File(downloadDir, "extracted")
            extractDir.deleteRecursively()
            extractDir.mkdirs()

            project.copy {
                from(project.zipTree(archiveFile))
                into(extractDir)
            }

            val rootVlcDir = extractDir.listFiles()?.firstOrNull { it.isDirectory && it.name.startsWith("vlc-") } ?: extractDir
            val rawExtractedSize = rootVlcDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            logger.lifecycle("Размер распакованного дистрибутива VLC до оптимизации: ${rawExtractedSize / (1024 * 1024)} МБ")

            vlcDir.mkdirs()
            File(rootVlcDir, "libvlc.dll").copyTo(File(vlcDir, "libvlc.dll"), overwrite = true)
            File(rootVlcDir, "libvlccore.dll").copyTo(File(vlcDir, "libvlccore.dll"), overwrite = true)

            val pluginsDest = File(vlcDir, "plugins")
            pluginsDest.deleteRecursively()
            File(rootVlcDir, "plugins").copyRecursively(pluginsDest, overwrite = true)

            // Удаляем неиспользуемые видео, 3D и GUI модули для оптимизации размера
            val unneeded = listOf(
                "gui", "video_output", "video_filter", "video_chroma", "video_splitter",
                "d3d11", "d3d9", "spu", "text_renderer", "lua", "stream_out",
                "services_discovery", "visualization", "control", "access_output", "mux"
            )
            for (dirName in unneeded) {
                File(pluginsDest, dirName).deleteRecursively()
            }

            val cacheGen = File(rootVlcDir, "vlc-cache-gen.exe")
            if (cacheGen.exists()) {
                logger.lifecycle("Генерация оптимизированного кэша plugins.dat...")
                project.exec {
                    commandLine(cacheGen.absolutePath, pluginsDest.absolutePath)
                }
            }

            val optimizedSize = vlcDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            logger.lifecycle("Размер оптимизированного комплекта VLC в desktopApp/resources/windows/vlc: ${optimizedSize / (1024 * 1024)} МБ")

            extractDir.deleteRecursively()
            logger.lifecycle("Нативные библиотеки VLC успешно подготовлены в ${vlcDir.absolutePath}.")
        }
    }
}

tasks.matching { it.name in listOf("prepareAppResources", "createDistributable", "packageMsi", "packageExe") }.configureEach {
    dependsOn(prepareVlcNatives)
    dependsOn(copyLicensesToAppResources)
}

tasks.matching { it.name == "createDistributable" }.configureEach {
    doLast {
        val appRoot = layout.buildDirectory.dir("compose/binaries/main/app/JVXMusic").get().asFile
        if (appRoot.exists()) {
            rootProject.file("LICENSE").copyTo(File(appRoot, "LICENSE"), overwrite = true)
            rootProject.file("THIRD_PARTY_LICENSES.md").copyTo(File(appRoot, "THIRD_PARTY_LICENSES.md"), overwrite = true)
        }
    }
}

