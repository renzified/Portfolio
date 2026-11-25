package com.rsalanga.knowledgebase.compose.mvvm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.MvvmTheme
import com.rsalanga.knowledgebase.compose.mvvm.shell.AppRoot

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
