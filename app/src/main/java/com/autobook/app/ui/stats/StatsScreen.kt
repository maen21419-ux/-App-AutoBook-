package com.autobook.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autobook.app.data.local.CategorySummary
import com.autobook.app.data.local.DailyTotal
import com.autobook.app.data.model.Category
import com.autobook.app.ui.components.categoryColor
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val monthExpense by viewModel.monthExpense.collectAsStateWithLifecycle()
    val categorySummary by viewModel.categorySummary.collectAsStateWithLifecycle()
    val dailyTotals by viewModel.dailyTotals.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (categorySummary.isEmpty()) {
            EmptyStatePlaceholder(title = "暂无统计数据", subtitle = "记账后会在这里看到图表分析")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                // 本月总额
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("本月支出", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(
                                "¥${"%.2f".format(monthExpense / 100.0)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 分类饼图
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("分类占比", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    PieChart(
                        data = categorySummary.map { it.category to it.total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }

                // 分类排行
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("分类排行", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(categorySummary) { item ->
                    CategoryRankItem(item)
                }

                // 近30天趋势
                if (dailyTotals.size >= 2) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("近30天趋势", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        LineChart(
                            data = dailyTotals.map { it.date to it.total },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════
//  饼图
// ═══════════════════════════════════════

@Composable
fun PieChart(
    data: List<Pair<Category, Long>>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.second }.toFloat()
    if (total == 0f) return

    val colors = data.map { categoryColor(it.first) }
    val sweepAngles = data.map { (it.second / total) * 360f }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 饼图
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        ) {
            var startAngle = -90f
            val strokeWidth = 40f
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2
            )
            val arcSize = Size(radius * 2, radius * 2)

            data.forEachIndexed { index, (_, value) ->
                val sweep = (value / total) * 360f
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                startAngle += sweep
            }

            // 内圆（甜甜圈效果）
            drawCircle(
                color = Color.White,
                radius = radius - strokeWidth
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 图例
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.take(6).forEachIndexed { index, (category, amount) ->
                val pct = if (total > 0) (amount / total * 100) else 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = colors[index])
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${category.emoji} ${category.label} ${"%.1f".format(pct)}%",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════
//  折线图
// ═══════════════════════════════════════

@Composable
fun LineChart(
    data: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.size < 2) return

    val maxVal = data.maxOf { it.second }.toFloat().coerceAtLeast(1f)
    val minVal = 0f

    Canvas(modifier = modifier) {
        val padding = 40f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2
        val stepX = chartWidth / (data.size - 1)

        // 绘制网格线
        for (i in 0..4) {
            val y = padding + chartHeight * i / 4
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1f
            )
        }

        // 绘制折线
        val path = Path()
        val points = data.mapIndexed { index, (_, value) ->
            val x = padding + index * stepX
            val y = padding + chartHeight * (1 - (value - minVal) / (maxVal - minVal))
            Offset(x, y)
        }

        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y)
            else path.lineTo(point.x, point.y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // 绘制数据点
        points.forEach { point ->
            drawCircle(color = lineColor, radius = 4f, center = point)
            drawCircle(color = Color.White, radius = 2f, center = point)
        }
    }
}

// ═══════════════════════════════════════
//  分类排行项
// ═══════════════════════════════════════

@Composable
fun CategoryRankItem(summary: CategorySummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = summary.category.emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = summary.category.label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
        Text(
            text = "¥${"%.2f".format(summary.total / 100.0)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStatePlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📊", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
