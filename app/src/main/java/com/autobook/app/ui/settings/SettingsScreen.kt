package com.autobook.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val smsEnabled by viewModel.smsEnabled.collectAsStateWithLifecycle()
    val dedupWindow by viewModel.dedupWindowSec.collectAsStateWithLifecycle()
    val notificationEnabled by viewModel.notificationEnabled.collectAsStateWithLifecycle()
    val notificationPermissionGranted by viewModel.notificationPermissionGranted.collectAsStateWithLifecycle()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()
    val accessibilityPermissionGranted by viewModel.accessibilityPermissionGranted.collectAsStateWithLifecycle()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 从系统设置页面返回时刷新权限状态（监听 onResume）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkNotificationPermission(context)
                viewModel.checkAccessibilityPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ──── DeepSeek API Key ────
            SettingsSection(title = "AI 分类") {
                val keySnapshot = apiKey
                SettingsItem(
                    icon = Icons.Default.Key,
                    title = "DeepSeek API Key",
                    subtitle = if (keySnapshot.isNullOrBlank()) "未设置（点击配置）" else "已设置 ${keySnapshot.take(8)}****",
                    onClick = { showApiKeyDialog = true }
                )
            }

            // ──── 监控开关 ────
            SettingsSection(title = "自动记账") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("短信监听", fontWeight = FontWeight.Medium)
                        Text("自动识别银行卡消费短信", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = smsEnabled,
                        onCheckedChange = { viewModel.toggleSmsEnabled() }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 去重窗口
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("去重窗口", fontWeight = FontWeight.Medium)
                        Text("${dedupWindow}秒 内相同金额+商户视为重复", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // 快速选择按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60 to "1分钟", 300 to "5分钟", 600 to "10分钟", 1800 to "30分钟").forEach { (sec, label) ->
                        FilterChip(
                            selected = dedupWindow == sec,
                            onClick = { viewModel.setDedupWindow(sec) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("通知栏监听", fontWeight = FontWeight.Medium)
                        Text(
                            if (notificationEnabled) "已开启 · 识别支付宝/微信支付通知"
                            else "识别支付宝/微信支付通知",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { viewModel.toggleNotificationEnabled(context) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Accessibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("界面识别监听", fontWeight = FontWeight.Medium)
                        Text(
                            if (accessibilityEnabled) "已开启 · 识别扫码付款结果页面"
                            else "识别支付宝/微信扫码付款",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = accessibilityEnabled,
                        onCheckedChange = { viewModel.toggleAccessibilityEnabled(context) }
                    )
                }
            }

            // ──── 权限 ────
            SettingsSection(title = "权限管理") {
                SettingsItem(
                    icon = Icons.Default.Sms,
                    title = "短信权限",
                    subtitle = "已授予" /* 运行时检查 */,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "通知监听权限",
                    subtitle = if (notificationPermissionGranted) "已授予" else "未授予 · 点击设置",
                    onClick = {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Accessibility,
                    title = "无障碍服务权限",
                    subtitle = if (accessibilityPermissionGranted) "已授予" else "未授予 · 点击设置",
                    onClick = {
                        context.startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS"))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.BatterySaver,
                    title = "电池优化白名单",
                    subtitle = "防止系统杀死后台服务",
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // ──── 数据 ────
            SettingsSection(title = "数据管理") {
                SettingsItem(
                    icon = Icons.Default.FileDownload,
                    title = "导出 CSV",
                    subtitle = "将交易记录导出为 Excel 兼容文件",
                    onClick = { viewModel.exportCsv(context) }
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "清空所有数据",
                    subtitle = "此操作不可撤销",
                    onClick = { /* TODO: 确认对话框 + 删除 */ }
                )
            }

            // ──── 关于 ────
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "AutoBook",
                    subtitle = "v1.0.0 · 自动记账助手",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ──── API Key 输入对话框 ────
    if (showApiKeyDialog) {
        var inputKey by remember { mutableStateOf(apiKey ?: "") }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("设置 DeepSeek API Key") },
            text = {
                Column {
                    Text(
                        "请输入您的 DeepSeek API Key。\n可在 platform.deepseek.com 获取。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setApiKey(inputKey.trim())
                        showApiKeyDialog = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("取消") }
            }
        )
    }
}

// ════════════════════════  共用组件  ════════════════════════

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
