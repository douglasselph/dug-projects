package dugsolutions.leaf.simulation.v35.experiment.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.round.domain.RoundCard
import java.io.File

/** Strict CSV loader for research-only Round-card effect replacements. */
class RoundExperimentConfigLoader(
    canonicalCards: Collection<RoundCard>
) {
    private val canonicalNamesByNormalized: Map<String, String> =
        canonicalCards.map(RoundCard::name).associateBy(String::normalizedRoundId)

    init {
        require(canonicalCards.isNotEmpty()) {
            "Cannot load Round experiment overrides without canonical Round cards"
        }
        require(canonicalNamesByNormalized.size == canonicalCards.size) {
            "Canonical Round card IDs are not unique after normalization"
        }
    }

    fun load(filePath: String): RoundExperimentConfig {
        val file = File(filePath)
        require(file.isFile) { "Round experiment override file not found: $filePath" }
        return parse(file.readText(), filePath)
    }

    internal fun parse(
        content: String,
        sourceName: String = "<memory>"
    ): RoundExperimentConfig {
        if (content.isBlank()) return RoundExperimentConfig.EMPTY
        val rows = parseCsv(content)
        if (rows.isEmpty() || rows.all { row -> row.all(String::isBlank) }) {
            return RoundExperimentConfig.EMPTY
        }

        val headers = rows.first().map { it.removePrefix("\uFEFF").trim() }
        require(headers.toSet().size == headers.size) {
            "Round experiment override CSV contains duplicate column names: $sourceName"
        }
        val columns = headers.mapIndexed { index, name -> name to index }.toMap()
        val required = setOf("round_card", "effect_slot", "effect")
        val missing = required - columns.keys
        require(missing.isEmpty()) {
            "Round experiment override CSV is missing required columns $missing: $sourceName"
        }

        val overrides = mutableListOf<RoundExperimentOverride>()
        val seen = linkedMapOf<Pair<String, RoundEffectSlot>, GameEffect>()

        rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .forEachIndexed { dataIndex, row ->
                val rowNumber = dataIndex + 2
                val rawName = value(row, columns, "round_card").trim()
                require(rawName.isNotEmpty()) {
                    "Blank 'round_card' in Round experiment override file $sourceName at row $rowNumber"
                }
                val normalized = rawName.normalizedRoundId()
                val canonicalName = canonicalNamesByNormalized[normalized]
                    ?: throw IllegalArgumentException(
                        "Unknown Round Card ID '$rawName' in Round experiment override file " +
                            "$sourceName at row $rowNumber"
                    )

                val slot = parseSlot(value(row, columns, "effect_slot"), sourceName, rowNumber, rawName)
                val effect = parseEffect(value(row, columns, "effect"), sourceName, rowNumber, rawName)
                val key = normalized to slot
                val prior = seen[key]
                require(prior == null) {
                    val kind = if (prior == effect) "Duplicate" else "Duplicate conflicting"
                    "$kind Round override for '$rawName' effect slot ${slotNumber(slot)} in " +
                        "$sourceName at row $rowNumber"
                }
                seen[key] = effect
                overrides += RoundExperimentOverride(canonicalName, slot, effect)
            }

        return RoundExperimentConfig.of(overrides)
    }

    private fun parseSlot(raw: String, sourceName: String, rowNumber: Int, cardName: String): RoundEffectSlot =
        when (raw.trim()) {
            "1" -> RoundEffectSlot.FIRST
            "2" -> RoundEffectSlot.SECOND
            else -> throw IllegalArgumentException(
                "Invalid effect_slot '${raw.trim()}' for Round card '$cardName' in $sourceName " +
                    "at row $rowNumber; expected 1 or 2"
            )
        }

    private fun parseEffect(raw: String, sourceName: String, rowNumber: Int, cardName: String): GameEffect {
        val expression = raw.trim()
        require(expression.isNotEmpty()) {
            "Blank effect for Round card '$cardName' in $sourceName at row $rowNumber"
        }
        val effect = GameEffect.entries.firstOrNull { it.name == expression }
            ?: throw IllegalArgumentException(
                "Unknown GameEffect '$expression' for Round card '$cardName' in $sourceName " +
                    "at row $rowNumber; expected an exact GameEffect constant name"
            )
        require(effect != GameEffect.UNKNOWN) {
            "Invalid Round effect override 'UNKNOWN' for '$cardName' in $sourceName at row $rowNumber"
        }
        return effect
    }

    private fun slotNumber(slot: RoundEffectSlot): Int =
        if (slot == RoundEffectSlot.FIRST) 1 else 2

    private fun value(row: List<String>, columns: Map<String, Int>, name: String): String =
        row.getOrElse(columns.getValue(name)) { "" }

    private fun parseCsv(content: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < content.length) {
            val c = content[index]
            when {
                c == '"' && inQuotes && index + 1 < content.length && content[index + 1] == '"' -> {
                    field.append('"'); index++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { row += field.toString(); field.setLength(0) }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && index + 1 < content.length && content[index + 1] == '\n') index++
                    row += field.toString(); field.setLength(0)
                    rows += row; row = mutableListOf()
                }
                else -> field.append(c)
            }
            index++
        }
        require(!inQuotes) { "Unterminated quoted CSV field" }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString(); rows += row
        }
        return rows
    }
}
