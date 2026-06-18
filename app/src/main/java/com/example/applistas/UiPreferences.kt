package com.example.applistas

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate

object UiPreferences {
    private const val PREFS_NAME = "app_ui_preferences"
    private const val KEY_THEME = "theme"
    private const val KEY_FONT_SIZE = "font_size_sp"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val DEFAULT_FONT_SIZE = 14

    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (getTheme(context) == THEME_LIGHT) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    fun saveTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
        applySavedTheme(context)
    }

    fun getTheme(context: Context): String {
        return prefs(context).getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
    }

    fun saveFontSize(context: Context, sizeSp: Int) {
        prefs(context).edit().putInt(KEY_FONT_SIZE, sizeSp).apply()
    }

    fun getFontSize(context: Context): Int {
        return prefs(context).getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    }

    fun applyFontSize(root: View, sizeSp: Int = getFontSize(root.context)) {
        if (root is TextView) {
            root.textSize = sizeSp.toFloat()
        }

        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                applyFontSize(root.getChildAt(index), sizeSp)
            }
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
