package com.autobook.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机自启接收器。
 * 设备重启后，确保短信监听服务保持活跃（不需要额外启动服务，SmsReceiver 由系统注册即可恢复）。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("AutoBook-BootReceiver", "设备已重启，SmsReceiver 自动恢复监听")
        // SmsReceiver 已在 AndroidManifest 中注册，系统会自动恢复，无需手动启动
    }
}
