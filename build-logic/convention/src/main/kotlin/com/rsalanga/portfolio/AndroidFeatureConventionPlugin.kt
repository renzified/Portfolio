package com.rsalanga.portfolio

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * The standard screen stack: Navigation 3 with serializable keys and adaptive
 * list-detail scenes, Paging, Koin, and the lifecycle/ViewModel Compose
 * integrations. Apply on top of `portfolio.android.application` or
 * `portfolio.android.library`; it applies `portfolio.android.compose` itself.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("portfolio.android.compose")
            pluginManager.apply(libs.pluginId("kotlin-serialization"))

            dependencies {
                add("implementation", libs.library("androidx-navigation3-runtime"))
                add("implementation", libs.library("androidx-navigation3-ui"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-navigation3"))
                add("implementation", libs.library("androidx-compose-material3-adaptive-navigation3"))
                add("implementation", libs.library("androidx-paging-compose"))

                add("implementation", platform(libs.library("koin-bom")))
                add("implementation", libs.library("koin-android"))
                add("implementation", libs.library("koin-androidx-compose"))

                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
