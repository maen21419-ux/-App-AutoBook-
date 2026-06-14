package com.autobook.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autobook.app.R
import com.autobook.app.data.repository.TransactionRepo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * 短信处理前台服务。
 *
 * 职责：
 * 1. 接收 SmsReceiver 转发的短信
 * 2. 调用 TransactionRepo.processSms() 完成解析→去重→分类→入库
 * 3. 显示前台通知，防止被系统杀死
 */
@AndroidEntryPoint
class SmsProcessingService : Service() {

    @Inject
    lateinit var transactionRepo: TransactionRepo

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "AutoBook-SmsService"
        const val CHANNEL_ID = "autobook_sms_processing"
        const val CHANNEL_NAME = "记账服务"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 前台通知保活
        startForeground(NOTIFICATION_ID, buildNotification("记账服务运行中"))

        intent?.let {
            val body = it.getStringExtra(SmsReceiver.EXTRA_SMS_BODY)
            val sender = it.getStringExtra(SmsReceiver.EXTRA_SMS_SENDER)
            val timestamp = it.getLongExtra(SmsReceiver.EXTRA_SMS_TIMESTAMP, System.currentTimeMillis())

            if (body != null) {
                serviceScope.launch {
                    try {
                        val result = transactionRepo.processSms(body, sender ?: "未知", timestamp)

                        if (result != null) {
                            Log.i(TAG, "记账成功: ${result.merchant} ${result.amountYuan}元 [${result.category.label}]")

                            // 发送成功通知
                            showTransactionNotification(result.merchant, result.amountYuan, result.category.label)
                        } else {
                            Log.d(TAG, "短信被过滤/去重")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理短信失败", e)
                    }
                }
            }
        }

        // 处理完自动停止
        serviceScope.launch {
            delay(3000L)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "自动记账后台服务"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoBook")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun showTransactionNotification(merchant: String, amount: Double, category: String) {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("已记账 $category")
                .setContentText("$merchant ${"%.2f".format(amount)}元")
                .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Exception) {
            Log.w(TAG, "发送记账通知失败", e)
        }
    }
}
