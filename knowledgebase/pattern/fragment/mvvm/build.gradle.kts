plugins {
    id("portfolio.android.application")
    // Compose renders every screen; Fragments and the Navigation component host them.
    id("portfolio.android.compose")
    alias(libs.plugins.kotlin.serialization) // type-safe @Serializable nav keys (FMVVM-N1)
}

android {
    namespace = "com.rsalanga.knowledgebase.fragment.mvvm"
    defaultConfig {
        applicationId = "com.rsalanga.knowledgebase.fragment.mvvm"
        versionCode = 1
        versionName = "1.0"
    }
}

// Not `portfolio.android.feature`: that plugin brings Navigation 3. This module's stack is
// Fragment + Navigation component, which only this module needs, so it is declared here.
dependencies {
    implementation(libs.androidx.fragment.compose) // Fragment.content { } (FMVVM-F1)
    implementation(libs.androidx.navigation.fragment) // NavHostFragment, Kotlin DSL graph
    implementation(libs.androidx.lifecycle.viewmodel.compose) // SavedStateHandle.saveable
    implementation(libs.androidx.lifecycle.runtime.compose) // collectAsStateWithLifecycle
    implementation(libs.androidx.paging.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android) // Fragment.viewModel { }, activityViewModel { }

    testImplementation(libs.kotlinx.coroutines.test)

    // FMVVM-T5: Fragment + NavController tests.
    debugImplementation(libs.androidx.fragment.testing.manifest)
    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.navigation.testing)
}
