package com.example.tasteindia.ui.viewmodel

import com.example.tasteindia.model.Recipe

sealed interface RecipeUiState {
    object Loading : RecipeUiState
    data class Success(val recipes: List<Recipe>) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}
