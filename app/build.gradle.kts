plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.felipe.habitacioninteligente"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.felipe.habitacioninteligente"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Cifrado de datos sensibles (ISO 27400: proteccion de credenciales)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Firebase: Authentication + Cloud Firestore
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Cliente MQTT sobre WiFi (TLS)
    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")
}
