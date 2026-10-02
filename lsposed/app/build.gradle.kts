plugins {
    alias(libs.plugins.android.application)
}

val releaseStoreFile = providers.environmentVariable("TPB_KEYSTORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("TPB_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("TPB_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("TPB_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.vanquzie.tikprofilebanner"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.vanquzie.tikprofilebanner"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.2"

    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable += setOf(
            "AndroidGradlePluginVersion",
            "BlockedPrivateApi",
            "DataExtractionRules",
            "DiscouragedApi",
            "DiscouragedPrivateApi",
            "GradleDependency",
            "MonochromeLauncherIcon",
            "OldTargetApi",
            "PrivateApi",
            "SdCardPath",
            "SoonBlockedPrivateApi",
            "UseKtx",
            "ObsoleteSdkInt"
        )
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE",
                "META-INF/NOTICE.md",
            )
        }
    }
}

dependencies {
    implementation(libs.libxposed.service)
    testImplementation(libs.junit)
    compileOnly(libs.libxposed.api)
}
