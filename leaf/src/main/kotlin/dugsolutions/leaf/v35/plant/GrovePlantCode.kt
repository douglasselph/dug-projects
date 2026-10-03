package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Compact human-facing code for the nine Grove Plant slots, in rules order:
 * R5 R7 R9 V7 V9 V11 F11 F14 F17.
 *
 * Digits 1..4 select that numbered card. A 0 means "leave this slot random".
 */
object GrovePlantCode {
    const val RANDOM_PATTERN: String = "000000000"

    private val prefixes = listOf(
        "Root_05_", "Root_07_", "Root_09_",
        "Vine_07_", "Vine_09_", "Vine_11_",
        "Flower_11_", "Flower_14_", "Flower_17_"
    )

    fun validate(pattern: String): String {
        val code = pattern.trim()
        require(code.length == 9 && code.all { it in '0'..'4' }) {
            "Grove code must be exactly 9 digits, each 0..4: $pattern"
        }
        return code
    }

    /** Returns explicit card-name overrides; zero positions are omitted. */
    fun overrideNames(pattern: String): List<String> =
        validate(pattern).mapIndexedNotNull { index, digit ->
            if (digit == '0') null else prefixes[index] + "0$digit"
        }

    /**
     * Replaces every zero with an independently generated 1..4 selection.
     * Useful for choosing one concrete Grove once, then reusing it for a batch.
     */
    fun generate(pattern: String = RANDOM_PATTERN, randomizer: Randomizer = Randomizer.create()): String =
        validate(pattern).map { digit ->
            if (digit == '0') ('1'.code + randomizer.nextInt(4)).toChar() else digit
        }.joinToString("")

    /**
     * Human-readable Grove identity. Canonical Groves keep the compact 9-digit
     * code; research Groves containing a retyped/retiered card fall back to
     * stable card IDs because the 1..4 canonical code cannot represent them.
     */
    fun describe(cards: List<PlantCard>): String =
        runCatching { encode(cards) }.getOrElse {
            cards.joinToString(prefix = "[", postfix = "]") { it.name }
        }

    /** Encodes a resolved nine-card Grove back into its compact reference code. */
    fun encode(cards: List<PlantCard>): String {
        val byName = cards.associateBy { it.name.lowercase() }
        return prefixes.joinToString("") { prefix ->
            val matches = byName.values.filter { it.name.startsWith(prefix, ignoreCase = true) }
            require(matches.size == 1) { "Cannot encode Grove slot $prefix from ${cards.map { it.name }}" }
            val suffix = matches.single().name.substringAfterLast('_').toIntOrNull()
            require(suffix in 1..4) { "Plant card is not a numbered Grove alternative: ${matches.single().name}" }
            suffix.toString()
        }
    }
}
