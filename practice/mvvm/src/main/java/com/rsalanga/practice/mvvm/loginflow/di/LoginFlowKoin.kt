package com.rsalanga.practice.mvvm.loginflow.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

/**
 * The login flow's own Koin container, separate from any other practice app in this module.
 *
 * It is an isolated [KoinApplication] (not `startKoin`), so its definitions never leak into the
 * global context or into another practice app. It lives at process level, so singletons survive
 * configuration changes the same way ViewModels do.
 */
internal object LoginFlowKoin {

    @Volatile
    private var instance: KoinApplication? = null

    fun get(context: Context): KoinApplication = instance ?: synchronized(this) {
        instance ?: koinApplication {
            androidContext(context.applicationContext)
            modules(loginFlowModule)
        }.also { instance = it }
    }
}
