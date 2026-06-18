package com.example.applistas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.applistas.data.local.AppDatabase
import com.example.applistas.data.local.entity.Checklist
import com.example.applistas.data.local.entity.NotePriority
import com.example.applistas.data.repository.ChecklistRepository
import com.example.applistas.viewmodel.CheckListViewModel

class AddEditChecklistActivity : AppCompatActivity() {
    private lateinit var viewModel: CheckListViewModel
    private lateinit var titleInput: EditText
    private lateinit var priorityButtons: Map<NotePriority, TextView>

    private var checklistId: Int = NO_CHECKLIST_ID
    private var currentChecklist: Checklist? = null
    private var selectedPriority: NotePriority = NotePriority.HIGH
    private var hasLoadedChecklist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit_checklist)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addEditChecklistRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checklistId = intent.getIntExtra(EXTRA_CHECKLIST_ID, NO_CHECKLIST_ID)

        setupViewModel()
        setupViews()
        setupPriorityButtons()
        setupBottomMenu()
        observeChecklistForEditing()
        UiPreferences.applyFontSize(findViewById(R.id.addEditChecklistRoot))
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ChecklistRepository(database.checklistDao())
        val factory = ChecklistViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CheckListViewModel::class.java]
    }

    private fun setupViews() {
        titleInput = findViewById(R.id.checklistTitleInput)
        findViewById<TextView>(R.id.screenTitle).text =
            if (isEditing()) "Editar checklist" else "Criar nova checklist"

        findViewById<View>(R.id.saveChecklistButton).setOnClickListener {
            saveChecklist()
        }
    }

    private fun setupPriorityButtons() {
        priorityButtons = mapOf(
            NotePriority.HIGH to findViewById(R.id.priorityHigh),
            NotePriority.MEDIUM to findViewById(R.id.priorityMedium),
            NotePriority.LOW to findViewById(R.id.priorityLow)
        )

        priorityButtons.forEach { (priority, button) ->
            button.setOnClickListener {
                selectedPriority = priority
                updateSelectedPriority()
            }
        }

        updateSelectedPriority()
    }

    private fun setupBottomMenu() {
        findViewById<ImageButton>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<ImageButton>(R.id.navChecklist).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.addChecklistButton).setOnClickListener {
            titleInput.text.clear()
            currentChecklist = null
            checklistId = NO_CHECKLIST_ID
            selectedPriority = NotePriority.HIGH
            findViewById<TextView>(R.id.screenTitle).text = "Criar nova checklist"
            updateSelectedPriority()
        }
    }

    private fun observeChecklistForEditing() {
        if (!isEditing()) return

        viewModel.allChecklists.observe(this) { checklists ->
            if (hasLoadedChecklist) return@observe

            val checklist = checklists.firstOrNull { it.id == checklistId } ?: return@observe
            currentChecklist = checklist
            titleInput.setText(checklist.title)
            selectedPriority = checklist.priority
            updateSelectedPriority()
            hasLoadedChecklist = true
        }
    }

    private fun saveChecklist() {
        val title = titleInput.text.toString().trim()
        if (title.isEmpty()) {
            titleInput.error = "Digite o nome da checklist"
            return
        }

        viewModel.saveChecklist(
            title = title,
            priority = selectedPriority,
            currentChecklist = currentChecklist
        )
        finish()
    }

    private fun updateSelectedPriority() {
        priorityButtons.forEach { (priority, button) ->
            val background = if (priority == selectedPriority) {
                R.drawable.bg_filter_selected
            } else {
                R.drawable.bg_filter_outline
            }
            button.setBackgroundResource(background)
        }
    }

    private fun isEditing(): Boolean = checklistId != NO_CHECKLIST_ID

    private class ChecklistViewModelFactory(
        private val repository: ChecklistRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CheckListViewModel::class.java)) {
                return CheckListViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        const val EXTRA_CHECKLIST_ID = "extra_checklist_id"
        private const val NO_CHECKLIST_ID = -1
    }
}
