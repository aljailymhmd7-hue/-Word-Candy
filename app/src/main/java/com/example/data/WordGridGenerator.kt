package com.example.data

import com.example.model.GeneratedBoard
import com.example.model.GridCoord
import com.example.model.LevelData
import com.example.model.WordPlacement
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.random.Random

object WordGridGenerator {

    private val allDirections = listOf(
        0 to 1,   // right
        1 to 0,   // down
        1 to 1,   // down-right
        -1 to 1,  // up-right
        0 to -1,  // left
        -1 to 0,  // up
        -1 to -1, // up-left
        1 to -1   // down-left
    )

    private val easyDirections = listOf(
        0 to 1,   // right
        1 to 0,   // down
        1 to 1,   // down-right
        -1 to 1   // up-right
    )

    fun generate(levelData: LevelData, seedOffset: Int = 0, paletteSize: Int = 7): GeneratedBoard {
        val size = levelData.gridSize
        val seed = levelData.level * 10007L + seedOffset * 97L + levelData.words.hashCode()
        val random = Random(seed)

        val directions = if (levelData.level <= 30) easyDirections else allDirections
        val sortedWords = levelData.words
            .map { it.uppercase().filter { ch -> ch in 'A'..'Z' } }
            .filter { it.isNotEmpty() && it.length <= size }
            .sortedByDescending { it.length }

        for (attempt in 0 until 45) {
            val grid = Array(size) { CharArray(size) { ' ' } }
            val placements = mutableListOf<WordPlacement>()
            var allPlaced = true

            val activeDirs = if (attempt < 25) directions else allDirections
            for ((idx, word) in sortedWords.withIndex()) {
                val placedCoords = tryPlaceWord(grid, word, activeDirs, random)
                if (placedCoords == null) {
                    allPlaced = false
                    break
                }
                placements.add(
                    WordPlacement(
                        word = word,
                        coords = placedCoords,
                        colorIndex = idx % paletteSize
                    )
                )
            }

            if (allPlaced) {
                return finalizeBoard(size, grid, placements, random, paletteSize)
            }
        }

        // Guaranteed deterministic fallback: horizontal / vertical non-overlapping rows
        val fallbackGrid = Array(size) { CharArray(size) { ' ' } }
        val fallbackPlacements = mutableListOf<WordPlacement>()
        sortedWords.forEachIndexed { idx, word ->
            val row = idx % size
            val colStart = if (word.length < size) (idx * 2) % (size - word.length + 1) else 0
            val coords = mutableListOf<GridCoord>()
            for (i in word.indices) {
                val c = (colStart + i).coerceAtMost(size - 1)
                fallbackGrid[row][c] = word[i]
                coords.add(GridCoord(row, c))
            }
            fallbackPlacements.add(WordPlacement(word, coords, idx % paletteSize))
        }
        return finalizeBoard(size, fallbackGrid, fallbackPlacements, random, paletteSize)
    }

    fun shuffleFillerOnly(board: GeneratedBoard, paletteSize: Int = 7): GeneratedBoard {
        val random = Random(System.nanoTime())
        val protected = board.placements.flatMap { it.coords }.toSet()
        val newLetters = board.letters.mapIndexed { r, row ->
            row.mapIndexed { c, ch ->
                if (GridCoord(r, c) in protected) ch else ('A' + random.nextInt(26))
            }
        }
        val newColors = board.colorIndices.map { row ->
            row.map { random.nextInt(paletteSize) }
        }
        return board.copy(letters = newLetters, colorIndices = newColors)
    }

    fun computeLineCoords(start: GridCoord, end: GridCoord, gridSize: Int): List<GridCoord> {
        val dr = end.row - start.row
        val dc = end.col - start.col
        if (dr == 0 && dc == 0) return listOf(start)

        // Snap to nearest of the 8 valid compass axes
        val absR = abs(dr)
        val absC = abs(dc)
        val stepR: Int
        val stepC: Int
        val steps: Int

        when {
            absR * 2 < absC -> {
                stepR = 0
                stepC = dc.sign
                steps = absC
            }
            absC * 2 < absR -> {
                stepR = dr.sign
                stepC = 0
                steps = absR
            }
            else -> {
                stepR = dr.sign
                stepC = dc.sign
                steps = max(absR, absC)
            }
        }

        val coords = mutableListOf<GridCoord>()
        for (i in 0..steps) {
            val r = start.row + i * stepR
            val c = start.col + i * stepC
            if (r in 0 until gridSize && c in 0 until gridSize) {
                coords.add(GridCoord(r, c))
            } else {
                break
            }
        }
        return coords
    }

    private fun tryPlaceWord(
        grid: Array<CharArray>,
        word: String,
        directions: List<Pair<Int, Int>>,
        random: Random
    ): List<GridCoord>? {
        val size = grid.size
        val candidates = mutableListOf<List<GridCoord>>()
        val dirs = directions.shuffled(random)

        for ((dr, dc) in dirs) {
            for (r in 0 until size) {
                for (c in 0 until size) {
                    val endR = r + dr * (word.length - 1)
                    val endC = c + dc * (word.length - 1)
                    if (endR !in 0 until size || endC !in 0 until size) continue

                    var fits = true
                    val coords = ArrayList<GridCoord>(word.length)
                    for (i in word.indices) {
                        val cr = r + dr * i
                        val cc = c + dc * i
                        val existing = grid[cr][cc]
                        if (existing != ' ' && existing != word[i]) {
                            fits = false
                            break
                        }
                        coords.add(GridCoord(cr, cc))
                    }
                    if (fits) {
                        candidates.add(coords)
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null
        val chosen = candidates[random.nextInt(candidates.size)]
        for (i in word.indices) {
            val coord = chosen[i]
            grid[coord.row][coord.col] = word[i]
        }
        return chosen
    }

    private fun finalizeBoard(
        size: Int,
        grid: Array<CharArray>,
        placements: List<WordPlacement>,
        random: Random,
        paletteSize: Int
    ): GeneratedBoard {
        val letters = List(size) { r ->
            List(size) { c ->
                val ch = grid[r][c]
                if (ch == ' ') ('A' + random.nextInt(26)) else ch
            }
        }
        val colorIndices = List(size) { r ->
            List(size) { c ->
                ((r * 3 + c * 5 + random.nextInt(paletteSize)) % paletteSize)
            }
        }
        return GeneratedBoard(
            gridSize = size,
            letters = letters,
            colorIndices = colorIndices,
            placements = placements
        )
    }
}
