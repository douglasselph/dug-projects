package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.VictoryPointIconParser
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import java.io.File

/**
 * Loads Plant card definitions from the v35 Plant CSV files.
 *
 * The registry intentionally uses the CSV "name" field as the stable card key.
 * It does not generate a separate integer ID.
 */
class PlantCardRegistry(
    private val gameEffectConverter: GameEffectConverter = GameEffectConverter()
) {

    private val cardsByName = linkedMapOf<String, PlantCard>()

    /**
     * Adds all cards from one CSV file to this registry.
     *
     * Call this once for the Root CSV and once for the Vine/Flower CSV,
     * or use the vararg overload.
     */
    fun loadFromCsv(filePath: String) {
        val file = File(filePath)
        require(file.isFile) {
            "CSV file not found at path: $filePath"
        }

        val rows = parseCsv(file.readText())
        require(rows.isNotEmpty()) {
            "CSV file is empty: $filePath"
        }

        val headers = rows.first()
            .map { it.removePrefix("\uFEFF").trim() }

        val columns = headers
            .mapIndexed { index, name -> name to index }
            .toMap()

        validateRequiredColumns(columns, filePath)

        rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .forEach { row ->
                val card = parseCard(row, columns, filePath)
                val key = card.name.normalizedName()

                require(key !in cardsByName) {
                    "Duplicate Plant card name '${card.name}' while loading $filePath"
                }

                cardsByName[key] = card
            }
    }

    fun loadFromCsv(vararg filePaths: String) {
        filePaths.forEach(::loadFromCsv)
    }

    fun getCard(name: String): PlantCard? =
        cardsByName[name.normalizedName()]

    fun getAllCards(): List<PlantCard> =
        cardsByName.values.toList()

    fun clear() {
        cardsByName.clear()
    }

    private fun parseCard(
        row: List<String>,
        columns: Map<String, Int>,
        filePath: String
    ): PlantCard {
        val name = required(row, columns, "name", filePath)
        val effectText = required(row, columns, "effect", filePath)
        val vpIcon = required(row, columns, "vp_icon", filePath)

        val effect = gameEffectConverter(
            effectText = effectText,
            sourceName = name
        )

        require(effect != GameEffect.UNKNOWN) {
            "Unknown effect for Plant card '$name' in $filePath: $effectText"
        }

        return PlantCard(
            quantity = requiredInt(row, columns, "quantity", filePath),
            name = name,
            title = required(row, columns, "title", filePath),
            type = parseType(required(row, columns, "type", filePath), name),
            cost = requiredInt(row, columns, "cost", filePath),
            lineIcon = optional(row, columns, "line_icon"),
            vpIcon = vpIcon,
            typeIcon = required(row, columns, "type_icon", filePath),
            fgColor = required(row, columns, "fg_color", filePath),
            textColor = required(row, columns, "text_color", filePath),
            fullImage = required(row, columns, "full_image", filePath),
            backgroundImage = requiredBackgroundImage(row, columns, filePath),
            cardBackgroundImage = required(row, columns, "bg_card_image2", filePath),
            effect = effect,
            scoringRule = parseScoringRule(
                vpIcon = vpIcon,
                cardName = name
            )
        )
    }

    private fun parseScoringRule(
        vpIcon: String,
        cardName: String
    ): PlantScoringRule =
        when (vpIcon.trim()) {
            "{{ images.victory_per_vine.url }}" ->
                PlantScoringRule.PerGraftedVine

            "{{ images.victory_per_butterfly.url }}" ->
                PlantScoringRule.PerButterfly

            else -> {
                val fixed = VictoryPointIconParser.fixedPoints(vpIcon)
                requireNotNull(fixed) {
                    "Unknown Plant VP icon '$vpIcon' for card '$cardName'"
                }
                PlantScoringRule.Fixed(fixed)
            }
        }

    private fun parseType(value: String, cardName: String): PlantType =
        when (value.trim().lowercase()) {
            "root" -> PlantType.ROOT
            "vine" -> PlantType.VINE
            "flower" -> PlantType.FLOWER
            else -> throw IllegalArgumentException(
                "Unknown Plant type '$value' for card '$cardName'"
            )
        }

    /**
     * The Root CSV currently calls this column "bg_image2", while the
     * Vine/Flower CSV calls the same concept "bg_image".
     */
    private fun requiredBackgroundImage(
        row: List<String>,
        columns: Map<String, Int>,
        filePath: String
    ): String {
        val columnName = when {
            "bg_image" in columns -> "bg_image"
            "bg_image2" in columns -> "bg_image2"
            else -> throw IllegalArgumentException(
                "CSV is missing background image column " +
                    "('bg_image' or 'bg_image2'): $filePath"
            )
        }

        return required(row, columns, columnName, filePath)
    }

    private fun validateRequiredColumns(
        columns: Map<String, Int>,
        filePath: String
    ) {
        val required = setOf(
            "quantity",
            "name",
            "title",
            "type",
            "cost",
            "line_icon",
            "vp_icon",
            "type_icon",
            "fg_color",
            "text_color",
            "full_image",
            "bg_card_image2",
            "effect"
        )

        val missing = required - columns.keys
        require(missing.isEmpty()) {
            "CSV is missing required columns $missing: $filePath"
        }

        require("bg_image" in columns || "bg_image2" in columns) {
            "CSV is missing background image column " +
                "('bg_image' or 'bg_image2'): $filePath"
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

    /**
     * RFC-4180-style parsing sufficient for the current card CSVs:
     * quoted fields, commas/newlines inside quoted fields, escaped quotes,
     * and CRLF/LF line endings.
     */
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
