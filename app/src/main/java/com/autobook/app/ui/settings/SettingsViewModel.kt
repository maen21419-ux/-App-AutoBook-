package com.autobook.app.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobook.app.data.repository.TransactionRepo
import com.autobook.app.domain.PrefsManager
import com.autobook.app.service.BillAccessibilityService
import com.autobook.app.service.NotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val transactionRepo: TransactionRepo
) : ViewModel() {

    val apiKey: StateFlow<String?> = prefsManager.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _smsEnabled = MutableStateFlow(true)
    val smsEnabled: StateFlow<Boolean> = _smsEnabled

    private val _dedupWindowSec = MutableStateFlow(300)
    val dedupWindowSec: StateFlow<Int> = _dedupWindowSec

    private val _notificationEnabled = MutableStateFlow(false)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted

    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled

    private val _accessibilityPermissionGranted = MutableStateFlow(false)
    val accessibilityPermissionGranted: StateFlow<Boolean> = _accessibilityPermissionGranted

    init {
        viewModelScope.launch {
            _smsEnabled.value = prefsManager.isSmsEnabled()
            _dedupWindowSec.value = prefsManager.getDedupWindowSec()
            _notificationEnabled.value = prefsManager.isNotificationEnabled()
            _accessibilityEnabled.value = prefsManager.isAccessibilityEnabled()
        }
    }

    /** 切换通知监听开关 */
    fun toggleNotificationEnabled(context: Context) {
        viewModelScope.launch {
            val newValue = !_notificationEnabled.value
            if (newValue) {
                // 开启前检查权限
                val hasPermission = NotificationListener.isNotificationListenerEnabled(context)
                _notificationPermissionGranted.value = hasPermission
                if (!hasPermission) {
                    Toast.makeText(
                        context,
                        "请先授予通知监听权限后再开启此功能",
                        Toast.LENGTH_LONG
                    ).show()
                    // 跳转到系统通知监听设置页
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    )
                    return@launch
                }
            }
            prefsManager.setNotificationEnabled(newValue)
            _notificationEnabled.value = newValue
        }
    }

    /** 刷新通知监听权限状态（从系统设置返回后调用） */
    fun checkNotificationPermission(context: Context) {
        _notificationPermissionGranted.value =
            NotificationListener.isNotificationListenerEnabled(context)
    }

    /** 切换无障碍监听开关 */
    fun toggleAccessibilityEnabled(context: Context) {
        viewModelScope.launch {
            val newValue = !_accessibilityEnabled.value
            if (newValue) {
                val hasPermission = BillAccessibilityService.isAccessibilityServiceEnabled(context)
                _accessibilityPermissionGranted.value = hasPermission
                if (!hasPermission) {
                    Toast.makeText(
                        context,
                        "请先开启无障碍服务后再启用此功能",
                        Toast.LENGTH_LONG
                    ).show()
                    context.startActivity(
                        Intent("android.settings.ACCESSIBILITY_SETTINGS")
                    )
                    return@launch
                }
            }
            prefsManager.setAccessibilityEnabled(newValue)
            _accessibilityEnabled.value = newValue
        }
    }

    /** 刷新无障碍权限状态 */
    fun checkAccessibilityPermission(context: Context) {
        _accessibilityPermissionGranted.value =
            BillAccessibilityService.isAccessibilityServiceEnabled(context)
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            prefsManager.setApiKey(key)
        }
    }

    fun toggleSmsEnabled() {
        viewModelScope.launch {
            val newValue = !_smsEnabled.value
            prefsManager.setSmsEnabled(newValue)
            _smsEnabled.value = newValue
        }
    }

    fun setDedupWindow(seconds: Int) {
        viewModelScope.launch {
            prefsManager.setDedupWindowSec(seconds)
            _dedupWindowSec.value = seconds
        }
    }

    /** 导出 CSV 到缓存目录并通过分享发送 */
    fun exportCsv(context: Context) {
        viewModelScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionRepo.getAll().first()
                }

                if (transactions.isEmpty()) {
                    Toast.makeText(context, "没有交易记录可导出", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE)
                val csv = buildString {
                    append("ID,金额(元),商户,分类,来源,时间,银行卡,备注\n")
                    transactions.forEach { tx ->
                        append("${tx.id},")
                        append("${"%.2f".format(tx.amountYuan)},")
                        append("\"${tx.merchant}\",")
                        append("${tx.category.label},")
                        append("${tx.source.name},")
                        append("${dateFormat.format(Date(tx.timestamp))},")
                        append("${tx.cardLast4 ?: ""},")
                        append("\"${tx.note}\"\n")
                    }
                }

                val file = File(context.cacheDir, "autobook_export_${System.currentTimeMillis()}.csv")
                file.writeText(csv, Charsets.UTF_8)

                // 通过分享发送
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "导出交易记录"))

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
