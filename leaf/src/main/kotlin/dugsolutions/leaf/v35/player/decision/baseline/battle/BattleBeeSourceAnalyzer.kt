package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.math.max

/**
 * Current-Battle denial value from stealing one Bee from a specific opponent.
 *
 * The actor-side result of Bee-loved Bloom is source-invariant: every legal source
 * gives the actor one Bee and the same round-wide Bee value. The only immediate
 * Battle distinction between opponent sources is therefore the Support move that
 * the chosen opponent loses. This helper asks how harmful that opponent's best
 * still-legal Bee placement could be to the actor if the Bee were not stolen.
 *
 * Done opponents cannot take another Support action and therefore contribute no
 * immediate Battle denial value. No future-round conservation is modeled here.
 */
class BattleBeeSourceAnalyzer(
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer()
) {
    operator fun invoke(
        context: DecisionContext,
        opponentId: PlayerId
    ): Double {
        val battle = context.battle ?: return 0.0
        if (battle.isDone(opponentId)) return 0.0

        val opponent = context.opponents.firstOrNull { it.id == opponentId } ?: return 0.0
        if (opponent.board.bees <= 0) return 0.0
        val beeValue = opponent.board.beeValue

        return battle.rows.mapNotNull { row ->
            if (row.closed) return@mapNotNull null
            val actor = row.forPlayer(context.self.id) ?: return@mapNotNull null
            val target = row.forPlayer(opponentId) ?: return@mapNotNull null
            if (actor.withdrawn || target.withdrawn) return@mapNotNull null

            val before = rowAssessor(context, row.row)
            if (!before.available) return@mapNotNull null

            val projectedRow = row.copy(
                players = row.players.map { player ->
                    if (player.playerId != opponentId) {
                        player
                    } else {
                        player.copy(
                            critters = player.critters + Critter.BEE,
                            critterTotal = player.critterTotal + beeValue,
                            total = player.total + beeValue
                        )
                    }
                }
            )
            val projectedBattle = battle.copy(
                rows = battle.rows.map { if (it.row == row.row) projectedRow else it }
            )
            val after = rowAssessor(context, row.row, projectedBattle)
            val analysis = actionAnalyzer(
                context = context,
                candidate = BattleActionRealization(
                    realization = row.row,
                    mode = BattleAnalysisMode.DETERMINISTIC,
                    rowChanges = listOf(BattleActionRowChange.from(before, after))
                )
            )

            max(0.0, -analysis.tacticalValue)
        }.maxOrNull() ?: 0.0
    }
}
