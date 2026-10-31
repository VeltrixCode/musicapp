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
            }
        }
    }
}
