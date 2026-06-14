package com.autobook.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.data.model.Category
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountYuan: Double, merchant: String, category: Category, timestamp: Long, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.OTHER) }
    var note by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }

    // 展示分类的支出类别（非收入/退款/待分类）
    val expenseCategories = Category.entries.filter {
        it !in listOf(Category.INCOME, Category.REFUND, Category.PENDING)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动记账", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 金额
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额（元）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("¥") }
                )

                // 商户
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("商户/描述") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 分类选择
                Text("分类", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    expenseCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text("${category.emoji} ${category.label}", fontSize = 13.sp) }
                        )
                    }
                }

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val yuan = amount.toDoubleOrNull()
                    if (yuan != null && yuan > 0 && merchant.isNotBlank()) {
                        onConfirm(yuan, merchant, selectedCategory, System.currentTimeMillis(), note)
                        onDismiss()
                    }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && merchant.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
