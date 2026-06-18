package com.example.applistas

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
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
import com.example.applistas.data.local.entity.Address
import com.example.applistas.data.local.entity.Note
import com.example.applistas.data.local.entity.NotePriority
import com.example.applistas.data.repository.NoteRepository
import com.example.applistas.viewmodel.NotesViewModel
import com.example.applistas.viewmodel.PriorityFilter
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var viewModel: NotesViewModel
    private lateinit var notesContainer: LinearLayout
    private lateinit var emptyMessage: TextView
    private lateinit var filterButtons: Map<PriorityFilter, TextView>
    private lateinit var sensorManager: SensorManager

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    private var currentNotes: List<Note> = emptyList()
    private var currentFilter: PriorityFilter = PriorityFilter.ALL
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        UiPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(OSMDROID_PREFS_NAME, MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
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
        setupShakeShortcut()
        observeNotes()
        UiPreferences.applyFontSize(findViewById(R.id.main))
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
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

    private fun setupShakeShortcut() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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

        val deleteButton = view.findViewById<ImageButton>(R.id.deleteNoteButton)
        val contentContainer = view.findViewById<View>(R.id.noteContentContainer)
        deleteButton.setOnClickListener {
            showDeleteNoteDialog(note, contentContainer, deleteButton)
        }

        bindLocation(view, note.address)
        UiPreferences.applyFontSize(view)

        setupSwipeToDelete(view, contentContainer, deleteButton) {
            val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.id)
            }
            startActivity(intent)
        }

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

    private fun showDeleteNoteDialog(
        note: Note,
        contentView: View,
        deleteButton: ImageButton
    ) {
        AlertDialog.Builder(this)
            .setMessage("Voc\u00ea deseja excluir esta nota")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.delete(note)
            }
            .setNegativeButton("N\u00e3o") { _, _ ->
                hideDeleteAction(contentView, deleteButton)
            }
            .setOnCancelListener {
                hideDeleteAction(contentView, deleteButton)
            }
            .show()
    }

    private fun bindLocation(view: View, address: Address?) {
        val locationContainer = view.findViewById<LinearLayout>(R.id.locationContainer)
        val locationLabel = view.findViewById<TextView>(R.id.locationLabel)
        val mapPreview = view.findViewById<MapView>(R.id.mapPreview)

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

        bindMapPreview(mapPreview, address)
    }

    private fun bindMapPreview(mapPreview: MapView, address: Address) {
        val point = GeoPoint(address.latitude, address.longitude)
        mapPreview.setTileSource(TileSourceFactory.MAPNIK)
        mapPreview.setMultiTouchControls(false)
        mapPreview.isClickable = false
        mapPreview.controller.setZoom(MAP_PREVIEW_ZOOM)
        mapPreview.controller.setCenter(point)
        mapPreview.overlays.clear()
        mapPreview.overlays.add(
            Marker(mapPreview).apply {
                position = point
                title = "Local da nota"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )
        mapPreview.invalidate()
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH
        val force = kotlin.math.sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (force > SHAKE_THRESHOLD && now - lastShakeTime > SHAKE_COOLDOWN_MS) {
            lastShakeTime = now
            startActivity(Intent(this, AddEditNoteActivity::class.java))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

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
        private const val SHAKE_THRESHOLD = 2.7f
        private const val SHAKE_COOLDOWN_MS = 1200L
        private const val SWIPE_THRESHOLD = 90f
        private const val TAP_SLOP = 12f
        private const val DELETE_REVEAL_DISTANCE = 72f
        private const val OSMDROID_PREFS_NAME = "osmdroid_preferences"
        private const val MAP_PREVIEW_ZOOM = 15.0
    }
}
