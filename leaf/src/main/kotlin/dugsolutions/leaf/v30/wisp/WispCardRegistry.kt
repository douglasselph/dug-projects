package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.wisp.domain.GenWispCardID
import dugsolutions.leaf.v30.wisp.domain.WispCard
import java.io.File

class WispCardRegistry {
    companion object {
        private const val EXPECTED_COLUMN_COUNT = 8
    }

    private object Column {
        const val QUANTITY = 0
        const val NAME = 1
        const val TITLE = 2
        const val COUNT = 3
        const val EFFECT = 4
        const val LINE_ICONS = 5
        const val LINE_ICONS_HEIGHT = 6
        const val MAIN_BACKDROP = 7
    }

    private val cards: MutableMap<String, WispCard> = mutableMapOf()

    fun loadFromCsv(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("CSV file not found at path: $filePath")
        }
        parseCsv(file.readText())
            .drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .forEach { row ->
                val card = parseCardFromCsv(row)
                cards[card.name] = card
            }
    }

    fun getCard(name: String): WispCard? = cards[name]

    fun getAllCards(): List<WispCard> = cards.values.toList()

    private fun parseCardFromCsv(parts: List<String>): WispCard {
        if (parts.size < EXPECTED_COLUMN_COUNT || parts[Column.NAME].isBlank()) {
            throw IllegalArgumentException("Invalid wisp card row: $parts")
        }
        val name = parts[Column.NAME].trim()
        return WispCard(
            id = GenWispCardID.generateId(name),
            quantity = parseInt(parts[Column.QUANTITY], "quantity", parts),
            name = name,
            title = parts[Column.TITLE].trim(),
            count = parseInt(parts[Column.COUNT], "count", parts),
            effect = parts[Column.EFFECT].trim(),
            lineIcons = parseOptional(parts[Column.LINE_ICONS]),
            lineIconsHeight = parseInt(parts[Column.LINE_ICONS_HEIGHT], "line_icons_height", parts),
            mainBackdrop = parseOptional(parts[Column.MAIN_BACKDROP])
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

    private fun parseOptional(value: String): String? {
        return value.trim().takeIf { it.isNotEmpty() }
    }
}
