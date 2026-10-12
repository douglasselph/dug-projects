package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer

/** Stable identity, not presentation wording, for one scoring influence. */
data class ScoreInfluence(
    val id: String,
    val description: String,
    val value: ScoreValue
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_.-]*"))) { "Invalid score influence id: $id" }
        require(description.isNotBlank())
    }
}

/** Inclusive integer range; all draws use strategy randomness, never the game RNG. */
sealed class ScoreValue {
    abstract fun sample(randomizer: StrategyRandomizer): Int
    data class Fixed(val amount: Int) : ScoreValue() {
        override fun sample(randomizer: StrategyRandomizer): Int = amount
    }
    data class Range(val minimum: Int, val maximum: Int) : ScoreValue() {
        init {
            require(minimum <= maximum) { "Invalid score range $minimum..$maximum" }
            require(maximum.toLong() - minimum.toLong() < Int.MAX_VALUE.toLong()) {
                "Score range too large: $minimum..$maximum"
            }
        }
        override fun sample(randomizer: StrategyRandomizer): Int =
            if (minimum == maximum) minimum else minimum + randomizer.nextInt(maximum - minimum + 1)
    }
    companion object {
        fun parse(raw: String): ScoreValue {
            val value = raw.trim()
            val pieces = value.split("..")
            return when (pieces.size) {
                1 -> Fixed(value.toInt())
                2 -> Range(pieces[0].trim().toInt(), pieces[1].trim().toInt())
                else -> throw IllegalArgumentException("Expected integer or inclusive min..max range: $raw")
            }
        }
    }
}

/** An immutable *realized* score. A sampled range is never drawn again for diagnostics. */
data class ScoreContribution(
    val id: String,
    val description: String,
    val amount: Int,
    val configuredValue: ScoreValue
)

data class DecisionScore(
    val actionId: String,
    val contributions: List<ScoreContribution>
) {
    val total: Int get() = contributions.sumOf { it.amount }
    fun explanation(): String = buildString {
        appendLine("Action: $actionId")
        contributions.forEach { part ->
            val range = (part.configuredValue as? ScoreValue.Range)?.let {
                " [range: ${it.minimum}..${it.maximum}]"
            } ?: ""
            appendLine("  ${part.id}: ${part.amount} (${part.description})$range")
        }
        append("  Total: $total")
    }
    fun toPriorityScore(): PriorityScore = PriorityScore(
        base = 0,
        adjustments = contributions.map {
            val range = (it.configuredValue as? ScoreValue.Range)?.let { v -> " [range: ${v.minimum}..${v.maximum}]" } ?: ""
            ScoreAdjustment(it.amount, "${it.id}: ${it.description}$range")
        }
    )
}

/** Score candidates once and retain the random draws used for each influence. */
class DecisionScoreBuilder(
    private val actionId: String,
    private val randomizer: StrategyRandomizer
) {
    private val contributions = mutableListOf<ScoreContribution>()
    fun add(influence: ScoreInfluence): DecisionScoreBuilder = apply {
        require(contributions.none { it.id == influence.id }) { "Duplicate score influence: ${influence.id}" }
        contributions += ScoreContribution(
            influence.id, influence.description, influence.value.sample(randomizer), influence.value
        )
    }
    fun build(): DecisionScore = DecisionScore(actionId, contributions.toList())
}
