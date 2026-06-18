package com.example.applistas

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
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
import com.example.applistas.data.local.entity.Address
import com.example.applistas.data.local.entity.Note
import com.example.applistas.data.local.entity.NotePriority
import com.example.applistas.data.repository.NoteRepository
import com.example.applistas.viewmodel.NotesViewModel
import com.example.applistas.viewmodel.PriorityFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: NotesViewModel
    private lateinit var notesContainer: LinearLayout
    private lateinit var emptyMessage: TextView
    private lateinit var filterButtons: Map<PriorityFilter, TextView>

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    private var currentNotes: List<Note> = emptyList()
    private var currentFilter: PriorityFilter = PriorityFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewModel()
        setupViews()
        setupFilters()
        setupBottomMenu()
        observeNotes()
        UiPreferences.applyFontSize(findViewById(R.id.main))
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NoteRepository(database.noteDao())
        val factory = NotesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]
    }

    private fun setupViews() {
        notesContainer = findViewById(R.id.notesContainer)
        emptyMessage = findViewById(R.id.emptyMessage)
    }

    private fun setupFilters() {
        filterButtons = mapOf(
            PriorityFilter.ALL to findViewById(R.id.filterAll),
            PriorityFilter.HIGH to findViewById(R.id.filterHigh),
            PriorityFilter.MEDIUM to findViewById(R.id.filterMedium),
            PriorityFilter.LOW to findViewById(R.id.filterLow)
        )

        filterButtons.forEach { (filter, button) ->
            button.setOnClickListener {
                viewModel.setFilter(filter)
            }
        }

        updateSelectedFilter(PriorityFilter.ALL)
    }

    private fun setupBottomMenu() {
        findViewById<ImageButton>(R.id.navHome).setOnClickListener {
            viewModel.setFilter(PriorityFilter.ALL)
        }

        findViewById<ImageButton>(R.id.navChecklist).setOnClickListener {
            startActivity(Intent(this, ChecklistActivity::class.java))
        }

        findViewById<ImageButton>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.addNoteButton).setOnClickListener {
            startActivity(Intent(this, AddEditNoteActivity::class.java))
        }
    }

    private fun observeNotes() {
        viewModel.allNotes.observe(this) { notes ->
            currentNotes = notes
            renderNotes()
        }

        viewModel.filter.observe(this) { filter ->
            currentFilter = filter
            updateSelectedFilter(filter)
            renderNotes()
        }
    }

    private fun renderNotes() {
        val filteredNotes = when (currentFilter) {
            PriorityFilter.ALL -> currentNotes
            PriorityFilter.HIGH -> currentNotes.filter { it.priority == NotePriority.HIGH }
            PriorityFilter.MEDIUM -> currentNotes.filter { it.priority == NotePriority.MEDIUM }
            PriorityFilter.LOW -> currentNotes.filter { it.priority == NotePriority.LOW }
        }

        notesContainer.removeAllViews()
        emptyMessage.visibility = if (filteredNotes.isEmpty()) View.VISIBLE else View.GONE

        filteredNotes.forEach { note ->
            notesContainer.addView(createNoteView(note))
        }
    }

    private fun createNoteView(note: Note): View {
        val view = LayoutInflater.from(this).inflate(
            R.layout.item_note_preview,
            notesContainer,
            false
        )

        view.findViewById<TextView>(R.id.noteTitle).text = note.title
        view.findViewById<TextView>(R.id.noteContent).text = note.content
        view.findViewById<TextView>(R.id.noteDate).text = "Adicionada: ${formatDate(note.timestamp)}"
        view.findViewById<TextView>(R.id.notePriority).text = "Prioridade: ${formatPriority(note.priority)}"

        bindLocation(view, note.address)
        UiPreferences.applyFontSize(view)

        view.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.id)
            }
            startActivity(intent)
        }

        return view
    }

    private fun bindLocation(view: View, address: Address?) {
        val locationContainer = view.findViewById<LinearLayout>(R.id.locationContainer)
        val locationLabel = view.findViewById<TextView>(R.id.locationLabel)

        if (address == null) {
            locationContainer.visibility = View.GONE
            return
        }

        locationContainer.visibility = View.VISIBLE
        locationLabel.text = "Localizacao: %.5f, %.5f".format(
            Locale.US,
            address.latitude,
            address.longitude
        )

        // TODO: carregar uma imagem real do mapa quando a integracao for adicionada.
        // Exemplo futuro: usar Google Static Maps ou Maps SDK com as coordenadas acima.
    }

    private fun updateSelectedFilter(selectedFilter: PriorityFilter) {
        filterButtons.forEach { (filter, button) ->
            val background = if (filter == selectedFilter) {
                R.drawable.bg_filter_selected
            } else {
                R.drawable.bg_filter_outline
            }
            button.setBackgroundResource(background)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }

    private fun formatPriority(priority: NotePriority): String {
        return when (priority) {
            NotePriority.HIGH -> "Alta"
            NotePriority.MEDIUM -> "Media"
            NotePriority.LOW -> "Baixa"
        }
    }

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
}
