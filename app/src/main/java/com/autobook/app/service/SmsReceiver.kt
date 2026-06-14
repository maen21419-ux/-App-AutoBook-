package com.autobook.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.autobook.app.domain.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * 短信广播接收器。
 * 监听所有接收到的短信，提取交易信息并处理。
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefsManager: PrefsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (runBlocking { !prefsManager.isSmsEnabled() }) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        for (message in messages) {
            val body = message.messageBody ?: continue
            val sender = message.originatingAddress ?: continue
            val timestamp = message.timestampMillis

            Log.d(TAG, "收到短信: sender=$sender, body=${body.take(50)}...")

            // 启动前台服务处理
            val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
                putExtra(EXTRA_SMS_BODY, body)
                putExtra(EXTRA_SMS_SENDER, sender)
                putExtra(EXTRA_SMS_TIMESTAMP, timestamp)
            }
            context.startForegroundService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "AutoBook-SmsReceiver"
        const val EXTRA_SMS_BODY = "sms_body"
        const val EXTRA_SMS_SENDER = "sms_sender"
        const val EXTRA_SMS_TIMESTAMP = "sms_timestamp"
    }
}
