package com.autobook.app.domain

import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务界面解析引擎（微信 / 支付宝支付结果页）。
 *
 * 捕获扫码付款等不产生通知栏消息的交易场景。
 *
 * 工作原理：
 * 1. AccessibilityService 检测到微信/支付宝窗口变化
 * 2. 从根节点遍历视图树
 * 3. 查找"支付成功"关键词 → 确认在支付结果页
 * 4. 提取金额（¥XX.XX）和商户名
 * 5. 返回 ParsedNotification 交给 TransactionRepo 入库
 */
object AccessibilityParser {

    // ──── 支付成功关键词（确认在支付结果页） ────
    private val PAY_SUCCESS_KEYWORDS = listOf(
        "支付成功", "付款成功", "交易成功", "支付完成",
        "付款完成", "交易完成", "扣款成功",
    )

    // ──── 金额格式 ────
    private val AMOUNT_REGEX = Regex("""[¥￥]\s*(\d+\.?\d{0,2})""")
    private val AMOUNT_REGEX2 = Regex("""(\d+\.\d{2})\s*元""")

    // ──── 退款关键词 ────
    private val REFUND_KEYWORDS = listOf("退款", "退货", "撤销", "冲正", "退回", "返还")

    // ──── 收入关键词 ────
    private val INCOME_KEYWORDS = listOf("收款", "到账", "转入", "收到转账", "收到红包")

    /**
     * 从视图树解析支付结果。
     *
     * @param rootNode 无障碍事件的根节点
     * @param packageName 来源 App 包名
     * @return ParsedNotification，若无法识别则返回 ParsedIgnored
     */
    fun parse(rootNode: AccessibilityNodeInfo?, packageName: String): ParsedNotification {
        if (rootNode == null) return ParsedIgnored

        // 收集页面所有文本
        val allTexts = mutableListOf<String>()
        collectTexts(rootNode, allTexts)
        val combined = allTexts.joinToString(" | ")

        // ── 1. 确认在支付结果页 ──
        if (!isPaymentResultPage(combined)) {
            return ParsedIgnored
        }

        // ── 2. 提取金额 ──
        val amountYuan = extractAmount(allTexts) ?: return ParsedIgnored

        // ── 3. 判断交易类型 ──
        val transType = detectTransactionType(combined)

        // ── 4. 提取商户 ──
        val merchant = extractMerchant(allTexts, packageName, combined)

        // ── 5. 计算金额（支出为正，收入为负） ──
        val finalAmountFen = when (transType) {
            TransactionType.INCOME, TransactionType.REFUND -> -Math.round(amountYuan * 100)
            TransactionType.EXPENSE -> Math.round(amountYuan * 100)
            TransactionType.IGNORE -> 0
        }

        return ParsedNotification(
            amountFen = finalAmountFen,
            merchant = merchant,
            transactionType = transType,
            timestamp = null // 使用通知接收时间
        )
    }

    // ══════════════════════════════════════════════
    //  私有方法
    // ══════════════════════════════════════════════

    /** 递归收集页面所有 TextView 文本 */
    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>) {
        val text = node.text?.toString()?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim() ?: ""

        if (text.isNotEmpty() && text !in out) {
            out.add(text)
        }
        if (contentDesc.isNotEmpty() && contentDesc != text && contentDesc !in out) {
            out.add(contentDesc)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, out)
            child.recycle()
        }
    }

    /** 检查是否为支付结果页 */
    private fun isPaymentResultPage(combined: String): Boolean {
        return PAY_SUCCESS_KEYWORDS.any { combined.contains(it) }
    }

    /** 提取金额（元） */
    private fun extractAmount(texts: List<String>): Double? {
        // 优先查找 ¥XX.XX 格式（金额特征最明显）
        for (text in texts) {
            AMOUNT_REGEX.find(text)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null && amount > 0 && amount < 1_000_000) {
                    return amount
                }
            }
        }
        // 兜底：XX.XX元 格式
        for (text in texts) {
            AMOUNT_REGEX2.find(text)?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull()
                if (amount != null && amount > 0 && amount < 1_000_000) {
                    return amount
                }
            }
        }
        return null
    }

    /** 判断交易类型 */
    private fun detectTransactionType(combined: String): TransactionType {
        if (REFUND_KEYWORDS.any { it in combined }) {
            return TransactionType.REFUND
        }
        if (INCOME_KEYWORDS.any { it in combined }) {
            return TransactionType.INCOME
        }
        return TransactionType.EXPENSE
    }

    /** 提取商户名 */
    private fun extractMerchant(
        texts: List<String>,
        pkgName: String,
        combined: String
    ): String {
        // ── 策略：金额之前的文本通常是商户名 ──
        val amountIndex = texts.indexOfFirst {
            AMOUNT_REGEX.containsMatchIn(it) || AMOUNT_REGEX2.containsMatchIn(it)
        }
        if (amountIndex > 0) {
            // 金额前面的文本可能包含商户信息
            val candidate = texts[amountIndex - 1]
            if (candidate.length in 2..30 &&
                !candidate.contains("¥") &&
                !candidate.contains("￥") &&
                !candidate.matches(Regex("""^\d+\.?\d*$"""))
            ) {
                return candidate
            }
        }

        // ── 策略：查找常见商户标签后的文本 ──
        val merchantLabels = listOf("收款方", "商户", "交易商户", "收款商户", "付款给")
        for (text in texts) {
            for (label in merchantLabels) {
                if (label in text && text.length > text.indexOf(label) + label.length) {
                    val raw = text.substringAfter(label).trim()
                        .replace(Regex("""^[：:\s]+"""), "")
                        .take(20)
                    if (raw.length in 2..30) return raw
                }
            }
        }

        // ── 兜底：App名 + 文本指纹 ──
        val textFingerprint = combined.replace(Regex("""\s+"""), "").take(8).hashCode().toString(16)
        return when (pkgName) {
            NotificationParser.WECHAT_PKG -> "微信支付-$textFingerprint"
            NotificationParser.ALIPAY_PKG -> "支付宝-$textFingerprint"
            else -> "支付-$textFingerprint"
        }
    }

    // 预定义的 IGNORE 结果
    private val ParsedIgnored = ParsedNotification(
        amountFen = 0,
        merchant = "",
        transactionType = TransactionType.IGNORE,
        timestamp = null
    )
}
