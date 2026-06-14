package com.autobook.app.domain

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** DeepSeek Chat API 客户端 */
@Singleton
class DeepSeekApiClient @Inject constructor() {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://api.deepseek.com/v1/chat/completions"
        private const val MODEL = "deepseek-chat"
    }

    /**
     * 调用 DeepSeek API 对交易分类。
     *
     * @param apiKey  用户提供的 API Key
     * @param merchant 商户名
     * @param amountYuan 金额（元）
     * @param rawText 原始短信文本
     * @return 分类标签字符串，如 "餐饮"、"交通" 等；失败返回 null
     */
    suspend fun classify(
        apiKey: String,
        merchant: String,
        amountYuan: Double,
        rawText: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildString {
                append("你是一个记账分类助手。根据用户的消费信息，将交易归类到以下类别之一：\n")
                append("餐饮、交通、购物、居住、娱乐、医疗、教育、通讯、日用、收入、退款、其他\n\n")
                append("分类规则：\n")
                append("- 餐饮：餐厅、外卖、咖啡、奶茶、零食、水果、买菜\n")
                append("- 交通：打车、公交、地铁、加油、停车、高速费、共享单车\n")
                append("- 购物：衣服、数码、家电、化妆品、网购（非食品）\n")
                append("- 居住：房租、房贷、物业、水电、燃气、维修\n")
                append("- 娱乐：电影、游戏、旅游、KTV、运动健身、演出\n")
                append("- 医疗：医院、药房、体检、挂号\n")
                append("- 教育：学费、培训、书籍、文具\n")
                append("- 通讯：话费、宽带、流量\n")
                append("- 日用：超市、便利店、理发、洗衣、日用品\n")
                append("- 收入：工资、报销、退款到账、转账存入\n")
                append("- 退款：消费退款、退货退款\n")
                append("- 其他：无法明确归类的消费\n\n")
                append("只回复分类名称，不要解释。例如：餐饮")
            }

            val userMessage = buildString {
                append("商户：$merchant\n")
                append("金额：${"%.2f".format(amountYuan)}元\n")
                append("原始短信：${rawText.take(200)}")
            }

            val requestBody = ChatRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userMessage)
                ),
                temperature = 0.1,
                maxTokens = 10
            )

            val httpRequest = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string() ?: return@withContext null

            if (!response.isSuccessful) {
                return@withContext null
            }

            val chatResponse = gson.fromJson(body, ChatResponse::class.java)
            val content = chatResponse.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim() ?: return@withContext null

            // 清理回复（去掉可能的标点、引号等）
            content.replace("\"", "")
                .replace("'", "")
                .replace("。", "")
                .replace(".", "")
                .trim()
        } catch (e: Exception) {
            null
        }
    }

    // ──── API 数据类 ────

    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @SerializedName("max_tokens") val maxTokens: Int
    )

    private data class ChatMessage(
        val role: String,
        val content: String
    )

    private data class ChatResponse(
        val choices: List<Choice>?
    ) {
        data class Choice(
            val message: Message?
        ) {
            data class Message(
                val content: String?
            )
        }
    }
}
