package com.autobook.app.data.local

import androidx.room.TypeConverter
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.SourceType

/** Room 类型转换器：枚举 ↔ 字符串 */
class Converters {
    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category =
        Category.entries.firstOrNull { it.name == value } ?: Category.OTHER

    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType =
        SourceType.entries.firstOrNull { it.name == value } ?: SourceType.MANUAL
}
