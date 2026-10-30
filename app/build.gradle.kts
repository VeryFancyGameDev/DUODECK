plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.duodeck.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duodeck.launcher"
        minSdk = 26
        targetSdk = 35
        // CI passes the run number so every build is newer than the last (needed for in-app updates)
        versionCode = (project.findProperty("duodeckBuild") as String?)?.toIntOrNull() ?: 1
        versionName = "3.1"
    }

    signingConfigs {
        // A fixed key (kept in the repo) so each new build installs over the old one without uninstalling.
        create("duodeck") {
            storeFile = rootProject.file("keystore/duodeck.jks")
            storePassword = "duodeck-open"
            keyAlias = "duodeck"
            keyPassword = "duodeck-open"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("duodeck")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
