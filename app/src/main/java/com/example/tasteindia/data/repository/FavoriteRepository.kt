package com.example.tasteindia.data.repository

import android.content.Context

object FavoriteRepository {
    private const val PREFS_NAME = "tasteindia_favorites_prefs"
    private const val KEY_FAVORITES = "favorite_meal_ids"

    fun getFavoriteIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun addFavorite(context: Context, idMeal: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.add(idMeal)) {
            prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        }
    }

    fun removeFavorite(context: Context, idMeal: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.remove(idMeal)) {
            prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        }
    }

    fun isFavorite(context: Context, idMeal: String): Boolean {
        return getFavoriteIds(context).contains(idMeal)
    }
}
