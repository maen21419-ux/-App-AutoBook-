package com.autobook.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.SourceType
import com.autobook.app.data.model.Transaction
import com.autobook.app.service.RetryWorker
import com.autobook.app.ui.components.AddTransactionDialog
import com.autobook.app.ui.components.formatDateFull
import com.autobook.app.ui.home.HomeScreen
import com.autobook.app.ui.home.HomeViewModel
import com.autobook.app.ui.list.TransactionListScreen
import com.autobook.app.ui.settings.SettingsScreen
import com.autobook.app.ui.stats.StatsScreen
import com.autobook.app.ui.theme.AutoBookTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filter { !it.value }
        if (denied.isNotEmpty()) {
            Toast.makeText(this, "部分权限被拒绝，可能影响自动记账功能", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求所需权限
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) permissionsToRequest.add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        // 启动定期重试 PENDING 分类的 WorkManager
        RetryWorker.schedule(this)

        setContent {
            AutoBookTheme {
                MainScreen()
            }
        }
    }
}

// ═══════════════════════════════════════
//  主界面：底部导航 + NavHost
// ═══════════════════════════════════════

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        BottomNavItem("home", "首页", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("transactions", "账单", Icons.Filled.List, Icons.Outlined.List),
        BottomNavItem("stats", "统计", Icons.Filled.PieChart, Icons.Outlined.PieChart),
        BottomNavItem("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onTransactionClick = { tx ->
                        selectedTransaction = tx
                        showDetailDialog = true
                    },
                    onAddClick = { showAddDialog = true }
                )
            }
            composable("transactions") {
                TransactionListScreen(
                    onTransactionClick = { tx ->
                        selectedTransaction = tx
                        showDetailDialog = true
                    }
                )
            }
            composable("stats") { StatsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }

    // ──── 手动记账对话框 ────
    if (showAddDialog) {
        val homeVM: HomeViewModel = hiltViewModel()
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { yuan, merchant, category, ts, note ->
                homeVM.addManual(yuan, merchant, category, ts, note)
                showAddDialog = false
                Toast.makeText(context, "记账成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ──── 交易详情对话框 ────
    if (showDetailDialog && selectedTransaction != null) {
        val homeVM: HomeViewModel = hiltViewModel()
        TransactionDetailDialog(
            transaction = selectedTransaction!!,
            onDismiss = { showDetailDialog = false },
            onCategoryChange = { tx, newCat ->
                homeVM.updateCategory(tx, newCat)
                // 更新当前显示的 transaction
                selectedTransaction = tx.copy(category = newCat)
            }
        )
    }
}

// ═══════════════════════════════════════
//  交易详情弹窗
// ═══════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onCategoryChange: (Transaction, Category) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("交易详情", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "¥${"%.2f".format(transaction.amountYuan)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                DetailRow("商户", transaction.merchant)
                DetailRow("分类", "${transaction.category.emoji} ${transaction.category.label}")
                DetailRow("时间", formatDateFull(transaction.timestamp))
                DetailRow("来源", when (transaction.source) {
                    SourceType.SMS -> "银行短信"
                    SourceType.NOTIFICATION -> "通知栏"
                    SourceType.MANUAL -> "手动录入"
                    else -> transaction.source.name
                })
                if (transaction.cardLast4 != null) {
                    DetailRow("银行卡", "${transaction.bankName ?: ""} 尾号${transaction.cardLast4}")
                }
                if (transaction.note.isNotBlank()) {
                    DetailRow("备注", transaction.note)
                }
                if (transaction.rawText.isNotBlank()) {
                    Text("原始短信", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        transaction.rawText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("修改分类", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Category.entries.filter { it != Category.PENDING }.forEach { category ->
                        FilterChip(
                            selected = transaction.category == category,
                            onClick = { onCategoryChange(transaction, category) },
                            label = { Text("${category.emoji} ${category.label}", fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp)
    }
}
