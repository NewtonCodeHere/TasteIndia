package com.example.tasteindia

import com.example.tasteindia.model.Recipe
import com.example.tasteindia.ui.viewmodel.RecipeViewModel
import com.example.tasteindia.ui.viewmodel.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeFilterTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sampleRecipes = listOf(
        Recipe("1", "Chicken Tikka Masala", "thumb1", true),
        Recipe("2", "Aloo Gobi", "thumb2", false),
        Recipe("3", "Butter Chicken", "thumb3", true),
        Recipe("4", "Dal Makhani", "thumb4", false)
    )

    @Test
    fun `test search query filtering`() {
        val viewModel = RecipeViewModel()
        val filtered = viewModel.filterAndSortRecipes(
            baseRecipes = sampleRecipes,
            searchQuery = "chicken",
            selectedCategoryMealIds = null,
            selectedIngredientMealIds = null,
            favoritesOnly = false,
            sortOrder = SortOrder.AZ
        )
        assertEquals(2, filtered.size)
        assertEquals("Butter Chicken", filtered[0].strMeal)
        assertEquals("Chicken Tikka Masala", filtered[1].strMeal)
    }

    @Test
    fun `test sorting A to Z and Z to A`() {
        val viewModel = RecipeViewModel()
        val sortedAZ = viewModel.filterAndSortRecipes(
            baseRecipes = sampleRecipes,
            searchQuery = "",
            selectedCategoryMealIds = null,
            selectedIngredientMealIds = null,
            favoritesOnly = false,
            sortOrder = SortOrder.AZ
        )
        assertEquals("Aloo Gobi", sortedAZ[0].strMeal)

        val sortedZA = viewModel.filterAndSortRecipes(
            baseRecipes = sampleRecipes,
            searchQuery = "",
            selectedCategoryMealIds = null,
            selectedIngredientMealIds = null,
            favoritesOnly = false,
            sortOrder = SortOrder.ZA
        )
        assertEquals("Dal Makhani", sortedZA[0].strMeal)
    }

    @Test
    fun `test favorites only filter`() {
        val viewModel = RecipeViewModel()
        val filtered = viewModel.filterAndSortRecipes(
            baseRecipes = sampleRecipes,
            searchQuery = "",
            selectedCategoryMealIds = null,
            selectedIngredientMealIds = null,
            favoritesOnly = true,
            sortOrder = SortOrder.AZ
        )
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.isFavorite })
    }

    @Test
    fun `test category and ingredient intersection`() {
        val viewModel = RecipeViewModel()
        val filtered = viewModel.filterAndSortRecipes(
            baseRecipes = sampleRecipes,
            searchQuery = "",
            selectedCategoryMealIds = setOf("1", "2"),
            selectedIngredientMealIds = setOf("1", "3"),
            favoritesOnly = false,
            sortOrder = SortOrder.AZ
        )
        assertEquals(1, filtered.size)
        assertEquals("Chicken Tikka Masala", filtered[0].strMeal)
    }
}
