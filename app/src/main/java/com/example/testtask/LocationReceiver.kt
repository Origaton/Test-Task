package com.example.testtask

import com.example.testtask.location_info.LocationInfo
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Класс создает клиент для обращения к серверу и получает данные с него
 * Возвращает data class LocationInfo
 */

class LocationReceiver {
    suspend fun getLocationInfo(): LocationInfo = coroutineScope {
        val retrofit = Retrofit
            .Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiRequests::class.java)
        val response = async(Dispatchers.IO) {
            api.serverRequest()
        }
        return@coroutineScope response.await()
    }
}