plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.timecapsule.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.timecapsule.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "2.8"
    }

    // Una sola llave para TODOS los APK (los de este PC y los de GitHub):
    // así "Continuar con Google" funciona para cualquier persona que instale la app.
    // Su huella (SHA-1) está registrada en Google Cloud. Ver firma/LEEME.md
    signingConfigs {
        create("timecapsule") {
            storeFile = file("firma/timecapsule.keystore")
            storePassword = "android"
            keyAlias = "timecapsule"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("timecapsule") }
        release {
            signingConfig = signingConfigs.getByName("timecapsule")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    // Pruebas automáticas en la computadora (sin celular): las clases de Android devuelven valores vacíos
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,DEPENDENCIES}" }
    }
}

dependencies {
    // Compose (interfaz)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Notificaciones programadas y sincronización
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Ubicación y geocercas (notificación al volver a un lugar)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Fotos, videos y mapas
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Códigos QR para invitar (solo genera la imagen; leerlo lo hace la cámara del celular)
    implementation("com.google.zxing:core:3.5.3")

    // Bloqueo con huella o PIN del celular
    implementation("androidx.biometric:biometric:1.1.0")

    // Nube: Supabase (cuentas, base de datos, archivos, IA)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    // Continuar con Google (inicio de sesión nativo de Android)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Pruebas automáticas
    testImplementation("junit:junit:4.13.2")
}
