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
            packageName = "MusicApp"
            packageVersion = "1.0.0"
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.xml",
                "java.prefs",
                "java.management",
                "jdk.unsupported",
                "jdk.crypto.ec"
            )
            windows {
                console = false
            }
        }
    }
}
