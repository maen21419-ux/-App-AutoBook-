package com.autobook.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.autobook.app.data.repository.TransactionRepo
import com.autobook.app.di.NotificationListenerEntryPoint
import com.autobook.app.domain.AccessibilityParser
import com.autobook.app.domain.PrefsManager
import com.autobook.app.domain.TransactionType
import dagger.hilt.EntryPoints
import kotlinx.coroutines.*

/**
 * 无障碍服务 — 监听微信/支付宝支付结果页面（扫码付款等不产生通知栏消息的场景）。
 *
 * 与 NotificationListenerService 互补：
 * - NotificationListener → 收款/转账/红包（有通知栏消息）
 * - AccessibilityService → 扫码付款/被扫付款（仅在 App 内显示结果）
 *
 * 同样使用 EntryPointAccessors 手动获取 Hilt 依赖（AccessibilityService 不支持 @AndroidEntryPoint）。
 */
class BillAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var prefsManager: PrefsManager? = null
    private var transactionRepo: TransactionRepo? = null

    /** 去重：同一交易可能触发多次窗口变化事件 */
    private var lastEventTime = 0L
    private var lastEventFingerprint = 0

    companion object {
        private const val TAG = "AutoBook-A11yService"
        private const val DEBOUNCE_MS = 3000L // 3 秒内同一交易去重

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains(context.packageName) == true
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "无障碍服务已连接")

        // 配置监听参数
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            // 仅监听微信和支付宝
            packageNames = arrayOf(
                "com.tencent.mm",
                "com.eg.android.AlipayGphone"
            )
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return

        // 仅处理窗口变化（支付结果页切换）
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val className = event.className?.toString() ?: ""
        val source = event.source ?: return

        // 去重：3 秒内同一 Activity + 同一内容指纹跳过
        val fingerprint = "$pkgName|$className|${source.text?.toString() ?: ""}".hashCode()
        val now = System.currentTimeMillis()
        if (now - lastEventTime < DEBOUNCE_MS && fingerprint == lastEventFingerprint) {
            source.recycle()
            return
        }
        lastEventTime = now
        lastEventFingerprint = fingerprint

        Log.d(TAG, "窗口变化: pkg=$pkgName class=$className")

        serviceScope.launch {
            try {
                if (!ensureDeps()) return@launch

                // 检查开关
                if (!prefsManager!!.isAccessibilityEnabled()) {
                    return@launch
                }

                // 解析界面
                val parsed = AccessibilityParser.parse(source, pkgName)
                source.recycle()

                if (parsed.transactionType == TransactionType.IGNORE) {
                    return@launch
                }

                Log.d(TAG, "识别到支付: ${parsed.merchant} ${parsed.amountFen / 100.0}元")

                // 入库
                val result = transactionRepo!!.processNotification(
                    amountFen = parsed.amountFen,
                    merchant = parsed.merchant,
                    timestamp = parsed.timestamp,
                    rawTitle = "无障碍-界面识别",
                    rawText = "$pkgName | $className"
                )

                if (result != null) {
                    Log.i(TAG, "无障碍记账成功: ${result.merchant} ${result.amountYuan}元 [${result.category.label}]")
                } else {
                    Log.d(TAG, "交易被去重: $pkgName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "无障碍处理失败: pkg=$pkgName class=$className", e)
                try { source.recycle() } catch (_: Exception) {}
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        prefsManager = null
        transactionRepo = null
        super.onDestroy()
    }

    // ══════════════════════════════════════════════
    //  Hilt 依赖（同 NotificationListener 模式）
    // ══════════════════════════════════════════════

    private fun ensureDeps(): Boolean {
        if (prefsManager != null && transactionRepo != null) return true
        return try {
            val entryPoint = EntryPoints.get(
                applicationContext,
                NotificationListenerEntryPoint::class.java
            )
            prefsManager = entryPoint.prefsManager()
            transactionRepo = entryPoint.transactionRepo()
            Log.i(TAG, "Hilt 依赖注入成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Hilt 依赖注入失败，延迟重试", e)
            false
        }
    }
}
