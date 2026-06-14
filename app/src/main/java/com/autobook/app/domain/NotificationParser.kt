package com.autobook.app.domain

import java.text.SimpleDateFormat
import java.util.*

/**
 * 通知栏交易解析引擎（支付宝 / 微信支付）。
 *
 * 解析流程：
 * 1. 过滤非交易通知（验证码、登录提醒、社交消息等）
 * 2. 提取金额（元 → 分）
 * 3. 判断交易类型（支出/收入/退款）
 * 4. 提取商户名
 * 5. 提取交易时间（兜底使用通知时间）
 */
object NotificationParser {

    // Package names (shared with NotificationListener)
    const val ALIPAY_PKG = "com.eg.android.AlipayGphone"
    const val WECHAT_PKG = "com.tencent.mm"
    const val UNIONPAY_PKG = "com.unionpay"

    // ──── 金额正则（按优先级） ────
    // 微信/支付宝通知常见的金额格式
    private val AMOUNT_PATTERNS = listOf(
        // "支付金额 ¥12.34" / "支付金额 12.34元" / "支付金额：12.34"
        Regex("""支付金额[：:\s]*¥?\s*(\d+\.?\d{0,2})"""),
        // "¥12.34" (最常见)
        Regex("""¥\s*(\d+\.?\d{0,2})"""),
        // "消费 ¥12.34"
        Regex("""消费\s*¥?\s*(\d+\.?\d{0,2})"""),
        // "付款成功 12.34元"
        Regex("""付款[成功已]*\s*(\d+\.?\d{0,2})\s*元"""),
        // "交易金额 ¥12.34"
        Regex("""交易金额[：:\s]*¥?\s*(\d+\.?\d{0,2})"""),
        // "XX元" 通用兜底
        Regex("""(\d+\.?\d{0,2})\s*元"""),
    )

    // ──── 商户提取模式 ────
    private val MERCHANT_PATTERNS = listOf(
        // "收款方: XXX" / "收款方：XXX"
        Regex("""收款方[：:]\s*(.+?)(?:\n|$)"""),
        // "交易商户: XXX"
        Regex("""交易商户[：:]\s*(.+?)(?:\n|$)"""),
        // "商户名称: XXX" / "商户：XXX"
        Regex("""商户(?:名称)?[：:]\s*(.+?)(?:\n|$)"""),
        // "向XXX付款"
        Regex("""向\s*(.+?)\s*付款"""),
        // "XXX 向你收款" (opposite direction)
        Regex("""(.+?)\s*向[你您]收款"""),
        // "在XXX消费"
        Regex("""在\s*(.+?)\s*消费"""),
        // 商户名后跟动作: "XXX消费..." / "XXX支付..." (非开头锚定)
        Regex("""(.+?)(?:消费|支付|交易)\s*\d"""),
    )

    // ──── 日期时间 ────
    private val DATE_TIME_FULL = Regex("""(\d{4})[-/年](\d{1,2})[-/月](\d{1,2})[日\s]*\s*(\d{1,2}):(\d{2})""")
    private val DATE_ONLY = Regex("""(\d{1,2})月(\d{1,2})日""")
    private val TIME_ONLY = Regex("""(\d{1,2}):(\d{2})""")

    // ──── 退款关键词 ────
    private val REFUND_KEYWORDS = listOf("退款", "退货", "撤销", "冲正", "退回", "返还")

    // ──── 收入关键词 ────
    private val INCOME_KEYWORDS = listOf(
        "收款", "到账", "转入", "收到转账", "收到红包", "转账给你",
        "入账", "收入", "报销到账"
    )

    // ──── 过滤关键词 ────
    // 非交易通知：登录、验证、安全、社交消息等
    // 注意：不含"消息"（过于宽泛，会误杀支付通知）
    private val FILTER_KEYWORDS = listOf(
        "验证码", "校验码", "动态码", "短信验证", "登录验证",
        "登录", "安全提醒", "安全检测", "账号保护",
        "语音", "视频通话", "视频邀请", "语音通话",
        "蚂蚁森林", "蚂蚁庄园", "芝麻信用", "积分",
        "你已成功", "设置成功", "修改成功", "绑定成功",
        "活动", "优惠", "福利", "红包雨",
        "群聊", "群发", "公众号",
    )

    // ──── 微信支付专属：标题关键词（用于判断是否为支付通知） ────
    private val WECHAT_PAY_TITLES = listOf(
        "微信支付", "支付凭证", "转账通知", "红包", "退款",
    )

    // ──── 支付宝专属：标题关键词 ────
    private val ALIPAY_PAY_TITLES = listOf(
        "支付", "付款", "转账", "退款", "消费", "收款",
        "交易提醒", "账单", "到账", "入账", "支付宝",
    )

    // ──── 银联专属：标题关键词 ────
    private val UNIONPAY_PAY_TITLES = listOf(
        "支付", "付款", "消费", "交易", "转账", "退款", "到账",
    )

    /**
     * 解析通知栏消息，返回交易信息；若非交易通知则返回 IGNORE 类型。
     *
     * @param title 通知标题 (android.title)
     * @param text  通知正文 (android.text / android.bigText)
     * @param packageName 来源 App 包名
     */
    fun parse(title: String, text: String, packageName: String): ParsedNotification {
        val combined = "$title | $text"

        // ── 1. 过滤 ──
        if (shouldIgnore(title, text, packageName)) {
            return ParsedIgnored
        }

        // ── 2. 提取金额 ──
        val amountYuan = extractAmount(combined) ?: return ParsedIgnored

        // ── 3. 判断交易类型 ──
        val transType = detectTransactionType(combined)

        // ── 4. 提取商户 ──
        val merchant = extractMerchant(combined, packageName)

        // ── 5. 提取时间 ──
        val ts = extractTimestamp(combined)

        // 金额正负：支出为正，收入/退款为负
        val finalAmountFen = when (transType) {
            TransactionType.INCOME, TransactionType.REFUND -> -Math.round(amountYuan * 100)
            TransactionType.EXPENSE -> Math.round(amountYuan * 100)
            TransactionType.IGNORE -> 0
        }

        return ParsedNotification(
            amountFen = finalAmountFen,
            merchant = merchant,
            transactionType = transType,
            timestamp = ts
        )
    }

    // ══════════════════════════════════════════════
    //  私有方法
    // ══════════════════════════════════════════════

    /** 是否应忽略此通知 */
    private fun shouldIgnore(title: String, text: String, pkgName: String): Boolean {
        val combined = "$title $text"

        // 过短的消息大概率不是交易通知
        if (combined.length < 4) return true

        // 通用过滤关键词
        if (FILTER_KEYWORDS.any { it in combined }) return true

        // 平台专属：检查标题/正文是否与支付相关
        return when (pkgName) {
            WECHAT_PKG -> {
                !WECHAT_PAY_TITLES.any { title.contains(it) || text.contains(it) }
            }
            ALIPAY_PKG -> {
                !ALIPAY_PAY_TITLES.any { title.contains(it) || text.contains(it) }
            }
            UNIONPAY_PKG -> {
                !UNIONPAY_PAY_TITLES.any { title.contains(it) || text.contains(it) }
            }
            else -> false
        }
    }

    /** 从通知文本提取金额（元） */
    private fun extractAmount(combined: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            pattern.find(combined)?.let { match ->
                val amountStr = match.groupValues[1]
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0 && amount < 1_000_000) {
                    // 过滤掉明显不是交易金额的大数字（如订单号）
                    return amount
                }
            }
        }
        return null
    }

    /** 判断交易类型 */
    private fun detectTransactionType(combined: String): TransactionType {
        // 退款优先判断
        if (REFUND_KEYWORDS.any { it in combined }) {
            return TransactionType.REFUND
        }
        // 收入
        if (INCOME_KEYWORDS.any { it in combined }) {
            return TransactionType.INCOME
        }
        // 默认消费支出
        return TransactionType.EXPENSE
    }

    /** 提取商户名 */
    private fun extractMerchant(combined: String, pkgName: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            pattern.find(combined)?.let { match ->
                val raw = match.groupValues[1].trim()
                    .replace(Regex("""[\n\r]+.*"""), "") // 去掉换行后的内容
                    .trim()
                if (raw.length in 2..30 &&
                    // 排除一些明显不是商户名的匹配
                    !raw.contains("支付") &&
                    !raw.matches(Regex("""^\d+\.?\d*$""")) // 纯数字
                ) {
                    return raw
                }
            }
        }

        // 兜底：根据 App + 文本指纹返回唯一名称，避免不同交易的去重冲突
        val textFingerprint = combined.replace(Regex("""\s+"""), "").take(8).hashCode().toString(16)
        return when (pkgName) {
            WECHAT_PKG -> "微信支付-$textFingerprint"
            ALIPAY_PKG -> "支付宝-$textFingerprint"
            else -> "支付-$textFingerprint"
        }
    }

    /** 提取交易时间戳（毫秒），null 表示使用通知接收时间 */
    private fun extractTimestamp(combined: String): Long? {
        val now = Calendar.getInstance()

        // 完整日期时间: 2024-01-15 14:30
        DATE_TIME_FULL.find(combined)?.let { match ->
            val (year, month, day, hour, minute) = match.groupValues.drop(1).map { it.toInt() }
            now.set(Calendar.YEAR, year)
            now.set(Calendar.MONTH, month - 1)
            now.set(Calendar.DAY_OF_MONTH, day)
            now.set(Calendar.HOUR_OF_DAY, hour)
            now.set(Calendar.MINUTE, minute)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            return now.timeInMillis
        }

        // 仅日期（无时间）
        DATE_ONLY.find(combined)?.let { match ->
            val (month, day) = match.groupValues.drop(1).map { it.toInt() }
            now.set(Calendar.MONTH, month - 1)
            now.set(Calendar.DAY_OF_MONTH, day)
            now.set(Calendar.HOUR_OF_DAY, 12)
            now.set(Calendar.MINUTE, 0)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            return now.timeInMillis
        }

        // 仅时间（假设今天）
        TIME_ONLY.find(combined)?.let { match ->
            val (hour, minute) = match.groupValues.drop(1).map { it.toInt() }
            now.set(Calendar.HOUR_OF_DAY, hour)
            now.set(Calendar.MINUTE, minute)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            return now.timeInMillis
        }

        return null
    }

    // 预定义的 IGNORE 结果，避免重复 new
    private val ParsedIgnored = ParsedNotification(
        amountFen = 0,
        merchant = "",
        transactionType = TransactionType.IGNORE,
        timestamp = null
    )
}

/**
 * 通知栏解析结果。
 *
 * @param amountFen      交易金额（分），支出为正，收入为负
 * @param merchant       商户名称 / 交易描述
 * @param transactionType 交易类型
 * @param timestamp      交易时间戳（毫秒），null 则使用通知接收时间
 */
data class ParsedNotification(
    val amountFen: Long,
    val merchant: String,
    val transactionType: TransactionType,
    val timestamp: Long?
)
