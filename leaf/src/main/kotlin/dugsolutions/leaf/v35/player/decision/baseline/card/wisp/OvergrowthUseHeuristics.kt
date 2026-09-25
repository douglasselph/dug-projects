package dugsolutions.leaf.v35.player.decision.baseline.card.wisp

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Small shared facts used only to decide whether Human Baseline is willing to
 * spend Overgrowth now. The actual effect-target strategy remains authoritative
 * for which die is selected after the Wisp is committed.
 */
object OvergrowthUseHeuristics {

    /**
     * Starting size of the legal target that reaches the largest available
     * two-step result. Ties prefer the larger starting die, matching the human
     * intent to save Overgrowth for the strongest persistent upgrade.
     */
    fun preferredTargetSides(context: DecisionContext): Int? {
        val candidateSides =
            if (context.phase == RoundCardType.BATTLE) {
                context.battle?.rows.orEmpty()
                    .asSequence()
                    .filterNot { it.closed }
                    .mapNotNull { it.forPlayer(context.self.id) }
                    .flatMap { it.dice.asSequence() }
                    .map { it.sides }
                    .toList()
            } else {
                context.self.board.hand.map { it.sides }
            }

        return candidateSides
            .distinct()
            .mapNotNull { sides ->
                secondAvailableLargerSide(context, sides)?.let { result ->
                    TargetResult(startingSides = sides, resultingSides = result)
                }
            }
            .maxWithOrNull(
                compareBy<TargetResult> { it.resultingSides }
                    .thenBy { it.startingSides }
            )
            ?.startingSides
    }

    private fun secondAvailableLargerSide(
        context: DecisionContext,
        startingSides: Int
    ): Int? {
        val from = DieSides.entries.firstOrNull { it.value == startingSides } ?: return null
        return DieSides.entries
            .asSequence()
            .filter { it.value > from.value }
            .filter { (context.grove.graftBed[it] ?: 0) > 0 }
            .map { it.value }
            .drop(1)
            .firstOrNull()
    }

    private data class TargetResult(
        val startingSides: Int,
        val resultingSides: Int
    )
}
