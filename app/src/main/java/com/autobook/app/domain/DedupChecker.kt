package com.autobook.app.domain

import com.autobook.app.data.local.TransactionDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 去重检查器。
 *
 * 策略：在可配置的时间窗口内，相同金额 + 相同商户 → 视为重复。
 * 场景：银行可能对同一笔交易发送多条短信（消费提醒 + 余额变动），
 *       或者信号差时同一短信被重复投递。
 *
 * 默认窗口：5 分钟（300 秒），用户可在设置中调整。
 */
@Singleton
class DedupChecker @Inject constructor(
    private val prefsManager: PrefsManager
) {
    companion object {
        const val DEFAULT_WINDOW_SEC = 300     // 默认 5 分钟
        const val MIN_WINDOW_SEC = 30          // 最小 30 秒
        const val MAX_WINDOW_SEC = 3600        // 最大 1 小时
    }

    /**
     * 检查是否为重复交易。
     * @return true 表示重复，应丢弃
     */
    suspend fun isDuplicate(
        dao: TransactionDao,
        amountFen: Long,
        merchant: String,
        timestamp: Long
    ): Boolean {
        val windowSec = prefsManager.getDedupWindowSec()
            .coerceIn(MIN_WINDOW_SEC, MAX_WINDOW_SEC)
        val windowMs = windowSec * 1000L
        val count = dao.countDuplicate(
            amount = amountFen,
            merchant = merchant,
            ts = timestamp,
            windowMs = windowMs
        )
        return count > 0
    }
}
