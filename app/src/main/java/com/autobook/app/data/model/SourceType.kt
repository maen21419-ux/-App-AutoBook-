package com.autobook.app.data.model

/** 交易来源类型 */
enum class SourceType {
    /** 银行短信自动识别 */
    SMS,
    /** 通知栏监听识别 (V2) */
    NOTIFICATION,
    /** 无障碍服务 (V2 兜底) */
    ACCESSIBILITY,
    /** 用户手动录入 */
    MANUAL,
    /** CSV 文件导入 */
    CSV_IMPORT
}
