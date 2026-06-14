package com.autobook.app.di

import com.autobook.app.data.repository.TransactionRepo
import com.autobook.app.domain.PrefsManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for NotificationListenerService。
 *
 * NotificationListenerService 不支持 @AndroidEntryPoint（系统绑定机制导致注入失败），
 * 因此通过 EntryPointAccessors 手动获取依赖。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationListenerEntryPoint {
    fun prefsManager(): PrefsManager
    fun transactionRepo(): TransactionRepo
}
