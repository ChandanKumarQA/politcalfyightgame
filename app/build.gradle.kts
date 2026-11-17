plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.comedyarena.politicalfight"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.comedyarena.politicalfight"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.4"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:23.3.0")
}
