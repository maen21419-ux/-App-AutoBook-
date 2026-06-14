package com.autobook.app.data.local

import androidx.room.*
import com.autobook.app.data.model.Category
import com.autobook.app.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** 插入一条交易，返回自增 ID */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    /** 批量插入 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    /** 更新交易（主要用于修改分类/备注） */
    @Update
    suspend fun update(transaction: Transaction)

    /** 批量更新分类 */
    @Update
    suspend fun updateAll(transactions: List<Transaction>)

    /** 删除交易 */
    @Delete
    suspend fun delete(transaction: Transaction)

    // ────────────── 查询 ──────────────

    /** 所有交易，按时间倒序 */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    /** 分页查询 */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPaged(limit: Int, offset: Int): Flow<List<Transaction>>

    /** 按分类筛选 */
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: Category): Flow<List<Transaction>>

    /** 按时间范围查询 */
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp DESC")
    fun getByTimeRange(startMs: Long, endMs: Long): Flow<List<Transaction>>

    /** 搜索商户名/原始文本 */
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Transaction>>

    /** 今日交易 */
    @Query("SELECT * FROM transactions WHERE timestamp >= :todayStartMs ORDER BY timestamp DESC")
    fun getTodayTransactions(todayStartMs: Long): Flow<List<Transaction>>

    /** 今日总支出（分），排除退款和收入 */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :todayStartMs AND amount > 0 AND category != 'REFUND'")
    fun getTodayTotalExpense(todayStartMs: Long): Flow<Long>

    /** 本月总支出 */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :monthStartMs AND amount > 0 AND category != 'REFUND'")
    fun getMonthTotalExpense(monthStartMs: Long): Flow<Long>

    /** 本月分类汇总 */
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE timestamp >= :monthStartMs AND amount > 0 AND category != 'REFUND' GROUP BY category ORDER BY total DESC")
    fun getMonthCategorySummary(monthStartMs: Long): Flow<List<CategorySummary>>

    /** 近 30 天每日总额 */
    @Query("SELECT DATE(timestamp / 1000, 'unixepoch', 'localtime') as date, SUM(amount) as total FROM transactions WHERE timestamp >= :thirtyDaysAgoMs AND amount > 0 AND category != 'REFUND' GROUP BY date ORDER BY date ASC")
    fun getDailyTotals30Days(thirtyDaysAgoMs: Long): Flow<List<DailyTotal>>

    /** 查询待分类的交易 */
    @Query("SELECT * FROM transactions WHERE category = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingClassifications(): List<Transaction>

    /** 去重检查：指定时间窗口内同金额 + 同商户 */
    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND merchant = :merchant AND ABS(timestamp - :ts) <= :windowMs")
    suspend fun countDuplicate(amount: Long, merchant: String, ts: Long, windowMs: Long): Int

    /** 按 ID 查询 */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    /** 交易总数 */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

/** 分类汇总数据类 */
data class CategorySummary(
    val category: Category,
    val total: Long
)

/** 每日总额数据类 */
data class DailyTotal(
    val date: String,
    val total: Long
)
