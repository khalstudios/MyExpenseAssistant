package com.khaltech.expenseassistant.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.khaltech.expenseassistant.service.PermissionStatus
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.ui.account.AccountScreen
import com.khaltech.expenseassistant.ui.add.AddTransactionScreen
import com.khaltech.expenseassistant.ui.budget.BudgetBreakdownScreen
import com.khaltech.expenseassistant.ui.budget.BudgetScreen
import com.khaltech.expenseassistant.ui.detail.TransactionDetailScreen
import com.khaltech.expenseassistant.ui.history.AllTransactionsScreen
import com.khaltech.expenseassistant.ui.insights.InsightsScreen
import com.khaltech.expenseassistant.data.repo.taggedWith
import com.khaltech.expenseassistant.ui.insights.PeriodSelection
import com.khaltech.expenseassistant.ui.insights.Periods
import com.khaltech.expenseassistant.ui.pro.LocalPro
import com.khaltech.expenseassistant.ui.pro.ProHost
import com.khaltech.expenseassistant.recurring.RecurringExpense
import com.khaltech.expenseassistant.ui.recurring.AddRecurringScreen
import com.khaltech.expenseassistant.ui.tag.TagScreen
import com.khaltech.expenseassistant.ui.category.CategoriesScreen
import com.khaltech.expenseassistant.ui.category.CategoryScreen
import com.khaltech.expenseassistant.ui.tag.TagsScreen
import com.khaltech.expenseassistant.ui.merchant.MerchantScreen
import com.khaltech.expenseassistant.ui.merchant.MerchantsScreen
import com.khaltech.expenseassistant.data.repo.merchantKey
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.ui.category.CustomCategoryActions
import com.khaltech.expenseassistant.ui.category.LocalCategoriesInUse
import com.khaltech.expenseassistant.ui.category.LocalCategoryColorOverrides
import com.khaltech.expenseassistant.ui.category.LocalCategoryIconOverrides
import com.khaltech.expenseassistant.ui.category.LocalCategoryUsage
import com.khaltech.expenseassistant.ui.category.LocalCustomCategoryActions
import com.khaltech.expenseassistant.ui.category.LocalCustomCategoryCount
import com.khaltech.expenseassistant.di.ServiceLocator
import com.khaltech.expenseassistant.notify.TransactionNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    /** A transaction a tapped "Transaction recorded" alert asked to open; cleared once it is open. */
    private var openTransactionId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        takeTransactionToOpen(intent)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val iconStore = remember { ServiceLocator.categoryIconStore(context) }
                    val iconOverrides by iconStore.overrides.collectAsStateWithLifecycle()
                    val colorStore = remember { ServiceLocator.categoryColorStore(context) }
                    val colorOverrides by colorStore.overrides.collectAsStateWithLifecycle()
                    // The same instance AppContent resolves below, this Activity being the store
                    // owner for both, so the picker's category list is read from one source.
                    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                    val categoriesInUse by homeViewModel.categoriesInUse.collectAsStateWithLifecycle()
                    val categoryUsage by homeViewModel.categoryUsage.collectAsStateWithLifecycle()
                    val customCategories by homeViewModel.customCategories.collectAsStateWithLifecycle()
                    val customCategoryActions = remember(homeViewModel) {
                        CustomCategoryActions(
                            countTransactions = homeViewModel::transactionsUnderCustomCategory,
                            delete = { homeViewModel.deleteCustomCategory(it) },
                            update = { name, newName, colorHex, iconKey ->
                                homeViewModel.updateCustomCategory(name, newName, colorHex, iconKey)
                            },
                        )
                    }
                    CompositionLocalProvider(
                        LocalCategoryIconOverrides provides iconOverrides,
                        LocalCategoryColorOverrides provides colorOverrides,
                        LocalCategoriesInUse provides categoriesInUse,
                        LocalCustomCategoryActions provides customCategoryActions,
                        LocalCategoryUsage provides categoryUsage,
                        LocalCustomCategoryCount provides customCategories.size,
                    ) {
                        ProHost {
                            AppShell(
                                openTransactionId = openTransactionId,
                                onTransactionOpened = { openTransactionId = null },
                            )
                        }
                    }
                }
            }
        }
    }

    /** The app was already open when the alert was tapped. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeTransactionToOpen(intent)
    }

    private fun takeTransactionToOpen(intent: Intent?) {
        val id = intent?.getLongExtra(TransactionNotifier.EXTRA_TRANSACTION_ID, -1L) ?: -1L
        if (id < 0) return
        openTransactionId = id
        // Taken once: rotating the screen re-reads this intent and must not open the details again.
        intent?.removeExtra(TransactionNotifier.EXTRA_TRANSACTION_ID)
    }
}

private enum class Tab(val label: String) { HOME("Home"), INSIGHTS("Insights"), BUDGET("Budget"), PROFILE("Profile") }

private sealed interface Route {
    data object Add : Route
    data object Budgets : Route
    data class Detail(val id: Long) : Route
    data class CategoryTransactions(val category: Category) : Route
    /** A null [period] means the whole history, which is what the transaction detail screen wants. */
    data class TagTransactions(val tag: String, val period: PeriodSelection? = null) : Route
    data object NeedsReview : Route
    data object AllTransactions : Route
    /** The home summary's income or expenditure, over the scope it showed when tapped. */
    data class SummaryTransactions(val flow: SummaryFlow, val scope: SummaryScope) : Route
    data object Categories : Route
    data object Tags : Route
    data object Merchants : Route
    data class Merchant(val key: String) : Route
    /** A null [item] is a new entry; otherwise the row being opened. */
    data class EditRecurring(val item: RecurringExpense? = null) : Route
}

/** One opened screen; [key] is unique per visit so the same screen opened twice keeps separate state. */
private data class BackStackEntry(val route: Route, val key: String = UUID.randomUUID().toString())

private const val MAIN_STATE_KEY = "main"

/** A little shorter than Material's 80dp navigation bar. */
private val BottomBarHeight = 68.dp

/** Long enough to read one line, short enough not to sit over the app. */
private const val TipVisibleMillis = 10_000L
private const val TipEveryOpenings = 3
private const val HintDelayMillis = 700L

/** Clears the centre add button, which overhangs the bottom bar. */
private val FabClearance = 56.dp

/** Dims the app behind the tip so only the message reads. */
private val ScrimColor = Color(0x99000000)


@OptIn(ExperimentalMaterial3Api::class)
/**
 * The app plus one message bar over whichever screen is showing, so a screen that closes itself
 * after a save can still say so on the screen it returns to.
 */
@Composable
private fun AppShell(openTransactionId: Long?, onTransactionOpened: () -> Unit) {
    val messages = remember { SnackbarHostState() }
    var overTabs by remember { mutableStateOf(true) }
    // Any touch dismisses the message, and still reaches what was touched.
    Box(Modifier.fillMaxSize().dismissMessageOnTouch(messages)) {
        AppContent(
            messages = messages,
            onOverTabsChange = { overTabs = it },
            openTransactionId = openTransactionId,
            onTransactionOpened = onTransactionOpened,
        )
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        SnackbarHost(
            messages,
            // On the tabs it sits just above the bottom bar, where the Profile screen shows its messages.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navInset + if (overTabs) BottomBarHeight else 16.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppContent(
    messages: SnackbarHostState,
    onOverTabsChange: (Boolean) -> Unit,
    openTransactionId: Long?,
    onTransactionOpened: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val messageScope = rememberCoroutineScope()
    fun showMessage(text: String) {
        messageScope.launch { messages.showBriefly(text) }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentState by viewModel.recentState.collectAsStateWithLifecycle()
    val hasAnyTransactions by viewModel.hasAnyTransactions.collectAsStateWithLifecycle()
    val hasAnyBudget by viewModel.hasAnyBudget.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val monthlyBudgetAnalytics by viewModel.monthlyBudgetAnalytics.collectAsStateWithLifecycle()
    val yearlyBudgetAnalytics by viewModel.yearlyBudgetAnalytics.collectAsStateWithLifecycle()
    val recurring by viewModel.recurring.collectAsStateWithLifecycle()
    val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
    val merchantSuggestions by viewModel.merchantSuggestions.collectAsStateWithLifecycle()
    val noteSuggestions by viewModel.noteSuggestions.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val tagUsage by viewModel.tagUsage.collectAsStateWithLifecycle()
    val summaryScope by viewModel.summaryScope.collectAsStateWithLifecycle()
    val dailyBudget by viewModel.dailyBudgetMinor.collectAsStateWithLifecycle()
    val spendingStatus by viewModel.spendingStatus.collectAsStateWithLifecycle()
    val explicitDailyBudget by viewModel.explicitDailyBudgetMinor.collectAsStateWithLifecycle()
    val spendingTips by viewModel.spendingTips.collectAsStateWithLifecycle()
    val needsReview by viewModel.needsReviewTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tab by remember { mutableStateOf(Tab.HOME) }
    // Screens opened over the tabs, newest last. Each one keeps its own saved state, so going back
    // returns to the previous screen scrolled to where it was left.
    val backStack = remember { mutableStateListOf<BackStackEntry>() }
    val saveableStateHolder = rememberSaveableStateHolder()
    fun navigate(route: Route) {
        // A double tap on a row would otherwise open the same screen twice.
        if (backStack.lastOrNull()?.route != route) backStack.add(BackStackEntry(route))
    }
    fun goBack() {
        // Drop the closed screen's state so opening it again starts fresh.
        backStack.removeLastOrNull()?.let { saveableStateHolder.removeState(it.key) }
    }

    var notificationAccess by remember { mutableStateOf(PermissionStatus.isNotificationAccessGranted(context)) }

    val userPreferences = remember { ServiceLocator.userPreferences(context) }
    val pro = LocalPro.current
    var helpMenuOpen by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    // A new user gets one short welcome with a single setup step; the full walkthrough waits under Help.
    var showWelcome by remember { mutableStateOf(!userPreferences.isTutorialSeen()) }
    var showCaptureDisclosure by remember { mutableStateOf(false) }

    // Bank SMS also need the SMS app's own notifications: asked once just after access is granted,
    // then a Home card if days go by without a single SMS notification arriving.
    var showSmsPrompt by remember { mutableStateOf(false) }
    var showSmsNotice by remember { mutableStateOf(false) }
    fun offerSmsPrompt() {
        if (PermissionStatus.isNotificationAccessGranted(context) &&
            !userPreferences.isSmsPromptSeen() && !userPreferences.isSmsNotificationSeen()
        ) {
            showSmsPrompt = true
        }
    }

    // Payment and budget alerts need runtime permission from Android 13 onwards. It is asked for
    // once capture is switched on, or when the first budget is set, when those alerts start to mean
    // something, not at first launch.
    val postNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { offerSmsPrompt() }

    // Only the change from no budget to a first one asks, so a user who already has budgets and
    // said no is not asked again on every launch.
    var budgetsSeenEmpty by remember { mutableStateOf(false) }
    LaunchedEffect(hasAnyBudget) {
        when (hasAnyBudget) {
            false -> budgetsSeenEmpty = true
            true -> if (budgetsSeenEmpty && PermissionStatus.needsPostNotificationsPermission(context)) {
                budgetsSeenEmpty = false
                postNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            null -> Unit
        }
    }

    // Re-check after the user returns from system settings.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val granted = PermissionStatus.isNotificationAccessGranted(context)
            if (granted) userPreferences.markCaptureConnected(System.currentTimeMillis())
            if (granted && !notificationAccess) {
                if (PermissionStatus.needsPostNotificationsPermission(context)) {
                    postNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    offerSmsPrompt()
                }
            }
            notificationAccess = granted
            showSmsNotice = granted && !showSmsPrompt && userPreferences.isSmsCheckDue(System.currentTimeMillis())
        }
    }

    // Every 3 days until automatic backups are on, starting 3 days after install so a new user is
    // not offered Pro straight away. A free user is taken to the upgrade rather than to a setup
    // dialog they could not finish.
    var showBackupReminder by remember { mutableStateOf(false) }
    var openAutoBackupSetup by remember { mutableStateOf(false) }

    val dialogOpen = showWelcome || showCaptureDisclosure || showTutorial || showSmsPrompt || showBackupReminder

    var tip by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dialogOpen) {
        val now = System.currentTimeMillis()
        if (!dialogOpen && tip == null && userPreferences.isBackupReminderDue(now)) {
            showBackupReminder = true
            userPreferences.markBackupReminderShown(now)
        }
    }

    // A tip every third opening: shown on the first batch of data, then it closes itself. It waits
    // for any open dialog. The opening is counted once, not again when the screen is recreated on
    // rotation.
    var tipShown by rememberSaveable { mutableStateOf(false) }
    val openingsSinceTip = rememberSaveable { userPreferences.recordOpening() }
    LaunchedEffect(spendingTips, dialogOpen) {
        // Reads the reminder state itself: the reminder may have opened earlier in this same frame.
        if (!tipShown && tip == null && openingsSinceTip >= TipEveryOpenings && !dialogOpen &&
            !showBackupReminder && spendingTips.isNotEmpty()
        ) {
            tip = spendingTips.random()
            tipShown = true
            userPreferences.markTipShown()
        }
    }
    LaunchedEffect(tip) {
        if (tip != null) {
            delay(TipVisibleMillis)
            tip = null
        }
    }

    // A new install's feature hints, in the same bar: each once, when its feature is on screen with
    // nothing else competing for attention.
    val dueHint = when {
        dialogOpen || tip != null || backStack.isNotEmpty() -> null
        tab == Tab.HOME && hasAnyTransactions == true -> FeatureHint.EDIT_TRANSACTION
        tab == Tab.INSIGHTS -> FeatureHint.INSIGHTS
        tab == Tab.BUDGET -> FeatureHint.BUDGET
        else -> null
    }?.takeIf { userPreferences.isHintPending(it.key) }
    LaunchedEffect(dueHint) {
        val hint = dueHint ?: return@LaunchedEffect
        // Lets the screen settle first; leaving it in the meantime cancels the hint for later.
        delay(HintDelayMillis)
        tip = hint.text
        userPreferences.markHintShown(hint.key)
    }

    BackHandler(enabled = backStack.isNotEmpty()) { goBack() }

    // A tapped "Transaction recorded" alert opens that transaction over whatever is showing; Back
    // returns there.
    LaunchedEffect(openTransactionId) {
        openTransactionId?.let { id ->
            navigate(Route.Detail(id))
            onTransactionOpened()
        }
    }

    val top = backStack.lastOrNull()
    SideEffect { onOverTabsChange(top == null) }
    if (top != null) {
        saveableStateHolder.SaveableStateProvider(top.key) {
            when (val current = top.route) {
                Route.Add -> {
                    AddTransactionScreen(
                        onBack = { goBack() },
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
                            goBack()
                            showMessage("Transaction added")
                        },
                        customCategories = customCategories,
                        tagSuggestions = tagSuggestions,
                        merchantSuggestions = merchantSuggestions,
                        noteSuggestions = noteSuggestions,
                    )
                }

                is Route.EditRecurring -> {
                    val opened = current.item
                    AddRecurringScreen(
                        onBack = { goBack() },
                        existing = opened,
                        onSave = { input ->
                            viewModel.saveRecurringPlan(
                                id = input.id,
                                merchant = input.merchant,
                                category = input.category,
                                amountMinor = input.amountMinor,
                                cadence = input.cadence,
                                nextDueAt = input.nextDueAt,
                            )
                            goBack()
                        },
                        // Nothing to delete on a blank form, so the action is simply absent there.
                        onDelete = opened?.let { item ->
                            {
                                viewModel.deleteRecurring(item.manualId, item.merchantKey)
                                goBack()
                            }
                        },
                    )
                }

                Route.Budgets -> {
                    BudgetScreen(
                        onBack = { goBack() },
                        onOpenCategory = { navigate(Route.CategoryTransactions(it)) },
                    )
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
                            onBack = { goBack() },
                            // The screen closes itself after saving, so the message lands on the one beneath.
                            onSave = { edits ->
                                viewModel.saveDetails(detail.id, edits)
                                showMessage("Transaction saved")
                            },
                            onDelete = {
                                viewModel.delete(detail.id)
                                goBack()
                                showMessage("Transaction deleted")
                            },
                            customCategories = customCategories,
                            tagSuggestions = tagSuggestions,
                            onOpenTag = { tag -> navigate(Route.TagTransactions(tag)) },
                            merchantSuggestions = merchantSuggestions,
                            noteSuggestions = noteSuggestions,
                        )
                    } ?: Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    )
                }

                is Route.CategoryTransactions -> {
                    val categoryList = (state.transactions + analytics.transactions)
                        .distinctBy { it.id }
                        .filter { it.category == current.category }
                    CategoryScreen(
                        category = current.category,
                        transactions = categoryList,
                        onBack = { goBack() },
                        onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                        onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                        onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                        customCategories = customCategories,
                        onDelete = { id -> viewModel.delete(id) },
                    )
                }

                Route.NeedsReview -> {
                    // Its own scaffold, like the other pushed screens: the window is edge to edge, so
                    // without one the list runs under the status bar and the navigation bar.
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = { Text("Needs a category", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = { goBack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        HomeScreen(
                            state = recentState,
                            notificationAccessGranted = notificationAccess,
                            onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                            onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                            customCategories = customCategories,
                            onDelete = { id -> viewModel.delete(id) },
                            onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                            needsReviewFilter = true,
                            transactionOverride = needsReview,
                            onClearFilter = { goBack() },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }

                is Route.TagTransactions -> {
                    val period = current.period
                    // Filtered from the list already in memory rather than re-queried: an async
                    // load left the screen blank for a frame, which flashed the window background.
                    val list = remember(current, allTransactions) {
                        allTransactions.taggedWith(
                            current.tag,
                            period?.let { Periods.start(it) },
                            period?.let { Periods.endExclusive(it) },
                        )
                    }
                    TagScreen(
                        tag = current.tag,
                        transactions = list,
                        periodLabel = period?.let { Periods.label(it) },
                        onBack = { goBack() },
                        onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                    )
                }

                Route.Categories -> {
                    CategoriesScreen(
                        customCategories = customCategories,
                        onBack = { goBack() },
                    )
                }

                Route.Tags -> {
                    TagsScreen(
                        tags = tagUsage,
                        onBack = { goBack() },
                        onOpenTag = { tag -> navigate(Route.TagTransactions(tag)) },
                    )
                }

                Route.Merchants -> {
                    MerchantsScreen(
                        merchants = merchants,
                        onBack = { goBack() },
                        onOpenMerchant = { key -> navigate(Route.Merchant(key)) },
                    )
                }

                is Route.Merchant -> {
                    val merchant = merchants.firstOrNull { it.key == current.key }
                    val payments = remember(current.key, allTransactions) {
                        allTransactions.filter { it.direction == Direction.DEBIT && it.merchantKey == current.key }
                    }
                    merchant?.let {
                        MerchantScreen(
                            merchant = it,
                            payments = payments,
                            onBack = { goBack() },
                            onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                        )
                    } ?: Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    )
                }

                Route.AllTransactions -> {
                    AllTransactionsScreen(
                        transactions = allTransactions,
                        onBack = { goBack() },
                        onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                        onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                        onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                        customCategories = customCategories,
                        onDelete = { id -> viewModel.delete(id) },
                    )
                }

                is Route.SummaryTransactions -> {
                    val periodLabel = when (current.scope) {
                        SummaryScope.MONTH -> currentMonthYearName()
                        SummaryScope.YEAR -> yearToDateLabel()
                        SummaryScope.ALL -> current.scope.label
                    }
                    AllTransactionsScreen(
                        // recentState already holds exactly the summary's scope; the card is not reachable
                        // to change it while this screen is open.
                        transactions = recentState.transactions.filter { current.flow.includes(it) },
                        title = "${current.flow.title} · $periodLabel",
                        emptyText = current.flow.emptyText,
                        onBack = { goBack() },
                        onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                        onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                        onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                        customCategories = customCategories,
                        onDelete = { id -> viewModel.delete(id) },
                    )
                }
            }
        }
        return
    }

    val topBarTitle = when (tab) {
        Tab.HOME -> "Kahan Gaya Paisa"
        Tab.INSIGHTS -> "Insights"
        Tab.BUDGET -> "Budget"
        Tab.PROFILE -> "Profile"
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { AppLogo(Modifier.padding(start = 16.dp, end = 4.dp)) },
                title = { Text(topBarTitle, fontWeight = FontWeight.SemiBold) },
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
                onClick = { navigate(Route.Add) },
                modifier = Modifier.offset(y = 54.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            // Material's bar is 80dp with no height setting; the fixed height also has to cover the
            // system navigation area the bar pads itself above.
            val systemNavHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            NavigationBar(
                modifier = Modifier
                    .height(BottomBarHeight + systemNavHeight)
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
        // Keeps the visible tab's scroll position while a screen is open on top of it.
        saveableStateHolder.SaveableStateProvider(MAIN_STATE_KEY) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    state = recentState,
                    notificationAccessGranted = notificationAccess,
                    noTransactionsYet = hasAnyTransactions == false,
                    showSmsNotice = showSmsNotice,
                    onCheckSmsSettings = {
                        runCatching { context.startActivity(PermissionStatus.smsNotificationSettingsIntent(context)) }
                    },
                    onDismissSmsNotice = {
                        userPreferences.dismissSmsNotice()
                        showSmsNotice = false
                    },
                    onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                    onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                    customCategories = customCategories,
                    summaryScope = summaryScope,
                    onSummaryScopeChange = viewModel::setSummaryScope,
                    budgetState = monthlyBudgetAnalytics,
                    dailyBudgetMinor = dailyBudget,
                    dailyBudgetIsExplicit = explicitDailyBudget > 0,
                    onSetDailyBudget = viewModel::setDailyBudget,
                    spendingStatus = spendingStatus,
                    onOpenNeedsReview = { navigate(Route.NeedsReview) },
                    onOpenAllTransactions = { navigate(Route.AllTransactions) },
                    onOpenSummaryTransactions = { flow -> navigate(Route.SummaryTransactions(flow, summaryScope)) },
                    onDelete = { id -> viewModel.delete(id) },
                    onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                    modifier = Modifier.padding(padding),
                )

                Tab.INSIGHTS -> InsightsScreen(
                    state = analytics,
                    recurring = recurring,
                    onRangeChange = viewModel::setRange,
                    onShiftPeriod = viewModel::shiftPeriod,
                    onJumpTo = viewModel::jumpTo,
                    onResetToCurrent = viewModel::resetToCurrent,
                    onOpenCategory = { navigate(Route.CategoryTransactions(it)) },
                    onOpenTag = { tag -> navigate(Route.TagTransactions(tag, analytics.selection)) },
                    onOpenNeedsReview = { navigate(Route.NeedsReview) },
                    onAddRecurring = { navigate(Route.EditRecurring()) },
                    onOpenRecurring = { item -> navigate(Route.EditRecurring(item)) },
                    onOpenMerchant = { key -> navigate(Route.Merchant(key)) },
                    onOpenTransaction = { id -> navigate(Route.Detail(id)) },
                    modifier = Modifier.padding(padding),
                )

                Tab.BUDGET -> BudgetBreakdownScreen(
                    monthly = monthlyBudgetAnalytics,
                    yearly = yearlyBudgetAnalytics,
                    onManageBudgets = { navigate(Route.Budgets) },
                    onOpenCategory = { navigate(Route.CategoryTransactions(it)) },
                    modifier = Modifier.padding(padding),
                )

                Tab.PROFILE -> AccountScreen(
                    openAutoBackupSetup = openAutoBackupSetup,
                    onAutoBackupSetupHandled = { openAutoBackupSetup = false },
                    onOpenBudgets = { navigate(Route.Budgets) },
                    onOpenNeedsReview = { navigate(Route.NeedsReview) },
                    onOpenCategories = { navigate(Route.Categories) },
                    onOpenTags = { navigate(Route.Tags) },
                    onOpenMerchants = { navigate(Route.Merchants) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

        // Lifted clear of the bottom bar and its centre button, over a scrim that dims everything
        // else. A tap anywhere outside closes it.
        tip?.let { message ->
            val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(ScrimColor)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { tip = null },
            )
            SpendingTipBar(
                text = message,
                onClose = { tip = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = navInset + BottomBarHeight + FabClearance,
                    ),
            )
        }
    }

    if (showBackupReminder) {
        BackupReminderDialog(
            needsPro = !pro.isPro,
            onEnable = {
                showBackupReminder = false
                if (pro.isPro) {
                    openAutoBackupSetup = true
                    tab = Tab.PROFILE
                } else {
                    pro.onUpgrade()
                }
            },
            onDismiss = { showBackupReminder = false },
        )
    }

    if (showSmsPrompt) {
        SmsNotificationDialog(
            onOpenSettings = {
                showSmsPrompt = false
                userPreferences.markSmsPromptSeen()
                runCatching { context.startActivity(PermissionStatus.smsNotificationSettingsIntent(context)) }
            },
            onDismiss = {
                showSmsPrompt = false
                userPreferences.markSmsPromptSeen()
            },
        )
    }

    if (showWelcome) {
        WelcomeDialog(
            captureEnabled = notificationAccess,
            onSetUpCapture = {
                showWelcome = false
                userPreferences.markTutorialSeen()
                userPreferences.queueHints(FeatureHint.entries.map { it.key })
                showCaptureDisclosure = true
            },
            onDismiss = {
                showWelcome = false
                userPreferences.markTutorialSeen()
                userPreferences.queueHints(FeatureHint.entries.map { it.key })
            },
        )
    }

    // Consent is asked for here, before the user reaches the system screen that grants it.
    if (showCaptureDisclosure) {
        DisclosureDialog(
            disclosure = Disclosures.Notifications,
            onAccept = {
                showCaptureDisclosure = false
                runCatching { context.startActivity(PermissionStatus.notificationAccessIntent()) }
            },
            onDismiss = { showCaptureDisclosure = false },
        )
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
