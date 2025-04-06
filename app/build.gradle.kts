plugins {
    alias(libs.plugins.android.application) apply true
    alias(libs.plugins.kotlin.android) apply true
    alias(libs.plugins.google.gms.google.services) apply true
}

android {
    namespace = "com.example.aplikasipertama"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aplikasipertama"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.google.firebase:firebase-auth:21.1.0")
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore:25.1.3")
        implementation ("androidx.core:core-ktx:1.12.0")
        implementation ("androidx.appcompat:appcompat:1.6.1")

    // Google Ads SDK
}

apply(plugin = "com.google.gms.google-services")
