package dugsolutions.leaf.simulation.v35.strategy.planned

/**
 * Exact Plant-copy goals layered over ordinary Human Baseline play.
 *
 * Counts are desired final owned copies, not purchase attempts. Once the
 * requested count is owned, the plan contributes no further purchase bonus.
 */
data class CreaturePlan(
    val targets: Map<TargetCard, Int>
) {
    init {
        require(targets.isNotEmpty()) { "Creature plan must contain at least one target" }
        require(targets.values.all { it > 0 }) {
            "Creature plan target counts must all be positive: $targets"
        }
    }

    fun targetCount(card: TargetCard): Int = targets[card] ?: 0

    fun isTarget(card: TargetCard): Boolean = targetCount(card) > 0

    companion object {
        fun of(vararg targets: Pair<String, Int>): CreaturePlan =
            CreaturePlan(
                targets = targets.associate { (cardName, count) ->
                    TargetCard(cardName) to count
                }
            )
    }
}
