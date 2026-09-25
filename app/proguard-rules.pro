# StockPro Production ProGuard / R8 Rules
# docs/phases.md § Phase 6: ProGuard keep rules for Room, Coroutines, Firebase, WorkManager, Domain models

# -----------------------------------------------------------------------------
# Kotlin & General Reflection
# -----------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# -----------------------------------------------------------------------------
# AndroidX & Compose
# -----------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------------
# Room Persistence Library
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.company.stockpro.core.database.StockProDatabase_Impl {
    public <init>();
    *;
}
-keep class com.company.stockpro.core.database.dao.** { *; }
-keep class com.company.stockpro.core.database.entity.** { *; }
-keep interface com.company.stockpro.core.database.dao.** { *; }

# -----------------------------------------------------------------------------
# Domain Models & Value Classes
# -----------------------------------------------------------------------------
-keep class com.company.stockpro.core.domain.model.** { *; }
-keep class com.company.stockpro.core.domain.util.** { *; }

# -----------------------------------------------------------------------------
# WorkManager
# -----------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.company.stockpro.core.data.worker.OutboxWorker { *; }

# -----------------------------------------------------------------------------
# Firebase (Auth, Firestore, Cloud Functions, Messaging)
# -----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# -----------------------------------------------------------------------------
# Kotlinx Coroutines
# -----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
