package dev.malkolm.recipeapp.ui.components

import androidx.compose.runtime.compositionLocalOf
import dev.malkolm.recipeapp.domain.model.Feature

/**
 * The features that are on (dependencies already applied), provided once in MainActivity so any
 * screen can hide what belongs to a switched-off feature without its ViewModel knowing about it.
 * Defaults to everything on, e.g. for previews.
 */
val LocalEnabledFeatures = compositionLocalOf { Feature.entries.toSet() }
