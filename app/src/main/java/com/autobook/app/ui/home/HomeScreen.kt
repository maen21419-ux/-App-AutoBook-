package com.autobook.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autobook.app.data.model.Transaction
import com.autobook.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTransactionClick: (Transaction) -> Unit,
    onAddClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val todayExpense by viewModel.todayExpense.collectAsStateWithLifecycle()
    val todayTransactions by viewModel.todayTransactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutoBook", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "手动记账",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        if (todayTransactions.isEmpty() && recentTransactions.isEmpty()) {
            EmptyState(
                title = "还没有交易记录",
                subtitle = "收到银行短信后会自动记账\n或点击下方按钮手动添加",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 今日支出卡片
                item {
                    TodayCard(
                        expense = todayExpense,
                        count = todayTransactions.size
                    )
                }

                // 今日交易
                if (todayTransactions.isNotEmpty()) {
                    item {
                        SectionHeader(title = "今日交易")
                    }
                    items(todayTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }

                // 最近交易
                if (recentTransactions.isNotEmpty()) {
                    item {
                        SectionHeader(title = "最近记录")
                    }
                    items(recentTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(expense: Long, count: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日支出",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¥${"%.2f".format(expense / 100.0)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "共 $count 笔",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
