package com.example.tasteindia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.remote.NetworkModule
import com.example.tasteindia.data.repository.RecipeRepository
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

class RecipeViewModel(
    private val repository: RecipeRepository = RecipeRepository(NetworkModule.apiService)
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Loading)
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private val _selectedRecipeId = MutableStateFlow<String?>(null)
    val selectedRecipeId: StateFlow<String?> = _selectedRecipeId.asStateFlow()

    private val _recipeDetailsState = MutableStateFlow<RecipeDetailsUiState>(RecipeDetailsUiState.Idle)
    val recipeDetailsState: StateFlow<RecipeDetailsUiState> = _recipeDetailsState.asStateFlow()

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            val result = repository.getIndianRecipes()
            _uiState.value = result.fold(
                onSuccess = { recipes ->
                    if (recipes.isEmpty()) {
                        RecipeUiState.Error("No Indian recipes found. (Note: TheMealDB fallback searches configured Indian queries like 'chicken curry', 'biryani', etc.)")
                    } else {
                        RecipeUiState.Success(recipes)
                    }
                },
                onFailure = { throwable ->
                    val errorMessage = throwable.localizedMessage ?: "Network or API failure. Please check your internet connection."
                    RecipeUiState.Error(errorMessage)
                }
            )
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
        val currentState = _uiState.value
        if (currentState is RecipeUiState.Success) {
            val updatedRecipes = currentState.recipes.map { recipe ->
                if (recipe.idMeal == recipeId) {
                    recipe.copy(isFavorite = !recipe.isFavorite)
                } else {
                    recipe
                }
            }
            _uiState.value = RecipeUiState.Success(updatedRecipes)
        }
    }
}
