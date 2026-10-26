package com.example.tasteindia

import com.example.tasteindia.data.remote.CategoryResponse
import com.example.tasteindia.data.remote.IngredientResponse
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

        override suspend fun searchMealsByFirstLetter(letter: String): MealResponse {
            if (shouldThrow) throw RuntimeException("Network error")
            return MealResponse(meals = emptyList())
        }

        override suspend fun lookupMeal(id: String): MealResponse {
            if (shouldThrow) throw RuntimeException("Network error")
            return MealResponse(meals = lookupResultsMap[id])
        }

        override suspend fun getCategories(list: String): CategoryResponse {
            return CategoryResponse(emptyList())
        }

        override suspend fun getIngredients(list: String): IngredientResponse {
            return IngredientResponse(emptyList())
        }

        override suspend fun getMealsByCategory(category: String): MealResponse {
            return MealResponse(emptyList())
        }

        override suspend fun getMealsByIngredient(ingredient: String): MealResponse {
            return MealResponse(emptyList())
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
    fun `when primary endpoint returns meals null, fallback searches and looks up details for strArea Indian`() = runTest {
        val searchResults = mapOf(
            "chicken curry" to listOf(
                MealDto("101", "Indian Chicken Curry", "t1", null),
                MealDto("102", "Chinese Chicken", "t2", null)
            )
        )

        val lookupResults = mapOf(
            "101" to listOf(MealDto("101", "Indian Chicken Curry", "t1", "Indian")),
            "102" to listOf(MealDto("102", "Chinese Chicken", "t2", "Chinese"))
        )

        val apiService = FakeMealApiService(
            primaryMeals = null,
            searchResultsMap = searchResults,
            lookupResultsMap = lookupResults
        )
        val repository = RecipeRepository(apiService)
        repository.fallbackQueries = listOf("chicken curry")

        val result = repository.getIndianRecipes(forceRefresh = true)

        assertTrue(result.isSuccess)
        val recipes = result.getOrNull()!!
        assertEquals(1, recipes.size)
        assertEquals("101", recipes[0].idMeal)
        assertEquals("Indian Chicken Curry", recipes[0].strMeal)
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
