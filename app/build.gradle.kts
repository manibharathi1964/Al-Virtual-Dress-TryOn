plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mj.virtualdress"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mj.virtualdress"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        val vtoApiBaseUrl = providers.gradleProperty("vtoApiBaseUrl")
            .orElse("http://10.0.2.2:8080")
            .get()
        buildConfigField("String", "VTO_API_BASE_URL", "\"$vtoApiBaseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta2")
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta4")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
