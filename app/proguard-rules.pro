# Room
-keep class com.drift.sleep.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Compose
-dontwarn androidx.compose.**

# Keep SleepRecord for Room
-keepclassmembers class com.drift.sleep.data.SleepRecord { *; }
