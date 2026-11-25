package com.rsalanga.knowledgebase.fragment.mvvm.feature.checkoutflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.CheckoutFlowKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.PaymentStepKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.ReviewStepKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.graphOwner
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val CARD_LENGTH = 16

/*
 * FMVVM-V5: the wizard is a nested graph (`navigation<CheckoutFlowKey> { … }` in AppNavGraph).
 * Each step is its own Fragment destination. The shared, not-yet-saved draft lives in
 * CheckoutFlowViewModel, scoped to the *graph's* back-stack entry with `graphOwner<…>()`:
 * every step gets the same instance, and it is cleared when the graph is popped, i.e. when the
 * user leaves the whole flow.
 *
 * Each step Fragment obtains the flow VM itself; it is never passed between Fragments or below
 * the Fragment (FMVVM-L3).
 */

/** Leaves the whole flow: pops the graph entry, which clears the flow VM. */
private fun Fragment.exitCheckoutFlow() {
    findNavController().popBackStack<CheckoutFlowKey>(inclusive = true)
}

class AddressStepFragment : Fragment() {
    private val flowViewModel: CheckoutFlowViewModel by viewModel(
        ownerProducer = graphOwner<CheckoutFlowKey>()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by flowViewModel.uiState.collectAsStateWithLifecycle()
        AddressStepScreen(
            hasAddress = state.hasAddress,
            address = flowViewModel.address,
            onNextClick = { findNavController().navigate(PaymentStepKey) },
            onBack = { exitCheckoutFlow() }
        )
    }
}

class PaymentStepFragment : Fragment() {
    private val flowViewModel: CheckoutFlowViewModel by viewModel(
        ownerProducer = graphOwner<CheckoutFlowKey>()
    )

    /** Step-local logic, scoped to this step's Fragment only. */
    private val stepViewModel: PaymentViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by stepViewModel.uiState.collectAsStateWithLifecycle()
        PaymentStepScreen(
            canContinue = state.canContinue,
            cardNumber = stepViewModel.cardNumber,
            // The step VM hands its result to the Fragment, which calls the flow VM. The two
            // VMs never reference each other (FMVVM-L7).
            onNextClick = {
                flowViewModel.setCard(state.last4)
                findNavController().navigate(ReviewStepKey)
            },
            onBack = { navigateUp() }
        )
    }
}

class ReviewStepFragment : Fragment() {
    private val flowViewModel: CheckoutFlowViewModel by viewModel(
        ownerProducer = graphOwner<CheckoutFlowKey>()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by flowViewModel.uiState.collectAsStateWithLifecycle()

        // FMVVM-E3 / E5: "finished" is an outcome; this Fragment leaves the flow.
        LaunchedEffect(state.finished) {
            if (state.finished) {
                flowViewModel.finishHandled()
                exitCheckoutFlow()
            }
        }

        ReviewStepScreen(
            state = state,
            onPlaceOrderClick = flowViewModel::placeOrder,
            onBack = { navigateUp() }
        )
    }
}

@Composable
fun AddressStepScreen(
    hasAddress: Boolean,
    address: TextFieldState,
    onNextClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    StepScaffold(title = "1 · Address", onBack = onBack, modifier = modifier) {
        OutlinedTextField(
            state = address,
            label = { Text("Delivery address") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onNextClick, enabled = hasAddress) { Text("Next") }
        RuleNote(
            "V5: the address lives in the flow VM, scoped to the nested graph, so later steps " +
                "read the same instance."
        )
    }
}

@Composable
fun PaymentStepScreen(
    canContinue: Boolean,
    cardNumber: TextFieldState,
    onNextClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    StepScaffold(title = "2 · Payment", onBack = onBack, modifier = modifier) {
        OutlinedTextField(
            state = cardNumber,
            label = { Text("Card number (16 digits)") },
            inputTransformation = InputTransformation.maxLength(CARD_LENGTH),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onNextClick, enabled = canContinue) { Text("Next") }
        RuleNote("V5: card validation is step-local logic in PaymentViewModel.")
    }
}

@Composable
fun ReviewStepScreen(
    state: CheckoutFlowUiState,
    onPlaceOrderClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    StepScaffold(title = "3 · Review", onBack = onBack, modifier = modifier) {
        Text("Ship to: ${state.address}")
        Text("Card: •••• ${state.cardLast4.orEmpty()}")
        Button(onClick = onPlaceOrderClick, enabled = !state.isPlacing) {
            Text(if (state.isPlacing) "Placing…" else "Place order")
        }
        RuleNote("Placing the order pops the whole graph; the flow VM is cleared with it.")
    }
}

@Composable
private fun StepScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    DemoScaffold(title = title, onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { content() }
    }
}
