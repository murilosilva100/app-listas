package com.example.applistas.data.repository

import com.example.applistas.data.local.dao.NoteDao
import com.example.applistas.data.local.entity.Note
import com.example.applistas.data.local.entity.NotePriority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NoteRepositoryTest {

    private lateinit var noteDao: NoteDao
    private lateinit var noteRepository: NoteRepository

    @Before
    fun setUp() {
        noteDao = mockk()
        every { noteDao.getAllNotes() } returns flowOf(emptyList())
        noteRepository = NoteRepository(noteDao)
    }

    @Test
    fun `allNotes should return flow from dao`() = runTest {
        val notes = listOf(Note(
            id = 1,
            title = "Test",
            priority = NotePriority.HIGH,
            content = "Content")
        )
        every { noteDao.getAllNotes() } returns flowOf(notes)
        // Re-instantiate to pick up the new mock for the val property
        noteRepository = NoteRepository(noteDao)

        val result = noteRepository.allNotes.first()
        assertEquals(notes, result)
    }

    @Test
    fun `insert should call dao insertNote`() = runTest {
        val note = Note(
            id = 1,
            title = "Test",
            priority = NotePriority.HIGH,
            content = "Content"
        )
        coEvery { noteDao.insertNote(note) } returns Unit

        noteRepository.insert(note)

        coVerify { noteDao.insertNote(note) }
    }

    @Test
    fun `delete should call dao deleteNote`() = runTest {
        val note = Note(
            id = 1,
            title = "Test",
            priority = NotePriority.HIGH,
            content = "Content"
        )
        coEvery { noteDao.deleteNote(note) } returns Unit

        noteRepository.delete(note)

        coVerify { noteDao.deleteNote(note) }
    }
}
