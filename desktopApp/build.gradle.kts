import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

kotlin { jvmToolchain(17) }

compose.desktop {
    application {
        mainClass = "com.example.musicapp.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "MusicApp"
            packageVersion = "1.0.0"
            windows {
                console = false
            }
        }
    }
}
