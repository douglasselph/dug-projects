package dugsolutions.leaf.simulation.v35.experiment.plant

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
 * `card_id,cost,available,scoring`
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

        val rows = parseCsv(content)
        if (rows.isEmpty() || rows.all { row -> row.all(String::isBlank) }) {
            return PlantExperimentConfig.EMPTY
        }

        val headers = rows.first().map { it.removePrefix("\uFEFF").trim() }
        require(headers.toSet().size == headers.size) {
            "Plant experiment override CSV contains duplicate column names: $sourceName"
        }
        val columns = headers.mapIndexed { index, name -> name to index }.toMap()
        validateColumns(columns, sourceName)

        val overrides = linkedMapOf<String, PlantExperimentOverride>()
        val seenIds = linkedSetOf<String>()

        rows.drop(1)
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

                val override = PlantExperimentOverride(
                    cost = cost,
                    available = available,
                    scoringRule = scoringRule
                )

                if (cost != null || available != null || scoringRule != null) {
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
        row.getOrElse(requireNotNull(columns[columnName])) { "" }

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
