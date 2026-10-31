import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                val fx = "21.0.4"
                implementation("org.openjfx:javafx-base:$fx:win")
                implementation("org.openjfx:javafx-graphics:$fx:win")
                implementation("org.openjfx:javafx-media:$fx:win")
                // ID3 / audio tag reading on desktop
                implementation("net.jthink:jaudiotagger:3.0.1")
            }
        }
    }
}

android {
    namespace = "com.example.musicapp.shared"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
