package com.autobook.app.data.repository

import com.autobook.app.data.local.TransactionDao
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.SourceType
import com.autobook.app.data.model.Transaction
import com.autobook.app.domain.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易数据仓库。
 * 封装解析 → 去重 → 分类 → 入库的完整流程。
 */
@Singleton
class TransactionRepo @Inject constructor(
    private val dao: TransactionDao,
    private val classifier: TransactionClassifier,
    private val dedupChecker: DedupChecker
) {

    // ═══════════════════════════════
    //  查询
    // ═══════════════════════════════

    fun getAll(): Flow<List<Transaction>> = dao.getAll()

    fun getPaged(limit: Int, offset: Int): Flow<List<Transaction>> =
        dao.getPaged(limit, offset)

    fun getByCategory(category: Category): Flow<List<Transaction>> =
        dao.getByCategory(category)

    fun getByTimeRange(startMs: Long, endMs: Long): Flow<List<Transaction>> =
        dao.getByTimeRange(startMs, endMs)

    fun search(query: String): Flow<List<Transaction>> = dao.search(query)

    fun getTodayTransactions(todayStartMs: Long): Flow<List<Transaction>> =
        dao.getTodayTransactions(todayStartMs)

    fun getTodayTotalExpense(todayStartMs: Long): Flow<Long> =
        dao.getTodayTotalExpense(todayStartMs)

    fun getMonthTotalExpense(monthStartMs: Long): Flow<Long> =
        dao.getMonthTotalExpense(monthStartMs)

    fun getMonthCategorySummary(monthStartMs: Long) =
        dao.getMonthCategorySummary(monthStartMs)

    fun getDailyTotals30Days(thirtyDaysAgoMs: Long) =
        dao.getDailyTotals30Days(thirtyDaysAgoMs)

    suspend fun getById(id: Long): Transaction? = dao.getById(id)

    // ═══════════════════════════════
    //  写入
    // ═══════════════════════════════

    /**
     * 处理短信 → 解析 → 去重 → 分类 → 入库。
     *
     * @param body 短信正文
     * @param sender 发送号码
     * @param receivedTimestamp 接收时间（ms）
     * @return 入库的 Transaction，若被去重/过滤则返回 null
     */
    suspend fun processSms(
        body: String,
        sender: String,
        receivedTimestamp: Long
    ): Transaction? {
        // 1. 解析
        val parsed = SmsParser.parse(body, sender, receivedTimestamp)
        if (parsed.transactionType == TransactionType.IGNORE) return null

        // 2. 去重
        val ts = parsed.timestamp ?: receivedTimestamp
        if (dedupChecker.isDuplicate(dao, parsed.amountFen, parsed.merchant, ts)) {
            return null
        }

        // 3. 构建实体 → 入库 → 分类
        val transaction = Transaction(
            amount = parsed.amountFen,
            merchant = parsed.merchant,
            category = Category.PENDING,
            source = SourceType.SMS,
            timestamp = ts,
            cardLast4 = parsed.cardLast4,
            bankName = parsed.bankName,
            rawText = body
        )

        return saveAndClassify(transaction)
    }

    /**
     * 处理通知栏交易 → 去重 → 分类 → 入库。
     *
     * @param amountFen    金额（分），支出为正，收入为负（已由 Parser 处理符号）
     * @param merchant     商户名称
     * @param timestamp    交易时间戳（null = 使用当前时间）
     * @param rawTitle     通知标题
     * @param rawText      通知正文
     * @return 入库的 Transaction，若被去重/过滤则返回 null
     */
    suspend fun processNotification(
        amountFen: Long,
        merchant: String,
        timestamp: Long?,
        rawTitle: String,
        rawText: String
    ): Transaction? {
        val ts = timestamp ?: System.currentTimeMillis()

        // 1. 去重
        if (dedupChecker.isDuplicate(dao, amountFen, merchant, ts)) {
            return null
        }

        // 2. 构建实体 → 入库 → 分类
        val transaction = Transaction(
            amount = amountFen,
            merchant = merchant,
            category = Category.PENDING,
            source = SourceType.NOTIFICATION,
            timestamp = ts,
            cardLast4 = null,
            bankName = null,
            rawText = "$rawTitle | $rawText"
        )

        return saveAndClassify(transaction)
    }

    /** 私有：入库 + 分类，返回完整 Transaction */
    private suspend fun saveAndClassify(transaction: Transaction): Transaction? {
        val id = dao.insert(transaction)
        if (id == -1L) return null

        val saved = transaction.copy(id = id)
        val category = classifier.classify(saved)
        val updated = saved.copy(category = category)
        dao.update(updated)
        return updated
    }

    /**
     * 手动录入一笔交易。
     */
    suspend fun addManual(
        amountYuan: Double,
        merchant: String,
        category: Category,
        timestamp: Long,
        note: String = ""
    ): Transaction {
        val tx = Transaction(
            amount = Transaction.yuanToFen(amountYuan),
            merchant = merchant,
            category = category,
            source = SourceType.MANUAL,
            timestamp = timestamp,
            note = note
        )
        val id = dao.insert(tx)
        return tx.copy(id = id)
    }

    /**
     * 更新交易分类。
     */
    suspend fun updateCategory(transaction: Transaction, newCategory: Category) {
        dao.update(transaction.copy(category = newCategory))
    }

    /**
     * 更新备注。
     */
    suspend fun updateNote(transaction: Transaction, note: String) {
        dao.update(transaction.copy(note = note))
    }

    /**
     * 删除交易。
     */
    suspend fun delete(transaction: Transaction) {
        dao.delete(transaction)
    }

    /**
     * 重试所有 PENDING 分类。
     */
    suspend fun retryPendingClassifications() {
        classifier.retryPending(dao)
    }
}
