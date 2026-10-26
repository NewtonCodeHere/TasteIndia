package com.example.tasteindia

import com.example.tasteindia.data.remote.MealResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MealSerializationTest {

    @Test
    fun `test Moshi parsing and ingredient normalization`() {
        val json = """
            {
              "meals": [
                {
                  "idMeal": "52795",
                  "strMeal": "Chicken Handi",
                  "strCategory": "Chicken",
                  "strArea": "Indian",
                  "strInstructions": "Heat oil in a pan...",
                  "strMealThumb": "thumb.jpg",
                  "strIngredient1": "Chicken",
                  "strMeasure1": "500g",
                  "strIngredient2": "Onions",
                  "strMeasure2": "2 chopped",
                  "strIngredient3": "",
                  "strMeasure3": "",
                  "strIngredient4": null,
                  "strMeasure4": null
                }
              ]
            }
        """.trimIndent()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(MealResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        val meal = response?.meals?.first()
        assertNotNull(meal)
        assertEquals("52795", meal?.idMeal)
        assertEquals("Chicken Handi", meal?.strMeal)
        assertEquals("Indian", meal?.strArea)

        val ingredients = meal?.getIngredientsWithMeasurements()
        assertNotNull(ingredients)
        // Should only contain Chicken and Onions, ignoring blank/null ingredients
        assertEquals(2, ingredients?.size)
        assertEquals(Pair("Chicken", "500g"), ingredients?.get(0))
        assertEquals(Pair("Onions", "2 chopped"), ingredients?.get(1))
    }
}
