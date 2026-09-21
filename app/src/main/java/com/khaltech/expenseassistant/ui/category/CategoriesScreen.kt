package com.khaltech.expenseassistant.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.repo.CustomCategoryOption
import com.khaltech.expenseassistant.ui.pro.LocalPro

/**
 * Every category this user can file under, reached from Profile, each opening the same editor the
 * picker's Edit mode uses.
 *
 * There is no "create" here on purpose: a category of the user's own exists only on the
 * transactions filed under it, so it is always made from the picker, on a transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    customCategories: List<CustomCategoryOption>,
    onBack: () -> Unit,
) {
    val editor = rememberCategoryEditorState()
    val offered = Category.offered(LocalPro.current.isPro, LocalCategoriesInUse.current)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Tap a category to change its colour or icon. Ones you made can also be renamed or " +
                    "deleted. To make a new one, choose \"Create your own category\" when picking a " +
                    "category for a transaction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CategoryEditList(
                builtIn = offered,
                custom = customCategories,
                onEditBuiltIn = { editor.builtIn = it },
                onEditCustom = { editor.custom = it },
                showCounts = true,
            )
        }
    }

    CategoryEditorDialogs(editor, customCategories)
}
