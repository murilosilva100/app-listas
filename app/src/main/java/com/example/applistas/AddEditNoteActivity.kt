package com.example.applistas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.applistas.data.local.AppDatabase
import com.example.applistas.data.local.entity.Address
import com.example.applistas.data.local.entity.Note
import com.example.applistas.data.local.entity.NotePriority
import com.example.applistas.data.repository.NoteRepository
import com.example.applistas.viewmodel.NotesViewModel
import java.util.Locale

class AddEditNoteActivity : AppCompatActivity() {
    private lateinit var viewModel: NotesViewModel
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var selectedLocationText: TextView
    private lateinit var priorityButtons: Map<NotePriority, TextView>

    private var noteId: Int = NO_NOTE_ID
    private var currentNote: Note? = null
    private var selectedPriority: NotePriority = NotePriority.HIGH
    private var selectedAddress: Address? = null
    private var hasLoadedNote = false

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val data = result.data ?: return@registerForActivityResult
        val latitude = data.getDoubleExtra(MapPickerActivity.EXTRA_SELECTED_LATITUDE, Double.NaN)
        val longitude = data.getDoubleExtra(MapPickerActivity.EXTRA_SELECTED_LONGITUDE, Double.NaN)
        if (latitude.isNaN() || longitude.isNaN()) return@registerForActivityResult

        selectedAddress = Address(latitude, longitude)
        updateSelectedLocationText()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit_note)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addEditNoteRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        noteId = intent.getIntExtra(EXTRA_NOTE_ID, NO_NOTE_ID)

        setupViewModel()
        setupViews()
        setupPriorityButtons()
        setupBottomMenu()
        observeNoteForEditing()
        UiPreferences.applyFontSize(findViewById(R.id.addEditNoteRoot))
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NoteRepository(database.noteDao())
        val factory = NotesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]
    }

    private fun setupViews() {
        titleInput = findViewById(R.id.noteTitleInput)
        contentInput = findViewById(R.id.noteContentInput)
        selectedLocationText = findViewById(R.id.selectedLocationText)

        findViewById<TextView>(R.id.screenTitle).text =
            if (isEditing()) "Editar nota" else "Adicionar nova nota"

        findViewById<View>(R.id.locationButton).setOnClickListener {
            openMapPicker()
        }

        findViewById<View>(R.id.saveNoteButton).setOnClickListener {
            saveNote()
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
            startActivity(Intent(this, ChecklistActivity::class.java))
            finish()
        }

        findViewById<ImageButton>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.addNoteButton).setOnClickListener {
            clearFormForNewNote()
        }
    }

    private fun observeNoteForEditing() {
        if (!isEditing()) return

        viewModel.allNotes.observe(this) { notes ->
            if (hasLoadedNote) return@observe

            val note = notes.firstOrNull { it.id == noteId } ?: return@observe
            currentNote = note
            titleInput.setText(note.title)
            contentInput.setText(note.content)
            selectedPriority = note.priority
            selectedAddress = note.address
            updateSelectedPriority()
            updateSelectedLocationText()
            hasLoadedNote = true
        }
    }

    private fun openMapPicker() {
        val intent = Intent(this, MapPickerActivity::class.java)
        selectedAddress?.let { address ->
            intent.putExtra(MapPickerActivity.EXTRA_INITIAL_LATITUDE, address.latitude)
            intent.putExtra(MapPickerActivity.EXTRA_INITIAL_LONGITUDE, address.longitude)
        }
        mapPickerLauncher.launch(intent)
    }

    private fun saveNote() {
        val title = titleInput.text.toString().trim()
        val content = contentInput.text.toString().trim()

        if (title.isEmpty()) {
            titleInput.error = "Digite o titulo da nota"
            return
        }

        viewModel.saveNote(
            title = title,
            content = content,
            priority = selectedPriority,
            address = selectedAddress,
            currentNote = currentNote
        )
        finish()
    }

    private fun clearFormForNewNote() {
        titleInput.text.clear()
        contentInput.text.clear()
        currentNote = null
        noteId = NO_NOTE_ID
        selectedPriority = NotePriority.HIGH
        selectedAddress = null
        findViewById<TextView>(R.id.screenTitle).text = "Adicionar nova nota"
        updateSelectedPriority()
        updateSelectedLocationText()
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

    private fun updateSelectedLocationText() {
        val address = selectedAddress
        if (address == null) {
            selectedLocationText.visibility = View.GONE
            selectedLocationText.text = ""
            return
        }

        selectedLocationText.visibility = View.VISIBLE
        selectedLocationText.text = "localiza\u00e7\u00e3o: %.6f, %.6f".format(
            Locale.US,
            address.latitude,
            address.longitude
        )
    }

    private fun isEditing(): Boolean = noteId != NO_NOTE_ID

    private class NotesViewModelFactory(
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                return NotesViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        private const val NO_NOTE_ID = -1
    }
}
