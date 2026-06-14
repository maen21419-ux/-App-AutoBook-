package com.autobook.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 交易记录实体。
 *
 * @param id          自增主键
 * @param amount      金额（单位：分），退款为负值
 * @param merchant    商户名称 / 交易描述
 * @param category    交易分类
 * @param source      数据来源
 * @param timestamp   交易时间戳（毫秒）
 * @param createdAt   记录创建时间
 * @param cardLast4   银行卡后四位，无则为 null
 * @param bankName    发卡行名称，无则为 null
 * @param rawText     原始短信/通知文本
 * @param note        用户备注
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["amount", "merchant", "timestamp"], unique = false)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val merchant: String,
    val category: Category = Category.PENDING,
    val source: SourceType = SourceType.SMS,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val cardLast4: String? = null,
    val bankName: String? = null,
    val rawText: String = "",
    val note: String = ""
) {
    /** 金额（元），用于显示 */
    val amountYuan: Double get() = amount / 100.0

    companion object {
        /** 从元转换为分 */
        fun yuanToFen(yuan: Double): Long = Math.round(yuan * 100)
    }
}
