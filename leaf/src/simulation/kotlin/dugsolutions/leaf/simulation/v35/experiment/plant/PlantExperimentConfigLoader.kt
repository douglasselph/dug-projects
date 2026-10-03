package dugsolutions.leaf.simulation.v35.experiment.plant

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import java.io.File

/**
 * Loads research-only Plant experiment overrides from a human-readable CSV file.
 *
 * The file describes interventions only. Blank fields leave the canonical card
 * definition unchanged. The returned configuration is immutable and must be
 * explicitly supplied to a research GameConfig; loading the file itself has no
 * side effects. Cost, availability, and typed scoring rules are independent
 * experiment dimensions.
 *
 * Supported columns:
 * `card_id,cost,available,scoring,effect`
 *
 * Unquoted `#` starts a comment that runs to the end of the physical line.
 * This supports both full-line notes and inline annotations in resync files.
 * A `#` inside a quoted CSV field remains literal data.
 *
 * `effect` is optional for backwards compatibility and accepts an exact
 * [GameEffect] constant name. Blank means canonical effect.
 *
 * `scoring` accepts explicit structured expressions such as `FIXED:3`,
 * `PER_GRAFTED_VINE`, `PER_GRAFTED_FLOWER`, `PER_BUTTERFLY`, and
 * `PER_OWNED_D4`. Blank means canonical scoring.
 */
class PlantExperimentConfigLoader(
    canonicalCards: Collection<PlantCard>
) {
    private val canonicalIdsByNormalizedId: Map<String, String> =
        canonicalCards.map(PlantCard::name)
            .associateBy { it.normalizedPlantIdForLoader() }

    init {
        require(canonicalCards.isNotEmpty()) {
            "Cannot load Plant experiment overrides without canonical Plant cards"
        }
        require(canonicalIdsByNormalizedId.size == canonicalCards.size) {
            "Canonical Plant IDs are not unique after normalization"
        }
    }

    fun load(filePath: String): PlantExperimentConfig {
        val file = File(filePath)
        require(file.isFile) {
            "Plant experiment override file not found: $filePath"
        }
        return parse(file.readText(), filePath)
    }

    internal fun parse(content: String, sourceName: String = "<memory>"): PlantExperimentConfig {
        if (content.isBlank()) return PlantExperimentConfig.EMPTY

        val rows = parseCsv(stripComments(content))
        if (rows.isEmpty() || rows.all { row -> row.all(String::isBlank) }) {
            return PlantExperimentConfig.EMPTY
        }
        val contentRows = rows.dropWhile { row -> row.all(String::isBlank) }

        val headers = contentRows.first().map { it.removePrefix("\uFEFF").trim() }
        require(headers.toSet().size == headers.size) {
            "Plant experiment override CSV contains duplicate column names: $sourceName"
        }
        val columns = headers.mapIndexed { index, name -> name to index }.toMap()
        validateColumns(columns, sourceName)

        val overrides = linkedMapOf<String, PlantExperimentOverride>()
        val seenIds = linkedSetOf<String>()

        contentRows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .forEachIndexed { dataIndex, row ->
                val rowNumber = dataIndex + 2
                val rawCardId = value(row, columns, "card_id").trim()
                require(rawCardId.isNotEmpty()) {
                    "Blank 'card_id' in Plant experiment override file $sourceName at row $rowNumber"
                }

                val normalizedId = rawCardId.normalizedPlantIdForLoader()
                val canonicalId = canonicalIdsByNormalizedId[normalizedId]
                    ?: throw IllegalArgumentException(
                        "Unknown Plant ID '$rawCardId' in Plant experiment override file " +
                            "$sourceName at row $rowNumber"
                    )

                require(seenIds.add(normalizedId)) {
                    "Duplicate Plant ID '$rawCardId' in Plant experiment override file " +
                        "$sourceName at row $rowNumber"
                }

                val cost = parseCost(
                    value(row, columns, "cost"),
                    sourceName,
                    rowNumber,
                    rawCardId
                )
                val available = parseAvailable(
                    value(row, columns, "available"),
                    sourceName,
                    rowNumber,
                    rawCardId
                )
                val scoringRule = parseScoringRule(
                    value(row, columns, "scoring"),
                    sourceName,
                    rowNumber,
                    rawCardId
                )
                val effect = parseEffect(
                    value(row, columns, "effect"),
                    sourceName,
                    rowNumber,
                    rawCardId
                )

                val override = PlantExperimentOverride(
                    cost = cost,
                    available = available,
                    scoringRule = scoringRule,
                    effect = effect
                )

                if (cost != null || available != null || scoringRule != null || effect != null) {
                    overrides[canonicalId] = override
                }
            }

        return PlantExperimentConfig.of(overrides)
    }

    private fun validateColumns(columns: Map<String, Int>, sourceName: String) {
        val required = setOf("card_id", "cost", "available", "scoring")
        val missing = required - columns.keys
        require(missing.isEmpty()) {
            "Plant experiment override CSV is missing required columns $missing: $sourceName"
        }
    }

    private fun parseCost(
        raw: String,
        sourceName: String,
        rowNumber: Int,
        cardId: String
    ): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        val cost = value.toIntOrNull()
            ?: throw IllegalArgumentException(
                "Invalid cost '$value' for Plant '$cardId' in $sourceName at row $rowNumber"
            )
        require(cost >= 0) {
            "Invalid negative cost '$value' for Plant '$cardId' in $sourceName at row $rowNumber"
        }
        return cost
    }

    private fun parseAvailable(
        raw: String,
        sourceName: String,
        rowNumber: Int,
        cardId: String
    ): Boolean? =
        when (val value = raw.trim().lowercase()) {
            "" -> null
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException(
                "Invalid available Boolean '$value' for Plant '$cardId' in $sourceName " +
                    "at row $rowNumber; expected true, false, or blank"
            )
        }



    private fun parseEffect(
        raw: String,
        sourceName: String,
        rowNumber: Int,
        cardId: String
    ): GameEffect? {
        val expression = raw.trim()
        if (expression.isEmpty()) return null

        val effect = GameEffect.entries.firstOrNull { it.name == expression }
            ?: throw IllegalArgumentException(
                "Invalid effect override '$expression' for Plant '$cardId' in $sourceName " +
                    "at row $rowNumber; expected an exact GameEffect constant name"
            )
        require(effect != GameEffect.UNKNOWN) {
            "Invalid effect override 'UNKNOWN' for Plant '$cardId' in $sourceName at row $rowNumber"
        }
        return effect
    }

    private fun parseScoringRule(
        raw: String,
        sourceName: String,
        rowNumber: Int,
        cardId: String
    ) = raw.trim().takeIf { it.isNotEmpty() }?.let { expression ->
        try {
            PlantScoringRuleCodec.parse(expression)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid scoring override '$expression' for Plant '$cardId' in $sourceName " +
                    "at row $rowNumber: ${error.message}",
                error
            )
        }
    }

    private fun value(
        row: List<String>,
        columns: Map<String, Int>,
        columnName: String
    ): String =
        columns[columnName]?.let { index -> row.getOrElse(index) { "" } } ?: ""

    /**
     * Removes research-note comments before CSV parsing while preserving line
     * boundaries for useful row numbers. An unquoted `#` comments out the rest
     * of that physical line; quoted `#` characters remain data.
     */
    private fun stripComments(content: String): String {
        val result = StringBuilder(content.length)
        var inQuotes = false
        var index = 0

        while (index < content.length) {
            val char = content[index]

            if (inQuotes) {
                result.append(char)
                if (char == '"') {
                    if (index + 1 < content.length && content[index + 1] == '"') {
                        result.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                }
            } else {
                when (char) {
                    '"' -> {
                        inQuotes = true
                        result.append(char)
                    }
                    '#' -> {
                        while (index + 1 < content.length &&
                            content[index + 1] != '\n' &&
                            content[index + 1] != '\r'
                        ) {
                            index++
                        }
                    }
                    else -> result.append(char)
                }
            }

            index++
        }

        return result.toString()
    }

    /**
     * RFC-4180-style parsing matching the project's card-registry conventions:
     * quoted fields, commas/newlines inside quoted fields, escaped quotes, and
     * CRLF/LF line endings.
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
                    char == '"' && index + 1 < content.length && content[index + 1] == '"' -> {
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
                        if (index + 1 < content.length && content[index + 1] == '\n') {
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
            "Plant experiment override CSV ended inside a quoted field"
        }

        if (value.isNotEmpty() || row.isNotEmpty()) {
            finishRow()
        }

        return rows
    }
}

private fun String.normalizedPlantIdForLoader(): String {
    val normalized = trim().lowercase()
    require(normalized.isNotEmpty()) { "Plant ID cannot be blank" }
    return normalized
}
