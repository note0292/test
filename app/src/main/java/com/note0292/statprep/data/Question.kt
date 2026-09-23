package com.note0292.statprep.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 統計検定準1級の出題範囲に沿った分野。[id] は questions.json / textbook の category と対応する。
 * [optional] の分野（発展分野）は設定で有効にしたときだけ学習対象になる。
 */
enum class Category(val id: String, val label: String, val optional: Boolean = false) {
    PROBABILITY("probability", "確率・確率分布"),
    ESTIMATION("estimation", "推定"),
    TESTING("testing", "仮説検定"),
    REGRESSION("regression", "回帰分析・線形モデル"),
    MULTIVARIATE("multivariate", "多変量解析"),
    ANOVA("anova", "分散分析・実験計画"),
    CATEGORICAL("categorical", "分割表・カテゴリカルデータ"),
    TIME_SERIES("time_series", "時系列解析"),
    BAYES("bayes", "ベイズ統計"),
    NONPARAMETRIC("nonparametric", "ノンパラメトリック・リサンプリング"),
    MARKOV("markov", "確率過程・シミュレーション"),
    SAMPLING("sampling", "標本調査・欠測データ"),
    EFFECT("effect", "効果測定・A/Bテスト", optional = true),
    CAUSAL("causal", "因果推論", optional = true);

    companion object {
        fun fromId(id: String): Category? = entries.firstOrNull { it.id == id }

        fun active(includeOptional: Boolean): List<Category> = entries.filter { includeOptional || !it.optional }
    }
}

@Serializable
data class Question(
    val id: String,
    val category: String,
    val question: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
) {
    val categoryEnum: Category get() = Category.fromId(category) ?: error("unknown category: $category")
}

@Serializable
data class QuestionBank(val questions: List<Question>)

object QuestionParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<Question> = json.decodeFromString<QuestionBank>(text).questions
}
