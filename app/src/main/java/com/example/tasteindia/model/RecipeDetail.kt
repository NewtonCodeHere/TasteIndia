package com.example.tasteindia.model

data class RecipeDetail(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String,
    val strCategory: String,
    val strArea: String,
    val strInstructions: String,
    val strSource: String,
    val strYoutube: String,
    val ingredients: List<Pair<String, String>>
)
