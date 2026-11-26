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
@Config(sdk = [36])
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

        val level24En = enLevels[23]
        assertEquals(listOf("CAT", "BALL", "RAIN", "STAR", "CLOUD"), level24En.words)
        val board = WordGridGenerator.generate(level24En)
        assertEquals(6, board.gridSize)
        assertEquals(5, board.placements.size)
        board.placements.forEach { placement ->
            val spelled = placement.coords.joinToString("") { board.letters[it.row][it.col].toString() }
            assertEquals(placement.word, spelled)
        }

        val level24Fr = frLevels[23]
        assertEquals(listOf("CHAT", "BALLE", "PLUIE", "ETOILE", "NUAGE"), level24Fr.words)
        assertTrue(WordGridGenerator.generate(enLevels.last()).placements.size == 6)
    }
}
