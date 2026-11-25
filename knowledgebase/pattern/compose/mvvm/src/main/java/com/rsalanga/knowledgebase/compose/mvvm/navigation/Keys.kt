package com.rsalanga.knowledgebase.compose.mvvm.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// MVVM-N1 / N2: one `XKey` per destination. Keys hold IDs and small parameters only — never
// domain objects, UI models or lists. The destination loads everything else by ID.

@Serializable
data object CatalogKey : NavKey

@Serializable
data class ChapterKey(val number: Int) : NavKey

// ---- Tasks (chapters 1, 5, 6, 7, 10) ------------------------------------------------------

@Serializable
data object TaskListKey : NavKey

@Serializable
data class TaskDetailKey(val taskId: Long) : NavKey

/** `taskId == null` opens the editor for a new task. */
@Serializable
data class EditTaskKey(val taskId: Long?) : NavKey

/** MVVM-V7: a dialog destination with its own ViewModel. */
@Serializable
data class RenameTaskKey(val taskId: Long) : NavKey

/** MVVM-N1: `selectedLabelId` is a small parameter that configures the picker. */
@Serializable
data class LabelPickerKey(val selectedLabelId: Long?) : NavKey

/** MVVM-N4: explicit result key, declared next to the result screen's key. Payload: label id. */
const val LABEL_PICKED_RESULT = "label_picked"

// ---- Chapter 2 ----------------------------------------------------------------------------

@Serializable
data object GameKey : NavKey

@Serializable
data object LiveGameKey : NavKey

// ---- Chapter 3 ----------------------------------------------------------------------------

@Serializable
data object UiElementStateKey : NavKey

@Serializable
data object OrdersKey : NavKey

// ---- Chapter 4 ----------------------------------------------------------------------------

@Serializable
data object StoreKey : NavKey

@Serializable
data object CheckoutKey : NavKey

/** MVVM-V5: the whole wizard is one entry on the app back stack. */
@Serializable
data object CheckoutFlowKey : NavKey

// ---- Chapter 6 ----------------------------------------------------------------------------

@Serializable
data object ProfileKey : NavKey

@Serializable
data object FeedKey : NavKey

@Serializable
data object ReportKey : NavKey

// ---- Chapter 7 ----------------------------------------------------------------------------

@Serializable
data object NoteEditorKey : NavKey

// ---- Chapter 8 ----------------------------------------------------------------------------

@Serializable
data object SignUpKey : NavKey

@Serializable
data object SearchKey : NavKey

@Serializable
data object VolumeKey : NavKey

// ---- Chapter 10 ---------------------------------------------------------------------------

@Serializable
data object AccountKey : NavKey

@Serializable
data object SignInKey : NavKey
