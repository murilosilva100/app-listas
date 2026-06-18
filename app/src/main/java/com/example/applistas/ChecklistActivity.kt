package com.example.applistas

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
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
import com.example.applistas.data.repository.ChecklistRepository
import com.example.applistas.viewmodel.CheckListViewModel
import com.example.applistas.data.local.entity.NotePriority

class ChecklistActivity : AppCompatActivity() {
    private lateinit var viewModel: CheckListViewModel
    private lateinit var checklistContainer: LinearLayout
    private lateinit var emptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checklist)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.checklistRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewModel()
        setupViews()
        setupBottomMenu()
        observeChecklists()
        UiPreferences.applyFontSize(findViewById(R.id.checklistRoot))
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ChecklistRepository(database.checklistDao())
        val factory = ChecklistViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CheckListViewModel::class.java]
    }

    private fun setupViews() {
        checklistContainer = findViewById(R.id.checklistContainer)
        emptyMessage = findViewById(R.id.emptyChecklistMessage)

        findViewById<View>(R.id.createChecklistButton).setOnClickListener {
            openCreateChecklist()
        }
    }

    private fun setupBottomMenu() {
        findViewById<ImageButton>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<ImageButton>(R.id.navChecklist).setOnClickListener {
            // Ja estamos na tela de checklists.
        }

        findViewById<ImageButton>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.addChecklistButton).setOnClickListener {
            openCreateChecklist()
        }
    }

    private fun observeChecklists() {
        viewModel.allChecklists.observe(this) { checklists ->
            renderChecklists(checklists)
        }
    }

    private fun renderChecklists(checklists: List<Checklist>) {
        checklistContainer.removeAllViews()
        emptyMessage.visibility = if (checklists.isEmpty()) View.VISIBLE else View.GONE

        checklists.forEach { checklist ->
            checklistContainer.addView(createChecklistView(checklist))
        }
    }

    private fun createChecklistView(checklist: Checklist): View {
        val view = LayoutInflater.from(this).inflate(
            R.layout.item_checklist_preview,
            checklistContainer,
            false
        )

        val checkBox = view.findViewById<CheckBox>(R.id.checklistDone)
        val title = view.findViewById<TextView>(R.id.checklistTitle)
        val status = view.findViewById<TextView>(R.id.checklistStatus)
        val editButton = view.findViewById<ImageButton>(R.id.editChecklistButton)

        checkBox.isChecked = checklist.isCompleted
        checkBox.isClickable = false
        title.text = checklist.title
        status.text = if (checklist.isCompleted) "Concluido" else "Pendente"

        view.findViewById<TextView>(R.id.checklistPriority).text =
            "Prioridade: ${formatPriority(checklist.priority)}"

        title.paintFlags = if (checklist.isCompleted) {
            title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        view.alpha = if (checklist.isCompleted) 0.72f else 1f
        view.setOnClickListener {
            viewModel.toggleCompleted(checklist)
        }

        editButton.setOnClickListener {
            openEditChecklist(checklist.id)
        }

        UiPreferences.applyFontSize(view)
        return view
    }

    private fun openCreateChecklist() {
        startActivity(Intent(this, AddEditChecklistActivity::class.java))
    }

    private fun openEditChecklist(checklistId: Int) {
        val intent = Intent(this, AddEditChecklistActivity::class.java).apply {
            putExtra(AddEditChecklistActivity.EXTRA_CHECKLIST_ID, checklistId)
        }
        startActivity(intent)
    }

    private fun formatPriority(priority: NotePriority): String {
        return when (priority) {
            NotePriority.HIGH -> "Alta"
            NotePriority.MEDIUM -> "Media"
            NotePriority.LOW -> "Baixa"
        }
    }

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
}
