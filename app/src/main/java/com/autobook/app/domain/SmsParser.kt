package com.autobook.app.domain

import java.text.SimpleDateFormat
import java.util.*

/**
 * 银行短信解析引擎。
 *
 * 解析流程：
 * 1. 过滤非交易短信（验证码、余额提醒、营销等）
 * 2. 提取金额（元 → 分）
 * 3. 判断交易类型（支出/收入/退款）
 * 4. 提取卡号后四位
 * 5. 识别发卡行
 * 6. 尝试提取商户名
 * 7. 提取交易时间（兜底使用短信接收时间）
 */
object SmsParser {

    // ──── 金额正则 ────
    // 匹配: "消费XX元", "支出XX元", "存入XX元", "退款XX元", "人民币XX元"
    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:消费|支出|扣款|支付|交易|付款)[^\d]*?(\d+\.?\d{0,2})\s*元"""),
        Regex("""存入[^\d]*?(\d+\.?\d{0,2})\s*元"""),
        Regex("""退款[^\d]*?(\d+\.?\d{0,2})\s*元"""),
        Regex("""入账[^\d]*?(\d+\.?\d{0,2})\s*元"""),
        Regex("""(?:转账|转入|汇入)[^\d]*?(\d+\.?\d{0,2})\s*元"""),
        Regex("""人民币\s*(\d+\.?\d{0,2})\s*元?"""),
        Regex("""(\d+\.?\d{0,2})\s*元\s*[，。,\.]"""),
        // 通用: "XX元" 作为最后兜底
        Regex("""(\d+\.?\d{0,2})\s*元""")
    )

    // ──── 卡号后四位 ────
    private val CARD_LAST4_PATTERN = Regex("""尾号\s*(\d{4})""")

    // ──── 银行签名 ────
    private val SIGNATURE_PATTERN = Regex("""【(.+?)】""")

    // ──── 日期时间 ────
    private val DATE_TIME_FULL = Regex("""(\d{1,2})月(\d{1,2})日\s*(\d{1,2}):(\d{2})""")
    private val DATE_ONLY = Regex("""(\d{1,2})月(\d{1,2})日""")
    private val TIME_ONLY = Regex("""(\d{1,2}):(\d{2}):?(\d{2})?""")

    // ──── 退款关键词 ────
    private val REFUND_KEYWORDS = listOf("退款", "退货", "撤销", "冲正", "退回", "返还")

    // ──── 收入关键词 ────
    private val INCOME_KEYWORDS = listOf(
        "存入", "入账", "转入", "转账存入", "汇款", "工资", "报销", "收款",
        "到账", "汇入"
    )

    // ──── 过滤关键词（验证码/提醒/营销） ────
    private val FILTER_KEYWORDS = listOf(
        "验证码", "校验码", "动态码", "短信验证", "登录验证",
        "余额", "账单", "还款提醒", "还款通知",
        "积分", "活动", "优惠", "推荐", "专享",
        "申请", "审核", "审批", "激活", "绑定",
        "注册", "签约", "开通", "关闭",
        "已寄出", "快递", "物流",
        "您已成功", "设置成功", "修改成功", "重置成功"
    )

    // ──── 商户提取模式 ────
    private val MERCHANT_PATTERNS = listOf(
        // 支付宝/微信识别
        Regex("""(支付宝|微信支付|财付通|微信|云闪付|京东支付|美团支付|拼多多|抖音支付)"""),
        // "在XXX消费"
        Regex("""在\s*(.+?)\s*(?:消费|支付|交易|付款)"""),
        // "XXX消费XX元"
        Regex("""(.+?)\s*(?:消费|支出|支付|交易)\s*(\d+\.?\d*)"""),
    )

    // ──── 支付宝/微信快捷支付商户提取 ────
    // 格式: "支付宝-XX商户消费XX元" 或 "财付通-XX商户"
    private val ALIPAY_WECHAT_MERCHANT = Regex("""(?:支付宝|微信支付|财付通|微信)[\-—]\s*(.+?)\s*(?:消费|支付|交易|$)""")

    // ──── 银行识别 ────
    // 给正则用的银行关键词列表
    private val BANK_KEYWORDS = listOf(
        "工商", "建设", "农业", "中国银行", "招商", "招行", "交通", "交行",
        "邮储", "邮政", "中信", "光大", "民生", "浦发", "兴业", "平安",
        "广发", "华夏", "北京银行", "上海银行", "宁波银行", "南京银行",
        "杭州银行", "江苏银行", "浙商", "渤海", "恒丰"
    )

    /**
     * 解析短信，返回交易信息；若为非交易短信则返回 IGNORE 类型。
     *
     * @param body 短信正文
     * @param sender 发送号码
     * @param receivedTimestamp 收到短信的时间（毫秒，作为兜底时间戳）
     */
    fun parse(body: String, sender: String, receivedTimestamp: Long): ParsedSms {
        // ── 1. 过滤 ──
        if (shouldIgnore(body)) {
            return ParsedSms(
                amountFen = 0,
                merchant = "",
                cardLast4 = null,
                bankName = null,
                transactionType = TransactionType.IGNORE,
                timestamp = null
            )
        }

        // ── 2. 提取金额 ──
        val amountYuan = extractAmount(body) ?: return ParsedSms(
            amountFen = 0,
            merchant = "",
            cardLast4 = null,
            bankName = null,
            transactionType = TransactionType.IGNORE,
            timestamp = null
        )

        // ── 3. 判断交易类型 ──
        val transType = detectTransactionType(body)

        // ── 4. 提取卡号 ──
        val cardLast4 = CARD_LAST4_PATTERN.find(body)?.groupValues?.getOrNull(1)

        // ── 5. 识别银行 ──
        val bankName = detectBank(body, sender)

        // ── 6. 提取商户 ──
        val merchant = extractMerchant(body)

        // ── 7. 提取时间 ──
        val ts = extractTimestamp(body, receivedTimestamp)

        // 金额正负：支出为正，收入/退款为负
        val finalAmountFen = when (transType) {
            TransactionType.INCOME, TransactionType.REFUND -> -Math.round(amountYuan * 100)
            TransactionType.EXPENSE -> Math.round(amountYuan * 100)
            TransactionType.IGNORE -> 0
        }

        return ParsedSms(
            amountFen = finalAmountFen,
            merchant = merchant.ifEmpty { bankName ?: "未知商户" },
            cardLast4 = cardLast4,
            bankName = bankName,
            transactionType = transType,
            timestamp = ts
        )
    }

    // ══════════════════════════════════════════════
    //  私有方法
    // ══════════════════════════════════════════════

    /** 是否应忽略此短信 */
    private fun shouldIgnore(body: String): Boolean {
        if (body.length < 5) return true
        return FILTER_KEYWORDS.any { it in body }
    }

    /** 从短信正文提取金额（元） */
    private fun extractAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            pattern.find(body)?.let { match ->
                val amountStr = match.groupValues[1]
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }
        return null
    }

    /** 判断交易类型 */
    private fun detectTransactionType(body: String): TransactionType {
        // 退款优先判断
        if (REFUND_KEYWORDS.any { it in body }) {
            return TransactionType.REFUND
        }
        // 收入
        if (INCOME_KEYWORDS.any { it in body }) {
            return TransactionType.INCOME
        }
        // 默认消费支出
        return TransactionType.EXPENSE
    }

    /** 识别发卡行 */
    private fun detectBank(body: String, sender: String): String? {
        // 优先通过发送号码匹配
        BankSmsRegistry.findBySender(sender)?.let { return it.name }

        // 通过签名匹配
        BankSmsRegistry.findBySignature(body)?.let { return it.name }

        // 通过正文中的银行关键词匹配
        SIGNATURE_PATTERN.find(body)?.let { match ->
            val sig = match.groupValues[1]
            BANK_KEYWORDS.firstOrNull { it in sig }?.let { kw ->
                // 尝试找到完整的银行名
                return BankSmsRegistry.BANKS.firstOrNull { bank ->
                    bank.signatures.any { s -> s in sig }
                }?.name ?: "${kw}银行"
            }
        }

        // 正文中直接查找银行关键词
        BANK_KEYWORDS.firstOrNull { it in body }?.let {
            return BankSmsRegistry.BANKS.firstOrNull { bank ->
                bank.signatures.any { s -> s in body }
            }?.name ?: "${it}银行"
        }

        return null
    }

    /** 提取商户名 */
    private fun extractMerchant(body: String): String {
        // 先检查支付宝/微信快捷支付
        ALIPAY_WECHAT_MERCHANT.find(body)?.let { match ->
            val raw = match.groupValues[1].trim()
            if (raw.length in 2..30) return raw
        }

        // 检查支付平台
        val platformMatch = MERCHANT_PATTERNS[0].find(body)
        if (platformMatch != null) {
            val platform = platformMatch.groupValues[1]
            // 尝试提取具体商户
            ALIPAY_WECHAT_MERCHANT.find(body)?.let { match ->
                val detail = match.groupValues[1].trim()
                if (detail.length in 2..30) return "$platform-$detail"
            }
            return platform
        }

        // "在XXX消费"
        MERCHANT_PATTERNS[1].find(body)?.let { match ->
            val m = match.groupValues[1].trim()
            if (m.length in 2..30) return m
        }

        // "XXX消费XX元"
        MERCHANT_PATTERNS[2].find(body)?.let { match ->
            val m = match.groupValues[1].trim()
            if (m.length in 2..30 && !m.any { it.isDigit() }) return m
        }

        // 兜底：返回卡片银行描述
        val bank = detectBank(body, "")
        if (bank != null) return "${bank}消费"

        return "未知消费"
    }

    /** 提取交易时间戳 */
    private fun extractTimestamp(body: String, fallback: Long): Long? {
        val now = Calendar.getInstance()

        // 完整日期时间
        DATE_TIME_FULL.find(body)?.let { match ->
            val (month, day, hour, minute) = match.groupValues.drop(1).map { it.toInt() }
            now.set(Calendar.MONTH, month - 1)
            now.set(Calendar.DAY_OF_MONTH, day)
            now.set(Calendar.HOUR_OF_DAY, hour)
            now.set(Calendar.MINUTE, minute)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            return now.timeInMillis
        }

        // 仅日期（无时间）
        DATE_ONLY.find(body)?.let { match ->
            val (month, day) = match.groupValues.drop(1).map { it.toInt() }
            now.set(Calendar.MONTH, month - 1)
            now.set(Calendar.DAY_OF_MONTH, day)
            now.set(Calendar.HOUR_OF_DAY, 12) // 默认中午
            now.set(Calendar.MINUTE, 0)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            return now.timeInMillis
        }

        // 仅时间（假设今天）
        TIME_ONLY.find(body)?.let { match ->
            val (hour, minute) = match.groupValues.drop(1).take(2).map { it.toInt() }
            now.set(Calendar.HOUR_OF_DAY, hour)
            now.set(Calendar.MINUTE, minute)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            return now.timeInMillis
        }

        // 兜底：null 表示使用收到短信的时间
        return null
    }
}
