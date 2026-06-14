package com.autobook.app.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.autobook.app.data.repository.TransactionRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker：定时重试 PENDING 分类。
 *
 * 触发条件：
 * - 定期执行（每 15 分钟）
 * - 网络恢复时
 */
@HiltWorker
class RetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepo: TransactionRepo
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "开始重试 PENDING 分类...")
        return try {
            transactionRepo.retryPendingClassifications()
            Log.d(TAG, "PENDING 分类重试完成")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "PENDING 分类重试失败", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutoBook-RetryWorker"
        private const val WORK_NAME = "pending_classification_retry"

        /** 调度定时重试和网络恢复重试 */
        fun schedule(context: Context) {
            // 定时重试：每 15 分钟
            val periodicRequest = PeriodicWorkRequestBuilder<RetryWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            // 网络恢复时立即触发
            val networkRequest = OneTimeWorkRequestBuilder<RetryWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
