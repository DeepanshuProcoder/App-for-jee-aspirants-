package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firestore.JeeRepository
import com.example.domain.model.ThemeMode
import com.example.notifications.ReminderScheduler
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarPlannerScreen
import com.example.ui.screens.ChapterDetailScreen
import com.example.ui.screens.ErrorNotebookScreen
import com.example.ui.screens.FormulaBankScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.PracticeTestScreen
import com.example.ui.screens.RevisionSessionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TestAnalysisScreen
import com.example.ui.screens.TodaysRevisionScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.theme.JeeRevisionTheme
import com.example.ui.viewmodel.JeeMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

object AppRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_TODAY = "today"
    const val ROUTE_SUBJECTS = "subjects"
    const val ROUTE_CALENDAR = "calendar"
    const val ROUTE_TOOLS = "tools"
    const val ROUTE_TESTS = "tests"
    const val ROUTE_ERRORS = "errors"
    const val ROUTE_FORMULAS = "formulas"
    const val ROUTE_ANALYTICS = "analytics"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_CHAPTER_DETAIL = "chapter_detail/{chapterId}"
    const val ROUTE_REVISION_SESSION = "revision_session/{chapterId}?topicId={topicId}"
    const val ROUTE_PRACTICE_TEST = "practice_test?chapterId={chapterId}&topicId={topicId}"

    fun chapterDetail(chapterId: String) = "chapter_detail/$chapterId"
    fun revisionSession(chapterId: String, topicId: String?): String =
        if (topicId.isNullOrBlank()) "revision_session/$chapterId"
        else "revision_session/$chapterId?topicId=$topicId"

    fun practiceTest(chapterId: String? = null, topicId: String? = null): String {
        val params = mutableListOf<String>()
        if (!chapterId.isNullOrBlank()) params.add("chapterId=$chapterId")
        if (!topicId.isNullOrBlank()) params.add("topicId=$topicId")
        return if (params.isEmpty()) "practice_test" else "practice_test?${params.joinToString("&")}"
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val PrimaryTabs = listOf(
    NavTab(AppRoutes.ROUTE_DASHBOARD, "Home", Icons.Rounded.Dashboard, "nav_home"),
    NavTab(AppRoutes.ROUTE_TODAY, "Today", Icons.Rounded.Bolt, "nav_today"),
    NavTab(AppRoutes.ROUTE_SUBJECTS, "Subjects", Icons.Rounded.MenuBook, "nav_subjects"),
    NavTab(AppRoutes.ROUTE_CALENDAR, "Calendar", Icons.Rounded.CalendarMonth, "nav_calendar"),
    NavTab(AppRoutes.ROUTE_TOOLS, "Tools", Icons.Rounded.Build, "nav_tools")
)

private val WideRailTabs = PrimaryTabs + listOf(
    NavTab(AppRoutes.ROUTE_ANALYTICS, "Analytics", Icons.Rounded.Insights, "nav_analytics"),
    NavTab(AppRoutes.ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings, "nav_settings")
)

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val activeUser = currentUser
    if (activeUser == null) {
        JeeRevisionTheme(themeMode = ThemeMode.DARK) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        }
    } else {
        key(activeUser.uid) {
            AuthenticatedJeeWorkspace(user = activeUser)
        }
    }
}

@Composable
private fun AuthenticatedJeeWorkspace(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: JeeMainViewModel = viewModel(
        key = user.uid,
        factory = remember(user.uid, context.applicationContext) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = JeeRepository(context.applicationContext)
                    return JeeMainViewModel(
                        repository = repo,
                        currentUserId = user.uid,
                        displayName = user.displayName ?: "JEE Aspirant",
                        email = user.email
                    ) as T
                }
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val practiceState by viewModel.practiceTestState.collectAsStateWithLifecycle()
    val themeMode = ThemeMode.fromString(uiState.userProfile?.darkModePreference ?: ThemeMode.DARK.name)

    LaunchedEffect(uiState.userProfile, uiState.dashboardMetrics) {
        ReminderScheduler.syncDailyReminder(context, uiState.userProfile, uiState.dashboardMetrics)
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.ROUTE_DASHBOARD
    val hideChrome = currentRoute.startsWith("revision_session") ||
        currentRoute.startsWith("chapter_detail") ||
        currentRoute.startsWith("practice_test")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val err = uiState.errorMessage
        if (err != null) {
            snackbarHostState.showSnackbar("Sync notice: $err")
            viewModel.clearError()
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.ROUTE_DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    JeeRevisionTheme(themeMode = themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!hideChrome && !isWideScreen) {
                        NavigationBar {
                            PrimaryTabs.forEach { tab ->
                                val selected = currentRoute == tab.route ||
                                    (tab.route == AppRoutes.ROUTE_TOOLS && currentRoute in setOf(
                                        AppRoutes.ROUTE_TESTS,
                                        AppRoutes.ROUTE_ERRORS,
                                        AppRoutes.ROUTE_FORMULAS,
                                        AppRoutes.ROUTE_ANALYTICS,
                                        AppRoutes.ROUTE_SETTINGS
                                    ))
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (!hideChrome && isWideScreen) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            Spacer(modifier = Modifier.weight(0.2f))
                            WideRailTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (uiState.isLoading && uiState.chapters.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading your JEE Revision OS workspace...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoutes.ROUTE_DASHBOARD,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 960.dp)
                            ) {
                                composable(AppRoutes.ROUTE_DASHBOARD) {
                                    HomeDashboardScreen(
                                        uiState = uiState,
                                        onStartTodaysRevision = { navigateTopTab(AppRoutes.ROUTE_TODAY) },
                                        onStartQueueItem = { item ->
                                            navController.navigate(
                                                AppRoutes.revisionSession(item.chapterId, item.topicId)
                                            )
                                        },
                                        onOpenChapter = { chapterId ->
                                            navController.navigate(AppRoutes.chapterDetail(chapterId))
                                        },
                                        onOpenSubjects = { navigateTopTab(AppRoutes.ROUTE_SUBJECTS) },
                                        onOpenAnalytics = { navController.navigate(AppRoutes.ROUTE_ANALYTICS) }
                                    )
                                }

                                composable(AppRoutes.ROUTE_TODAY) {
                                    TodaysRevisionScreen(
                                        uiState = uiState,
                                        onStartRevision = { item ->
                                            navController.navigate(
                                                AppRoutes.revisionSession(item.chapterId, item.topicId)
                                            )
                                        },
                                        onOpenChapter = { chapterId ->
                                            navController.navigate(AppRoutes.chapterDetail(chapterId))
                                        },
                                        onReschedule = viewModel::rescheduleItem,
                                        onSkipWithReason = viewModel::skipQueueItem,
                                        onOpenSubjects = { navigateTopTab(AppRoutes.ROUTE_SUBJECTS) },
                                        onStartPracticeTest = { item ->
                                            viewModel.resetPracticeTest()
                                            navController.navigate(
                                                AppRoutes.practiceTest(item.chapterId, item.topicId)
                                            )
                                        }
                                    )
                                }

                                composable(AppRoutes.ROUTE_SUBJECTS) {
                                    SubjectsScreen(
                                        uiState = uiState,
                                        onOpenChapter = { chapterId ->
                                            navController.navigate(AppRoutes.chapterDetail(chapterId))
                                        },
                                        onAddChapter = viewModel::addChapter,
                                        onToggleArchive = viewModel::toggleArchiveChapter,
                                        onSwapOrder = viewModel::swapChapterOrder
                                    )
                                }

                                composable(AppRoutes.ROUTE_CALENDAR) {
                                    CalendarPlannerScreen(
                                        uiState = uiState,
                                        onToggleBlockDate = viewModel::toggleBlockedDate,
                                        onAddEvent = viewModel::addCalendarEvent,
                                        onToggleEventDone = viewModel::toggleCalendarEventDone,
                                        onDeleteEvent = viewModel::deleteCalendarEvent,
                                        onAutoBalance = viewModel::autoBalanceCalendar,
                                        onRescheduleChapter = { ch, newDate, reason ->
                                            viewModel.rescheduleItem(
                                                item = com.example.domain.model.RevisionQueueItem(
                                                    key = ch.id,
                                                    subject = ch.subjectEnum,
                                                    chapterId = ch.id,
                                                    chapterName = ch.name,
                                                    topicId = null,
                                                    topicName = null,
                                                    category = com.example.domain.model.QueueCategory.DUE_TODAY,
                                                    dueDate = ch.nextRevisionDate,
                                                    originalDueDate = ch.originalDueDate,
                                                    overdueDays = 0,
                                                    estimatedMinutes = 25,
                                                    estimatedRetention = ch.estimatedRetention,
                                                    importance = ch.importance.toInt(),
                                                    currentIntervalDays = ch.currentIntervalDays.toInt(),
                                                    status = ch.statusEnum,
                                                    priorityScore = 0.0,
                                                    subtitleReason = ""
                                                ),
                                                newDate = newDate,
                                                reason = reason
                                            )
                                        },
                                        onOpenChapter = { chapterId ->
                                            navController.navigate(AppRoutes.chapterDetail(chapterId))
                                        }
                                    )
                                }

                                composable(AppRoutes.ROUTE_TOOLS) {
                                    ToolsHubScreen(
                                        uiState = uiState,
                                        onOpenPracticeTest = {
                                            viewModel.resetPracticeTest()
                                            navController.navigate(AppRoutes.practiceTest())
                                        },
                                        onOpenTests = { navController.navigate(AppRoutes.ROUTE_TESTS) },
                                        onOpenErrorNotebook = { navController.navigate(AppRoutes.ROUTE_ERRORS) },
                                        onOpenFormulaBank = { navController.navigate(AppRoutes.ROUTE_FORMULAS) },
                                        onOpenAnalytics = { navController.navigate(AppRoutes.ROUTE_ANALYTICS) },
                                        onOpenSettings = { navController.navigate(AppRoutes.ROUTE_SETTINGS) }
                                    )
                                }

                                composable(AppRoutes.ROUTE_TESTS) {
                                    BackHandler { navController.popBackStack() }
                                    TestAnalysisScreen(
                                        uiState = uiState,
                                        onAddTest = viewModel::addTestRecord,
                                        onDeleteTest = viewModel::deleteTestRecord,
                                        onScheduleExtraRevision = viewModel::scheduleExtraChapterRevision,
                                        onOpenPracticeTest = {
                                            viewModel.resetPracticeTest()
                                            navController.navigate(AppRoutes.practiceTest())
                                        }
                                    )
                                }

                                composable(AppRoutes.ROUTE_ERRORS) {
                                    BackHandler { navController.popBackStack() }
                                    ErrorNotebookScreen(
                                        uiState = uiState,
                                        onAddError = viewModel::addErrorRecord,
                                        onScheduleErrorRevision = viewModel::scheduleErrorForRevision,
                                        onUpdateErrorStatus = viewModel::updateErrorStatus,
                                        onDeleteError = viewModel::deleteErrorRecord
                                    )
                                }

                                composable(AppRoutes.ROUTE_FORMULAS) {
                                    BackHandler { navController.popBackStack() }
                                    FormulaBankScreen(
                                        uiState = uiState,
                                        onAddNote = viewModel::addFormulaNote,
                                        onDeleteNote = viewModel::deleteFormulaNote
                                    )
                                }

                                composable(AppRoutes.ROUTE_ANALYTICS) {
                                    BackHandler { navController.popBackStack() }
                                    AnalyticsScreen(
                                        uiState = uiState,
                                        onOpenChapter = { chapterId ->
                                            navController.navigate(AppRoutes.chapterDetail(chapterId))
                                        },
                                        onBack = if (!isWideScreen) {
                                            { navController.popBackStack() }
                                        } else null
                                    )
                                }

                                composable(AppRoutes.ROUTE_SETTINGS) {
                                    BackHandler { navController.popBackStack() }
                                    SettingsScreen(
                                        uiState = uiState,
                                        userId = user.uid,
                                        onUpdateSettings = viewModel::updateSettings,
                                        onBuildJsonBackup = viewModel::buildJsonBackup,
                                        onBuildCsvExport = viewModel::buildCsvExport,
                                        onRestoreBackup = viewModel::restoreBackup,
                                        onSignOut = {
                                            GoogleAuthHelper.signOut(credentialManager, scope)
                                        },
                                        onBack = if (!isWideScreen) {
                                            { navController.popBackStack() }
                                        } else null
                                    )
                                }

                                composable(
                                    route = AppRoutes.ROUTE_CHAPTER_DETAIL,
                                    arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
                                ) { entry ->
                                    BackHandler { navController.popBackStack() }
                                    val chapterId = entry.arguments?.getString("chapterId").orEmpty()
                                    val chapter = uiState.chapters.firstOrNull { it.id == chapterId }
                                    if (chapter != null) {
                                        ChapterDetailScreen(
                                            chapter = chapter,
                                            uiState = uiState,
                                            onBack = { navController.popBackStack() },
                                            onStartChapterRevision = {
                                                navController.navigate(AppRoutes.revisionSession(chapter.id, null))
                                            },
                                            onStartTopicRevision = { topic ->
                                                navController.navigate(AppRoutes.revisionSession(chapter.id, topic.id))
                                            },
                                            onStartChapterPracticeTest = {
                                                viewModel.resetPracticeTest()
                                                navController.navigate(AppRoutes.practiceTest(chapter.id, null))
                                            },
                                            onStartTopicPracticeTest = { topic ->
                                                viewModel.resetPracticeTest()
                                                navController.navigate(AppRoutes.practiceTest(chapter.id, topic.id))
                                            },
                                            onUpdateStatus = { status ->
                                                viewModel.updateChapterStatus(chapter, status)
                                            },
                                            onRenameChapter = { newName ->
                                                viewModel.renameChapter(chapter, newName)
                                            },
                                            onSaveNotes = { notes, formulas, mistakes, diffQ, imp ->
                                                viewModel.updateChapterNotesAndRatings(
                                                    chapter, notes, formulas, mistakes, diffQ, imp
                                                )
                                            },
                                            onAddTopic = { topicName ->
                                                viewModel.addTopic(chapter, topicName)
                                            },
                                            onUpdateTopicStatus = viewModel::updateTopicStatus,
                                            onSwapTopicOrder = viewModel::swapTopicOrder,
                                            onRenameTopic = viewModel::renameTopic
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Chapter not found.")
                                        }
                                    }
                                }

                                composable(
                                    route = AppRoutes.ROUTE_REVISION_SESSION,
                                    arguments = listOf(
                                        navArgument("chapterId") { type = NavType.StringType },
                                        navArgument("topicId") {
                                            type = NavType.StringType
                                            nullable = true
                                            defaultValue = null
                                        }
                                    )
                                ) { entry ->
                                    BackHandler { navController.popBackStack() }
                                    val chapterId = entry.arguments?.getString("chapterId").orEmpty()
                                    val topicId = entry.arguments?.getString("topicId")
                                    val chapter = uiState.chapters.firstOrNull { it.id == chapterId }
                                    val topic = if (!topicId.isNullOrBlank()) {
                                        uiState.topics.firstOrNull { it.id == topicId }
                                    } else null

                                    if (chapter != null) {
                                        val chapterNotes = remember(uiState.formulaNotes, chapter.id, chapter.subject) {
                                            uiState.formulaNotes.filter {
                                                it.chapterId == chapter.id || (it.chapterId.isBlank() && it.subject == chapter.subject)
                                            }
                                        }
                                        RevisionSessionScreen(
                                            chapter = chapter,
                                            topic = topic,
                                            chapterNotes = chapterNotes,
                                            previewSchedule = { outcome, und, conf, diff, att, corr, hints, unsol, mins ->
                                                viewModel.previewNextSchedule(
                                                    chapter, topic, outcome, und, conf, diff, att, corr, hints, unsol, mins
                                                )
                                            },
                                            onSubmitSession = { mins, recall, checklist, att, corr, inc, hints, unsol, forgot, unclear, mistake, practice, und, conf, diff, outcome, saveErr, errCat, onRes ->
                                                viewModel.submitRevisionSession(
                                                    chapter, topic, mins, recall, checklist, att, corr, inc, hints, unsol,
                                                    forgot, unclear, mistake, practice, und, conf, diff, outcome, saveErr, errCat, onRes
                                                )
                                            },
                                            onClose = { navController.popBackStack() }
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Chapter unavailable.")
                                        }
                                    }
                                }

                                composable(
                                    route = AppRoutes.ROUTE_PRACTICE_TEST,
                                    arguments = listOf(
                                        navArgument("chapterId") {
                                            type = NavType.StringType
                                            nullable = true
                                            defaultValue = null
                                        },
                                        navArgument("topicId") {
                                            type = NavType.StringType
                                            nullable = true
                                            defaultValue = null
                                        }
                                    )
                                ) { entry ->
                                    val initialChapterId = entry.arguments?.getString("chapterId")
                                    val initialTopicId = entry.arguments?.getString("topicId")
                                    PracticeTestScreen(
                                        uiState = uiState,
                                        practiceState = practiceState,
                                        initialChapterId = initialChapterId,
                                        initialTopicId = initialTopicId,
                                        onGenerateTest = viewModel::generatePracticeTest,
                                        onUpdateAnswer = viewModel::updatePracticeAnswer,
                                        onGoToQuestion = viewModel::goToPracticeQuestion,
                                        onSubmitTest = viewModel::submitPracticeTest,
                                        onSaveErrorToNotebook = viewModel::saveMissedQuestionToErrorNotebook,
                                        onResetPracticeTest = viewModel::resetPracticeTest,
                                        onStartRevision = { chapterId, topicId ->
                                            navController.navigate(AppRoutes.revisionSession(chapterId, topicId))
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
