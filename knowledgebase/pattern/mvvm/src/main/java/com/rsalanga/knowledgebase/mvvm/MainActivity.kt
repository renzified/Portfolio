package com.rsalanga.knowledgebase.mvvm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rsalanga.knowledgebase.mvvm.core.ui.MvvmTheme
import com.rsalanga.knowledgebase.mvvm.shell.AppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MvvmTheme {
                AppRoot()
            }
        }
    }
}
