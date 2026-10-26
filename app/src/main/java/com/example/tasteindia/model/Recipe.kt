package com.example.tasteindia.model

data class Recipe(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String,
    val isFavorite: Boolean = false
)
