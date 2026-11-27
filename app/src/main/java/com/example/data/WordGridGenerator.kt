package com.example.data

import com.example.model.GeneratedBoard
import com.example.model.GridCoord
import com.example.model.LevelData
import com.example.model.WordPlacement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt
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

        for (attempt in 0 until 120) {
            val grid = Array(size) { CharArray(size) { ' ' } }
            val placements = mutableListOf<WordPlacement>()
            var allPlaced = true

            val activeDirs = if (attempt < 55) directions else allDirections
            val wordOrder = if (attempt < 40) sortedWords else sortedWords.shuffled(random)
            for ((idx, word) in wordOrder.withIndex()) {
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

        // Guaranteed deterministic fallback with zero overlap:
        // - When words.size <= gridSize: 1 horizontal word per row.
        // - When words.size == gridSize + 1 (e.g. 10 words of max 8 letters on 9x9):
        //   1 word vertically in the last column + gridSize horizontal words in columns 0..(gridSize - 2).
        val fallbackGrid = Array(size) { CharArray(size) { ' ' } }
        val fallbackPlacements = mutableListOf<WordPlacement>()
        if (sortedWords.size <= size) {
            sortedWords.forEachIndexed { idx, word ->
                val row = idx
                val maxStart = (size - word.length).coerceAtLeast(0)
                val colStart = if (maxStart > 0) random.nextInt(maxStart + 1) else 0
                val coords = ArrayList<GridCoord>(word.length)
                for (i in word.indices) {
                    val c = colStart + i
                    fallbackGrid[row][c] = word[i]
                    coords.add(GridCoord(row, c))
                }
                fallbackPlacements.add(WordPlacement(word, coords, idx % paletteSize))
            }
        } else {
            val colWordIndex = sortedWords.indexOfLast { it.length <= size }
            var rowIdx = 0
            val placedIndices = mutableSetOf<Int>()
            if (colWordIndex >= 0 && sortedWords.count { it.length <= size - 1 } >= sortedWords.size - 1) {
                val colWord = sortedWords[colWordIndex]
                val lastCol = size - 1
                val colCoords = ArrayList<GridCoord>(colWord.length)
                for (r in colWord.indices) {
                    fallbackGrid[r][lastCol] = colWord[r]
                    colCoords.add(GridCoord(r, lastCol))
                }
                fallbackPlacements.add(WordPlacement(colWord, colCoords, colWordIndex % paletteSize))
                placedIndices.add(colWordIndex)

                for ((idx, word) in sortedWords.withIndex()) {
                    if (idx in placedIndices) continue
                    if (rowIdx < size && word.length <= size - 1) {
                        val rowCoords = ArrayList<GridCoord>(word.length)
                        for (c in word.indices) {
                            fallbackGrid[rowIdx][c] = word[c]
                            rowCoords.add(GridCoord(rowIdx, c))
                        }
                        fallbackPlacements.add(WordPlacement(word, rowCoords, idx % paletteSize))
                        placedIndices.add(idx)
                        rowIdx++
                    }
                }
            }

            sortedWords.forEachIndexed { idx, word ->
                if (idx !in placedIndices) {
                    val placed = tryPlaceWord(fallbackGrid, word, listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0), random)
                        ?: tryPlaceWord(fallbackGrid, word, allDirections, random)
                    if (placed != null) {
                        fallbackPlacements.add(WordPlacement(word, placed, idx % paletteSize))
                    }
                }
            }
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

    /**
     * Picks the grid coordinate whose rendered tile center is closest to (xPx, yPx).
     * Accounting for cell spacing and clamping to [0, gridSize - 1] ensures even
     * touches on gaps or the outer frame padding select the nearest edge letter accurately.
     */
    fun findClosestCoord(
        xPx: Float,
        yPx: Float,
        areaWidthPx: Float,
        areaHeightPx: Float,
        spacingPx: Float,
        gridSize: Int
    ): GridCoord? {
        if (areaWidthPx <= 0f || areaHeightPx <= 0f || gridSize <= 0) return null
        val totalSpacing = spacingPx * (gridSize - 1).coerceAtLeast(0)
        val tileW = ((areaWidthPx - totalSpacing) / gridSize.toFloat()).coerceAtLeast(1f)
        val tileH = ((areaHeightPx - totalSpacing) / gridSize.toFloat()).coerceAtLeast(1f)
        val pitchX = tileW + spacingPx
        val pitchY = tileH + spacingPx

        var bestCol = 0
        var bestDistX = Float.MAX_VALUE
        for (c in 0 until gridSize) {
            val centerX = tileW * 0.5f + c * pitchX
            val dist = abs(xPx - centerX)
            if (dist < bestDistX) {
                bestDistX = dist
                bestCol = c
            }
        }

        var bestRow = 0
        var bestDistY = Float.MAX_VALUE
        for (r in 0 until gridSize) {
            val centerY = tileH * 0.5f + r * pitchY
            val dist = abs(yPx - centerY)
            if (dist < bestDistY) {
                bestDistY = dist
                bestRow = r
            }
        }

        return GridCoord(bestRow, bestCol)
    }

    /**
     * Snaps a drag vector from the anchor cell's exact center to the pointer's current
     * pixel position along the closest of the 8 compass axes, eliminating diagonal wobble
     * caused by pre-rounding pointer coordinates before angle snapping.
     */
    fun computeSnappedLineFromPointer(
        anchor: GridCoord,
        pointerXPx: Float,
        pointerYPx: Float,
        areaWidthPx: Float,
        areaHeightPx: Float,
        spacingPx: Float,
        gridSize: Int
    ): List<GridCoord> {
        if (areaWidthPx <= 0f || areaHeightPx <= 0f || gridSize <= 0) return listOf(anchor)
        val totalSpacing = spacingPx * (gridSize - 1).coerceAtLeast(0)
        val tileW = ((areaWidthPx - totalSpacing) / gridSize.toFloat()).coerceAtLeast(1f)
        val tileH = ((areaHeightPx - totalSpacing) / gridSize.toFloat()).coerceAtLeast(1f)
        val pitchX = tileW + spacingPx
        val pitchY = tileH + spacingPx

        val anchorCx = tileW * 0.5f + anchor.col * pitchX
        val anchorCy = tileH * 0.5f + anchor.row * pitchY
        val dxPx = pointerXPx - anchorCx
        val dyPx = pointerYPx - anchorCy

        val closest = findClosestCoord(pointerXPx, pointerYPx, areaWidthPx, areaHeightPx, spacingPx, gridSize)
        if (closest == null || closest == anchor) return listOf(anchor)

        val angle = atan2(dyPx.toDouble(), dxPx.toDouble())
        val octant = ((angle / (PI / 4.0)).roundToInt() % 8 + 8) % 8
        val (stepR, stepC) = when (octant) {
            0 -> 0 to 1
            1 -> 1 to 1
            2 -> 1 to 0
            3 -> 1 to -1
            4 -> 0 to -1
            5 -> -1 to -1
            6 -> -1 to 0
            else -> -1 to 1
        }

        val unitStepX = stepC * pitchX
        val unitStepY = stepR * pitchY
        val denom = unitStepX * unitStepX + unitStepY * unitStepY
        if (denom <= 0f) return listOf(anchor)

        val steps = ((dxPx * unitStepX + dyPx * unitStepY) / denom).roundToInt().coerceAtLeast(0)
        if (steps == 0) return listOf(anchor)

        val coords = ArrayList<GridCoord>(steps + 1)
        for (i in 0..steps) {
            val r = anchor.row + i * stepR
            val c = anchor.col + i * stepC
            if (r in 0 until gridSize && c in 0 until gridSize) {
                coords.add(GridCoord(r, c))
            } else {
                break
            }
        }
        return coords
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
                        if (existing != ' ') {
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
