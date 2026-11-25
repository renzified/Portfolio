package com.rsalanga.practice.mvvm.loginflow

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rsalanga.practice.mvvm.loginflow.di.LoginFlowKoin
import com.rsalanga.practice.mvvm.loginflow.identifier.IdentifierRoute
import org.koin.compose.KoinIsolatedContext

class LoginFlowActivity : ComponentActivity() {

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
            // Everything below resolves from the login flow's own container, not a global one
            KoinIsolatedContext(context = LoginFlowKoin.get(this)) {
                MaterialTheme {
                    IdentifierRoute()
                }
            }
        }
    }
}
