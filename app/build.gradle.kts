plugins {
    id("com.android.application")
}

android {
    namespace = "jp.isshokai"
    compileSdk = 36

    defaultConfig {
        applicationId = "jp.isshokai"
        minSdk = 23
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
}
