package com.note0292.statprep.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.note0292.statprep.data.Category
import com.note0292.statprep.data.ProgressStore
import com.note0292.statprep.data.QuestionRepository
import com.note0292.statprep.data.QuizMode
import com.note0292.statprep.data.TextbookRepository
import com.note0292.statprep.ui.screens.CategoryScreen
import com.note0292.statprep.ui.screens.ChapterScreen
import com.note0292.statprep.ui.screens.HomeScreen
import com.note0292.statprep.ui.screens.LessonScreen
import com.note0292.statprep.ui.screens.QuizScreen
import com.note0292.statprep.ui.screens.SettingsScreen
import com.note0292.statprep.ui.screens.StatsScreen
import com.note0292.statprep.ui.screens.TextbookScreen

const val RANDOM_COUNT = 10
const val MOCK_COUNT = 30

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val TEXTBOOK = "textbook"
    const val CHAPTER = "chapter/{category}"
    const val LESSON = "lesson/{lesson}"
    const val QUIZ = "quiz/{mode}?arg={arg}"

    fun chapter(category: Category) = "chapter/${category.id}"
    fun lesson(id: String) = "lesson/$id"
    fun quiz(mode: String, arg: String = "") = "quiz/$mode?arg=$arg"
}

private fun parseMode(mode: String, arg: String): QuizMode = when (mode) {
    "category" -> QuizMode.ByCategory(Category.fromId(arg) ?: Category.PROBABILITY)
    "weak" -> QuizMode.Weak
    "bookmarked" -> QuizMode.Bookmarked
    "mock" -> QuizMode.Mock(MOCK_COUNT)
    else -> QuizMode.Random(RANDOM_COUNT)
}

@Composable
fun AppNavigation(repository: QuestionRepository, textbook: TextbookRepository, store: ProgressStore) {
    val nav = rememberNavController()
    val progress by store.progress.collectAsStateWithLifecycle()
    val includeOptional by store.includeOptional.collectAsStateWithLifecycle()

    // 発展分野が無効なときは、その分野の問題・章をすべての画面から除外する
    val categories = remember(includeOptional) { Category.active(includeOptional) }
    val questions = remember(includeOptional) {
        repository.questions.filter { includeOptional || !it.categoryEnum.optional }
    }
    val lessonIds = remember(includeOptional) {
        categories.flatMap { textbook.chapter(it)?.lessons.orEmpty() }.map { it.id }
    }
    val openChapter: (Category) -> Unit = { nav.navigate(Routes.chapter(it)) }
    val practice: (Category) -> Unit = { nav.navigate(Routes.quiz("category", it.id)) }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                questions = questions,
                progress = progress,
                lessonsRead = lessonIds.count { it in progress.readLessons },
                lessonsTotal = lessonIds.size,
                onStartQuiz = { mode -> nav.navigate(Routes.quiz(mode)) },
                onCategories = { nav.navigate(Routes.CATEGORIES) },
                onTextbook = { nav.navigate(Routes.TEXTBOOK) },
                onStats = { nav.navigate(Routes.STATS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.TEXTBOOK) {
            TextbookScreen(
                categories = categories,
                textbook = textbook,
                progress = progress,
                onSelect = openChapter,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.CHAPTER,
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { entry ->
            val category = Category.fromId(entry.arguments?.getString("category").orEmpty()) ?: Category.PROBABILITY
            ChapterScreen(
                category = category,
                chapter = textbook.chapter(category),
                questionCount = repository.questions.count { it.category == category.id },
                progress = progress,
                onLesson = { nav.navigate(Routes.lesson(it)) },
                onPractice = { practice(category) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.LESSON,
            arguments = listOf(navArgument("lesson") { type = NavType.StringType }),
        ) { entry ->
            val found = textbook.lesson(entry.arguments?.getString("lesson").orEmpty())
            if (found == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                val (chapter, index) = found
                LessonScreen(
                    chapter = chapter,
                    index = index,
                    onRead = store::markLessonRead,
                    onOpenLesson = { id ->
                        // 前後のレッスンへは積み重ねずに置き換えて遷移する
                        nav.navigate(Routes.lesson(id)) { popUpTo(Routes.LESSON) { inclusive = true } }
                    },
                    onPractice = { practice(chapter.categoryEnum) },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable(Routes.CATEGORIES) {
            CategoryScreen(
                categories = categories,
                questions = questions,
                progress = progress,
                onSelect = practice,
                onChapter = openChapter,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                categories = categories,
                questions = questions,
                progress = progress,
                onReset = store::reset,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                includeOptional = includeOptional,
                onIncludeOptionalChange = store::setIncludeOptional,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.QUIZ,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("arg") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val mode = parseMode(
                entry.arguments?.getString("mode").orEmpty(),
                entry.arguments?.getString("arg").orEmpty(),
            )
            QuizScreen(
                mode = mode,
                // 分野別は発展分野でも解けるように全問題から選ぶ
                questions = if (mode is QuizMode.ByCategory) repository.questions else questions,
                progress = progress,
                store = store,
                onOpenChapter = openChapter,
                onExit = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
