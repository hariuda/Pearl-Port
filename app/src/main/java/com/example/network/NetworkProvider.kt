package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkProvider {
    val cseApi: CseApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.cse.lk/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CseApi::class.java)
    }

    suspend fun getGoogleCryptoPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://www.google.com/finance/quote/$symbol-LKR")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext null
            val regex = """data-last-price="([0-9.]+)"""".toRegex()
            val matchResult = regex.find(html)
            matchResult?.groupValues?.get(1)?.toDoubleOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
