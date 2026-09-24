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
import com.note0292.statprep.data.game.Curriculum
import com.note0292.statprep.ui.game.BadgesScreen
import com.note0292.statprep.ui.game.GameHomeScreen
import com.note0292.statprep.ui.game.MapScreen
import com.note0292.statprep.ui.game.StageScreen
import com.note0292.statprep.ui.screens.CategoryScreen
import com.note0292.statprep.ui.screens.ChapterScreen
import com.note0292.statprep.ui.screens.HomeScreen
import com.note0292.statprep.ui.screens.LessonScreen
import com.note0292.statprep.ui.screens.QuizScreen
import com.note0292.statprep.ui.screens.SettingsScreen
import com.note0292.statprep.ui.screens.StatsScreen
import com.note0292.statprep.ui.screens.TextbookScreen
import java.time.LocalDate

const val RANDOM_COUNT = 10
const val MOCK_COUNT = 30

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val MAP = "map"
    const val BADGES = "badges"
    const val STAGE = "stage/{day}"
    const val CATEGORIES = "categories"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val TEXTBOOK = "textbook"
    const val CHAPTER = "chapter/{category}"
    const val LESSON = "lesson/{lesson}"
    const val QUIZ = "quiz/{mode}?arg={arg}"

    fun stage(day: Int) = "stage/$day"
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
    val game by store.game.collectAsStateWithLifecycle()
    val curriculum = remember { Curriculum.build(textbook.allChapters(), repository.questions) }
    // 日付が変わっても画面に戻れば更新されるよう、再コンポーズのたびに取り直す
    val today = LocalDate.now()

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
            GameHomeScreen(
                state = game,
                curriculum = curriculum,
                today = today,
                onBegin = store::startGame,
                onPlay = { day -> nav.navigate(Routes.stage(day)) },
                onMap = { nav.navigate(Routes.MAP) },
                onBadges = { nav.navigate(Routes.BADGES) },
                onLibrary = { nav.navigate(Routes.LIBRARY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.STAGE,
            arguments = listOf(navArgument("day") { type = NavType.IntType }),
        ) { entry ->
            StageScreen(
                day = entry.arguments?.getInt("day") ?: Curriculum.BONUS_DAY,
                curriculum = curriculum,
                store = store,
                onExit = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.MAP) { MapScreen(game, curriculum, onBack = { nav.popBackStack() }) }
        composable(Routes.BADGES) { BadgesScreen(game, onBack = { nav.popBackStack() }) }
        composable(Routes.LIBRARY) {
            HomeScreen(
                questions = questions,
                progress = progress,
                lessonsRead = lessonIds.count { it in progress.readLessons },
                lessonsTotal = lessonIds.size,
                onStartQuiz = { mode -> nav.navigate(Routes.quiz(mode)) },
                onCategories = { nav.navigate(Routes.CATEGORIES) },
                onTextbook = { nav.navigate(Routes.TEXTBOOK) },
                onStats = { nav.navigate(Routes.STATS) },
                onBack = { nav.popBackStack() },
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
                onResetGame = store::resetGame,
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
                onExit = {
                    if (!nav.popBackStack(Routes.LIBRARY, inclusive = false)) nav.popBackStack()
                },
            )
        }
    }
}
