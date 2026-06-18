package com.example.applistas

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteChecklistButton)
        val contentContainer = view.findViewById<View>(R.id.checklistContentContainer)

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
        setupSwipeToDelete(view, contentContainer, deleteButton) {
            viewModel.toggleCompleted(checklist)
        }

        editButton.setOnClickListener {
            openEditChecklist(checklist.id)
        }
        deleteButton.setOnClickListener {
            showDeleteChecklistDialog(checklist, contentContainer, deleteButton)
        }

        UiPreferences.applyFontSize(view)
        return view
    }

    private fun setupSwipeToDelete(
        card: View,
        contentView: View,
        deleteButton: ImageButton,
        onRegularClick: () -> Unit
    ) {
        var downX = 0f
        var downY = 0f
        var startTranslationX = 0f

        card.setOnTouchListener { touchedView, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    startTranslationX = contentView.translationX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - downX
                    val deltaY = event.y - downY
                    if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
                        val nextTranslation = (startTranslationX + deltaX)
                            .coerceIn(-DELETE_REVEAL_DISTANCE, 0f)
                        contentView.translationX = nextTranslation
                        deleteButton.visibility = if (nextTranslation < -10f) View.VISIBLE else View.GONE
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - downX
                    val deltaY = event.y - downY

                    when {
                        deltaX < -SWIPE_THRESHOLD && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) -> {
                            revealDeleteAction(contentView, deleteButton)
                        }
                        deltaX > SWIPE_THRESHOLD && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) -> {
                            hideDeleteAction(contentView, deleteButton)
                        }
                        kotlin.math.abs(deltaX) > TAP_SLOP -> {
                            if (contentView.translationX <= -DELETE_REVEAL_DISTANCE / 2f) {
                                revealDeleteAction(contentView, deleteButton)
                            } else {
                                hideDeleteAction(contentView, deleteButton)
                            }
                        }
                        else -> {
                            touchedView.performClick()
                            onRegularClick()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> true
            }
        }
    }

    private fun revealDeleteAction(contentView: View, deleteButton: ImageButton) {
        deleteButton.visibility = View.VISIBLE
        contentView.animate()
            .translationX(-DELETE_REVEAL_DISTANCE)
            .setDuration(160L)
            .start()
    }

    private fun hideDeleteAction(contentView: View, deleteButton: ImageButton) {
        contentView.animate()
            .translationX(0f)
            .setDuration(160L)
            .withEndAction {
                deleteButton.visibility = View.GONE
            }
            .start()
    }

    private fun showDeleteChecklistDialog(
        checklist: Checklist,
        contentView: View,
        deleteButton: ImageButton
    ) {
        AlertDialog.Builder(this)
            .setMessage("Voc\u00ea deseja excluir esta checklist")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.delete(checklist)
            }
            .setNegativeButton("N\u00e3o") { _, _ ->
                hideDeleteAction(contentView, deleteButton)
            }
            .setOnCancelListener {
                hideDeleteAction(contentView, deleteButton)
            }
            .show()
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

    companion object {
        private const val SWIPE_THRESHOLD = 90f
        private const val TAP_SLOP = 12f
        private const val DELETE_REVEAL_DISTANCE = 72f
    }
}
