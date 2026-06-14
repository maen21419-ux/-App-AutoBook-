package com.autobook.app.domain

import com.autobook.app.data.local.TransactionDao
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易分类器 — 三层递进策略。
 *
 * ```
 * 交易进来
 *   → 1. MerchantRuleEngine 精准匹配（本地、零延迟、零成本）
 *     → 命中 → 直接返回分类（约 80% 交易止于此）
 *   → 2. MerchantRuleEngine 关键词匹配（本地）
 *     → 命中 → 直接返回分类（约 10%）
 *   → 3. DeepSeek API 在线分类（约 10% 需要语义理解）
 *     → 成功 → 返回分类
 *     → 失败（断网/超时/API Error）→ PENDING，等待重试
 * ```
 */
@Singleton
class TransactionClassifier @Inject constructor(
    private val apiClient: DeepSeekApiClient,
    private val prefsManager: PrefsManager
) {

    /**
     * 对一笔交易进行分类。
     *
     * @return 分类结果（PENDING 表示需要后续重试）
     */
    suspend fun classify(transaction: Transaction): Category {
        // ── 第 1-2 层：本地规则引擎 ──
        val ruleResult = MerchantRuleEngine.classify(
            merchant = transaction.merchant,
            rawText = transaction.rawText
        )
        if (ruleResult != null) return ruleResult

        // ── 第 3 层：DeepSeek API ──
        val apiKey = prefsManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return Category.OTHER // 无 API Key，兜底为"其他"
        }

        val result = apiClient.classify(
            apiKey = apiKey,
            merchant = transaction.merchant,
            amountYuan = transaction.amountYuan,
            rawText = transaction.rawText
        )

        return if (result != null) {
            Category.fromLabel(result)
        } else {
            Category.PENDING // API 失败，标记待重试
        }
    }

    /**
     * 批量重试 PENDING 分类。
     * 由 WorkManager / RetryWorker 在网络恢复时触发。
     */
    suspend fun retryPending(dao: TransactionDao) {
        val pending = dao.getPendingClassifications()
        if (pending.isEmpty()) return

        val apiKey = prefsManager.getApiKey() ?: return

        for (tx in pending) {
            // 重试时同样先走规则引擎（可能有规则更新）
            val ruleResult = MerchantRuleEngine.classify(tx.merchant, tx.rawText)
            if (ruleResult != null) {
                dao.update(tx.copy(category = ruleResult))
                continue
            }

            val result = apiClient.classify(
                apiKey = apiKey,
                merchant = tx.merchant,
                amountYuan = tx.amountYuan,
                rawText = tx.rawText
            )
            if (result != null) {
                val category = Category.fromLabel(result)
                if (category != Category.PENDING) {
                    dao.update(tx.copy(category = category))
                }
            }
        }
    }
}
