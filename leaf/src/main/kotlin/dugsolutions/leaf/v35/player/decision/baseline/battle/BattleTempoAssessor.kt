package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.abs

enum class BattleTempoPressure {
    NONE,
    WEAK,
    MODERATE,
    STRONG
}

data class BattleTempoAssessment(
    val pressure: BattleTempoPressure,
    val closeRows: List<BattleRowAssessment>,
    val relevantOpponentIds: Set<PlayerId>,
    val supportCapacity: BattleSupportCapacityAssessment?
) {
    val softSupportBonus: Int
        get() = when (pressure) {
            BattleTempoPressure.NONE -> 0
            BattleTempoPressure.WEAK -> 1
            BattleTempoPressure.MODERATE -> 2
            BattleTempoPressure.STRONG -> 3
        }
}

/**
 * Bounded acting-last assessment used only after a worthwhile contest exists.
 *
 * It uses Live Threat rather than Score Benchmark, compares against the strongest
 * relevant opponent rather than a coalition, and never makes an otherwise bad
 * Support legal or worthwhile.
 */
class BattleTempoAssessor(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val capacityAssessor: BattleSupportCapacityAssessor = BattleSupportCapacityAssessor()
) {
    operator fun invoke(
        context: DecisionContext,
        legalChoices: Collection<BattleTurnAction>,
        finalMains: Collection<BattleMainAction>
    ): BattleTempoAssessment {
        val battle = context.battle ?: return none()
        if (finalMains.isEmpty()) return none()

        val closeRows = battle.rows
            .map { rowAssessor(context, it.row) }
            .filter { row ->
                row.available &&
                    !row.securedForNow &&
                    row.liveThreatPlayerIds.isNotEmpty() &&
                    row.liveThreatMargin?.let { abs(it) <= policy.battleCloseMargin(context) } == true
            }
        if (closeRows.isEmpty()) return none()

        val visibleOpponentIds = context.opponents.map { it.id }.toSet()
        val relevant = closeRows.flatMap { it.liveThreatPlayerIds }.toSet().intersect(visibleOpponentIds)
        if (relevant.isEmpty()) return none()
        val capacity = capacityAssessor(
            context = context,
            legalChoices = legalChoices,
            relevantLiveThreatOpponentIds = relevant
        )
        val pressure = when {
            relevant.isEmpty() -> BattleTempoPressure.NONE
            capacity.own.totalMoves < capacity.strongestRelevantOpponentCapacity -> BattleTempoPressure.WEAK
            capacity.own.totalMoves == capacity.strongestRelevantOpponentCapacity -> BattleTempoPressure.MODERATE
            else -> BattleTempoPressure.STRONG
        }
        return BattleTempoAssessment(pressure, closeRows, relevant, capacity)
    }

    private fun none() = BattleTempoAssessment(
        pressure = BattleTempoPressure.NONE,
        closeRows = emptyList(),
        relevantOpponentIds = emptySet(),
        supportCapacity = null
    )
}
