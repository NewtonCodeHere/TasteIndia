package com.example.tasteindia.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tasteindia.ui.viewmodel.SortOrder

@Composable
fun RecipeFilterBar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    ingredients: List<String>,
    selectedIngredient: String?,
    onIngredientSelected: (String?) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesToggled: (Boolean) -> Unit,
    sortOrder: SortOrder,
    onSortChanged: (SortOrder) -> Unit,
    resultCount: Int,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Indian recipes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        // Filters row: Category horizontal scrollable chips
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All Categories") }
            )
            categories.take(15).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategorySelected(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat) }
                )
            }
        }

        // Ingredients horizontal chips
        val ingScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(ingScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedIngredient == null,
                onClick = { onIngredientSelected(null) },
                label = { Text("All Ingredients") }
            )
            ingredients.take(20).forEach { ing ->
                FilterChip(
                    selected = selectedIngredient == ing,
                    onClick = { onIngredientSelected(if (selectedIngredient == ing) null else ing) },
                    label = { Text(ing) }
                )
            }
        }

        // Toggles & Sorting
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Favorites", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = favoritesOnly,
                    onCheckedChange = onFavoritesToggled
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = {
                    onSortChanged(if (sortOrder == SortOrder.AZ) SortOrder.ZA else SortOrder.AZ)
                }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort Order")
                }
                Text(
                    text = if (sortOrder == SortOrder.AZ) "A–Z" else "Z–A",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Result count & Clear All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$resultCount recipes found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onClearAll) {
                Text("Clear All")
            }
        }
    }
}
