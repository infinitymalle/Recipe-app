package dev.malkolm.recipeapp.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/** Robolectric for [Uri.parse]. */
@RunWith(AndroidJUnit4::class)
class AppFilesTest {
    @Test
    fun `app-made ids are safe, ids that could name another folder are not`() {
        assertTrue(AppFiles.isSafeId("3f2a6c1e-9b1d-4c8e-a1f0-5d2e7b9c0a11"))
        assertTrue(AppFiles.isSafeId("example-fluffy-pancakes"))
        assertFalse(AppFiles.isSafeId(".."))
        assertFalse(AppFiles.isSafeId("../databases"))
        assertFalse(AppFiles.isSafeId("a/b"))
        assertFalse(AppFiles.isSafeId(""))
    }

    @Test
    fun `only paths inside the photo folders are safe`() {
        assertTrue(AppFiles.isSafeRelativePath("recipes/r1/photo.jpg"))
        assertTrue(AppFiles.isSafeRelativePath("backgrounds/cart.jpg"))
        assertFalse(AppFiles.isSafeRelativePath("../databases/recipes.db"))
        assertFalse(AppFiles.isSafeRelativePath("recipes/../../databases/recipes.db"))
        assertFalse(AppFiles.isSafeRelativePath("/data/data/dev.malkolm.recipeapp/databases/recipes.db"))
        assertFalse(AppFiles.isSafeRelativePath("shared_prefs/settings.xml"))
        assertFalse(AppFiles.isSafeRelativePath("recipes"))
        assertFalse(AppFiles.isSafeRelativePath("recipes//photo.jpg"))
        assertFalse(AppFiles.isSafeRelativePath("recipes\\..\\x"))
    }

    @Test
    fun `only content URIs are read from a share`() {
        assertTrue(AppFiles.isReadableSharedUri(Uri.parse("content://media/external/images/1")))
        assertFalse(
            AppFiles.isReadableSharedUri(Uri.parse("file:///data/data/dev.malkolm.recipeapp/databases/recipes.db"))
        )
        assertFalse(AppFiles.isReadableSharedUri(Uri.parse("/sdcard/photo.jpg")))
    }

    @Test
    fun `only web links are opened`() {
        assertTrue(AppFiles.isWebLink("https://example.com/pancakes"))
        assertTrue(AppFiles.isWebLink(" HTTP://example.com "))
        assertFalse(AppFiles.isWebLink("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(AppFiles.isWebLink("file:///data/data/dev.malkolm.recipeapp/databases/recipes.db"))
        assertFalse(AppFiles.isWebLink("javascript:alert(1)"))
        assertFalse(AppFiles.isWebLink("not a link"))
    }
}
