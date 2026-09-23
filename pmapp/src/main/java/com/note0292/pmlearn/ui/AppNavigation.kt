package com.note0292.pmlearn.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.note0292.pmlearn.data.Curriculum
import com.note0292.pmlearn.data.ProgressStore
import com.note0292.pmlearn.data.QuizMode
import com.note0292.pmlearn.ui.screens.GlossaryScreen
import com.note0292.pmlearn.ui.screens.HomeScreen
import com.note0292.pmlearn.ui.screens.LessonScreen
import com.note0292.pmlearn.ui.screens.QuizScreen
import com.note0292.pmlearn.ui.screens.StageScreen
import com.note0292.pmlearn.ui.screens.StatsScreen

const val FINAL_EXAM_COUNT = 30

object Routes {
    const val HOME = "home"
    const val STAGE = "stage/{stageId}"
    const val LESSON = "lesson/{lessonId}"
    const val QUIZ = "quiz/{mode}?arg={arg}"
    const val GLOSSARY = "glossary"
    const val STATS = "stats"

    fun stage(id: String) = "stage/$id"
    fun lesson(id: String) = "lesson/$id"
    fun quiz(mode: String, arg: String = "") = "quiz/$mode?arg=$arg"
}

private fun parseMode(mode: String, arg: String): QuizMode = when (mode) {
    "lesson" -> QuizMode.Lesson(arg)
    "final" -> QuizMode.Final(FINAL_EXAM_COUNT)
    else -> QuizMode.Review
}

@Composable
fun AppNavigation(curriculum: Curriculum, store: ProgressStore) {
    val nav = rememberNavController()
    val progress by store.progress.collectAsStateWithLifecycle()
    val openLesson: (String) -> Unit = { nav.navigate(Routes.lesson(it)) }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                curriculum = curriculum,
                progress = progress,
                onStage = { nav.navigate(Routes.stage(it)) },
                onLesson = openLesson,
                onReview = { nav.navigate(Routes.quiz("review")) },
                onFinalExam = { nav.navigate(Routes.quiz("final")) },
                onGlossary = { nav.navigate(Routes.GLOSSARY) },
                onStats = { nav.navigate(Routes.STATS) },
            )
        }
        composable(Routes.STAGE, arguments = listOf(navArgument("stageId") { type = NavType.StringType })) { entry ->
            val stage = curriculum.stage(entry.arguments?.getString("stageId").orEmpty()) ?: return@composable
            StageScreen(
                stage = stage,
                progress = progress,
                onLesson = openLesson,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.LESSON, arguments = listOf(navArgument("lessonId") { type = NavType.StringType })) { entry ->
            val lesson = curriculum.lesson(entry.arguments?.getString("lessonId").orEmpty()) ?: return@composable
            LessonScreen(
                lesson = lesson,
                stage = curriculum.stageOf(lesson.id) ?: return@composable,
                progress = progress,
                onSaveNote = { store.saveNote(lesson.id, it) },
                onStartQuiz = { nav.navigate(Routes.quiz("lesson", lesson.id)) },
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
                curriculum = curriculum,
                progress = progress,
                store = store,
                onExit = { nav.popBackStack() },
                onOpenLesson = { id ->
                    // 理解度チェック → 次のレッスンへ進むときは、元のレッスン画面とチェック画面を閉じる
                    nav.navigate(Routes.lesson(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.GLOSSARY) {
            GlossaryScreen(curriculum = curriculum, onBack = { nav.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(
                curriculum = curriculum,
                progress = progress,
                onReset = store::reset,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
