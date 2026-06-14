package com.autobook.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobook.app.data.local.CategorySummary
import com.autobook.app.data.local.DailyTotal
import com.autobook.app.data.repository.TransactionRepo
import com.autobook.app.ui.components.getMonthStartMs
import com.autobook.app.ui.components.getThirtyDaysAgoMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repo: TransactionRepo
) : ViewModel() {

    private val monthStartMs = getMonthStartMs()
    private val thirtyDaysAgoMs = getThirtyDaysAgoMs()

    val monthExpense: StateFlow<Long> = repo.getMonthTotalExpense(monthStartMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categorySummary: StateFlow<List<CategorySummary>> =
        repo.getMonthCategorySummary(monthStartMs)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTotals: StateFlow<List<DailyTotal>> = repo.getDailyTotals30Days(thirtyDaysAgoMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
