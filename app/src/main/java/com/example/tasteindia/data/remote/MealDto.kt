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
    val strArea: String? = null,
    @Json(name = "strCategory")
    val strCategory: String? = null,
    @Json(name = "strInstructions")
    val strInstructions: String? = null,
    // Ingredients
    @Json(name = "strIngredient1") val strIngredient1: String? = null,
    @Json(name = "strIngredient2") val strIngredient2: String? = null,
    @Json(name = "strIngredient3") val strIngredient3: String? = null,
    @Json(name = "strIngredient4") val strIngredient4: String? = null,
    @Json(name = "strIngredient5") val strIngredient5: String? = null,
    @Json(name = "strIngredient6") val strIngredient6: String? = null,
    @Json(name = "strIngredient7") val strIngredient7: String? = null,
    @Json(name = "strIngredient8") val strIngredient8: String? = null,
    @Json(name = "strIngredient9") val strIngredient9: String? = null,
    @Json(name = "strIngredient10") val strIngredient10: String? = null,
    @Json(name = "strIngredient11") val strIngredient11: String? = null,
    @Json(name = "strIngredient12") val strIngredient12: String? = null,
    @Json(name = "strIngredient13") val strIngredient13: String? = null,
    @Json(name = "strIngredient14") val strIngredient14: String? = null,
    @Json(name = "strIngredient15") val strIngredient15: String? = null,
    @Json(name = "strIngredient16") val strIngredient16: String? = null,
    @Json(name = "strIngredient17") val strIngredient17: String? = null,
    @Json(name = "strIngredient18") val strIngredient18: String? = null,
    @Json(name = "strIngredient19") val strIngredient19: String? = null,
    @Json(name = "strIngredient20") val strIngredient20: String? = null,
    // Measurements
    @Json(name = "strMeasure1") val strMeasure1: String? = null,
    @Json(name = "strMeasure2") val strMeasure2: String? = null,
    @Json(name = "strMeasure3") val strMeasure3: String? = null,
    @Json(name = "strMeasure4") val strMeasure4: String? = null,
    @Json(name = "strMeasure5") val strMeasure5: String? = null,
    @Json(name = "strMeasure6") val strMeasure6: String? = null,
    @Json(name = "strMeasure7") val strMeasure7: String? = null,
    @Json(name = "strMeasure8") val strMeasure8: String? = null,
    @Json(name = "strMeasure9") val strMeasure9: String? = null,
    @Json(name = "strMeasure10") val strMeasure10: String? = null,
    @Json(name = "strMeasure11") val strMeasure11: String? = null,
    @Json(name = "strMeasure12") val strMeasure12: String? = null,
    @Json(name = "strMeasure13") val strMeasure13: String? = null,
    @Json(name = "strMeasure14") val strMeasure14: String? = null,
    @Json(name = "strMeasure15") val strMeasure15: String? = null,
    @Json(name = "strMeasure16") val strMeasure16: String? = null,
    @Json(name = "strMeasure17") val strMeasure17: String? = null,
    @Json(name = "strMeasure18") val strMeasure18: String? = null,
    @Json(name = "strMeasure19") val strMeasure19: String? = null,
    @Json(name = "strMeasure20") val strMeasure20: String? = null
) {
    fun getIngredientsWithMeasurements(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val ingredients = listOf(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
            strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
            strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
            strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
        )
        val measures = listOf(
            strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5,
            strMeasure6, strMeasure7, strMeasure8, strMeasure9, strMeasure10,
            strMeasure11, strMeasure12, strMeasure13, strMeasure14, strMeasure15,
            strMeasure16, strMeasure17, strMeasure18, strMeasure19, strMeasure20
        )
        for (i in ingredients.indices) {
            val ing = ingredients[i]?.trim()
            val measure = measures[i]?.trim()
            if (!ing.isNullOrEmpty() && ing != "null") {
                val measureText = if (!measure.isNullOrEmpty() && measure != "null") measure else ""
                list.add(Pair(ing, measureText))
            }
        }
        return list
    }
}

@JsonClass(generateAdapter = true)
data class CategoryResponse(
    @Json(name = "meals")
    val meals: List<CategoryDto>?
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "strCategory")
    val strCategory: String?
)

@JsonClass(generateAdapter = true)
data class IngredientResponse(
    @Json(name = "meals")
    val meals: List<IngredientDto>?
)

@JsonClass(generateAdapter = true)
data class IngredientDto(
    @Json(name = "strIngredient")
    val strIngredient: String?
)
