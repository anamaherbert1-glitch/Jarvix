plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "com.example.jarvis"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.example.jarvis"
  minSdk = 26
  targetSdk = 35
  versionCode = 2
  versionName = "0.2.0"
 }
}
dependencies {
 implementation("androidx.activity:activity-ktx:1.10.0")
 implementation("org.nanohttpd:nanohttpd:2.3.1")
}
