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

    @GET("lookup.php")
    suspend fun lookupMeal(
        @Query("i") id: String
    ): MealResponse
}
