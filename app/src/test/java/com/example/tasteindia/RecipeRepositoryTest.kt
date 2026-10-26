package com.example.tasteindia

import com.example.tasteindia.data.remote.MealApiService
import com.example.tasteindia.data.remote.MealDto
import com.example.tasteindia.data.remote.MealResponse
import com.example.tasteindia.data.repository.RecipeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeRepositoryTest {

    class FakeMealApiService(
        var primaryMeals: List<MealDto>? = null,
        var searchResultsMap: Map<String, List<MealDto>> = emptyMap(),
        var lookupResultsMap: Map<String, List<MealDto>> = emptyMap(),
        var shouldThrow: Boolean = false
    ) : MealApiService {
        override suspend fun getIndianMeals(area: String): MealResponse {
            if (shouldThrow) throw RuntimeException("Network error")
            return MealResponse(meals = primaryMeals)
        }

        override suspend fun searchMeals(query: String): MealResponse {
            if (shouldThrow) throw RuntimeException("Network error")
            return MealResponse(meals = searchResultsMap[query])
        }

        override suspend fun lookupMeal(id: String): MealResponse {
            if (shouldThrow) throw RuntimeException("Network error")
            return MealResponse(meals = lookupResultsMap[id])
        }
    }

    @Test
    fun `when primary endpoint returns valid meals, repository returns recipes`() = runTest {
        val fakeMeals = listOf(
            MealDto("1", "Chicken Curry", "thumb1", "Indian"),
            MealDto("2", "Dal Makhani", "thumb2", "Indian")
        )
        val apiService = FakeMealApiService(primaryMeals = fakeMeals)
        val repository = RecipeRepository(apiService)

        val result = repository.getIndianRecipes(forceRefresh = true)

        assertTrue(result.isSuccess)
        val recipes = result.getOrNull()!!
        assertEquals(2, recipes.size)
        assertEquals("Chicken Curry", recipes[0].strMeal)
    }

    @Test
    fun `when primary endpoint returns meals null, fallback searches and looks up details for strArea Indian or India`() = runTest {
        val searchResults = mapOf(
            "chicken" to listOf(
                MealDto("101", "Indian Chicken Curry", "t1", null),
                MealDto("102", "Chinese Chicken", "t2", null),
                MealDto("103", "India Spice Chicken", "t3", null)
            )
        )

        val lookupResults = mapOf(
            "101" to listOf(MealDto("101", "Indian Chicken Curry", "t1", "Indian")),
            "102" to listOf(MealDto("102", "Chinese Chicken", "t2", "Chinese")),
            "103" to listOf(MealDto("103", "India Spice Chicken", "t3", "India")) // accepts "India"
        )

        val apiService = FakeMealApiService(
            primaryMeals = null,
            searchResultsMap = searchResults,
            lookupResultsMap = lookupResults
        )
        val repository = RecipeRepository(apiService)
        repository.fallbackQueries = listOf("chicken")

        val result = repository.getIndianRecipes(forceRefresh = true)

        assertTrue(result.isSuccess)
        val recipes = result.getOrNull()!!
        assertEquals(2, recipes.size)
        assertTrue(recipes.any { it.idMeal == "101" })
        assertTrue(recipes.any { it.idMeal == "103" })
    }

    @Test
    fun `when primary returns empty list and search returns no Indian recipes, returns empty list`() = runTest {
        val apiService = FakeMealApiService(primaryMeals = emptyList(), searchResultsMap = emptyMap())
        val repository = RecipeRepository(apiService)

        val result = repository.getIndianRecipes(forceRefresh = true)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `when api throws exception, repository returns failure`() = runTest {
        val apiService = FakeMealApiService(shouldThrow = true)
        val repository = RecipeRepository(apiService)

        val result = repository.getIndianRecipes(forceRefresh = true)

        assertTrue(result.isFailure)
    }
}
