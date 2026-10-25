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
        require(cards.size == prefixes.size) {
            "Cannot encode a Grove with ${cards.size} cards; expected ${prefixes.size}"
        }
        val byName = cards.associateBy { it.name.lowercase() }
        require(byName.size == cards.size) { "Cannot encode duplicate Grove Plant IDs" }
        return prefixes.mapIndexed { index, prefix ->
            val matches = byName.values.filter { it.name.startsWith(prefix, ignoreCase = true) }
            require(matches.size == 1) { "Cannot encode Grove slot $prefix from ${cards.map { it.name }}" }
            val card = matches.single()
            val expectedType = when (index) {
                in 0..2 -> dugsolutions.leaf.v35.plant.domain.PlantType.ROOT
                in 3..5 -> dugsolutions.leaf.v35.plant.domain.PlantType.VINE
                else -> dugsolutions.leaf.v35.plant.domain.PlantType.FLOWER
            }
            val expectedCost = listOf(5, 7, 9, 7, 9, 11, 11, 14, 17)[index]
            require(card.type == expectedType && card.cost == expectedCost) {
                "Cannot encode research-retiered/retyped Grove card ${card.name}: " +
                    "effective=${card.type} ${card.cost}, canonical=$expectedType $expectedCost"
            }
            val suffix = card.name.substringAfterLast('_').toIntOrNull()
            require(suffix in 1..4) { "Plant card is not a numbered Grove alternative: ${card.name}" }
            suffix.toString()
        }.joinToString("")
    }
}
