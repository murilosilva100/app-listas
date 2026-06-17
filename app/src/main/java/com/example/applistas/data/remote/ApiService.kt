package com.example.applistas.data.remote

import com.example.applistas.data.local.entity.Note
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("notes")
    suspend fun getNotes(): Response<List<Note>>

    @POST("notes/sync")
    suspend fun syncNotes(@Body notes: List<Note>): Response<Unit>
}
