package com.example.applistas.data.remote

import com.example.applistas.data.local.entity.Checklist
import com.example.applistas.data.local.entity.Note
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("checklist/sync")
    suspend fun syncChecklist(@Body checklists: List<Checklist>): Response<Unit>

    @POST("note/sync")
    suspend fun syncNotes(@Body notes: List<Note>): Response<Unit>
}
