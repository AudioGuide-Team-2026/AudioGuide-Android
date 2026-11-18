package com.project.audioguide.data.remote

import com.project.audioguide.data.remote.response.SightEntityResponse
import retrofit2.http.GET

interface ApiService {
    @GET("sights")
    suspend fun loadSights(): List<SightEntityResponse>
}