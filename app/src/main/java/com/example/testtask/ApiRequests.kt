package com.example.testtask

import com.example.testtask.location_info.LocationInfo
import retrofit2.http.GET

/**
 * Интерфейс для создания запроса на сервер
 */

//https://waadsu.com/api/russia.geo.json
const val URL = "https://waadsu.com/api/"

interface ApiRequests {
    @GET("russia.geo.json")
    suspend fun serverRequest(): LocationInfo

}