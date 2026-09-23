package com.note0292.statprep.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.note0292.statprep.ui.screens.CategoryScreen
import com.note0292.statprep.ui.screens.HomeScreen
import com.note0292.statprep.ui.screens.NotesScreen
import com.note0292.statprep.ui.screens.QuizScreen
import com.note0292.statprep.ui.screens.StatsScreen

const val RANDOM_COUNT = 10
const val MOCK_COUNT = 30

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val STATS = "stats"
    const val NOTES = "notes?category={category}"
    const val QUIZ = "quiz/{mode}?arg={arg}"

    fun notes(category: Category? = null) = "notes?category=${category?.id.orEmpty()}"
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
fun AppNavigation(repository: QuestionRepository, store: ProgressStore) {
    val nav = rememberNavController()
    val progress by store.progress.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                questions = repository.questions,
                progress = progress,
                onStartQuiz = { mode -> nav.navigate(Routes.quiz(mode)) },
                onCategories = { nav.navigate(Routes.CATEGORIES) },
                onNotes = { nav.navigate(Routes.notes()) },
                onStats = { nav.navigate(Routes.STATS) },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoryScreen(
                questions = repository.questions,
                progress = progress,
                onSelect = { nav.navigate(Routes.quiz("category", it.id)) },
                onNotes = { nav.navigate(Routes.notes(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.NOTES,
            arguments = listOf(navArgument("category") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            NotesScreen(
                initialCategory = Category.fromId(entry.arguments?.getString("category").orEmpty()),
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                questions = repository.questions,
                progress = progress,
                onReset = store::reset,
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
                questions = repository.questions,
                progress = progress,
                store = store,
                onExit = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
