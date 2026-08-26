package com.expenseassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
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
import com.expenseassistant.ui.account.AccountScreen
import com.expenseassistant.ui.add.AddTransactionScreen
import com.expenseassistant.ui.budget.BudgetScreen
import com.expenseassistant.ui.detail.TransactionDetailScreen
import com.expenseassistant.ui.insights.InsightsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface {
                    AppShell()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
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
                        paymentMode = input.paymentMode,
                        occurredAt = input.occurredAt,
                        description = input.description,
                        tags = input.tags,
                    )
                    route = Route.Main
                },
            )
            return
        }

        Route.Budgets -> {
            BudgetScreen(onBack = { route = Route.Main })
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
            val transaction = state.transactions.firstOrNull { it.id == current.id }
            if (transaction != null) {
                TransactionDetailScreen(
                    transaction = transaction,
                    onBack = { route = Route.Main },
                    onCategoryChange = { viewModel.recategorize(transaction.id, it) },
                    onDescriptionChange = { viewModel.updateDescription(transaction.id, it) },
                    onTagsChange = { viewModel.updateTags(transaction.id, it) },
                    onPaymentModeChange = { viewModel.updatePaymentMode(transaction.id, it) },
                    onDelete = {
                        viewModel.delete(transaction.id)
                        route = Route.Main
                    },
                )
                return
            }
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
                modifier = Modifier.padding(top = 40.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.TRANSACTIONS,
                    onClick = { tab = Tab.TRANSACTIONS },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text(Tab.TRANSACTIONS.label) },
                )
                // Spacer so the centre FAB does not overlap either tab.
                NavigationBarItem(
                    selected = false,
                    enabled = false,
                    onClick = {},
                    icon = {},
                )
                NavigationBarItem(
                    selected = tab == Tab.INSIGHTS,
                    onClick = { tab = Tab.INSIGHTS },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = null) },
                    label = { Text(Tab.INSIGHTS.label) },
                )
            }
        },
    ) { padding ->
        when (tab) {
            Tab.TRANSACTIONS -> HomeScreen(
                state = state,
                analytics = analytics,
                notificationAccessGranted = notificationAccess,
                accessibilityGranted = accessibility,
                onCategoryChange = { id, category -> viewModel.recategorize(id, category) },
                onDelete = { id -> viewModel.delete(id) },
                onOpenTransaction = { id -> route = Route.Detail(id) },
                onRangeChange = viewModel::setRange,
                onShiftPeriod = viewModel::shiftPeriod,
                onJumpTo = viewModel::jumpTo,
                onResetToCurrent = viewModel::resetToCurrent,
                modifier = Modifier.padding(padding),
            )

            Tab.INSIGHTS -> InsightsScreen(
                state = analytics,
                onRangeChange = viewModel::setRange,
                onShiftPeriod = viewModel::shiftPeriod,
                onJumpTo = viewModel::jumpTo,
                onResetToCurrent = viewModel::resetToCurrent,
                onManageBudgets = { route = Route.Budgets },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
