package com.rsalanga.practice.mvvm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsalanga.practice.mvvm.loginflow.LoginFlowActivity
import kotlin.reflect.KClass

class MainActivity : ComponentActivity() {

    companion object Util {
        fun launchPractice(context: Context, activity: KClass<out Activity>) {
            val intent = Intent(context, activity.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            )
        )

        // SystemBarStyle.auto leaves contrast enforcement on, so 3-button nav still gets a system scrim
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Contents()
            }
        }
    }

    @Preview
    @Composable
    private fun Contents() {
        val contentPadding = WindowInsets.safeContent.asPaddingValues()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            item {
                val context = LocalContext.current
                Item(label = "Login flow") {
                    launchPractice(
                        context = context,
                        activity = LoginFlowActivity::class
                    )
                }
            }
        }
    }

    @Composable
    private fun Item(label: String, onClick: () -> Unit) {
        Surface(onClick = onClick) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(text = label, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}
