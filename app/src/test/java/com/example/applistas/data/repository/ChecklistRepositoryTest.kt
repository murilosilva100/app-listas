package com.example.applistas.data.repository

import com.example.applistas.data.local.dao.ChecklistDao
import com.example.applistas.data.local.entity.Checklist
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

class ChecklistRepositoryTest {

    private lateinit var checklistDao: ChecklistDao
    private lateinit var checklistRepository: ChecklistRepository

    @Before
    fun setUp() {
        checklistDao = mockk()
        every { checklistDao.getAllChecklists() } returns flowOf(emptyList())
        checklistRepository = ChecklistRepository(checklistDao)
    }

    @Test
    fun `allChecklists should return flow from dao`() = runTest {
        val checklists = listOf(
            Checklist(
                id = 1,
                title = "Test Checklist"
            )
        )
        every { checklistDao.getAllChecklists() } returns flowOf(checklists)
        // Re-instantiate to pick up the new mock for the val property
        checklistRepository = ChecklistRepository(checklistDao)

        val result = checklistRepository.allChecklists.first()
        assertEquals(checklists, result)
    }

    @Test
    fun `insertChecklist should call dao insertChecklist and return id`() = runTest {
        val checklist = Checklist(
            id = 1,
            title = "New Checklist"
        )
        coEvery { checklistDao.insertChecklist(checklist) } returns 1L

        val result = checklistRepository.insert(checklist)

        assertEquals(1, result)
        coVerify { checklistDao.insertChecklist(checklist) }
    }

    @Test
    fun `deleteChecklist should call dao deleteChecklist`() = runTest {
        val checklist = Checklist(
            id = 1,
            title = "Delete Me"
        )
        coEvery { checklistDao.deleteChecklist(checklist) } returns Unit

        checklistRepository.delete(checklist)

        coVerify { checklistDao.deleteChecklist(checklist) }
    }
}
