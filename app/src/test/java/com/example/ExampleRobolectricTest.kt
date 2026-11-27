package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LevelRepository
import com.example.data.WordGridGenerator
import com.example.model.AppLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Word Candy", appName)
    }

    @Test
    fun `load 1000 EN and FR levels and generate valid boards`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LevelRepository(context)
        val enLevels = repo.getLevels(AppLanguage.EN)
        val frLevels = repo.getLevels(AppLanguage.FR)

        assertEquals(1000, enLevels.size)
        assertEquals(1000, frLevels.size)

        val level1En = enLevels.first()
        assertEquals(3, level1En.words.size)
        assertEquals(6, level1En.gridSize)
        assertTrue(level1En.words.all { it.length in 3..4 })

        val level24En = enLevels[23]
        assertEquals(10, level24En.words.size)
        assertTrue(level24En.words.all { it.length in 5..8 })
        val board = WordGridGenerator.generate(level24En)
        assertEquals(9, board.gridSize)
        assertEquals(10, board.placements.size)
        board.placements.forEach { placement ->
            val spelled = placement.coords.joinToString("") { board.letters[it.row][it.col].toString() }
            assertEquals(placement.word, spelled)
        }

        val level24Fr = frLevels[23]
        assertEquals(10, level24Fr.words.size)
        assertTrue(level24Fr.words.all { it.length in 5..8 })
        assertEquals(10, WordGridGenerator.generate(enLevels.last()).placements.size)
    }

    @Test
    fun `firebase analytics initializes cleanly with provisioned project`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instance = com.example.analytics.FirebaseAnalyticsTracker.initialize(context)
        assertTrue(instance != null)
        com.example.analytics.FirebaseAnalyticsTracker.trackLevelDownload(1)
        com.example.analytics.FirebaseAnalyticsTracker.trackLevelComplete(1)
        val app = com.google.firebase.FirebaseApp.getInstance()
        assertEquals("gen-lang-client-0270070827", app.options.projectId)
    }

    @Test
    fun `analytics data client parses GA4 event counts retention and top levels`() {
        val accountSummariesJson = """
            {
              "accountSummaries": [
                {
                  "displayName": "WordCandy Account",
                  "propertySummaries": [
                    {
                      "property": "properties/987654321",
                      "displayName": "Word Candy Android"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val props = com.example.analytics.FirebaseAnalyticsDataClient.parseAccountSummaries(accountSummariesJson)
        assertEquals(1, props.size)
        assertEquals("987654321", props.first().propertyId)
        assertEquals("Word Candy Android", props.first().displayName)

        val eventsJson = """
            {
              "rows": [
                { "dimensionValues": [{ "value": "first_open" }], "metricValues": [{ "value": "1200" }, { "value": "1180" }] },
                { "dimensionValues": [{ "value": "app_remove" }], "metricValues": [{ "value": "180" }, { "value": "180" }] },
                { "dimensionValues": [{ "value": "level_download" }], "metricValues": [{ "value": "4520" }, { "value": "940" }] }
              ]
            }
        """.trimIndent()
        val counts = com.example.analytics.FirebaseAnalyticsDataClient.parseEventCounts(eventsJson)
        assertEquals(1200L, counts["first_open"])
        assertEquals(180L, counts["app_remove"])
        assertEquals(4520L, counts["level_download"])

        val retentionJson = """
            {
              "rows": [
                {
                  "metricValues": [
                    { "value": "850" },
                    { "value": "420" },
                    { "value": "0.345" }
                  ]
                }
              ]
            }
        """.trimIndent()
        val retention = com.example.analytics.FirebaseAnalyticsDataClient.parseUserRetention(retentionJson)
        assertEquals(850L, retention.activeUsers)
        assertEquals(420L, retention.newUsers)
        assertEquals(34.5, retention.dauPerMauPercent ?: 0.0, 0.01)

        val topLevelsJson = """
            {
              "rows": [
                { "dimensionValues": [{ "value": "Level 1" }], "metricValues": [{ "value": "910" }] },
                { "dimensionValues": [{ "value": "Level 4" }], "metricValues": [{ "value": "640" }] }
              ]
            }
        """.trimIndent()
        val topLevels = com.example.analytics.FirebaseAnalyticsDataClient.parseTopDownloadedLevels(topLevelsJson)
        assertEquals(2, topLevels.size)
        assertEquals("Level 1", topLevels.first().levelName)
        assertEquals(910L, topLevels.first().downloadCount)
    }
}
