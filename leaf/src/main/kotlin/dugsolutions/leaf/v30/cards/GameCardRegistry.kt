package dugsolutions.leaf.v30.cards

import androidx.compose.ui.graphics.Color
import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.GenCardID
import dugsolutions.leaf.v30.cards.domain.GameCard
import java.io.File

class GameCardRegistry {

    companion object {
        private const val EXPECTED_COLUMN_COUNT = 14
    }

    private object Column {
        const val QUANTITY = 0
        const val NAME = 1
        const val TYPE = 3
        const val COST = 4
        const val LINE_ICON = 6
        const val FG_COLOR = 8
        const val TEXT_COLOR = 9
        const val FULL_IMAGE = 10
        const val BG_IMAGE2 = 11
        const val BG_CARD_IMAGE2 = 12
        const val EFFECT = 13
    }

    private val cards: MutableMap<String, GameCard> = mutableMapOf()

    // region public

    fun loadFromCsv(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("CSV file not found at path: $filePath")
        }
        parseCsv(file.readText())
            .drop(1) // Skip header row
            .filter { row -> row.any { it.isNotBlank() } }
            .forEach { row ->
                val card = parseCardFromCsv(row)
                cards[card.name] = card
            }
    }

    fun getCard(name: String): GameCard? = cards[name]

    fun getAllCards(): List<GameCard> = cards.values.toList()

    // endregion public

    private fun parseCardFromCsv(parts: List<String>): GameCard {
        if (parts.size < EXPECTED_COLUMN_COUNT || parts[0].isBlank()) {
            throw IllegalArgumentException("Invalid card row: $parts")
        }

        val quantity = parseInt(parts[Column.QUANTITY], "quantity", parts)
        val name = parts[Column.NAME].trim()
        val type = CardType.from(parts[Column.TYPE].trim())
            ?: throw IllegalArgumentException("Unknown card type: ${parts[Column.TYPE]}")
        val cost = parseInt(parts[Column.COST], "cost", parts)

        return GameCard(
            id = GenCardID.generateId(name),
            quantity = quantity,
            name = name,
            type = type,
            cost = cost,
            lineIcon = parseOptional(parts[Column.LINE_ICON]),
            fgColor = parseColor(parts[Column.FG_COLOR]),
            textColor = parseColor(parts[Column.TEXT_COLOR]),
            fullImage = parseOptional(parts[Column.FULL_IMAGE]),
            bgImage2 = parseOptional(parts[Column.BG_IMAGE2]),
            bgCardImage2 = parseOptional(parts[Column.BG_CARD_IMAGE2]),
            effect = parts[Column.EFFECT].trim()
        )
    }

    private fun parseCsv(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val value = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < content.length) {
            val char = content[index]
            when {
                char == '"' && inQuotes && content.getOrNull(index + 1) == '"' -> {
                    value.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    row.add(value.toString())
                    value.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    row.add(value.toString())
                    value.clear()
                    rows.add(row.toList())
                    row.clear()
                    if (char == '\r' && content.getOrNull(index + 1) == '\n') {
                        index++
                    }
                }
                else -> value.append(char)
            }
            index++
        }

        if (value.isNotEmpty() || row.isNotEmpty()) {
            row.add(value.toString())
            rows.add(row.toList())
        }

        return rows
    }

    private fun parseInt(value: String, columnName: String, row: List<String>): Int {
        return value.trim().toIntOrNull()
            ?: throw IllegalArgumentException("Invalid $columnName value '${value}' in row: $row")
    }

    private fun parseColor(value: String): Color {
        val normalized = value.trim().removePrefix("#")
        if (normalized.length != 6) {
            throw IllegalArgumentException("Invalid color value: $value")
        }
        val rgb = normalized.toLongOrNull(radix = 16)
            ?: throw IllegalArgumentException("Invalid color value: $value")
        return Color(0xFF000000 or rgb)
    }

    private fun parseOptional(value: String): String? {
        return value.trim().takeIf { it.isNotEmpty() }
    }

}
