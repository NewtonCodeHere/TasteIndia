package com.example.tasteindia.data.repository

import com.example.tasteindia.data.remote.MealApiService
import com.example.tasteindia.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeRepository(
    private val apiService: MealApiService
) {

    private var cachedRecipes: List<Recipe>? = null

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
                    // 2. Fallback mechanism: Search candidates -> Lookup details -> Verify strArea == "Indian" or "India"
                    Logger.d(TAG, "Primary result count is zero/null. Starting fallback search queries: $fallbackQueries")
                    val candidateIds = mutableSetOf<String>()

                    for (query in fallbackQueries) {
                        try {
                            Logger.d(TAG, "Fallback search query: '$query'")
                            val searchResponse = apiService.searchMeals(query)
                            val searchMeals = searchResponse.meals
                            val mealSummaries = searchMeals?.map { "${it.idMeal}:${it.strMeal}" } ?: emptyList()
                            Logger.d(TAG, "Fallback search query '$query' returned ${searchMeals?.size ?: 0} meals: $mealSummaries")
                            if (!searchMeals.isNullOrEmpty()) {
                                for (meal in searchMeals) {
                                    meal.idMeal?.let { candidateIds.add(it) }
                                }
                            }
                        } catch (e: Exception) {
                            Logger.e(TAG, "Exception during fallback search query '$query'", e)
                        }
                    }

                    Logger.d(TAG, "Fallback candidates count before filtering: ${candidateIds.size}. Candidate IDs: $candidateIds")

                    val collectedRecipes = mutableMapOf<String, Recipe>()

                    // 3. Fetch full details for each candidate using lookup.php?i={id}
                    for (idMeal in candidateIds) {
                        try {
                            Logger.d(TAG, "Calling details lookup endpoint for id=$idMeal (lookup.php?i=$idMeal)")
                            val lookupResponse = apiService.lookupMeal(idMeal)
                            val lookupMeals = lookupResponse.meals
                            if (!lookupMeals.isNullOrEmpty()) {
                                val detailsMeal = lookupMeals.first()
                                val area = detailsMeal.strArea?.trim()
                                Logger.d(TAG, "Details response for id=$idMeal: name=${detailsMeal.strMeal}, strArea=$area")

                                // Normalize strArea: accept "Indian" or "India" case-insensitively
                                val isIndian = area != null && (
                                    area.equals("Indian", ignoreCase = true) ||
                                    area.equals("India", ignoreCase = true)
                                )

                                if (isIndian) {
                                    val id = detailsMeal.idMeal ?: continue
                                    val name = detailsMeal.strMeal ?: continue
                                    val thumb = detailsMeal.strMealThumb ?: continue

                                    Logger.d(TAG, "Verified Indian recipe matched: id=$id, name=$name, strArea=$area")
                                    val recipe = Recipe(
                                        idMeal = id,
                                        strMeal = name,
                                        strMealThumb = thumb,
                                        isFavorite = false
                                    )
                                    collectedRecipes.putIfAbsent(recipe.idMeal, recipe)
                                } else {
                                    Logger.d(TAG, "Discarded non-Indian recipe id=$idMeal (name=${detailsMeal.strMeal}, strArea=$area)")
                                }
                            } else {
                                Logger.d(TAG, "Lookup response returned empty meals list for id=$idMeal")
                            }
                        } catch (e: Exception) {
                            Logger.e(TAG, "Exception during lookup details for id=$idMeal", e)
                        }
                    }

                    val finalRecipes = collectedRecipes.values.toList()
                    Logger.d(TAG, "Verified Indian recipes count (final list count): ${finalRecipes.size}")
                    finalRecipes
                }

                cachedRecipes = verifiedRecipes
                Result.success(verifiedRecipes)
            } catch (e: Exception) {
                Logger.e(TAG, "API failure during primary fetch or fallback execution", e)
                Result.failure(e)
            }
        }
    }
}
