package com.example.applistas

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    private lateinit var themeValue: TextView
    private lateinit var fontSizeValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupBottomMenu()
        refreshValues()
        UiPreferences.applyFontSize(findViewById(R.id.settingsRoot))
    }

    private fun setupViews() {
        themeValue = findViewById(R.id.themeValue)
        fontSizeValue = findViewById(R.id.fontSizeValue)

        findViewById<View>(R.id.themeRow).setOnClickListener {
            showThemeMenu()
        }
        themeValue.setOnClickListener {
            showThemeMenu()
        }

        findViewById<View>(R.id.fontSizeRow).setOnClickListener {
            showFontSizeMenu()
        }
        fontSizeValue.setOnClickListener {
            showFontSizeMenu()
        }

        findViewById<View>(R.id.syncRow).setOnClickListener {
            // TODO: executar sincronizacao com API REST quando o fluxo de nuvem for implementado.
            Toast.makeText(this, "Sincronizacao em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomMenu() {
        findViewById<ImageButton>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<ImageButton>(R.id.navChecklist).setOnClickListener {
            startActivity(Intent(this, ChecklistActivity::class.java))
            finish()
        }

        findViewById<ImageButton>(R.id.navSettings).setOnClickListener {
            // Ja estamos na tela de configuracoes.
        }

        findViewById<ImageButton>(R.id.addNoteButton).setOnClickListener {
            startActivity(Intent(this, AddEditNoteActivity::class.java))
        }
    }

    private fun showThemeMenu() {
        PopupMenu(this, themeValue).apply {
            menu.add(Menu.NONE, THEME_LIGHT_ID, Menu.NONE, "Claro")
            menu.add(Menu.NONE, THEME_DARK_ID, Menu.NONE, "Escuro")
            setOnMenuItemClickListener { item ->
                val theme = if (item.itemId == THEME_LIGHT_ID) {
                    UiPreferences.THEME_LIGHT
                } else {
                    UiPreferences.THEME_DARK
                }
                UiPreferences.saveTheme(this@SettingsActivity, theme)
                refreshValues()
                true
            }
            show()
        }
    }

    private fun showFontSizeMenu() {
        PopupMenu(this, fontSizeValue).apply {
            FONT_SIZE_OPTIONS.forEach { size ->
                menu.add(Menu.NONE, size, Menu.NONE, "${size}px")
            }
            setOnMenuItemClickListener { item ->
                UiPreferences.saveFontSize(this@SettingsActivity, item.itemId)
                refreshValues()
                UiPreferences.applyFontSize(findViewById(R.id.settingsRoot), item.itemId)
                true
            }
            show()
        }
    }

    private fun refreshValues() {
        themeValue.text = if (UiPreferences.getTheme(this) == UiPreferences.THEME_LIGHT) {
            "Claro"
        } else {
            "Escuro"
        }
        fontSizeValue.text = "${UiPreferences.getFontSize(this)}px"
    }

    companion object {
        private const val THEME_LIGHT_ID = 1
        private const val THEME_DARK_ID = 2
        private val FONT_SIZE_OPTIONS = listOf(8, 10, 12, 14, 16)
    }
}
