package com.expenseassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.expenseassistant.service.PermissionStatus
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.ui.account.AccountScreen
import com.expenseassistant.ui.add.AddTransactionScreen
import com.expenseassistant.ui.budget.BudgetBreakdownScreen
import com.expenseassistant.ui.budget.BudgetScreen
import com.expenseassistant.ui.detail.TransactionDetailScreen
import com.expenseassistant.ui.history.AllTransactionsScreen
import com.expenseassistant.ui.insights.InsightsScreen
import com.expenseassistant.ui.tag.TagScreen
import com.expenseassistant.ui.category.CategoryScreen
import com.expenseassistant.ui.category.LocalCategoryIconOverrides
import com.expenseassistant.di.ServiceLocator

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val iconStore = remember { ServiceLocator.categoryIconStore(context) }
                    val iconOverrides by iconStore.overrides.collectAsStateWithLifecycle()
                    CompositionLocalProvider(LocalCategoryIconOverrides provides iconOverrides) {
                        AppShell()
                    }
                }
            }
        }
    }
}

private enum class Tab(val label: String) { HOME("Home"), INSIGHTS("Insights"), BUDGET("Budget"), PROFILE("Profile") }

private sealed interface Route {
    data object Main : Route
    data object Add : Route
    data object Budgets : Route
    data class Detail(val id: Long) : Route
    data class CategoryTransactions(val category: Category) : Route
    data class TagTransactions(val tag: String) : Route
    data object NeedsReview : Route
    data object AllTransactions : Route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentState by viewModel.recentState.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val monthlyBudgetAnalytics by viewModel.monthlyBudgetAnalytics.collectAsStateWithLifecycle()
    val recurring by viewModel.recurring.collectAsStateWithLifecycle()
    val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
    val tagUsage by viewModel.tagUsage.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val summaryScope by viewModel.summaryScope.collectAsStateWithLifecycle()
    val needsReview by viewModel.needsReviewTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tab by remember { mutableStateOf(Tab.HOME) }
    var route by remember { mutableStateOf<Route>(Route.Main) }

    var notificationAccess by remember { mutableStateOf(PermissionStatus.isNotificationAccessGranted(context)) }

    val userPreferences = remember { ServiceLocator.userPreferences(context) }
    var showBackupNotice by remember {
        mutableStateOf(userPreferences.autoBackupSettings() == null && !userPreferences.isBackupNoticeDismissed())
    }
    var openAutoBackupSetup by remember { mutableStateOf(false) }
    var helpMenuOpen by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    // Re-check after the user returns from system settings.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationAccess = PermissionStatus.isNotificationAccessGranted(context)
            showBackupNotice = userPreferences.autoBackupSettings() == null && !userPreferences.isBackupNoticeDismissed()
        }
    }

    // Re-check after the user sets up backups from the Profile tab and comes back.
    LaunchedEffect(tab) {
        if (tab == Tab.HOME) {
            showBackupNotice = userPreferences.autoBackupSettings() == null && !userPreferences.isBackupNoticeDismissed()
        }
    }

    // Budget alerts need runtime permission from Android 13 onwards.
    var permissionPromptSettled by remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    }
    val postNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionPromptSettled = true }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The walkthrough runs itself once, after the system permission prompt is out of the way.
    LaunchedEffect(permissionPromptSettled) {
        if (permissionPromptSettled && !userPreferences.isTutorialSeen()) {
            showTutorial = true
        }
    }

    BackHandler(enabled = route != Route.Main) { route = Route.Main }

    when (val current = route) {
        Route.Add -> {
            AddTransactionScreen(
                onBack = { route = Route.Main },
                onSave = { input ->
                    viewModel.addManualTransaction(
                        amountMinor = input.amountMinor,
                        direction = input.direction,
                        merchant = input.merchant,
                        category = input.category,
                        customCategoryName = input.customCategoryName,
                        customCategoryColor = input.customCategoryColor,
                        customCategoryIcon = input.customCategoryIcon,
                        paymentMode = input.paymentMode,
                        occurredAt = input.occurredAt,
                        description = input.description,
                        tags = input.tags,
                    )
                    route = Route.Main
                },
                customCategories = customCategories,
            )
            return
        }

        Route.Budgets -> {
            BudgetScreen(
                onBack = { route = Route.Main },
                onOpenCategory = { route = Route.CategoryTransactions(it) },
            )
            return
        }

        is Route.Detail -> {
            // Looked up straight from the database so transactions outside the visible period still open.
            val transactionFlow = remember(current.id) { viewModel.observeTransaction(current.id) }
            // Seeded from the already-loaded list so the first frame isn't blank while Room emits.
            val seed = remember(current.id, allTransactions, recentState) {
                allTransactions.firstOrNull { it.id == current.id }
                    ?: recentState.transactions.firstOrNull { it.id == current.id }
            }
            val transaction by transactionFlow.collectAsStateWithLifecycle(initialValue = seed)
            transaction?.let { detail ->
                TransactionDetailScreen(
                    transaction = detail,
                    onBack = { route = Route.Main },
                    onSave = { edits -> viewModel.saveDetails(detail.id, edits) },
                    onDelete = {
                        viewModel.delete(detail.id)
                        route = Route.Main
                    },
                    customCategories = customCategories,
                    tagSuggestions = tagSuggestions,
                    onOpenTag = { tag -> route = Route.TagTransactions(tag) },
                )
            } ?: Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
            return
        }

        is Route.CategoryTransactions -> {
            val categoryList = (state.transactions + analytics.transactions)
                .distinctBy { it.id }
                .filter { it.category == current.category }
            CategoryScreen(
                category = current.category,
                transactions = categoryList,
                onBack = { route = Route.Main },
                onOpenTransaction = { id -> route = Route.Detail(id) },
                onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                customCategories = customCategories,
                onDelete = { id -> viewModel.delete(id) },
            )
            return
        }

        Route.NeedsReview -> {
            HomeScreen(
                state = recentState,
                notificationAccessGranted = notificationAccess,
                onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                customCategories = customCategories,
                onDelete = { id -> viewModel.delete(id) },
                onOpenTransaction = { id -> route = Route.Detail(id) },
                needsReviewFilter = true,
                transactionOverride = needsReview,
                onClearFilter = { route = Route.Main },
            )
            return
        }

        is Route.TagTransactions -> {
            var tagTransactions by remember(current.tag) { mutableStateOf<List<TransactionEntity>?>(null) }
            LaunchedEffect(current.tag) { tagTransactions = viewModel.transactionsForTag(current.tag) }
            val list = tagTransactions
            if (list != null) {
                TagScreen(
                    tag = current.tag,
                    transactions = list,
                    onBack = { route = Route.Main },
                    onOpenTransaction = { id -> route = Route.Detail(id) },
                )
            }
            return
        }

        Route.AllTransactions -> {
            AllTransactionsScreen(
                transactions = allTransactions,
                onBack = { route = Route.Main },
                onOpenTransaction = { id -> route = Route.Detail(id) },
                onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                customCategories = customCategories,
                onDelete = { id -> viewModel.delete(id) },
            )
            return
        }

        Route.Main -> Unit
    }

    val topBarTitle = when (tab) {
        Tab.HOME -> "Expense Assistant"
        Tab.INSIGHTS -> "Insights"
        Tab.BUDGET -> "Budget"
        Tab.PROFILE -> "Profile"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                actions = {
                    IconButton(onClick = { helpMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help")
                    }
                    DropdownMenu(expanded = helpMenuOpen, onDismissRequest = { helpMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Tutorial") },
                            leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                            onClick = {
                                helpMenuOpen = false
                                showTutorial = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { route = Route.Add },
                modifier = Modifier.offset(y = 54.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                        ),
                    ),
            ) {
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = tab == Tab.HOME,
                    onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(Tab.HOME.label) },
                )
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = tab == Tab.INSIGHTS,
                    onClick = { tab = Tab.INSIGHTS },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = null) },
                    label = { Text(Tab.INSIGHTS.label) },
                )
                // Reserve space for the floating action button so it doesn't overlap these two items.
                Spacer(modifier = Modifier.width(54.dp))
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = tab == Tab.BUDGET,
                    onClick = { tab = Tab.BUDGET },
                    icon = { Icon(Icons.Filled.Savings, contentDescription = null) },
                    label = { Text(Tab.BUDGET.label) },
                )
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = tab == Tab.PROFILE,
                    onClick = { tab = Tab.PROFILE },
                    icon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
                    label = { Text(Tab.PROFILE.label) },
                )
            }
        },
    ) { padding ->
        when (tab) {
            Tab.HOME -> HomeScreen(
                state = recentState,
                notificationAccessGranted = notificationAccess,
                showBackupNotice = showBackupNotice,
                onEnableBackup = {
                    openAutoBackupSetup = true
                    tab = Tab.PROFILE
                },
                onDismissBackupNotice = {
                    userPreferences.dismissBackupNotice()
                    showBackupNotice = false
                },
                onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                customCategories = customCategories,
                summaryScope = summaryScope,
                onSummaryScopeChange = viewModel::setSummaryScope,
                budgetState = monthlyBudgetAnalytics,
                onOpenNeedsReview = { route = Route.NeedsReview },
                onOpenAllTransactions = { route = Route.AllTransactions },
                onDelete = { id -> viewModel.delete(id) },
                onOpenTransaction = { id -> route = Route.Detail(id) },
                modifier = Modifier.padding(padding),
            )

            Tab.INSIGHTS -> InsightsScreen(
                state = analytics,
                recurring = recurring,
                tagUsage = tagUsage,
                onRangeChange = viewModel::setRange,
                onShiftPeriod = viewModel::shiftPeriod,
                onJumpTo = viewModel::jumpTo,
                onResetToCurrent = viewModel::resetToCurrent,
                onOpenCategory = { route = Route.CategoryTransactions(it) },
                onOpenTag = { tag -> route = Route.TagTransactions(tag) },
                onOpenNeedsReview = { route = Route.NeedsReview },
                modifier = Modifier.padding(padding),
            )

            Tab.BUDGET -> BudgetBreakdownScreen(
                state = monthlyBudgetAnalytics,
                onManageBudgets = { route = Route.Budgets },
                onOpenCategory = { route = Route.CategoryTransactions(it) },
                modifier = Modifier.padding(padding),
            )

            Tab.PROFILE -> AccountScreen(
                onOpenBudgets = { route = Route.Budgets },
                onOpenNeedsReview = { route = Route.NeedsReview },
                openAutoBackupSetup = openAutoBackupSetup,
                onAutoBackupSetupHandled = { openAutoBackupSetup = false },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showTutorial) {
        TutorialDialog(
            onDismiss = {
                showTutorial = false
                userPreferences.markTutorialSeen()
            },
        )
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) {
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4DD0C0),
            onPrimary = androidx.compose.ui.graphics.Color(0xFF06201C),
            primaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A36),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFB5EFE6),
            secondary = androidx.compose.ui.graphics.Color(0xFFFFAB91),
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3A2620),
            tertiary = androidx.compose.ui.graphics.Color(0xFFD7CCC8),
            tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF262223),
            error = androidx.compose.ui.graphics.Color(0xFFEF6C6C),
            errorContainer = androidx.compose.ui.graphics.Color(0xFF33211F),
            background = androidx.compose.ui.graphics.Color(0xFF15161A),
            onBackground = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
            surface = androidx.compose.ui.graphics.Color(0xFF202126),
            onSurface = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF202126),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFAAAAB2),
            // Card/sheet containers default to these; keep them flat so no grey band shows.
            surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF191A1F),
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF1D1E23),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFF202126),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF28292F),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF303137),
            outline = androidx.compose.ui.graphics.Color(0xFF3D3E45),
            outlineVariant = androidx.compose.ui.graphics.Color(0xFF2A2B31),
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF00695C),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFB2DFDB),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF003D36),
            secondary = androidx.compose.ui.graphics.Color(0xFFE0785F),
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD8CF),
            tertiary = androidx.compose.ui.graphics.Color(0xFF795548),
            tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF4EEEA),
            error = androidx.compose.ui.graphics.Color(0xFFC5544C),
            errorContainer = androidx.compose.ui.graphics.Color(0xFFFFF1ED),
            background = androidx.compose.ui.graphics.Color(0xFFF6F7FA),
            onBackground = androidx.compose.ui.graphics.Color(0xFF20242C),
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color(0xFF20242C),
            surfaceVariant = androidx.compose.ui.graphics.Color.White,
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF737780),
            surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
            surfaceContainerLow = androidx.compose.ui.graphics.Color.White,
            surfaceContainer = androidx.compose.ui.graphics.Color.White,
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFFAFBFD),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFF1F3F7),
            outline = androidx.compose.ui.graphics.Color(0xFFD8DCE3),
            outlineVariant = androidx.compose.ui.graphics.Color(0xFFE5E8ED),
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(26.dp),
        ),
        content = content,
    )
}
