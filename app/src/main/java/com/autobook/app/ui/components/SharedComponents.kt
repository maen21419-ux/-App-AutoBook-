package com.autobook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

/** 金额显示组件：支出红色，收入/退款绿色 */
@Composable
fun AmountText(amountFen: Long, fontSize: Int = 16) {
    val yuan = amountFen / 100.0
    val color = when {
        amountFen > 0 -> MaterialTheme.colorScheme.onSurface
        amountFen < 0 -> Color(0xFF4CAF50) // 收入/退款绿色
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = if (amountFen < 0) "+" else if (amountFen > 0) "-" else ""
    Text(
        text = "$prefix¥${
            "%.2f".format(kotlin.math.abs(yuan))
        }",
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

/** 分类图标 */
@Composable
fun CategoryIcon(category: Category, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(categoryColor(category).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = category.emoji, fontSize = (size * 0.5f).sp)
    }
}

/** 分类颜色 */
fun categoryColor(category: Category): Color = when (category) {
    Category.FOOD -> Color(0xFFFF7043)
    Category.TRANSPORT -> Color(0xFF42A5F5)
    Category.SHOPPING -> Color(0xFFAB47BC)
    Category.HOUSING -> Color(0xFF795548)
    Category.ENTERTAINMENT -> Color(0xFFFFCA28)
    Category.MEDICAL -> Color(0xFFEF5350)
    Category.EDUCATION -> Color(0xFF5C6BC0)
    Category.TELECOM -> Color(0xFF26A69A)
    Category.DAILY -> Color(0xFF66BB6A)
    Category.OTHER -> Color(0xFF78909C)
    Category.INCOME -> Color(0xFF4CAF50)
    Category.REFUND -> Color(0xFF4CAF50)
    Category.PENDING -> Color(0xFFBDBDBD)
}

/** 交易列表项 */
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(category = transaction.category, size = 44)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.category.label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.cardLast4 != null) {
                    Text(
                        text = " · 尾号${transaction.cardLast4}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AmountText(amountFen = transaction.amount, fontSize = 16)
    }
}

/** 日期格式化 */
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("M月d日", Locale.CHINESE)
    return sdf.format(Date(timestamp))
}

fun formatDateFull(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINESE)
    return sdf.format(Date(timestamp))
}

/** 获取当天零点的时间戳 */
fun getTodayStartMs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 获取当月1号零点的时间戳 */
fun getMonthStartMs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 获取30天前的零点时间戳 */
fun getThirtyDaysAgoMs(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -30)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** 空状态占位 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📝", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
