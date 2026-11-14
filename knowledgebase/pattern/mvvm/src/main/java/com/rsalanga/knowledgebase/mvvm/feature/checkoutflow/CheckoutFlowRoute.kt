package com.rsalanga.knowledgebase.mvvm.feature.checkoutflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.RuleNote
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

private const val CARD_LENGTH = 16

@Serializable
data object AddressStep : NavKey

@Serializable
data object PaymentStep : NavKey

@Serializable
data object ReviewStep : NavKey

/**
 * MVVM-V5: the whole wizard is ONE entry on the app back stack (`CheckoutFlowKey`). Its Route
 * owns the flow VM and hosts an inner back stack of steps in a nested NavDisplay.
 */
@Composable
fun CheckoutFlowRoute(onExit: () -> Unit, flowViewModel: CheckoutFlowViewModel = koinViewModel()) {
    val steps = rememberNavBackStack(AddressStep)
    val state by flowViewModel.uiState.collectAsStateWithLifecycle()
    val currentOnExit by rememberUpdatedState(onExit)

    LaunchedEffect(state.finished) {
        if (state.finished) {
            flowViewModel.finishHandled()
            currentOnExit()
        }
    }

    NavDisplay(
        backStack = steps,
        onBack = { if (steps.size > 1) steps.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator() // step VMs scoped per step (MVVM-L9)
        ),
        entryProvider = entryProvider {
            entry<AddressStep> {
                // Passing the flow VM into a step *Route* is allowed (it is a Route, MVVM-L3).
                AddressStepRoute(
                    flowViewModel = flowViewModel,
                    onNext = { steps.add(PaymentStep) },
                    onBack = onExit
                )
            }
            entry<PaymentStep> {
                PaymentStepRoute(
                    flowViewModel = flowViewModel,
                    onNext = { steps.add(ReviewStep) },
                    onBack = { steps.removeLastOrNull() }
                )
            }
            entry<ReviewStep> {
                ReviewStepRoute(
                    flowViewModel = flowViewModel,
                    onBack = { steps.removeLastOrNull() }
                )
            }
        }
    )
}

@Composable
private fun AddressStepRoute(
    flowViewModel: CheckoutFlowViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val state by flowViewModel.uiState.collectAsStateWithLifecycle()
    StepScaffold(title = "1 · Address", onBack = onBack) {
        OutlinedTextField(
            state = flowViewModel.address,
            label = { Text("Delivery address") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onNext, enabled = state.hasAddress) { Text("Next") }
        RuleNote("V5: the address lives in the flow VM, so later steps can read it.")
    }
}

@Composable
private fun PaymentStepRoute(
    flowViewModel: CheckoutFlowViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    stepViewModel: PaymentViewModel = koinViewModel() // scoped to this inner entry
) {
    val state by stepViewModel.uiState.collectAsStateWithLifecycle()
    StepScaffold(title = "2 · Payment", onBack = onBack) {
        OutlinedTextField(
            state = stepViewModel.cardNumber,
            label = { Text("Card number (16 digits)") },
            inputTransformation = InputTransformation.maxLength(CARD_LENGTH),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            // The step VM hands its result to the Route, which calls the flow VM (MVVM-L7).
            onClick = {
                flowViewModel.setCard(state.last4)
                onNext()
            },
            enabled = state.canContinue
        ) { Text("Next") }
        RuleNote("V5: card validation is step-local logic in PaymentViewModel.")
    }
}

@Composable
private fun ReviewStepRoute(flowViewModel: CheckoutFlowViewModel, onBack: () -> Unit) {
    val state by flowViewModel.uiState.collectAsStateWithLifecycle()
    StepScaffold(title = "3 · Review", onBack = onBack) {
        Text("Ship to: ${state.address}")
        Text("Card: •••• ${state.cardLast4.orEmpty()}")
        Button(onClick = flowViewModel::placeOrder, enabled = !state.isPlacing) {
            Text(if (state.isPlacing) "Placing…" else "Place order")
        }
        RuleNote("Placing the order leaves the whole flow; the flow VM is cleared with it.")
    }
}

@Composable
private fun StepScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    DemoScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { content() }
    }
}
