package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.round.domain.GenRoundCardID
import dugsolutions.leaf.v30.round.domain.RoundCard
import java.io.File

class RoundCardRegistry {
    companion object {
        private const val EXPECTED_COLUMN_COUNT = 16
    }

    private object Column {
        const val QUANTITY = 0
        const val NAME = 1
        const val TITLE = 2
        const val EFFECT_1_TITLE = 3
        const val EFFECT_1_TEXT = 4
        const val EFFECT_1_BG = 5
        const val EFFECT_1_TEXT_FG = 6
        const val EFFECT_1_IMAGE = 7
        const val EFFECT_1_ICON = 8
        const val EFFECT_2_TITLE = 9
        const val EFFECT_2_TEXT = 10
        const val EFFECT_2_BG = 11
        const val EFFECT_2_TEXT_FG = 12
        const val EFFECT_2_IMAGE = 13
        const val EFFECT_2_ICON = 14
        const val BACK_IMAGE = 15
    }

    private val cards: MutableMap<String, RoundCard> = mutableMapOf()

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

    fun getCard(name: String): RoundCard? = cards[name]

    fun getAllCards(): List<RoundCard> = cards.values.toList()

    private fun parseCardFromCsv(parts: List<String>): RoundCard {
        if (parts.size < EXPECTED_COLUMN_COUNT || parts[Column.NAME].isBlank()) {
            throw IllegalArgumentException("Invalid round card row: $parts")
        }
        val name = parts[Column.NAME].trim()
        return RoundCard(
            id = GenRoundCardID.generateId(name),
            quantity = parseInt(parts[Column.QUANTITY], "quantity", parts),
            name = name,
            title = parts[Column.TITLE].trim(),
            effect1Title = parts[Column.EFFECT_1_TITLE].trim(),
            effect1Text = parts[Column.EFFECT_1_TEXT].trim(),
            effect1Bg = parts[Column.EFFECT_1_BG].trim(),
            effect1TextFg = parts[Column.EFFECT_1_TEXT_FG].trim(),
            effect1Image = parseOptional(parts[Column.EFFECT_1_IMAGE]),
            effect1Icon = parseOptional(parts[Column.EFFECT_1_ICON]),
            effect2Title = parts[Column.EFFECT_2_TITLE].trim(),
            effect2Text = parts[Column.EFFECT_2_TEXT].trim(),
            effect2Bg = parts[Column.EFFECT_2_BG].trim(),
            effect2TextFg = parts[Column.EFFECT_2_TEXT_FG].trim(),
            effect2Image = parseOptional(parts[Column.EFFECT_2_IMAGE]),
            effect2Icon = parseOptional(parts[Column.EFFECT_2_ICON]),
            backImage = parseOptional(parts[Column.BACK_IMAGE])
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
