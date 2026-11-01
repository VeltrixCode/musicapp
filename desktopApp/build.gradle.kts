import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
            packageName = "JVXMusic"
            packageVersion = "1.0.0"
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

val prepareVlcNatives by tasks.registering {
    val vlcDir = project.layout.projectDirectory.dir("resources/vlc").asFile
    val libvlcDll = File(vlcDir, "libvlc.dll")
    outputs.file(libvlcDll)
    doLast {
        if (!libvlcDll.exists()) {
            val systemVlc = File("C:/Program Files/VideoLAN/VLC")
            if (systemVlc.exists() && File(systemVlc, "libvlc.dll").exists()) {
                logger.lifecycle("Подготовка нативных библиотек VLC из системного каталога...")
                vlcDir.mkdirs()
                File(systemVlc, "libvlc.dll").copyTo(File(vlcDir, "libvlc.dll"), overwrite = true)
                File(systemVlc, "libvlccore.dll").copyTo(File(vlcDir, "libvlccore.dll"), overwrite = true)
                val pluginsSrc = File(systemVlc, "plugins")
                val pluginsDest = File(vlcDir, "plugins")
                pluginsSrc.copyRecursively(pluginsDest, overwrite = true)

                // Удаляем неиспользуемые видео и GUI модули для оптимизации размера
                val unneeded = listOf("gui", "video_output", "video_filter", "video_chroma", "video_splitter", "d3d11", "d3d9", "spu", "text_renderer", "lua", "stream_out", "services_discovery", "visualization", "control", "access_output", "mux")
                for (dirName in unneeded) {
                    File(pluginsDest, dirName).deleteRecursively()
                }

                val cacheGen = File(systemVlc, "vlc-cache-gen.exe")
                if (cacheGen.exists()) {
                    project.exec {
                        commandLine(cacheGen.absolutePath, pluginsDest.absolutePath)
                    }
                }
            } else {
                logger.warn("Внимание: Нативные библиотеки VLC не найдены в $systemVlc. Приложение при запуске использует откат на JavaFX.")
            }
        }
    }
}

tasks.matching { it.name in listOf("prepareAppResources", "createDistributable", "packageMsi", "packageExe") }.configureEach {
    dependsOn(prepareVlcNatives)
}

