package com.autobook.app.service

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autobook.app.data.repository.TransactionRepo
import com.autobook.app.di.NotificationListenerEntryPoint
import com.autobook.app.domain.NotificationParser
import com.autobook.app.domain.PrefsManager
import com.autobook.app.domain.TransactionType
import dagger.hilt.EntryPoints
import kotlinx.coroutines.*

/**
 * 通知栏监听服务。
 *
 * 监听支付宝、微信支付通知，自动记账。
 * 由于 NotificationListenerService 不支持 @AndroidEntryPoint（系统绑定机制原因），
 * 使用 EntryPointAccessors 手动获取 Hilt 依赖。
 */
class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 缓存 Hilt 依赖引用，避免每次通知都反射查找 */
    private var prefsManager: PrefsManager? = null
    private var transactionRepo: TransactionRepo? = null

    companion object {
        private const val TAG = "AutoBook-NotifListener"

        fun isNotificationListenerEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(context.packageName) == true
        }
    }

    /** 初始化 Hilt 依赖（在首次通知到来时懒加载，确保 Hilt 已就绪） */
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
            Log.e(TAG, "Hilt 依赖注入失败，延迟到下次通知重试", e)
            false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName

        // 非目标App直接跳过
        if (pkgName != NotificationParser.ALIPAY_PKG &&
            pkgName != NotificationParser.WECHAT_PKG &&
            pkgName != NotificationParser.UNIONPAY_PKG
        ) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val bestText = bigText.ifEmpty { text }

        if (title.isEmpty() && bestText.isEmpty()) return

        serviceScope.launch {
            try {
                // 懒加载依赖
                if (!ensureDeps()) return@launch

                // 检查开关
                if (!prefsManager!!.isNotificationEnabled()) {
                    Log.d(TAG, "通知监听功能未开启，跳过: $pkgName")
                    return@launch
                }

                // 解析
                val parsed = NotificationParser.parse(title, bestText, pkgName)
                if (parsed.transactionType == TransactionType.IGNORE) {
                    Log.d(TAG, "非交易通知已忽略: $pkgName | $title")
                    return@launch
                }

                // 入库
                val result = transactionRepo!!.processNotification(
                    amountFen = parsed.amountFen,
                    merchant = parsed.merchant,
                    timestamp = parsed.timestamp,
                    rawTitle = title,
                    rawText = bestText
                )

                if (result != null) {
                    Log.i(TAG, "记账成功: ${result.merchant} ${result.amountYuan}元 [${result.category.label}]")
                } else {
                    Log.d(TAG, "交易被去重: $pkgName | $title")
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理通知失败: pkg=$pkgName title=$title", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        prefsManager = null
        transactionRepo = null
        super.onDestroy()
    }
}
