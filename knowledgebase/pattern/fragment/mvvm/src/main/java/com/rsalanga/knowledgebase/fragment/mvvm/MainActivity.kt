package com.rsalanga.knowledgebase.fragment.mvvm

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.fragment.NavHostFragment
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.MvvmTheme
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.Session
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.SignInKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.buildAppGraph
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.sessionRedirect
import com.rsalanga.knowledgebase.fragment.mvvm.shell.AppMessageHost
import com.rsalanga.knowledgebase.fragment.mvvm.shell.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * FMVVM-F4: the Activity is a thin shell. It hosts the NavHostFragment, sets the graph, and does
 * the two app-wide jobs that belong to the owner of the back stack — nothing screen-specific.
 *
 * - FMVVM-N7: it observes session state and rewrites the back stack (screens never listen for
 *   "logged out").
 * - FMVVM-N6: it shows the app-level message queue.
 * - FMVVM-V6: [AppViewModel] is the only Activity-scoped ViewModel, and holds only those two.
 */
class MainActivity : FragmentActivity(R.layout.activity_main) {

    private val viewModel: AppViewModel by viewModel()

    private val navController: NavController
        get() = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
            .navController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Setting the graph after a configuration change or process death restores the saved
        // back stack instead of starting over.
        navController.graph = navController.buildAppGraph()

        findViewById<ComposeView>(R.id.message_host).setContent {
            MvvmTheme {
                // FMVVM-L3: the host passes state + a callback down, never the ViewModel.
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                AppMessageHost(message = state.message, onMessageShown = viewModel::messageShown)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.session }
                    .distinctUntilChanged()
                    .collect(::applySession)
            }
        }
    }

    /** FMVVM-N7: only the back-stack owner reacts, by navigating and clearing the stack. */
    private fun applySession(session: Session) {
        val controller = navController
        val isOnSignIn = controller.currentDestination?.hasRoute<SignInKey>() == true
        val target = sessionRedirect(session, isOnSignIn) ?: return
        controller.navigate(target) {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}
