package com.example.network

import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Json

interface CseApi {
    @POST("api/tradeSummary")
    suspend fun getTradeSummary(): TradeSummaryResponse

    @GET("api/companyProfile")
    suspend fun getCompanyProfile(@Query("symbol") symbol: String): CompanyProfileResponse

    @POST("api/aspiData")
    suspend fun getAspiData(): AspiDataResponse
}

data class AspiDataResponse(
    val value: Double,
    val change: Double,
    val percentage: Double
)

data class TradeSummaryResponse(
    @Json(name = "reqTradeSummery") val reqTradeSummery: List<TradeSummaryData>?
)

data class TradeSummaryData(
    val symbol: String,
    val price: Double,
    val change: Double,
    val percentageChange: Double
)

data class CompanyProfileResponse(
    @Json(name = "reqComSumInfo") val reqComSumInfo: List<CompanySummaryInfo>?
)

data class CompanySummaryInfo(
    val name: String?,
    val sector: String?
)
