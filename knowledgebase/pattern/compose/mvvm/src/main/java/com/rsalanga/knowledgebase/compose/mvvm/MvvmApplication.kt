package com.rsalanga.knowledgebase.compose.mvvm

import android.app.Application
import com.rsalanga.knowledgebase.compose.mvvm.di.dataModule
import com.rsalanga.knowledgebase.compose.mvvm.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MvvmApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MvvmApplication)
            modules(dataModule, viewModelModule)
        }
    }
}
