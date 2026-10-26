package com.example.tasteindia.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {

    @GET("filter.php")
    suspend fun getIndianMeals(
        @Query("a") area: String = "Indian"
    ): MealResponse

    @GET("search.php")
    suspend fun searchMeals(
        @Query("s") query: String
    ): MealResponse

    @GET("search.php")
    suspend fun searchMealsByFirstLetter(
        @Query("f") letter: String
    ): MealResponse

    @GET("lookup.php")
    suspend fun lookupMeal(
        @Query("i") id: String
    ): MealResponse

    @GET("list.php")
    suspend fun getCategories(
        @Query("c") list: String = "list"
    ): CategoryResponse

    @GET("list.php")
    suspend fun getIngredients(
        @Query("i") list: String = "list"
    ): IngredientResponse

    @GET("filter.php")
    suspend fun getMealsByCategory(
        @Query("c") category: String
    ): MealResponse

    @GET("filter.php")
    suspend fun getMealsByIngredient(
        @Query("i") ingredient: String
    ): MealResponse
}
