package com.note0292.statprep.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 分野の種類。章番号の付け方と出題対象が変わる。 */
enum class CategoryKind {
    /** 統計のための基礎数学（微分積分・線形代数）。模擬試験には出題しない。 */
    MATH,

    /** 統計検定準1級の出題範囲。 */
    CORE,

    /** 発展分野。設定で有効にしたときだけ学習対象になる。 */
    ADVANCED,
}

/** 学習分野。[id] は questions.json / textbook の category と対応する。 */
enum class Category(val id: String, val label: String, val kind: CategoryKind = CategoryKind.CORE) {
    CALCULUS("calculus", "微分積分", CategoryKind.MATH),
    LINEAR_ALGEBRA("linear_algebra", "線形代数", CategoryKind.MATH),
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
    EFFECT("effect", "効果測定・A/Bテスト", CategoryKind.ADVANCED),
    CAUSAL("causal", "因果推論", CategoryKind.ADVANCED);

    val optional: Boolean get() = kind == CategoryKind.ADVANCED

    /** 種類ごとの通し番号の表記（"基礎1", "3", "発展2" など）。レッスン番号の接頭辞にも使う。 */
    val number: String
        get() {
            val n = entries.filter { it.kind == kind }.indexOf(this) + 1
            return when (kind) {
                CategoryKind.MATH -> "基礎$n"
                CategoryKind.CORE -> "$n"
                CategoryKind.ADVANCED -> "発展$n"
            }
        }

    /** 章の見出し（"基礎1　微分積分", "第3章　仮説検定" など）。 */
    val chapterTitle: String
        get() = (if (kind == CategoryKind.CORE) "第${number}章" else number) + "　" + label

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
