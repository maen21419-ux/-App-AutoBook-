package com.autobook.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.Transaction
import com.autobook.app.data.repository.TransactionRepo
import com.autobook.app.ui.components.getThirtyDaysAgoMs
import com.autobook.app.ui.components.getTodayStartMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TransactionRepo
) : ViewModel() {

    private val todayStartMs = getTodayStartMs()

    /** 今日总支出（分） */
    val todayExpense: StateFlow<Long> = repo.getTodayTotalExpense(todayStartMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 今日交易列表 */
    val todayTransactions: StateFlow<List<Transaction>> = repo.getTodayTransactions(todayStartMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 最近 10 条交易（不含今日） */
    val recentTransactions: StateFlow<List<Transaction>> = repo.getByTimeRange(
        startMs = getThirtyDaysAgoMs(),
        endMs = todayStartMs - 1
    ).map { it.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 手动记账 */
    fun addManual(amountYuan: Double, merchant: String, category: Category, timestamp: Long, note: String) {
        viewModelScope.launch {
            repo.addManual(amountYuan, merchant, category, timestamp, note)
        }
    }

    /** 修改分类 */
    fun updateCategory(transaction: Transaction, newCategory: Category) {
        viewModelScope.launch {
            repo.updateCategory(transaction, newCategory)
        }
    }
}
