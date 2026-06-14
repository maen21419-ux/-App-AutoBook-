package com.autobook.app.data.model

/**
 * 交易分类枚举。
 * 映射 DeepSeek LLM 返回的分类标签。
 */
enum class Category(val label: String, val emoji: String) {
    FOOD("餐饮", "🍜"),
    TRANSPORT("交通", "🚇"),
    SHOPPING("购物", "🛒"),
    HOUSING("居住", "🏠"),
    ENTERTAINMENT("娱乐", "🎮"),
    MEDICAL("医疗", "🏥"),
    EDUCATION("教育", "📚"),
    TELECOM("通讯", "📱"),
    DAILY("日用", "🧴"),
    OTHER("其他", "💸"),
    INCOME("收入", "💰"),
    REFUND("退款", "↩️"),
    PENDING("待分类", "⏳");

    companion object {
        /** 从 LLM 返回的标签字符串解析分类 */
        fun fromLabel(label: String): Category {
            val cleaned = label.trim().lowercase()
            return entries.firstOrNull {
                it.label == label.trim() || it.name.lowercase() == cleaned
            } ?: OTHER
        }
    }
}
