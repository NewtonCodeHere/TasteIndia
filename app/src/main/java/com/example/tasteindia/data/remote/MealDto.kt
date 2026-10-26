package com.example.tasteindia.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MealResponse(
    @Json(name = "meals")
    val meals: List<MealDto>?
)

@JsonClass(generateAdapter = true)
data class MealDto(
    @Json(name = "idMeal")
    val idMeal: String?,
    @Json(name = "strMeal")
    val strMeal: String?,
    @Json(name = "strMealThumb")
    val strMealThumb: String?,
    @Json(name = "strArea")
    val strArea: String? = null
)
