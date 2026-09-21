package dev.malkolm.recipeapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Entry point for Hilt: generates the app-wide dependency container. */
@HiltAndroidApp
class RecipeApplication : Application()
