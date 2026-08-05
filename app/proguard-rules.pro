# ProGuard Rules for Lootra and Lootra Super Admin

# Preserve line numbers and source files for useful crash reporting
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep all data models for Firebase Firestore, Room, and Moshi deserialization
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Room Database DAOs and Entities
-keep class com.example.data.dao.** { *; }
-keepclassmembers class com.example.data.dao.** { *; }
-keep class com.example.data.database.** { *; }
-keepclassmembers class com.example.data.database.** { *; }

# Keep Firebase Firestore & Auth classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Moshi models and adapters
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep Retrofit interfaces and models
-keep class com.example.data.network.** { *; }
-dontwarn retrofit2.**

# Keep Media3 ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Cloudinary classes
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# Keep Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Google Play Services & AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
