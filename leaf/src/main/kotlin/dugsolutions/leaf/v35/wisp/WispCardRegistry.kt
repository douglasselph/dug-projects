package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.wisp.domain.WispCard
import java.io.File

/**
 * Loads Wisp card definitions from the v35 Wisp CSV.
 *
 * The CSV "name" field is the stable card key. No generated integer IDs are used.
 */
class WispCardRegistry(
    private val gameEffectConverter: GameEffectConverter
) {

    private val cardsByName = linkedMapOf<String, WispCard>()

    fun loadFromCsv(filePath: String) {
        val file = File(filePath)
        require(file.isFile) {
            "CSV file not found at path: $filePath"
        }

        val rows = parseCsv(file.readText())
        require(rows.isNotEmpty()) {
            "CSV file is empty: $filePath"
        }

        val columns = rows.first()
            .map { it.removePrefix("\uFEFF").trim() }
            .mapIndexed { index, name -> name to index }
            .toMap()

        validateRequiredColumns(columns, filePath)

        rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .forEach { row ->
                val card = parseCard(row, columns, filePath)
                val key = card.name.normalizedName()

                require(key !in cardsByName) {
                    "Duplicate Wisp card name '${card.name}' while loading $filePath"
                }

                cardsByName[key] = card
            }
    }

    fun getCard(name: String): WispCard? =
        cardsByName[name.normalizedName()]

    fun getAllCards(): List<WispCard> =
        cardsByName.values.toList()

    fun clear() {
        cardsByName.clear()
    }

    private fun parseCard(
        row: List<String>,
        columns: Map<String, Int>,
        filePath: String
    ): WispCard {
        val name = required(row, columns, "name", filePath)
        val effectText = required(row, columns, "effect", filePath)

        val effect = gameEffectConverter(
            effectText = effectText,
            sourceName = name
        )

        require(effect != GameEffect.UNKNOWN) {
            "Unknown effect for Wisp card '$name' in $filePath: $effectText"
        }

        return WispCard(
            quantity = requiredPositiveInt(row, columns, "quantity", filePath),
            name = name,
            title = required(row, columns, "title", filePath),
            count = requiredInt(row, columns, "count", filePath),
            effect = effect,
            lineIcons = optional(row, columns, "line_icons"),
            lineIconsHeight = requiredInt(
                row,
                columns,
                "line_icons_height",
                filePath
            ),
            vpIcon = optional(row, columns, "vp_icon"),
            mainBackdrop = required(row, columns, "main_backdrop", filePath),
            playImmediately = effectText.contains("(Play immediately)", ignoreCase = true),
            battleOnly = effectText.contains("<battle/>", ignoreCase = true)
        )
    }

    private fun validateRequiredColumns(
        columns: Map<String, Int>,
        filePath: String
    ) {
        val required = setOf(
            "quantity",
            "name",
            "title",
            "count",
            "effect",
            "line_icons",
            "line_icons_height",
            "vp_icon",
            "main_backdrop"
        )

        val missing = required - columns.keys
        require(missing.isEmpty()) {
            "CSV is missing required columns $missing: $filePath"
        }
    }

    private fun required(
        row: List<String>,
        columns: Map<String, Int>,
        columnName: String,
        filePath: String
    ): String {
        val index = requireNotNull(columns[columnName]) {
            "CSV is missing required column '$columnName': $filePath"
        }

        val value = row.getOrElse(index) { "" }.trim()

        require(value.isNotEmpty()) {
            "Blank '$columnName' value in $filePath row: $row"
        }

        return value
    }

    private fun requiredInt(
        row: List<String>,
        columns: Map<String, Int>,
        columnName: String,
        filePath: String
    ): Int {
        val value = required(row, columns, columnName, filePath)
        return value.toIntOrNull()
            ?: throw IllegalArgumentException(
                "Invalid integer '$value' for '$columnName' in $filePath row: $row"
            )
    }

    private fun requiredPositiveInt(
        row: List<String>,
        columns: Map<String, Int>,
        columnName: String,
        filePath: String
    ): Int {
        val value = requiredInt(row, columns, columnName, filePath)
        require(value > 0) {
            "'$columnName' must be positive in $filePath row: $row"
        }
        return value
    }

    private fun optional(
        row: List<String>,
        columns: Map<String, Int>,
        columnName: String
    ): String? {
        val index = columns[columnName] ?: return null
        return row.getOrElse(index) { "" }
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun parseCsv(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val value = StringBuilder()
        var inQuotes = false
        var index = 0

        fun finishField() {
            row.add(value.toString())
            value.setLength(0)
        }

        fun finishRow() {
            finishField()
            rows.add(row)
            row = mutableListOf()
        }

        while (index < content.length) {
            val char = content[index]

            if (inQuotes) {
                when {
                    char == '"' &&
                        index + 1 < content.length &&
                        content[index + 1] == '"' -> {
                        value.append('"')
                        index++
                    }

                    char == '"' -> inQuotes = false
                    else -> value.append(char)
                }
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    ',' -> finishField()
                    '\n' -> finishRow()

                    '\r' -> {
                        if (index + 1 < content.length &&
                            content[index + 1] == '\n'
                        ) {
                            index++
                        }
                        finishRow()
                    }

                    else -> value.append(char)
                }
            }

            index++
        }

        require(!inQuotes) {
            "CSV ended inside a quoted field"
        }

        if (value.isNotEmpty() || row.isNotEmpty()) {
            finishRow()
        }

        return rows
    }

    private fun String.normalizedName(): String =
        trim().lowercase()
}
