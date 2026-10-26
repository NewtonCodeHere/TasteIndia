package com.example.tasteindia.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.remote.NetworkModule
import com.example.tasteindia.data.repository.FavoriteRepository
import com.example.tasteindia.data.repository.RecipeRepository
import com.example.tasteindia.model.Recipe
import com.example.tasteindia.model.RecipeDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecipeDetailsUiState {
    object Idle : RecipeDetailsUiState
    object Loading : RecipeDetailsUiState
    data class Success(val detail: RecipeDetail) : RecipeDetailsUiState
    data class Error(val message: String) : RecipeDetailsUiState
}

enum class SortOrder {
    AZ, ZA
}

class RecipeViewModel(
    private val repository: RecipeRepository = RecipeRepository(NetworkModule.apiService)
) : ViewModel() {

    private var appContext: Context? = null
    private val memoryFavorites = mutableSetOf<String>()

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Loading)
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    // Filter & Sort States
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedIngredient = MutableStateFlow<String?>(null)
    val favoritesOnly = MutableStateFlow(false)
    val sortOrder = MutableStateFlow(SortOrder.AZ)

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _ingredients = MutableStateFlow<List<String>>(emptyList())
    val ingredients: StateFlow<List<String>> = _ingredients.asStateFlow()

    // Cached ID sets for category and ingredient filters to avoid N+1 and unnecessary calls
    private val categoryMealIds = MutableStateFlow<Set<String>?>(null)
    private val ingredientMealIds = MutableStateFlow<Set<String>?>(null)

    // Base authoritative Indian recipes
    private val baseRecipes = MutableStateFlow<List<Recipe>>(emptyList())

    private val _selectedRecipeId = MutableStateFlow<String?>(null)
    val selectedRecipeId: StateFlow<String?> = _selectedRecipeId.asStateFlow()

    private val _recipeDetailsState = MutableStateFlow<RecipeDetailsUiState>(RecipeDetailsUiState.Idle)
    val recipeDetailsState: StateFlow<RecipeDetailsUiState> = _recipeDetailsState.asStateFlow()

    init {
        loadMetadata()
        loadRecipes()
    }

    fun initContext(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            // Reload recipes with context favorites if already loaded
            if (baseRecipes.value.isNotEmpty()) {
                updateVisibleRecipes()
            } else {
                loadRecipes()
            }
        }
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { _categories.value = it }
            repository.getIngredients().onSuccess { _ingredients.value = it }
        }
    }

    private fun getStoredFavoriteIds(): Set<String> {
        val ctx = appContext
        return if (ctx != null) {
            FavoriteRepository.getFavoriteIds(ctx)
        } else {
            memoryFavorites
        }
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            val result = repository.getIndianRecipes()
            val favoriteIds = getStoredFavoriteIds()
            _uiState.value = result.fold(
                onSuccess = { recipes ->
                    val recipesWithFavorites = recipes.map { recipe ->
                        recipe.copy(isFavorite = favoriteIds.contains(recipe.idMeal))
                    }
                    baseRecipes.value = recipesWithFavorites
                    updateVisibleRecipes()
                    if (recipesWithFavorites.isEmpty()) {
                        RecipeUiState.Error("No Indian recipes found.")
                    } else {
                        RecipeUiState.Success(recipesWithFavorites)
                    }
                },
                onFailure = { throwable ->
                    val errorMessage = throwable.localizedMessage ?: "Network or API failure. Please check your internet connection."
                    RecipeUiState.Error(errorMessage)
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        updateVisibleRecipes()
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.value = category
        if (category == null) {
            categoryMealIds.value = null
            updateVisibleRecipes()
        } else {
            viewModelScope.launch {
                repository.getMealIdsByCategory(category).onSuccess { ids ->
                    categoryMealIds.value = ids
                    updateVisibleRecipes()
                }.onFailure {
                    categoryMealIds.value = emptySet()
                    updateVisibleRecipes()
                }
            }
        }
    }

    fun onIngredientSelected(ingredient: String?) {
        selectedIngredient.value = ingredient
        if (ingredient == null) {
            ingredientMealIds.value = null
            updateVisibleRecipes()
        } else {
            viewModelScope.launch {
                repository.getMealIdsByIngredient(ingredient).onSuccess { ids ->
                    ingredientMealIds.value = ids
                    updateVisibleRecipes()
                }.onFailure {
                    ingredientMealIds.value = emptySet()
                    updateVisibleRecipes()
                }
            }
        }
    }

    fun onFavoritesOnlyToggled(enabled: Boolean) {
        favoritesOnly.value = enabled
        updateVisibleRecipes()
    }

    fun onSortOrderChanged(order: SortOrder) {
        sortOrder.value = order
        updateVisibleRecipes()
    }

    fun clearAllFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        selectedIngredient.value = null
        favoritesOnly.value = false
        sortOrder.value = SortOrder.AZ
        categoryMealIds.value = null
        ingredientMealIds.value = null
        updateVisibleRecipes()
    }

    private fun updateVisibleRecipes() {
        val currentBase = baseRecipes.value
        val favoriteIds = getStoredFavoriteIds()
        val syncedBase = currentBase.map { recipe ->
            recipe.copy(isFavorite = favoriteIds.contains(recipe.idMeal))
        }

        val query = searchQuery.value
        val catIds = categoryMealIds.value
        val ingIds = ingredientMealIds.value
        val favOnly = favoritesOnly.value
        val sort = sortOrder.value

        val filteredAndSorted = filterAndSortRecipes(
            baseRecipes = syncedBase,
            searchQuery = query,
            selectedCategoryMealIds = catIds,
            selectedIngredientMealIds = ingIds,
            favoritesOnly = favOnly,
            sortOrder = sort
        )

        _uiState.value = RecipeUiState.Success(filteredAndSorted)
    }

    // Pure testable function for filtering, intersection, and sorting
    fun filterAndSortRecipes(
        baseRecipes: List<Recipe>,
        searchQuery: String,
        selectedCategoryMealIds: Set<String>?,
        selectedIngredientMealIds: Set<String>?,
        favoritesOnly: Boolean,
        sortOrder: SortOrder
    ): List<Recipe> {
        val baseIds = baseRecipes.map { it.idMeal }.toSet()

        return baseRecipes.filter { recipe ->
            // 1. Authoritative base Indian set intersection
            if (!baseIds.contains(recipe.idMeal)) return@filter false

            // 2. Meal-name search
            if (searchQuery.isNotBlank() && !recipe.strMeal.contains(searchQuery, ignoreCase = true)) {
                return@filter false
            }

            // 3. Category filter intersection
            if (selectedCategoryMealIds != null && !selectedCategoryMealIds.contains(recipe.idMeal)) {
                return@filter false
            }

            // 4. Ingredient filter intersection
            if (selectedIngredientMealIds != null && !selectedIngredientMealIds.contains(recipe.idMeal)) {
                return@filter false
            }

            // 5. Favorites-only filter
            if (favoritesOnly && !recipe.isFavorite) {
                return@filter false
            }

            true
        }.let { filtered ->
            when (sortOrder) {
                SortOrder.AZ -> filtered.sortedBy { it.strMeal.lowercase() }
                SortOrder.ZA -> filtered.sortedByDescending { it.strMeal.lowercase() }
            }
        }
    }

    fun selectRecipe(idMeal: String) {
        _selectedRecipeId.value = idMeal
        viewModelScope.launch {
            _recipeDetailsState.value = RecipeDetailsUiState.Loading
            val result = repository.getRecipeDetails(idMeal)
            _recipeDetailsState.value = result.fold(
                onSuccess = { detail -> RecipeDetailsUiState.Success(detail) },
                onFailure = { error -> RecipeDetailsUiState.Error(error.localizedMessage ?: "Failed to load recipe details.") }
            )
        }
    }

    fun clearSelectedRecipe() {
        _selectedRecipeId.value = null
        _recipeDetailsState.value = RecipeDetailsUiState.Idle
    }

    fun toggleFavorite(recipeId: String) {
        val ctx = appContext
        val isCurrentlyFavorite = if (ctx != null) {
            FavoriteRepository.isFavorite(ctx, recipeId)
        } else {
            memoryFavorites.contains(recipeId)
        }

        if (ctx != null) {
            if (isCurrentlyFavorite) {
                FavoriteRepository.removeFavorite(ctx, recipeId)
            } else {
                FavoriteRepository.addFavorite(ctx, recipeId)
            }
        } else {
            if (isCurrentlyFavorite) {
                memoryFavorites.remove(recipeId)
            } else {
                memoryFavorites.add(recipeId)
            }
        }

        val updatedBase = baseRecipes.value.map { recipe ->
            if (recipe.idMeal == recipeId) {
                recipe.copy(isFavorite = !isCurrentlyFavorite)
            } else {
                recipe
            }
        }
        baseRecipes.value = updatedBase
        updateVisibleRecipes()
    }
}
