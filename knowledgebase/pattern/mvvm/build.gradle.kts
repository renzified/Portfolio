plugins {
    id("portfolio.android.application")
    id("portfolio.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rsalanga.knowledgebase.mvvm"
    defaultConfig {
        applicationId = "com.rsalanga.knowledgebase.mvvm"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.paging.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.kotlinx.coroutines.test)
}
