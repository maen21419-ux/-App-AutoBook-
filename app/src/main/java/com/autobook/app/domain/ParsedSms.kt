package com.autobook.app.domain

/**
 * 短信解析结果。
 *
 * @param amountFen     交易金额（分），支出为正，收入为负
 * @param merchant      商户名称 / 交易描述
 * @param cardLast4     银行卡后四位
 * @param bankName      发卡行名称
 * @param transactionType 交易类型
 * @param timestamp     交易时间戳（毫秒），null 则使用短信接收时间
 */
data class ParsedSms(
    val amountFen: Long,
    val merchant: String,
    val cardLast4: String?,
    val bankName: String?,
    val transactionType: TransactionType,
    val timestamp: Long?   // null = 使用收到短信的时间
)

enum class TransactionType {
    /** 消费支出 */
    EXPENSE,
    /** 收入/入账 */
    INCOME,
    /** 退款 */
    REFUND,
    /** 非交易短信，忽略 */
    IGNORE
}
