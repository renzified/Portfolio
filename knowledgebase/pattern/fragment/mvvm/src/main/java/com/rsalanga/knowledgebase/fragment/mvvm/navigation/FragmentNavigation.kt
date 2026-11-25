package com.rsalanga.knowledgebase.fragment.mvvm.navigation

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.fragment.findNavController

/**
 * FMVVM-V5 / L9: the ViewModelStoreOwner of a nested graph on the back stack, for a flow
 * ViewModel shared by the graph's step Fragments. Pass it as Koin's `ownerProducer`:
 *
 * ```
 * private val flowViewModel: CheckoutFlowViewModel by viewModel(ownerProducer = graphOwner<CheckoutFlowKey>())
 * ```
 *
 * The ViewModel is cleared when the graph's entry is popped, i.e. when the user leaves the flow.
 */
inline fun <reified G : Any> Fragment.graphOwner(): () -> ViewModelStoreOwner =
    { findNavController().getBackStackEntry<G>() }

/**
 * Every "Back" button in the demos. `navigateUp()` is a no-op on the start destination, so the
 * catalog can never pop itself and leave an empty host.
 */
fun Fragment.navigateUp() {
    findNavController().navigateUp()
}
