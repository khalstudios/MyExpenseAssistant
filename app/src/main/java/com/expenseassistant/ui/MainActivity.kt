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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import androidx.compose.ui.Alignment
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
import com.expenseassistant.ui.budget.BudgetScreen
import com.expenseassistant.ui.detail.TransactionDetailScreen
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
                Surface {
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

private enum class Tab(val label: String) { TRANSACTIONS("Transactions"), INSIGHTS("Insights") }

private sealed interface Route {
    data object Main : Route
    data object Add : Route
    data object Budgets : Route
    data object Account : Route
    data class Detail(val id: Long) : Route
    data class CategoryTransactions(val category: Category) : Route
    data class TagTransactions(val tag: String) : Route
    data object NeedsReview : Route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentState by viewModel.recentState.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val recurring by viewModel.recurring.collectAsStateWithLifecycle()
    val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
    val tagUsage by viewModel.tagUsage.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val summaryScope by viewModel.summaryScope.collectAsStateWithLifecycle()
    val needsReview by viewModel.needsReviewTransactions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tab by remember { mutableStateOf(Tab.TRANSACTIONS) }
    var route by remember { mutableStateOf<Route>(Route.Main) }

    var notificationAccess by remember { mutableStateOf(PermissionStatus.isNotificationAccessGranted(context)) }
    var accessibility by remember { mutableStateOf(PermissionStatus.isAccessibilityGranted(context)) }

    // Re-check after the user returns from system settings.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationAccess = PermissionStatus.isNotificationAccessGranted(context)
            accessibility = PermissionStatus.isAccessibilityGranted(context)
        }
    }

    // Budget alerts need runtime permission from Android 13 onwards.
    val postNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

        Route.Account -> {
            AccountScreen(
                onBack = { route = Route.Main },
                onOpenBudgets = { route = Route.Budgets },
            )
            return
        }

        is Route.Detail -> {
            // Looked up straight from the database so transactions outside the visible period still open.
            val transactionFlow = remember(current.id) { viewModel.observeTransaction(current.id) }
            val transaction by transactionFlow.collectAsStateWithLifecycle(initialValue = null)
            transaction?.let { detail ->
                TransactionDetailScreen(
                    transaction = detail,
                    onBack = { route = Route.Main },
                    onCategoryChange = { viewModel.recategorize(detail.id, it) },
                    onCategoryChangeCustom = { name, colorHex, iconKey ->
                        viewModel.recategorize(detail.id, Category.OTHER, name, colorHex, iconKey)
                    },
                    customCategories = customCategories,
                    onDescriptionChange = { viewModel.updateDescription(detail.id, it) },
                    onTagsChange = { viewModel.updateTags(detail.id, it) },
                    onPaymentModeChange = { viewModel.updatePaymentMode(detail.id, it) },
                    onCoreChange = { amountMinor, direction, merchant, occurredAt ->
                        viewModel.updateCore(detail.id, amountMinor, direction, merchant, occurredAt)
                    },
                    onDelete = {
                        viewModel.delete(detail.id)
                        route = Route.Main
                    },
                    tagSuggestions = tagSuggestions,
                    onOpenTag = { tag -> route = Route.TagTransactions(tag) },
                )
            }
            // Nothing to fall through to while the row loads, or if it was just deleted.
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
                accessibilityGranted = accessibility,
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

        Route.Main -> Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == Tab.TRANSACTIONS) "Expense Assistant" else "Insights") },
                actions = {
                    IconButton(onClick = { route = Route.Account }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { route = Route.Add },
                modifier = Modifier.offset(y = 44.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = tab == Tab.TRANSACTIONS,
                        onClick = { tab = Tab.TRANSACTIONS },
                        icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                        label = { Text(Tab.TRANSACTIONS.label) },
                    )
                    Spacer(Modifier.width(96.dp))
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = tab == Tab.INSIGHTS,
                        onClick = { tab = Tab.INSIGHTS },
                        icon = { Icon(Icons.Filled.PieChart, contentDescription = null) },
                        label = { Text(Tab.INSIGHTS.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.TRANSACTIONS -> HomeScreen(
                state = recentState,
                notificationAccessGranted = notificationAccess,
                accessibilityGranted = accessibility,
                onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                onCategoryChangeCustom = { id, name, colorHex, iconKey -> viewModel.recategorize(id, Category.OTHER, name, colorHex, iconKey) },
                customCategories = customCategories,
                summaryScope = summaryScope,
                onSummaryScopeChange = viewModel::setSummaryScope,
                onOpenNeedsReview = { route = Route.NeedsReview },
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
                onManageBudgets = { route = Route.Budgets },
                onOpenCategory = { route = Route.CategoryTransactions(it) },
                onOpenTag = { tag -> route = Route.TagTransactions(tag) },
                onOpenNeedsReview = { route = Route.NeedsReview },
                modifier = Modifier.padding(padding),
            )
        }
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
            background = androidx.compose.ui.graphics.Color(0xFF0D0D0F),
            onBackground = androidx.compose.ui.graphics.Color(0xFFF2F2F2),
            surface = androidx.compose.ui.graphics.Color(0xFF1A1A1D),
            onSurface = androidx.compose.ui.graphics.Color(0xFFF2F2F2),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A1A1D),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF9A9A9E),
            // Card/sheet containers default to these; keep them flat so no grey band shows.
            surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF141416),
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF1A1A1D),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1A1A1D),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF212125),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF26262B),
            outline = androidx.compose.ui.graphics.Color(0xFF3A3A3E),
            outlineVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2E),
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
            background = androidx.compose.ui.graphics.Color(0xFFF4F1EE),
            onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1A),
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1A),
            surfaceVariant = androidx.compose.ui.graphics.Color.White,
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6E6A67),
            surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
            surfaceContainerLow = androidx.compose.ui.graphics.Color.White,
            surfaceContainer = androidx.compose.ui.graphics.Color.White,
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFFAF7F4),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFF4F1EE),
            outline = androidx.compose.ui.graphics.Color(0xFFDCD2CB),
            outlineVariant = androidx.compose.ui.graphics.Color(0xFFE8E1DB),
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
