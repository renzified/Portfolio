package com.rsalanga.portfolio

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))
            configureCodeQuality()

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = AndroidConfig.TARGET_SDK
                buildTypes.named("release") {
                    optimization.enable = false
                }
            }

            dependencies {
                // Every app can opt into `installSplashScreen()` and a `Theme.SplashScreen` starting theme.
                add("implementation", libs.library("androidx-core-splashscreen"))
            }
        }
    }
}
