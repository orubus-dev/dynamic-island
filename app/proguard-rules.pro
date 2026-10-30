# Настройки хранятся через kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class app.orubus.island.data.**$$serializer { *; }
-keepclassmembers class app.orubus.island.data.** {
    *** Companion;
}
-keepclasseswithmembers class app.orubus.island.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
