plugins {
    id("portfolio.android.application")
    id("portfolio.android.compose")
    id("portfolio.android.feature")
}

android {
    namespace = "com.rsalanga.practice.mvvm"
    defaultConfig {
        applicationId = "com.rsalanga.practice.mvvm"
        versionCode = 1
        versionName = "1.0"
    }
}
