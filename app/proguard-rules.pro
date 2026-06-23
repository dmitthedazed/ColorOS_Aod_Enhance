# Xposed entry class
-keep class com.op.aod.enhance.HookEntryXposed { *; }

# YukiHookAPI core (must be kept for reflective calls)
-keep class com.highcapable.yukihookapi.** { *; }

# KavaRef reflection utility (must be kept for method lookup)
-keep class com.highcapable.kavaref.** { *; }

# Miuix UI component library (must be kept for Compose)
-keep class top.yukonga.miuix.** { *; }

# Data classes (prevent obfuscation from breaking ContentProvider column names)
-keep class com.op.aod.enhance.data.** { *; }

# Hook-side config classes (needed for reflection and serialization)
-keep class com.op.aod.enhance.hook.AodConfig { *; }
-keep class com.op.aod.enhance.hook.AodConfigReader { *; }

# Suppress warnings
-dontwarn com.highcapable.**
-dontwarn top.yukonga.miuix.**
