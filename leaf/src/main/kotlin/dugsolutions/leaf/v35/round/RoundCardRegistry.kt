package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import java.io.File

/**
 * Loads Round card definitions from the v35 Turn_Cards CSV.
 *
 * Each CSV row contains two independent game effects. Both are converted
 * through the shared GameEffectConverter.
 */
class RoundCardRegistry(
    private val gameEffectConverter: GameEffectConverter
) {

    private val cardsByName = linkedMapOf<String, RoundCard>()

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
                    "Duplicate Round card name '${card.name}' while loading $filePath"
                }

                cardsByName[key] = card
            }
    }

    fun getCard(name: String): RoundCard? =
        cardsByName[name.normalizedName()]

    fun getAllCards(): List<RoundCard> =
        cardsByName.values.toList()

    fun clear() {
        cardsByName.clear()
    }

    private fun parseCard(
        row: List<String>,
        columns: Map<String, Int>,
        filePath: String
    ): RoundCard {
        val name = required(row, columns, "name", filePath)

        return RoundCard(
            quantity = requiredPositiveInt(
                row,
                columns,
                "quantity",
                filePath
            ),
            name = name,
            type = parseType(
                required(row, columns, "title", filePath),
                name
            ),
            firstEffect = parseEffect(
                row = row,
                columns = columns,
                prefix = "effect_1",
                cardName = name,
                filePath = filePath
            ),
            secondEffect = parseEffect(
                row = row,
                columns = columns,
                prefix = "effect_2",
                cardName = name,
                filePath = filePath
            ),
            backImage = required(
                row,
                columns,
                "back_image",
                filePath
            )
        )
    }

    private fun parseEffect(
        row: List<String>,
        columns: Map<String, Int>,
        prefix: String,
        cardName: String,
        filePath: String
    ): RoundCardEffect {
        val effectText = required(
            row,
            columns,
            "${prefix}_text",
            filePath
        )

        val effect = gameEffectConverter(
            effectText = effectText,
            sourceName = "$cardName/$prefix"
        )

        require(effect != GameEffect.UNKNOWN) {
            "Unknown effect for Round card '$cardName' ($prefix) " +
                "in $filePath: $effectText"
        }

        return RoundCardEffect(
            title = required(
                row,
                columns,
                "${prefix}_title",
                filePath
            ),
            backgroundColor = required(
                row,
                columns,
                "${prefix}_bg",
                filePath
            ),
            textColor = required(
                row,
                columns,
                "${prefix}_text_fg",
                filePath
            ),
            image = required(
                row,
                columns,
                "${prefix}_image",
                filePath
            ),
            icon = optional(
                row,
                columns,
                "${prefix}_icon"
            ),
            effect = effect
        )
    }

    private fun parseType(
        value: String,
        cardName: String
    ): RoundCardType =
        when (value.trim().lowercase()) {
            "battle" -> RoundCardType.BATTLE
            "cultivation" -> RoundCardType.CULTIVATION
            else -> throw IllegalArgumentException(
                "Unknown Round card type '$value' for card '$cardName'"
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
            "effect_1_title",
            "effect_1_text",
            "effect_1_bg",
            "effect_1_text_fg",
            "effect_1_image",
            "effect_1_icon",
            "effect_2_title",
            "effect_2_text",
            "effect_2_bg",
            "effect_2_text_fg",
            "effect_2_image",
            "effect_2_icon",
            "back_image"
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
