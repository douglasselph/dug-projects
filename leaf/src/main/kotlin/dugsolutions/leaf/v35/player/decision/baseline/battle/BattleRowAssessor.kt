package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Immutable Human Baseline facts about one Strike Row at one decision point.
 *
 * This deliberately separates Strike reality from future-response information:
 * [scoreBenchmarkTotal] includes every participating opponent, even a Done one,
 * while [liveThreatTotal] considers only participating opponents who can still
 * take a Battle turn. These are observations, not PriorityScores.
 *
 * [securedForNow] and [potentiallyHopeless] are strategy assessments derived
 * fresh from the supplied context. They are never persistent row state.
 *
 * See `doc/HUMAN_BASELINE_BATTLE.md`, "Row facts: Strike reality versus future
 * response".
 */
data class BattleRowAssessment(
    val row: StrikeRow,
    val available: Boolean,
    val ownTotal: Int,
    val participatingPlayerIds: List<PlayerId>,
    val highTotal: Int?,
    val highPlayerIds: List<PlayerId>,
    val winnerIds: List<PlayerId>,
    val everyoneTied: Boolean,
    val currentlyWinning: Boolean,
    val woundRisk: Boolean,
    val scoreBenchmarkPlayerIds: List<PlayerId>,
    val scoreBenchmarkTotal: Int?,
    val scoreMargin: Int?,
    val liveThreatPlayerIds: List<PlayerId>,
    val liveThreatTotal: Int?,
    val liveThreatMargin: Int?,
    val allOpponentsDone: Boolean,
    val securedForNow: Boolean,
    val potentiallyHopeless: Boolean
)

/**
 * Builds the shared multiplayer-aware row facts used by later Battle scorers.
 *
 * The assessor follows the real Strike winner semantics, including shared high
 * winners when at least one lower participant exists and the all-player-tie
 * case where nobody wins. Withdrawal removes a player from the Strike; Done
 * does not. Done only affects [BattleRowAssessment.liveThreatPlayerIds].
 *
 * A caller may supply a projected [battle] view for hypothetical analysis in
 * later checkpoints. Cross-cutting secured/hopeless thresholds always come
 * through the injected [policy].
 */
class BattleRowAssessor(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy()
) {
    operator fun invoke(
        context: DecisionContext,
        row: StrikeRow,
        battle: BattleView? = context.battle
    ): BattleRowAssessment {
        val battleView = battle ?: return unavailable(row)
        val policyContext = if (battleView === context.battle) context else context.copy(battle = battleView)
        val rowView = battleView.rows.firstOrNull { it.row == row }
            ?: return unavailable(row)
        val actorId = context.self.id
        val own = rowView.forPlayer(actorId)
            ?: return unavailable(row)

        if (rowView.closed || own.withdrawn) {
            return unavailable(row, own.total)
        }

        val participating = rowView.players.filterNot { it.withdrawn }
        val opponents = participating.filterNot { it.playerId == actorId }
        val ownTotal = own.total

        val highTotal = participating.maxOfOrNull { it.total }
        val highPlayers = highTotal?.let { high ->
            participating.filter { it.total == high }
        }.orEmpty()
        val everyoneTied =
            participating.size > 1 && highPlayers.size == participating.size
        val winners = if (everyoneTied) emptyList() else highPlayers
        val currentlyWinning = winners.any { it.playerId == actorId }

        val scoreBenchmarkTotal = opponents.maxOfOrNull { it.total }
        val scoreBenchmarkPlayers = scoreBenchmarkTotal?.let { benchmark ->
            opponents.filter { it.total == benchmark }
        }.orEmpty()
        val scoreMargin = scoreBenchmarkTotal?.let { ownTotal - it }

        val liveOpponents = opponents.filterNot { battleView.isDone(it.playerId) }
        val liveThreatTotal = liveOpponents.maxOfOrNull { it.total }
        val liveThreatPlayers = liveThreatTotal?.let { threat ->
            liveOpponents.filter { it.total == threat }
        }.orEmpty()
        val liveThreatMargin = liveThreatTotal?.let { ownTotal - it }

        val winningTotal = winners.firstOrNull()?.total
        val woundRisk =
            !currentlyWinning &&
                winningTotal != null &&
                winningTotal - ownTotal >= WOUND_MARGIN

        val securedForNow =
            currentlyWinning &&
                (liveThreatTotal == null ||
                    requireNotNull(liveThreatMargin) >= policy.battleSecuredLead(policyContext))

        val potentiallyHopeless =
            !currentlyWinning &&
                scoreBenchmarkTotal != null &&
                scoreBenchmarkTotal - ownTotal >= policy.battleHopelessDeficit(policyContext)

        return BattleRowAssessment(
            row = row,
            available = true,
            ownTotal = ownTotal,
            participatingPlayerIds = participating.map { it.playerId },
            highTotal = highTotal,
            highPlayerIds = highPlayers.map { it.playerId },
            winnerIds = winners.map { it.playerId },
            everyoneTied = everyoneTied,
            currentlyWinning = currentlyWinning,
            woundRisk = woundRisk,
            scoreBenchmarkPlayerIds = scoreBenchmarkPlayers.map { it.playerId },
            scoreBenchmarkTotal = scoreBenchmarkTotal,
            scoreMargin = scoreMargin,
            liveThreatPlayerIds = liveThreatPlayers.map { it.playerId },
            liveThreatTotal = liveThreatTotal,
            liveThreatMargin = liveThreatMargin,
            allOpponentsDone = opponents.all { battleView.isDone(it.playerId) },
            securedForNow = securedForNow,
            potentiallyHopeless = potentiallyHopeless
        )
    }

    private fun unavailable(
        row: StrikeRow,
        ownTotal: Int = 0
    ): BattleRowAssessment =
        BattleRowAssessment(
            row = row,
            available = false,
            ownTotal = ownTotal,
            participatingPlayerIds = emptyList(),
            highTotal = null,
            highPlayerIds = emptyList(),
            winnerIds = emptyList(),
            everyoneTied = false,
            currentlyWinning = false,
            woundRisk = false,
            scoreBenchmarkPlayerIds = emptyList(),
            scoreBenchmarkTotal = null,
            scoreMargin = null,
            liveThreatPlayerIds = emptyList(),
            liveThreatTotal = null,
            liveThreatMargin = null,
            allOpponentsDone = false,
            securedForNow = false,
            potentiallyHopeless = false
        )

    private companion object {
        /** Game-rule Wound margin, mirrored here until Battle rules expose it publicly. */
        const val WOUND_MARGIN = 5
    }
}
