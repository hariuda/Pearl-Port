package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generatePortfolioInsights(portfolioData: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            return@withContext """
                ## AI Insights Unavailable
                Please configure your Gemini API Key in the `.env` file to enable AI-powered portfolio insights, anomaly detection, and rebalancing suggestions.
            """.trimIndent()
        }

        val prompt = """
            You are an expert financial advisor AI. I will provide you with a user's current investment portfolio data (equities, fixed deposits, crypto, etc.) in JSON format.
            
            Based on this data, provide the following insights:
            1. **Portfolio Health Score**: Provide a score out of 100 based on diversification, asset allocation, and risk.
            2. **Anomaly Detection**: Identify any unusual concentrations (e.g., too much weight in one sector or asset), underperforming assets, or risks.
            3. **Watchlist & Price Targets**: Based on the existing equities, suggest 2-3 logical watchlist additions in similar or complementary sectors, along with basic rationale.
            4. **Rebalancing Suggestions**: Suggest concrete actions to balance the portfolio (e.g., "sell X to buy Y", "increase fixed-income allocation").
            
            Format the response in plain text with clear spacing. Use uppercase headers and hyphen bullet points. Do NOT use markdown symbols like asterisks or hash symbols, as the UI does not parse markdown.
            
            Portfolio Data:
            $portfolioData
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${apiKey}")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string()
            if (response.isSuccessful && responseString != null) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                parts?.optJSONObject(0)?.optString("text")
            } else if (response.code == 429) {
                "AI Insights Unavailable: You have exceeded your Gemini API quota or rate limit. If you are on the free tier, please wait a moment and try again."
            } else {
                "Failed to generate insights. Error code: ${response.code}\n${responseString}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Network error occurred while fetching AI insights: ${e.message}"
        }
    }
}
