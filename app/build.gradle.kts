plugins {
 alias(libs.plugins.android.application)
 alias(libs.plugins.kotlin.android)
 alias(libs.plugins.kotlin.compose)
 alias(libs.plugins.kotlin.serialization)
}
android {
 namespace = "com.jpbott.bible"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.jpbott.bible"
  minSdk = 24
  targetSdk = 36
  versionCode = 1
  versionName = "1.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
 implementation(platform(libs.compose.bom))
 implementation(libs.compose.ui)
 implementation(libs.compose.material3)
 implementation(libs.activity.compose)
 implementation(libs.lifecycle.viewmodel)
 implementation(libs.lifecycle.runtime.compose)
 implementation(libs.datastore)
 implementation(libs.serialization.json)
 implementation(libs.okhttp)
 implementation(libs.retrofit)
 testImplementation(libs.junit)
 testImplementation(libs.coroutines.test)
 testImplementation(libs.mockwebserver)
 androidTestImplementation(platform(libs.compose.bom))
 androidTestImplementation(libs.compose.test)
 androidTestImplementation(libs.androidx.junit)
 debugImplementation(libs.compose.test.manifest)
}
