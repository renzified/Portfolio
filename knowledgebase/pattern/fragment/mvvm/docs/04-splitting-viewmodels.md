# 4. Splitting ViewModels

Splitting is about **ownership of logic**, not about recomposition performance (that's chapter 2).

## FMVVM-V1 — One ViewModel per destination (default, MUST)
Each navigation destination (each Fragment) gets one ViewModel, obtained with `by viewModel()`
and scoped to that Fragment (L9). It survives rotation and the back stack, and is cleared when the
destination is popped.

## FMVVM-V2 — Big ViewModel? Split into delegates, not more ViewModels (SHOULD)
When a VM grows (>~300 lines, or clearly separable concerns), extract plain classes that own one
concern. The VM composes them and remains the single state owner for the screen.

```kotlin
// A delegate: plain class, no ViewModel, gets the VM's scope.
class PromoCodeController(
    private val promoRepository: PromoRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PromoState())
    val state: StateFlow<PromoState> = _state.asStateFlow()

    fun apply(code: String) {
        _state.update { it.copy(isApplying = true, error = null) }
        scope.launch {
            promoRepository.validate(code)
                .onSuccess { discount -> _state.update { PromoState(applied = discount) } }
                .onFailure { _state.update { it.copy(isApplying = false, error = UiText.Res(R.string.promo_invalid)) } }
        }
    }
}

data class PromoState(
    val applied: Discount? = null,
    val isApplying: Boolean = false,
    val error: UiText? = null,
)

class CheckoutViewModel(
    cartRepository: CartRepository,
    promoRepository: PromoRepository,
) : ViewModel() {
    private val promo = PromoCodeController(promoRepository, viewModelScope)

    val uiState: StateFlow<CheckoutUiState> =
        combine(cartRepository.observeCart(), promo.state) { cart, promo ->
            CheckoutUiState(
                items = cart.items.map { it.toUi() },
                promo = promo,
                total = cart.total - (promo.applied?.amount ?: Money.ZERO),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    fun applyPromo(code: String) = promo.apply(code)
}
```

Why delegates over sibling VMs: one `uiState` for the screen (consistency, C3), the delegate can be
unit-tested alone with a `TestScope`, and it can be reused by another VM.

## FMVVM-V3 — Extra ViewModel on a screen: only for self-contained, reused sections (MAY)
Allowed when **all** are true:
1. The section has its own data source and logic (it doesn't need the host screen's state).
2. It appears on several screens.
3. The host screen doesn't need to read its state.

Example: a cart badge in the top bar of several screens. Both ViewModels are Fragment
properties; both are scoped to the Fragment.

```kotlin
class StoreFragment : Fragment() {
    private val viewModel: StoreViewModel by viewModel()
    private val cartBadgeViewModel: CartBadgeViewModel by viewModel()   // both obtained in the Fragment (L3)

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val cartCount by cartBadgeViewModel.count.collectAsStateWithLifecycle()
        StoreScreen(
            state = state,
            cartCount = cartCount,
            onCartClick = { findNavController().navigate(CheckoutKey) },
        )
    }
}
```

If the host needs the section's state, it's not self-contained → move the logic into the host VM
(as a delegate, V2) or into the data layer (V4).

Don't use a child Fragment for such a section. A self-contained section is a composable plus,
if needed, its own ViewModel obtained by the host Fragment.

## FMVVM-V4 — Shared across unrelated screens ⇒ repository is the source of truth (MUST)
Two screens that show/modify the same data each have their own VM observing the same repository.
They stay in sync because they observe the same `Flow`, not because they talk to each other.

```kotlin
class CartRepository(private val dao: CartDao) {
    fun observeCart(): Flow<Cart> = dao.observe()            // single source of truth
    suspend fun add(item: ProductId) = dao.insert(item)
}

class ProductViewModel(private val cart: CartRepository) : ViewModel() {
    fun addToCart(id: ProductId) { viewModelScope.launch { cart.add(id) } }
}
class CartBadgeViewModel(cart: CartRepository) : ViewModel() {
    val count = cart.observeCart().map { it.items.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
```

❌ Never:
```kotlin
class ProductViewModel(private val cartVm: CartBadgeViewModel) : ViewModel()   // L7
fun addToCart(id: ProductId, cartVm: CartBadgeViewModel)                       // L7
```

If data is in-memory only (no DB), the repository holds a `MutableStateFlow` and is scoped as a
singleton (or to the feature's DI component).

## FMVVM-V5 — Multi-step flows ⇒ a nested graph owning the shared ViewModel (MUST)
A wizard (checkout: address → payment → review) shares in-progress, not-yet-persisted data.
With the Navigation component the rule is:

- The flow is a **nested graph** whose route is the flow key (`CheckoutFlowKey`). Each step is its
  own Fragment destination inside it.
- The flow ViewModel is scoped to the **graph's back-stack entry**. Every step Fragment obtains it
  with `by viewModel(ownerProducer = graphOwner<CheckoutFlowKey>())` and gets the same instance.
- The flow VM is cleared when the graph entry is popped, i.e. when the user leaves the whole flow
  (`popBackStack<CheckoutFlowKey>(inclusive = true)`, or Back from the first step).

```kotlin
@Serializable data object CheckoutFlowKey      // the graph's route
@Serializable data object AddressStepKey
@Serializable data object PaymentStepKey
@Serializable data object ReviewStepKey

// AppNavGraph.kt
navigation<CheckoutFlowKey>(startDestination = AddressStepKey) {
    fragment<AddressStepFragment, AddressStepKey>()
    fragment<PaymentStepFragment, PaymentStepKey>()
    fragment<ReviewStepFragment, ReviewStepKey>()
}

// navigation/FragmentNavigation.kt
inline fun <reified G : Any> Fragment.graphOwner(): () -> ViewModelStoreOwner =
    { findNavController().getBackStackEntry<G>() }

class PaymentStepFragment : Fragment() {
    private val flowViewModel: CheckoutFlowViewModel by viewModel(
        ownerProducer = graphOwner<CheckoutFlowKey>()        // shared by every step
    )
    private val stepViewModel: PaymentViewModel by viewModel() // optional step-only logic

    @Composable
    private fun Content() {
        val state by stepViewModel.uiState.collectAsStateWithLifecycle()
        PaymentStepScreen(
            canContinue = state.canContinue,
            cardNumber = stepViewModel.cardNumber,
            onNextClick = {
                flowViewModel.setCard(state.last4)             // the Fragment hands it over (L7)
                findNavController().navigate(ReviewStepKey)
            },
        )
    }
}
```

The flow VM holds the draft (`CheckoutDraft`) and is the only writer of it. Step VMs, if any, own
step-local logic (e.g. card validation) and hand results to their Fragment, which calls the flow
VM. The two VMs never reference each other (L7). Every step obtains the flow VM itself; it is
never passed from one Fragment to another, and never below the Fragment (L3).

`getBackStackEntry<CheckoutFlowKey>()` is safe here because a graph's route is on the back stack
at most once. Don't use it to read a *destination's* key (N2).

## FMVVM-V6 — No Activity-scoped "god" ViewModel (MUST NOT)
An Activity-scoped VM used as an app-wide store lives forever, hides dependencies, and couples every
screen. In a Fragment app that is `by activityViewModel()` / `activityViewModels()`: it is
**forbidden in Fragments**. Use repositories (V4) for app-wide data and a nested graph (V5) for
flow data. The only Activity-scoped VM is the shell's `AppViewModel`, obtained by `MainActivity`
itself, for state truly about the Activity (session gate N7, message host N6).

## FMVVM-V7 — Dialogs and bottom sheets (MUST)
| Case | Owner |
|---|---|
| Dialog is a nav destination with its own data/logic (e.g. "Rename" that saves) | A `DialogFragment` declared with `dialog<XDialogFragment, XKey>()`, with its own ViewModel (V1 applies) |
| Dialog is part of the screen (confirm delete, pick sort order) | A Compose `AlertDialog` in the Screen. Visibility + selection in the **parent** UiState if logic reacts to it; in `remember` if purely visual (S2) |
| Confirm dialog whose result triggers business logic | Visibility in UI, result goes to parent VM via lambda |

```kotlin
// AppNavGraph.kt
dialog<RenameTaskDialogFragment, RenameTaskKey>()

class RenameTaskDialogFragment : DialogFragment() {
    private val viewModel: RenameTaskViewModel by viewModel()      // taskId from the key (P5)
    override fun onCreateView(...) = composeScreen {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.dismissed) {
            if (state.dismissed) { viewModel.dismissHandled(); findNavController().popBackStack() }
        }
        RenameTaskScreen(state, viewModel.name, onSaveClick = viewModel::save,
            onCancelClick = { findNavController().popBackStack() })
    }
}
```

- Close a dialog destination with `findNavController().popBackStack()`, not by calling `dismiss()`
  from outside. The Navigation component pops the entry for you when the user dismisses it (back,
  outside touch), which clears its VM.
- Don't `show()` a `DialogFragment` through the FragmentManager next to the NavController. It
  then isn't on the back stack, gets no key, and is restored behind Navigation's back.

```kotlin
// Confirm delete – visibility is UI state, the delete is a VM action
var confirmDelete by remember { mutableStateOf<TaskId?>(null) }
confirmDelete?.let { id ->
    AlertDialog(
        onDismissRequest = { confirmDelete = null },
        confirmButton = { TextButton(onClick = { onDeleteTask(id); confirmDelete = null }) { Text("Delete") } },
        text = { Text("Delete this task?") },
    )
}
```

## Summary

| Situation | Do |
|---|---|
| New screen | One VM (V1) |
| Screen VM getting big | Delegates inside it (V2) |
| Self-contained widget reused on many screens | Its own VM, obtained in each Fragment (V3) |
| Same data on unrelated screens | Repository + one VM per screen (V4) |
| Wizard sharing a draft | Nested graph owns shared VM (V5) |
| "Global app state" | Repository, never `activityViewModel()` (V6) |
| Dialog that saves | `DialogFragment` destination + own VM (V7) |
