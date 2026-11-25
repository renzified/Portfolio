package com.rsalanga.knowledgebase.fragment.mvvm.navigation

import kotlinx.serialization.Serializable

// FMVVM-N1 / N2: one `@Serializable` `XKey` per destination: the Navigation component's
// type-safe route. Keys hold IDs and small parameters only — never domain objects, UI models or
// lists. Navigation stores their fields in the destination's arguments Bundle (saved state),
// and the destination loads everything else by ID.

@Serializable
data object CatalogKey

@Serializable
data class ChapterKey(val number: Int)

// ---- Tasks (chapters 1, 5, 6, 7, 10) ------------------------------------------------------

@Serializable
data object TaskListKey

@Serializable
data class TaskDetailKey(val taskId: Long)

/** `taskId == null` opens the editor for a new task. */
@Serializable
data class EditTaskKey(val taskId: Long?)

/** FMVVM-V7: a `dialog<…>` destination (a DialogFragment) with its own ViewModel. */
@Serializable
data class RenameTaskKey(val taskId: Long)

/** FMVVM-N1: `selectedLabelId` is a small parameter that configures the picker. */
@Serializable
data class LabelPickerKey(val selectedLabelId: Long?)

/**
 * FMVVM-N4: explicit Fragment Result request key, declared next to the result screen's key.
 * Payload: [LABEL_PICKED_ID] (Long).
 */
const val LABEL_PICKED_RESULT = "label_picked"
const val LABEL_PICKED_ID = "labelId"

// ---- Chapter 2 ----------------------------------------------------------------------------

@Serializable
data object GameKey

@Serializable
data object LiveGameKey

// ---- Chapter 3 ----------------------------------------------------------------------------

@Serializable
data object UiElementStateKey

@Serializable
data object OrdersKey

// ---- Chapter 4 ----------------------------------------------------------------------------

@Serializable
data object StoreKey

@Serializable
data object CheckoutKey

/**
 * FMVVM-V5: the wizard is a nested graph. This key is the *graph's* route; its back-stack entry
 * owns the flow ViewModel. The steps are destinations inside it.
 */
@Serializable
data object CheckoutFlowKey

@Serializable
data object AddressStepKey

@Serializable
data object PaymentStepKey

@Serializable
data object ReviewStepKey

// ---- Chapter 6 ----------------------------------------------------------------------------

@Serializable
data object ProfileKey

@Serializable
data object FeedKey

@Serializable
data object ReportKey

// ---- Chapter 7 ----------------------------------------------------------------------------

@Serializable
data object NoteEditorKey

// ---- Chapter 8 ----------------------------------------------------------------------------

@Serializable
data object SignUpKey

@Serializable
data object SearchKey

@Serializable
data object VolumeKey

// ---- Chapter 10 ---------------------------------------------------------------------------

@Serializable
data object AccountKey

@Serializable
data object SignInKey
