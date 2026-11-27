package com.example.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AdminDateRange(
    val label: String,
    val startDate: String
) {
    LAST_7_DAYS("7D", "7daysAgo"),
    LAST_30_DAYS("30D", "30daysAgo"),
    ALL_TIME("ALL", "2020-01-01")
}

data class Ga4PropertySummary(
    val propertyId: String,
    val displayName: String,
    val accountName: String
)

data class LevelDownloadRank(
    val levelName: String,
    val downloadCount: Long
)

data class AdminAnalyticsReport(
    val propertyId: String,
    val dateRange: AdminDateRange,
    val totalInstalls: Long,
    val customAppInstalledCount: Long,
    val totalUninstalls: Long,
    val retentionRatePercent: Double?,
    val dauPerMauPercent: Double?,
    val activeUsers: Long,
    val newUsers: Long,
    val totalLevelDownloads: Long,
    val mostDownloadedLevel: LevelDownloadRank?,
    val topDownloadedLevels: List<LevelDownloadRank>,
    val customDimensionNote: String? = null,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

class AnalyticsApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

/**
 * Fetches live aggregate counts from Google Analytics Admin API v1beta & Data API v1beta
 * (the official reporting API for Firebase Analytics GA4 properties).
 */
class FirebaseAnalyticsDataClient(
    private val httpClient: OkHttpClient = defaultHttpClient()
) {

    suspend fun fetchAvailableProperties(accessToken: String): List<Ga4PropertySummary> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$ADMIN_API_BASE/accountSummaries?pageSize=50")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw AnalyticsApiException(
                        response.code,
                        extractGoogleApiError(rawBody, response.code)
                    )
                }
                parseAccountSummaries(rawBody)
            }
        }

    suspend fun fetchDashboardReport(
        accessToken: String,
        propertyId: String,
        dateRange: AdminDateRange
    ): AdminAnalyticsReport = withContext(Dispatchers.IO) {
        val cleanId = normalizePropertyId(propertyId)
        require(cleanId.isNotBlank()) { "GA4 Property ID is required." }

        val eventBody = JSONObject().apply {
            put("dateRanges", JSONArray().put(dateRangeJson(dateRange)))
            put("dimensions", JSONArray().put(JSONObject().put("name", "eventName")))
            put(
                "metrics",
                JSONArray()
                    .put(JSONObject().put("name", "eventCount"))
                    .put(JSONObject().put("name", "totalUsers"))
            )
            put(
                "dimensionFilter",
                JSONObject().put(
                    "filter",
                    JSONObject().apply {
                        put("fieldName", "eventName")
                        put(
                            "inListFilter",
                            JSONObject().put(
                                "values",
                                JSONArray()
                                    .put("first_open")
                                    .put("app_remove")
                                    .put("app_installed")
                                    .put("level_download")
                            )
                        )
                    }
                )
            )
        }

        val eventCounts = runReport(accessToken, cleanId, eventBody)
        val parsedEvents = parseEventCounts(eventCounts)

        val retentionBody = JSONObject().apply {
            put("dateRanges", JSONArray().put(dateRangeJson(dateRange)))
            put(
                "metrics",
                JSONArray()
                    .put(JSONObject().put("name", "activeUsers"))
                    .put(JSONObject().put("name", "newUsers"))
                    .put(JSONObject().put("name", "dauPerMau"))
            )
        }
        val retentionJson = runReport(accessToken, cleanId, retentionBody)
        val userMetrics = parseUserRetention(retentionJson)

        var topLevels: List<LevelDownloadRank> = emptyList()
        var customDimensionNote: String? = null
        val topLevelsBody = JSONObject().apply {
            put("dateRanges", JSONArray().put(dateRangeJson(dateRange)))
            put("dimensions", JSONArray().put(JSONObject().put("name", "customEvent:level_name")))
            put("metrics", JSONArray().put(JSONObject().put("name", "eventCount")))
            put(
                "dimensionFilter",
                JSONObject().put(
                    "filter",
                    JSONObject().apply {
                        put("fieldName", "eventName")
                        put(
                            "stringFilter",
                            JSONObject().apply {
                                put("matchType", "EXACT")
                                put("value", "level_download")
                            }
                        )
                    }
                )
            )
            put(
                "orderBys",
                JSONArray().put(
                    JSONObject().apply {
                        put("metric", JSONObject().put("metricName", "eventCount"))
                        put("desc", true)
                    }
                )
            )
            put("limit", 5)
        }

        try {
            val topLevelsJson = runReport(accessToken, cleanId, topLevelsBody)
            topLevels = parseTopDownloadedLevels(topLevelsJson)
        } catch (err: AnalyticsApiException) {
            if (err.statusCode == 400) {
                customDimensionNote =
                    "Register 'level_name' as an Event-scoped Custom Dimension in GA4 Admin to rank downloaded levels."
            } else {
                throw err
            }
        }

        val firstOpenCount = parsedEvents["first_open"] ?: 0L
        val appInstalledCount = parsedEvents["app_installed"] ?: 0L
        val totalInstalls = if (firstOpenCount > 0L) firstOpenCount else appInstalledCount
        val totalUninstalls = parsedEvents["app_remove"] ?: 0L
        val totalLevelDownloads = parsedEvents["level_download"] ?: topLevels.sumOf { it.downloadCount }

        val retentionPercent = if (totalInstalls > 0L) {
            ((totalInstalls - totalUninstalls).coerceAtLeast(0L).toDouble() / totalInstalls.toDouble()) * 100.0
        } else {
            userMetrics.dauPerMauPercent
        }

        AdminAnalyticsReport(
            propertyId = cleanId,
            dateRange = dateRange,
            totalInstalls = totalInstalls,
            customAppInstalledCount = appInstalledCount,
            totalUninstalls = totalUninstalls,
            retentionRatePercent = retentionPercent,
            dauPerMauPercent = userMetrics.dauPerMauPercent,
            activeUsers = userMetrics.activeUsers,
            newUsers = userMetrics.newUsers,
            totalLevelDownloads = totalLevelDownloads,
            mostDownloadedLevel = topLevels.firstOrNull(),
            topDownloadedLevels = topLevels,
            customDimensionNote = customDimensionNote
        )
    }

    private fun runReport(accessToken: String, cleanPropertyId: String, payload: JSONObject): String {
        val request = Request.Builder()
            .url("$DATA_API_BASE/properties/$cleanPropertyId:runReport")
            .header("Authorization", "Bearer $accessToken")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AnalyticsApiException(
                    response.code,
                    extractGoogleApiError(rawBody, response.code)
                )
            }
            return rawBody
        }
    }

    private fun dateRangeJson(dateRange: AdminDateRange): JSONObject =
        JSONObject().apply {
            put("startDate", dateRange.startDate)
            put("endDate", "today")
        }

    internal data class UserRetentionMetrics(
        val activeUsers: Long,
        val newUsers: Long,
        val dauPerMauPercent: Double?
    )

    companion object {
        const val ANALYTICS_READONLY_SCOPE = "https://www.googleapis.com/auth/analytics.readonly"
        private const val ADMIN_API_BASE = "https://analyticsadmin.googleapis.com/v1beta"
        private const val DATA_API_BASE = "https://analyticsdata.googleapis.com/v1beta"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun normalizePropertyId(raw: String): String =
            raw.trim().removePrefix("properties/").trim()

        internal fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        internal fun parseAccountSummaries(jsonString: String): List<Ga4PropertySummary> {
            val root = JSONObject(jsonString)
            val summaries = root.optJSONArray("accountSummaries") ?: return emptyList()
            val results = mutableListOf<Ga4PropertySummary>()
            for (i in 0 until summaries.length()) {
                val accountObj = summaries.optJSONObject(i) ?: continue
                val accountName = accountObj.optString("displayName", "Account")
                val props = accountObj.optJSONArray("propertySummaries") ?: continue
                for (j in 0 until props.length()) {
                    val propObj = props.optJSONObject(j) ?: continue
                    val propResource = propObj.optString("property", "")
                    val cleanId = normalizePropertyId(propResource)
                    val displayName = propObj.optString("displayName", cleanId)
                    if (cleanId.isNotBlank()) {
                        results.add(
                            Ga4PropertySummary(
                                propertyId = cleanId,
                                displayName = displayName,
                                accountName = accountName
                            )
                        )
                    }
                }
            }
            return results
        }

        internal fun parseEventCounts(jsonString: String): Map<String, Long> {
            val root = JSONObject(jsonString)
            val rows = root.optJSONArray("rows") ?: return emptyMap()
            val map = mutableMapOf<String, Long>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val eventName = row.optJSONArray("dimensionValues")
                    ?.optJSONObject(0)
                    ?.optString("value")
                    .orEmpty()
                val count = row.optJSONArray("metricValues")
                    ?.optJSONObject(0)
                    ?.optString("value")
                    ?.toLongOrNull() ?: 0L
                if (eventName.isNotBlank()) {
                    map[eventName] = count
                }
            }
            return map
        }

        internal fun parseUserRetention(jsonString: String): UserRetentionMetrics {
            val root = JSONObject(jsonString)
            val row = root.optJSONArray("rows")?.optJSONObject(0)
                ?: return UserRetentionMetrics(0L, 0L, null)
            val metrics = row.optJSONArray("metricValues")
                ?: return UserRetentionMetrics(0L, 0L, null)
            val activeUsers = metrics.optJSONObject(0)?.optString("value")?.toLongOrNull() ?: 0L
            val newUsers = metrics.optJSONObject(1)?.optString("value")?.toLongOrNull() ?: 0L
            val dauPerMauRatio = metrics.optJSONObject(2)?.optString("value")?.toDoubleOrNull()
            val dauPerMauPercent = dauPerMauRatio?.let { (it * 100.0).coerceIn(0.0, 100.0) }
            return UserRetentionMetrics(
                activeUsers = activeUsers,
                newUsers = newUsers,
                dauPerMauPercent = dauPerMauPercent
            )
        }

        internal fun parseTopDownloadedLevels(jsonString: String): List<LevelDownloadRank> {
            val root = JSONObject(jsonString)
            val rows = root.optJSONArray("rows") ?: return emptyList()
            val list = mutableListOf<LevelDownloadRank>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val levelName = row.optJSONArray("dimensionValues")
                    ?.optJSONObject(0)
                    ?.optString("value")
                    .orEmpty()
                val count = row.optJSONArray("metricValues")
                    ?.optJSONObject(0)
                    ?.optString("value")
                    ?.toLongOrNull() ?: 0L
                if (levelName.isNotBlank() && levelName != "(not set)") {
                    list.add(LevelDownloadRank(levelName = levelName, downloadCount = count))
                }
            }
            return list
        }

        internal fun extractGoogleApiError(rawBody: String, statusCode: Int): String {
            return runCatching {
                val err = JSONObject(rawBody).optJSONObject("error")
                val msg = err?.optString("message").orEmpty()
                if (msg.isNotBlank()) "HTTP $statusCode: $msg" else "Google Analytics API error ($statusCode)"
            }.getOrElse {
                "Google Analytics API error ($statusCode)"
            }
        }
    }
}
