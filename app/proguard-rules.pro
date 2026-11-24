# Настройки хранятся через kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class app.orubus.island.data.**$$serializer { *; }
-keepclassmembers class app.orubus.island.data.** {
    *** Companion;
}
-keepclasseswithmembers class app.orubus.island.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Conscrypt: провайдер и экспорт ключевого материала TLS (беспроводная отладка)
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**

# BouncyCastle: сборка самоподписанного сертификата для TLS-спарки
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# SPAKE2 (spake2-java, LGPL-3.0): Java-реализация протокола спарки
-keep class io.github.muntashirakon.crypto.** { *; }
