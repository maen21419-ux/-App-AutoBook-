package com.autobook.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.Transaction
import com.autobook.app.data.repository.TransactionRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repo: TransactionRepo
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    private val _showFilterSheet = MutableStateFlow(false)
    val showFilterSheet: StateFlow<Boolean> = _showFilterSheet

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _searchQuery, _selectedCategory
    ) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> repo.search(query)
            category != null -> repo.getByCategory(category)
            else -> repo.getAll()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: Category?) {
        _selectedCategory.value = category
        _showFilterSheet.value = false
    }

    fun toggleFilterSheet() {
        _showFilterSheet.value = !_showFilterSheet.value
    }
}
