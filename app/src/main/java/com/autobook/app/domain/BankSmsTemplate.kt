package com.autobook.app.domain

/**
 * 银行短信模板注册表。
 * 包含国内主要银行的短信发送号码、签名文本和特殊解析规则。
 */
object BankSmsRegistry {

    data class BankInfo(
        val name: String,               // 银行中文名
        val senderNumbers: List<String>, // 短信发送号码（短号）
        val signatures: List<String>,    // 短信签名（【】内的文本）
        val cardPrefixes: List<String>   // 银行卡 BIN 前缀（可选，用于识别）
    )

    /** 国内主要银行列表 */
    val BANKS: List<BankInfo> = listOf(
        BankInfo(
            name = "工商银行",
            senderNumbers = listOf("95588"),
            signatures = listOf("工商银行", "ICBC"),
            cardPrefixes = listOf("6222", "6212")
        ),
        BankInfo(
            name = "建设银行",
            senderNumbers = listOf("95533"),
            signatures = listOf("建设银行", "CCB"),
            cardPrefixes = listOf("6227", "6217")
        ),
        BankInfo(
            name = "农业银行",
            senderNumbers = listOf("95599"),
            signatures = listOf("农业银行", "ABC"),
            cardPrefixes = listOf("6228", "6212")
        ),
        BankInfo(
            name = "中国银行",
            senderNumbers = listOf("95566"),
            signatures = listOf("中国银行", "BOC"),
            cardPrefixes = listOf("6216", "6217")
        ),
        BankInfo(
            name = "招商银行",
            senderNumbers = listOf("95555"),
            signatures = listOf("招商银行", "招行", "CMB"),
            cardPrefixes = listOf("6225", "6214")
        ),
        BankInfo(
            name = "交通银行",
            senderNumbers = listOf("95559"),
            signatures = listOf("交通银行", "交行", "BOCOM"),
            cardPrefixes = listOf("6222", "6210")
        ),
        BankInfo(
            name = "邮储银行",
            senderNumbers = listOf("95580"),
            signatures = listOf("邮储银行", "邮政储蓄", "PSBC"),
            cardPrefixes = listOf("6221", "6217")
        ),
        BankInfo(
            name = "中信银行",
            senderNumbers = listOf("95558"),
            signatures = listOf("中信银行", "中信", "CITIC"),
            cardPrefixes = listOf("6226", "6217")
        ),
        BankInfo(
            name = "光大银行",
            senderNumbers = listOf("95595"),
            signatures = listOf("光大银行", "光大", "CEB"),
            cardPrefixes = listOf("6226", "6214")
        ),
        BankInfo(
            name = "民生银行",
            senderNumbers = listOf("95568"),
            signatures = listOf("民生银行", "民生", "CMBC"),
            cardPrefixes = listOf("6226", "6216")
        ),
        BankInfo(
            name = "浦发银行",
            senderNumbers = listOf("95528"),
            signatures = listOf("浦发银行", "浦发", "SPDB"),
            cardPrefixes = listOf("6225", "6217")
        ),
        BankInfo(
            name = "兴业银行",
            senderNumbers = listOf("95561"),
            signatures = listOf("兴业银行", "兴业", "CIB"),
            cardPrefixes = listOf("6229", "6210")
        ),
        BankInfo(
            name = "平安银行",
            senderNumbers = listOf("95511"),
            signatures = listOf("平安银行", "平安", "PAB"),
            cardPrefixes = listOf("6225", "6216")
        ),
        BankInfo(
            name = "广发银行",
            senderNumbers = listOf("95508"),
            signatures = listOf("广发银行", "广发", "CGB"),
            cardPrefixes = listOf("6225", "6214")
        ),
        BankInfo(
            name = "华夏银行",
            senderNumbers = listOf("95577"),
            signatures = listOf("华夏银行", "华夏", "HXB"),
            cardPrefixes = listOf("6226", "6212")
        )
    )

    /** 通过发送号码查找银行 */
    fun findBySender(sender: String): BankInfo? {
        val normalized = sender.trim().replace("+86", "")
        return BANKS.firstOrNull { bank ->
            bank.senderNumbers.any { it in normalized }
        }
    }

    /** 通过短信签名查找银行 */
    fun findBySignature(body: String): BankInfo? {
        return BANKS.firstOrNull { bank ->
            bank.signatures.any { sig -> sig in body }
        }
    }

    /** 通过正文综合判断银行 */
    fun detectBank(body: String, sender: String): BankInfo? {
        return findBySender(sender) ?: findBySignature(body)
    }
}
