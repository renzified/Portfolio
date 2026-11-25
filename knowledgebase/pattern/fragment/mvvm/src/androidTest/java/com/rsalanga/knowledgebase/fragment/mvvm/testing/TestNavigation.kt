package com.rsalanga.knowledgebase.fragment.mvvm.testing

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.Navigator
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.testing.TestNavHostController
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.buildAppGraph

/**
 * FMVVM-T5: attaches a [TestNavHostController] holding the **real** app graph to [fragment]'s
 * view, positioned at [startAt]. Call it inside `FragmentScenario.onFragment { }`.
 *
 * Why the extra navigators: the Kotlin DSL's `fragment<…>()` / `dialog<…>()` builders ask the
 * navigator provider for a `FragmentNavigator` / `DialogFragmentNavigator`. A plain
 * TestNavHostController only has stand-ins, so `buildAppGraph()` fails with a
 * ClassCastException. [RecordingFragmentNavigator] satisfies the builder and records navigation
 * on the back stack without running Fragment transactions.
 */
fun attachTestNavController(fragment: Fragment, startAt: Any): TestNavHostController {
    val context = fragment.requireContext()
    val fragmentManager = fragment.childFragmentManager
    val navController = TestNavHostController(context).apply {
        navigatorProvider.addNavigator(RecordingFragmentNavigator(context, fragmentManager))
        navigatorProvider.addNavigator(DialogFragmentNavigator(context, fragmentManager))
        graph = buildAppGraph()
        navigate(startAt)
    }
    Navigation.setViewNavController(fragment.requireView(), navController)
    return navController
}

/** Same name as [FragmentNavigator], so it replaces the test stand-in for "fragment". */
@Navigator.Name("fragment")
private class RecordingFragmentNavigator(context: Context, fragmentManager: FragmentManager) :
    FragmentNavigator(context, fragmentManager, View.NO_ID) {

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        entries.forEach(state::push)
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }
}
