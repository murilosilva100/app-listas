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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.applistas.data.local.AppDatabase
import com.example.applistas.data.remote.RetrofitInstance
import com.example.applistas.data.repository.SyncRepository
import com.example.applistas.viewmodel.ApiSyncViewModel
import com.example.applistas.viewmodel.SyncStatus

class SettingsActivity : AppCompatActivity() {
    private lateinit var syncViewModel: ApiSyncViewModel
    private lateinit var themeValue: TextView
    private lateinit var fontSizeValue: TextView
    private lateinit var syncValue: TextView

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

        setupViewModel()
        setupViews()
        setupBottomMenu()
        observeSyncStatus()
        refreshValues()
        UiPreferences.applyFontSize(findViewById(R.id.settingsRoot))
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SyncRepository(
            noteDao = database.noteDao(),
            checklistDao = database.checklistDao(),
            apiService = RetrofitInstance.api
        )
        val factory = ApiSyncViewModelFactory(repository)
        syncViewModel = ViewModelProvider(this, factory)[ApiSyncViewModel::class.java]
    }

    private fun setupViews() {
        themeValue = findViewById(R.id.themeValue)
        fontSizeValue = findViewById(R.id.fontSizeValue)
        syncValue = findViewById(R.id.syncValue)

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

        val syncClickListener = View.OnClickListener {
            if (syncViewModel.sync()) {
                syncValue.text = "Sincronizando..."
                Toast.makeText(this, "sincroniza\u00e7\u00e3o em andamento", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.syncRow).setOnClickListener(syncClickListener)
        syncValue.setOnClickListener(syncClickListener)
    }

    private fun observeSyncStatus() {
        syncViewModel.syncStatus.observe(this) { status ->
            when (status) {
                SyncStatus.LOADING -> Unit
                SyncStatus.SUCCESS -> {
                    syncValue.text = ""
                    Toast.makeText(this, "sincroniza\u00e7\u00e3o conclu\u00edda", Toast.LENGTH_SHORT).show()
                }
                SyncStatus.ERROR -> {
                    syncValue.text = ""
                    Toast.makeText(this, "erro ao sincronizar", Toast.LENGTH_SHORT).show()
                }
                SyncStatus.IDLE -> Unit
            }
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

    private class ApiSyncViewModelFactory(
        private val repository: SyncRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ApiSyncViewModel::class.java)) {
                return ApiSyncViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
