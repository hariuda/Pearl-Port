package com.example.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject

class MockCseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        if (url.contains("market/v1/quotes")) {
            val symbolsStr = request.url.queryParameter("symbols") ?: ""
            val symbols = symbolsStr.split(",")

            val jsonArray = JSONArray()
            symbols.forEach { symbol ->
                val basePrice = when(symbol) {
                    "COMB.N0000" -> 115.50
                    "JKH.N0000" -> 190.25
                    "SAMP.N0000" -> 85.00
                    "DIAL.N0000" -> 11.20
                    "HNB.N0000" -> 205.00
                    else -> 100.00
                }
                
                // Simulate some random fluctuation
                val fluctuation = (Math.random() - 0.5) * 5.0
                val currentPrice = basePrice + fluctuation
                
                val obj = JSONObject()
                obj.put("symbol", symbol)
                obj.put("price", currentPrice)
                obj.put("change", fluctuation)
                obj.put("changePercent", (fluctuation / basePrice) * 100)
                obj.put("timestamp", System.currentTimeMillis())
                jsonArray.put(obj)
            }

            val responseObj = JSONObject()
            responseObj.put("data", jsonArray)

            return Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(responseObj.toString().toResponseBody("application/json".toMediaTypeOrNull()))
                .addHeader("content-type", "application/json")
                .build()
        }

        return chain.proceed(request)
    }
}
