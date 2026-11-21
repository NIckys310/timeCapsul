# =========================================================
# TIME CAPSULE · Reglas de R8 (versión de producción)
# R8 quita el código que no se usa y acorta los nombres.
# Estas reglas protegen lo que se usa "por nombre" en tiempo de ejecución.
# =========================================================

# Para que los errores de la app digan en qué línea fallaron
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# kotlinx.serialization: los modelos que vienen de Supabase y de la IA
-keepclassmembers @kotlinx.serialization.Serializable class com.timecapsule.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
    <fields>;
}
-keepclasseswithmembers class com.timecapsule.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.timecapsule.app.**$$serializer { *; }
-keep class com.timecapsule.app.data.** { *; }
-dontnote kotlinx.serialization.**

# Supabase y Ktor (cliente HTTP)
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# Continuar con Google (Credential Manager)
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Mapas (osmdroid)
-dontwarn org.osmdroid.**
