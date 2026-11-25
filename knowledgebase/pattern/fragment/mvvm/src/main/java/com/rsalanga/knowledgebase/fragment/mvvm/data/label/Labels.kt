package com.rsalanga.knowledgebase.fragment.mvvm.data.label

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Label(val id: Long, val name: String)

interface LabelRepository {
    fun observeLabels(): Flow<List<Label>>
}

class InMemoryLabelRepository : LabelRepository {
    private val labels = MutableStateFlow(
        listOf(Label(1, "Home"), Label(2, "Work"), Label(3, "Errands"))
    )

    override fun observeLabels(): Flow<List<Label>> = labels.asStateFlow()
}
