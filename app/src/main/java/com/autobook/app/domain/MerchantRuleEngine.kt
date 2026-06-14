package com.autobook.app.domain

import com.autobook.app.data.model.Category

/**
 * 商户规则引擎。
 *
 * 三层递进分类策略：
 * 1. 精准匹配（商户名 → 分类映射表）
 * 2. 关键词匹配（商户名包含关键词 → 推断分类）
 * 3. 规则无法确定 → 交 DeepSeek API 在线分类
 *
 * 命中 1 或 2 的商户直接返回分类，无需 API 调用，零延迟零成本。
 */
object MerchantRuleEngine {

    /** 高频商户精准映射表 */
    private val EXACT_MAP: Map<String, Category> = mapOf(
        // ── 餐饮 ──
        "美团" to Category.FOOD, "美团外卖" to Category.FOOD, "饿了么" to Category.FOOD,
        "星巴克" to Category.FOOD, "瑞幸咖啡" to Category.FOOD, "Manner" to Category.FOOD,
        "肯德基" to Category.FOOD, "麦当劳" to Category.FOOD, "必胜客" to Category.FOOD,
        "海底捞" to Category.FOOD, "喜茶" to Category.FOOD, "奈雪的茶" to Category.FOOD,
        "茶百道" to Category.FOOD, "蜜雪冰城" to Category.FOOD, "古茗" to Category.FOOD,
        "霸王茶姬" to Category.FOOD, "M Stand" to Category.FOOD, "Tims" to Category.FOOD,
        "盒马" to Category.FOOD, "叮咚买菜" to Category.FOOD, "朴朴" to Category.FOOD,
        "每日优鲜" to Category.FOOD, "山姆" to Category.FOOD, "Costco" to Category.FOOD,
        "大润发" to Category.FOOD, "永辉" to Category.FOOD, "华润万家" to Category.FOOD,

        // ── 交通 ──
        "滴滴出行" to Category.TRANSPORT, "滴滴" to Category.TRANSPORT,
        "花小猪" to Category.TRANSPORT, "T3出行" to Category.TRANSPORT,
        "曹操出行" to Category.TRANSPORT, "首汽约车" to Category.TRANSPORT,
        "哈啰" to Category.TRANSPORT, "美团单车" to Category.TRANSPORT,
        "青桔" to Category.TRANSPORT, "摩拜" to Category.TRANSPORT,
        "中国石化" to Category.TRANSPORT, "中石油" to Category.TRANSPORT,
        "中石化" to Category.TRANSPORT, "壳牌" to Category.TRANSPORT,
        "12306" to Category.TRANSPORT, "铁路12306" to Category.TRANSPORT,
        "携程" to Category.TRANSPORT, "去哪儿" to Category.TRANSPORT,
        "飞猪" to Category.TRANSPORT, "航旅纵横" to Category.TRANSPORT,
        "ETC" to Category.TRANSPORT, "高速" to Category.TRANSPORT,

        // ── 购物 ──
        "淘宝" to Category.SHOPPING, "天猫" to Category.SHOPPING,
        "京东" to Category.SHOPPING, "拼多多" to Category.SHOPPING,
        "唯品会" to Category.SHOPPING, "得物" to Category.SHOPPING,
        "闲鱼" to Category.SHOPPING, "转转" to Category.SHOPPING,
        "苏宁" to Category.SHOPPING, "国美" to Category.SHOPPING,
        "当当" to Category.SHOPPING, "网易严选" to Category.SHOPPING,
        "小红书" to Category.SHOPPING, "抖音商城" to Category.SHOPPING,
        "快手小店" to Category.SHOPPING, "1688" to Category.SHOPPING,

        // ── 居住 ──
        "国家电网" to Category.HOUSING, "南方电网" to Category.HOUSING,
        "燃气" to Category.HOUSING, "燃气费" to Category.HOUSING,
        "自来水" to Category.HOUSING, "水务" to Category.HOUSING,
        "供暖" to Category.HOUSING,
        "链家" to Category.HOUSING, "贝壳" to Category.HOUSING,
        "自如" to Category.HOUSING, "我爱我家" to Category.HOUSING,

        // ── 娱乐 ──
        "猫眼" to Category.ENTERTAINMENT, "淘票票" to Category.ENTERTAINMENT,
        "大麦" to Category.ENTERTAINMENT, "永乐" to Category.ENTERTAINMENT,
        "哔哩哔哩" to Category.ENTERTAINMENT, "bilibili" to Category.ENTERTAINMENT,
        "B站" to Category.ENTERTAINMENT, "抖音" to Category.ENTERTAINMENT,
        "快手" to Category.ENTERTAINMENT, "虎牙" to Category.ENTERTAINMENT,
        "斗鱼" to Category.ENTERTAINMENT, "腾讯视频" to Category.ENTERTAINMENT,
        "爱奇艺" to Category.ENTERTAINMENT, "优酷" to Category.ENTERTAINMENT,
        "芒果TV" to Category.ENTERTAINMENT, "Netflix" to Category.ENTERTAINMENT,
        "Spotify" to Category.ENTERTAINMENT, "QQ音乐" to Category.ENTERTAINMENT,
        "网易云音乐" to Category.ENTERTAINMENT, "Apple Music" to Category.ENTERTAINMENT,
        "Steam" to Category.ENTERTAINMENT, "Epic" to Category.ENTERTAINMENT,
        "任天堂" to Category.ENTERTAINMENT, "PlayStation" to Category.ENTERTAINMENT,
        "欢乐谷" to Category.ENTERTAINMENT, "迪士尼" to Category.ENTERTAINMENT,
        "环球影城" to Category.ENTERTAINMENT, "KEEP" to Category.ENTERTAINMENT,
        "超级猩猩" to Category.ENTERTAINMENT, "乐刻" to Category.ENTERTAINMENT,

        // ── 医疗 ──
        "阿里健康" to Category.MEDICAL, "京东健康" to Category.MEDICAL,
        "丁香医生" to Category.MEDICAL, "好大夫" to Category.MEDICAL,
        "微医" to Category.MEDICAL, "平安好医生" to Category.MEDICAL,

        // ── 教育 ──
        "学而思" to Category.EDUCATION, "新东方" to Category.EDUCATION,
        "作业帮" to Category.EDUCATION, "猿辅导" to Category.EDUCATION,
        "得到" to Category.EDUCATION, "樊登读书" to Category.EDUCATION,
        "知乎" to Category.EDUCATION, "知识星球" to Category.EDUCATION,
        "慕课" to Category.EDUCATION, "极客时间" to Category.EDUCATION,

        // ── 通讯 ──
        "中国移动" to Category.TELECOM, "中国联通" to Category.TELECOM,
        "中国电信" to Category.TELECOM, "移动话费" to Category.TELECOM,
        "联通话费" to Category.TELECOM, "电信话费" to Category.TELECOM,

        // ── 日用 ──
        "711" to Category.DAILY, "全家" to Category.DAILY, "罗森" to Category.DAILY,
        "屈臣氏" to Category.DAILY, "名创优品" to Category.DAILY,
        "无印良品" to Category.DAILY, "MUJI" to Category.DAILY,

        // ── 收入 ──
        "工资" to Category.INCOME, "薪资" to Category.INCOME,
        "报销" to Category.INCOME, "差旅" to Category.INCOME,
        "奖金" to Category.INCOME, "劳务费" to Category.INCOME
    )

    /** 关键词 → 分类规则（按优先级排序，先匹配先返回） */
    private val KEYWORD_RULES: List<Pair<String, Category>> = listOf(
        // 餐饮关键词
        "外卖" to Category.FOOD, "餐厅" to Category.FOOD, "饭" to Category.FOOD,
        "奶茶" to Category.FOOD, "咖啡" to Category.FOOD, "蛋糕" to Category.FOOD,
        "面包" to Category.FOOD, "水果" to Category.FOOD, "烧烤" to Category.FOOD,
        "火锅" to Category.FOOD, "小吃" to Category.FOOD, "面馆" to Category.FOOD,
        "食堂" to Category.FOOD, "超市" to Category.FOOD, "买菜" to Category.FOOD,
        "美食" to Category.FOOD, "餐饮" to Category.FOOD, "食品" to Category.FOOD,

        // 交通关键词
        "打车" to Category.TRANSPORT, "出租车" to Category.TRANSPORT,
        "公交" to Category.TRANSPORT, "地铁" to Category.TRANSPORT,
        "加油" to Category.TRANSPORT, "停车" to Category.TRANSPORT,
        "火车" to Category.TRANSPORT, "机票" to Category.TRANSPORT,
        "高铁" to Category.TRANSPORT, "航班" to Category.TRANSPORT,
        "骑行" to Category.TRANSPORT,

        // 购物关键词
        "百货" to Category.SHOPPING, "商场" to Category.SHOPPING,
        "旗舰店" to Category.SHOPPING, "专卖店" to Category.SHOPPING,
        "服饰" to Category.SHOPPING, "数码" to Category.SHOPPING,
        "电器" to Category.SHOPPING, "手机" to Category.SHOPPING,
        "化妆品" to Category.SHOPPING, "电商" to Category.SHOPPING,

        // 居住关键词
        "房租" to Category.HOUSING, "物业" to Category.HOUSING,
        "电费" to Category.HOUSING, "水费" to Category.HOUSING,
        "燃气费" to Category.HOUSING, "暖气" to Category.HOUSING,
        "维修" to Category.HOUSING, "装修" to Category.HOUSING,
        "房贷" to Category.HOUSING, "公积金" to Category.HOUSING,

        // 娱乐关键词
        "电影" to Category.ENTERTAINMENT, "影院" to Category.ENTERTAINMENT,
        "游戏" to Category.ENTERTAINMENT, "KTV" to Category.ENTERTAINMENT,
        "演唱会" to Category.ENTERTAINMENT, "演出" to Category.ENTERTAINMENT,
        "运动" to Category.ENTERTAINMENT, "健身" to Category.ENTERTAINMENT,
        "旅游" to Category.ENTERTAINMENT, "酒店" to Category.ENTERTAINMENT,
        "会员" to Category.ENTERTAINMENT, "订阅" to Category.ENTERTAINMENT,

        // 医疗关键词
        "医院" to Category.MEDICAL, "诊所" to Category.MEDICAL,
        "药房" to Category.MEDICAL, "药店" to Category.MEDICAL,
        "挂号" to Category.MEDICAL, "体检" to Category.MEDICAL,
        "门诊" to Category.MEDICAL, "医保" to Category.MEDICAL,

        // 教育关键词
        "学费" to Category.EDUCATION, "培训" to Category.EDUCATION,
        "课程" to Category.EDUCATION, "书店" to Category.EDUCATION,
        "文具" to Category.EDUCATION, "学习" to Category.EDUCATION,

        // 通讯关键词
        "话费" to Category.TELECOM, "宽带" to Category.TELECOM,
        "流量" to Category.TELECOM, "充值" to Category.TELECOM,

        // 日用关键词
        "理发" to Category.DAILY, "洗衣" to Category.DAILY,
        "便利店" to Category.DAILY, "日用品" to Category.DAILY,
        "洗护" to Category.DAILY, "清洁" to Category.DAILY,

        // 收入关键词
        "转入" to Category.INCOME, "存入" to Category.INCOME,
        "入账" to Category.INCOME, "退款" to Category.REFUND,
        "退回" to Category.REFUND
    )

    // ──── 公开 API ────

    /**
     * 尝试通过规则匹配分类。
     *
     * @param merchant  商户名
     * @param rawText   原始短信文本（用于辅助匹配）
     * @return 匹配到的 Category，匹配不到返回 null
     */
    fun classify(merchant: String, rawText: String = ""): Category? {
        // 1. 精准匹配
        val exact = exactMatch(merchant) ?: exactMatch(rawText)
        if (exact != null) return exact

        // 2. 关键词匹配
        val keyword = keywordMatch(merchant) ?: keywordMatch(rawText)
        if (keyword != null) return keyword

        return null
    }

    // ──── 私有方法 ────

    private fun exactMatch(text: String): Category? {
        if (text.isBlank()) return null
        return EXACT_MAP[text.trim()]
    }

    private fun keywordMatch(text: String): Category? {
        if (text.isBlank()) return null
        return KEYWORD_RULES.firstOrNull { (keyword, _) ->
            keyword in text
        }?.second
    }
}
