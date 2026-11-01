import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.musicapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.musicapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val keystorePropertiesFile = rootProject.file("local.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    signingConfigs {
        create("release") {
            val keyStorePath = keystoreProperties.getProperty("jvx.signing.storeFile")
                ?: System.getenv("JVX_RELEASE_STORE_FILE")
            val keyStorePassword = keystoreProperties.getProperty("jvx.signing.storePassword")
                ?: System.getenv("JVX_RELEASE_STORE_PASSWORD")
            val keyAliasVal = keystoreProperties.getProperty("jvx.signing.keyAlias")
                ?: System.getenv("JVX_RELEASE_KEY_ALIAS")
            val keyPasswordVal = keystoreProperties.getProperty("jvx.signing.keyPassword")
                ?: System.getenv("JVX_RELEASE_KEY_PASSWORD")

            if (keyStorePath != null) {
                val f = file(keyStorePath)
                if (f.exists()) {
                    storeFile = f
                    storePassword = keyStorePassword
                    keyAlias = keyAliasVal
                    keyPassword = keyPasswordVal
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
