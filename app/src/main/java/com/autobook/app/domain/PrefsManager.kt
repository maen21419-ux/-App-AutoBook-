package com.autobook.app.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autobook_prefs")

/**
 * 偏好设置管理。
 *
 * 安全分层：
 * - API Key → EncryptedSharedPreferences（AES-256 加密，主密钥由 Android Keystore 保护）
 * - 其他开关 → DataStore（明文，仅功能开关无敏感数据）
 */
@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // ── EncryptedSharedPreferences keys ──
        private const val SECURE_PREFS_NAME = "autobook_secure_prefs"
        private const val KEY_API_KEY = "deepseek_api_key"

        // ── DataStore keys ──
        private val KEY_SMS_ENABLED = booleanPreferencesKey("sms_enabled")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_ACCESSIBILITY_ENABLED = booleanPreferencesKey("accessibility_enabled")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_DEDUP_WINDOW_SEC = intPreferencesKey("dedup_window_sec")
    }

    // ═══════════════════════════════════════
    //  加密存储层（敏感数据）
    // ═══════════════════════════════════════

    private val securePrefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            SECURE_PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ──── API Key ────

    /** 读取 API Key（加密存储） */
    val apiKeyFlow: Flow<String?> = context.dataStore.data.map {
        // Flow 只能从 DataStore 触发，但 API Key 实际存在 securePrefs。
        // 这里返回一个基于 DataStore 的派生 flow（外部调用者可订阅更新），
        // 实际读取仍走 securePrefs。
        getApiKeySync()
    }

    suspend fun getApiKey(): String? {
        return getApiKeySync()
    }

    /** 保存 API Key（AES-256 加密落盘） */
    suspend fun setApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
        // 触发 DataStore 更新以通知 Flow 订阅者
        context.dataStore.edit { /* touch */ }
    }

    private fun getApiKeySync(): String? {
        return try {
            securePrefs.getString(KEY_API_KEY, null)
        } catch (e: Exception) {
            // 解密失败（如主密钥损坏）返回 null
            null
        }
    }

    // ═══════════════════════════════════════
    //  DataStore 层（非敏感设置）
    // ═══════════════════════════════════════

    // ──── 短信监听 ────

    suspend fun isSmsEnabled(): Boolean {
        return context.dataStore.data.first()[KEY_SMS_ENABLED] ?: true
    }

    suspend fun setSmsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SMS_ENABLED] = enabled }
    }

    // ──── 通知监听 ────

    suspend fun isNotificationEnabled(): Boolean {
        return context.dataStore.data.first()[KEY_NOTIFICATION_ENABLED] ?: false
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_NOTIFICATION_ENABLED] = enabled }
    }

    // ──── 首次启动 ────

    suspend fun isFirstLaunch(): Boolean {
        return context.dataStore.data.first()[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { prefs -> prefs[KEY_FIRST_LAUNCH] = false }
    }

    // ──── 无障碍监听 ────

    suspend fun isAccessibilityEnabled(): Boolean {
        return context.dataStore.data.first()[KEY_ACCESSIBILITY_ENABLED] ?: false
    }

    suspend fun setAccessibilityEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCESSIBILITY_ENABLED] = enabled }
    }

    // ──── 去重窗口（秒）───

    suspend fun getDedupWindowSec(): Int {
        return context.dataStore.data.first()[KEY_DEDUP_WINDOW_SEC] ?: 300 // 默认 5 分钟
    }

    suspend fun setDedupWindowSec(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_DEDUP_WINDOW_SEC] = seconds }
    }
}
