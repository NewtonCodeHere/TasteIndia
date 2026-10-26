package com.example.tasteindia.data.repository

import com.example.tasteindia.data.remote.MealApiService
import com.example.tasteindia.model.Recipe
import com.example.tasteindia.model.RecipeDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class RecipeRepository(
    private val apiService: MealApiService
) {

    private var cachedRecipes: List<Recipe>? = null
    private var cachedCategories: List<String>? = null
    private var cachedIngredients: List<String>? = null
    private val categoryMealIdsCache = mutableMapOf<String, Set<String>>()
    private val ingredientMealIdsCache = mutableMapOf<String, Set<String>>()

    // Single-word search queries that reliably return matches on TheMealDB search.php endpoint
    var fallbackQueries: List<String> = listOf(
        "chicken",
        "curry",
        "biryani",
        "dal",
        "paneer",
        "tikka",
        "masala",
        "rice",
        "mutton",
        "fish",
        "prawn",
        "egg",
        "soup",
        "korma",
        "vindaloo",
        "tandoori",
        "samosa",
        "naan",
        "roti",
        "handi",
        "aloo",
        "chana",
        "palak"
    )

    private object Logger {
        fun d(tag: String, msg: String) {
            try {
                android.util.Log.d(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] D: $msg")
            }
        }
        fun e(tag: String, msg: String, tr: Throwable? = null) {
            try {
                android.util.Log.e(tag, msg, tr)
            } catch (_: Throwable) {
                println("[$tag] E: $msg")
                tr?.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "TasteIndia"
    }

    suspend fun getIndianRecipes(forceRefresh: Boolean = false): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            if (!forceRefresh && cachedRecipes != null) {
                Logger.d(TAG, "Returning cached recipes, count: ${cachedRecipes?.size}")
                return@withContext Result.success(cachedRecipes!!)
            }

            try {
                // 1. Try primary filter endpoint first
                Logger.d(TAG, "Calling primary endpoint: filter.php?a=Indian")
                val primaryResponse = apiService.getIndianMeals("Indian")
                val primaryMeals = primaryResponse.meals
                Logger.d(TAG, "Primary result count: ${primaryMeals?.size ?: 0}")

                val verifiedRecipes = if (!primaryMeals.isNullOrEmpty()) {
                    primaryMeals.mapNotNull { dto ->
                        val id = dto.idMeal ?: return@mapNotNull null
                        val name = dto.strMeal ?: return@mapNotNull null
                        val thumb = dto.strMealThumb ?: return@mapNotNull null
                        Recipe(
                            idMeal = id,
                            strMeal = name,
                            strMealThumb = thumb,
                            isFavorite = false
                        )
                    }.distinctBy { it.idMeal }
                } else {
                    // 2. Expanded discovery: fallbackQueries + alphabet search (search.php?f=a..z)
                    Logger.d(TAG, "Primary result count is zero/null. Starting expanded discovery (fallback queries + alphabet f=a..z)...")
                    val candidateIds = mutableSetOf<String>()
                    var searchFailures = 0

                    // Search via fallback keyword queries
                    for (query in fallbackQueries) {
                        try {
                            val searchResponse = apiService.searchMeals(query)
                            val searchMeals = searchResponse.meals
                            if (!searchMeals.isNullOrEmpty()) {
                                for (meal in searchMeals) {
                                    meal.idMeal?.let { candidateIds.add(it) }
                                }
                            }
                        } catch (e: Exception) {
                            searchFailures++
                            Logger.e(TAG, "Exception during fallback search query '$query'", e)
                        }
                    }

                    // Search via alphabetical letters 'a' through 'z'
                    for (char in 'a'..'z') {
                        try {
                            val searchResponse = apiService.searchMealsByFirstLetter(char.toString())
                            val searchMeals = searchResponse.meals
                            if (!searchMeals.isNullOrEmpty()) {
                                for (meal in searchMeals) {
                                    meal.idMeal?.let { candidateIds.add(it) }
                                }
                            }
                        } catch (_: Exception) {
                            searchFailures++
                        }
                    }

                    Logger.d(TAG, "Number of candidates discovered: ${candidateIds.size}. Unique recipe IDs count: ${candidateIds.size}. Search failures: $searchFailures")

                    val collectedRecipes = mutableMapOf<String, Recipe>()
                    var lookupFailures = 0

                    // 3. Fetch full details concurrently in chunks using lookup.php?i={id}
                    val candidateList = candidateIds.toList()
                    val chunkSize = 10
                    for (chunk in candidateList.chunked(chunkSize)) {
                        val deferredResults = chunk.map { idMeal ->
                            async(Dispatchers.IO) {
                                try {
                                    val lookupResponse = apiService.lookupMeal(idMeal)
                                    val lookupMeals = lookupResponse.meals
                                    if (!lookupMeals.isNullOrEmpty()) {
                                        val detailsMeal = lookupMeals.first()
                                        val area = detailsMeal.strArea?.trim()
                                        val isIndian = area != null && (
                                            area.equals("Indian", ignoreCase = true) ||
                                            area.equals("India", ignoreCase = true)
                                        )
                                        if (isIndian) {
                                            val id = detailsMeal.idMeal
                                            val name = detailsMeal.strMeal
                                            val thumb = detailsMeal.strMealThumb
                                            if (id != null && name != null && thumb != null) {
                                                Logger.d(TAG, "Verified Indian recipe matched: id=$id, name=$name, strArea=$area")
                                                Recipe(idMeal = id, strMeal = name, strMealThumb = thumb, isFavorite = false)
                                            } else null
                                        } else {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    lookupFailures++
                                    Logger.e(TAG, "Exception during lookup details for id=$idMeal", e)
                                    null
                                }
                            }
                        }
                        deferredResults.awaitAll().filterNotNull().forEach { recipe ->
                            collectedRecipes.putIfAbsent(recipe.idMeal, recipe)
                        }
                    }

                    val finalRecipes = collectedRecipes.values.toList()
                    Logger.d(TAG, "Verified Indian recipes count: ${finalRecipes.size}. Lookup failures: $lookupFailures. Final collection size: ${finalRecipes.size}")
                    finalRecipes
                }

                cachedRecipes = verifiedRecipes
                Result.success(verifiedRecipes)
            } catch (e: Exception) {
                Logger.e(TAG, "API failure during recipe discovery", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getRecipeDetails(idMeal: String): Result<RecipeDetail> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.d(TAG, "Fetching recipe details for id=$idMeal via lookup.php?i=$idMeal")
                val response = apiService.lookupMeal(idMeal)
                val meals = response.meals
                if (!meals.isNullOrEmpty()) {
                    val dto = meals.first()
                    val detail = RecipeDetail(
                        idMeal = dto.idMeal ?: idMeal,
                        strMeal = dto.strMeal ?: "Unknown Recipe",
                        strMealThumb = dto.strMealThumb ?: "",
                        strCategory = dto.strCategory ?: "General",
                        strArea = dto.strArea ?: "Indian",
                        strInstructions = dto.strInstructions ?: "No instructions provided.",
                        strSource = dto.strSource?.trim() ?: "",
                        strYoutube = dto.strYoutube?.trim() ?: "",
                        ingredients = dto.getIngredientsWithMeasurements()
                    )
                    Result.success(detail)
                } else {
                    Result.failure(Exception("Recipe details not found for ID: $idMeal"))
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to fetch recipe details for id=$idMeal", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCategories(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            if (cachedCategories != null) return@withContext Result.success(cachedCategories!!)
            try {
                val response = apiService.getCategories("list")
                val categories = response.meals?.mapNotNull { it.strCategory }?.sorted() ?: emptyList()
                cachedCategories = categories
                Result.success(categories)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to fetch categories", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getIngredients(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            if (cachedIngredients != null) return@withContext Result.success(cachedIngredients!!)
            try {
                val response = apiService.getIngredients("list")
                val ingredients = response.meals?.mapNotNull { it.strIngredient }?.sorted() ?: emptyList()
                cachedIngredients = ingredients
                Result.success(ingredients)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to fetch ingredients", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getMealIdsByCategory(category: String): Result<Set<String>> {
        return withContext(Dispatchers.IO) {
            if (categoryMealIdsCache.containsKey(category)) {
                return@withContext Result.success(categoryMealIdsCache[category]!!)
            }
            try {
                val response = apiService.getMealsByCategory(category)
                val ids = response.meals?.mapNotNull { it.idMeal }?.toSet() ?: emptySet()
                categoryMealIdsCache[category] = ids
                Result.success(ids)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to fetch meal IDs for category: $category", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getMealIdsByIngredient(ingredient: String): Result<Set<String>> {
        return withContext(Dispatchers.IO) {
            if (ingredientMealIdsCache.containsKey(ingredient)) {
                return@withContext Result.success(ingredientMealIdsCache[ingredient]!!)
            }
            try {
                val response = apiService.getMealsByIngredient(ingredient)
                val ids = response.meals?.mapNotNull { it.idMeal }?.toSet() ?: emptySet()
                ingredientMealIdsCache[ingredient] = ids
                Result.success(ids)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to fetch meal IDs for ingredient: $ingredient", e)
                Result.failure(e)
            }
        }
    }
}
