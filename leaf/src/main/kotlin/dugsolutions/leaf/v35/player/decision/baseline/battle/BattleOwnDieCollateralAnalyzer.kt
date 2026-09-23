package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Projects the complete same-row Battle realization for deterministic own-die
 * effects whose chosen die also changes opposing dice in that Strike Row.
 *
 * This mirrors the engine effect order without mutating state, so top-level
 * Plant valuation and downstream die targeting can share one factual analysis.
 */
class BattleOwnDieCollateralAnalyzer(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun <T> invoke(
        context: DecisionContext,
        effect: GameEffect,
        handIndex: Int,
        realization: T
    ): BattleActionAnalysis<T>? {
        val battle = context.battle ?: return null
        val actorId = context.self.id
        val rowView = battle.rows.firstOrNull { row ->
            row.forPlayer(actorId)?.dice?.any { it.handIndex == handIndex } == true
        } ?: return null
        val before = rowAssessor(context, rowView.row)
        if (!before.available) return null

        val projectedRow = when (effect) {
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW ->
                projectSappingSnapdragon(rowView, actorId, handIndex)

            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                projectBloomBackflip(rowView, actorId, handIndex)

            else -> return null
        } ?: return null

        val projectedBattle = battle.copy(
            rows = battle.rows.map { if (it.row == rowView.row) projectedRow else it }
        )
        val after = rowAssessor(context, rowView.row, projectedBattle)
        return actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = realization,
                mode = BattleAnalysisMode.DETERMINISTIC,
                rowChanges = listOf(BattleActionRowChange.from(before, after))
            )
        )
    }

    private fun projectSappingSnapdragon(
        row: BattleRowView,
        actorId: PlayerId,
        handIndex: Int
    ): BattleRowView? {
        val actor = row.forPlayer(actorId) ?: return null
        val chosen = actor.dice.firstOrNull { it.handIndex == handIndex } ?: return null

        var totalReduced = 0
        val reducedPlayers = row.players.map { player ->
            if (player.playerId == actorId) {
                player
            } else {
                var playerReduction = 0
                val dice = player.dice.map { die ->
                    val reducedValue = (die.value - 2).coerceAtLeast(1)
                    playerReduction += die.value - reducedValue
                    die.copy(value = reducedValue)
                }
                totalReduced += playerReduction
                player.withDiceAndDelta(dice, -playerReduction)
            }
        }

        val raisedValue = (chosen.value + 2 + totalReduced).coerceAtMost(chosen.sides)
        val actorDelta = raisedValue - chosen.value
        return row.copy(
            players = reducedPlayers.map { player ->
                if (player.playerId != actorId) player
                else player.withChangedDie(handIndex, raisedValue, actorDelta)
            }
        )
    }

    private fun projectBloomBackflip(
        row: BattleRowView,
        actorId: PlayerId,
        handIndex: Int
    ): BattleRowView? {
        val actor = row.forPlayer(actorId) ?: return null
        val chosen = actor.dice.firstOrNull { it.handIndex == handIndex } ?: return null
        val raisedValue = (chosen.value + 1).coerceAtMost(chosen.sides)
        val actorDelta = raisedValue - chosen.value

        return row.copy(
            players = row.players.map { player ->
                if (player.playerId == actorId) {
                    player.withChangedDie(handIndex, raisedValue, actorDelta)
                } else {
                    var playerDelta = 0
                    val dice = player.dice.map { die ->
                        val flippedValue = if (die.value > raisedValue && die.sides > 4) {
                            (die.sides + 1) - die.value
                        } else {
                            die.value
                        }
                        playerDelta += flippedValue - die.value
                        die.copy(value = flippedValue)
                    }
                    player.withDiceAndDelta(dice, playerDelta)
                }
            }
        )
    }

    private fun BattlePlayerRowView.withChangedDie(
        handIndex: Int,
        value: Int,
        delta: Int
    ): BattlePlayerRowView =
        withDiceAndDelta(
            dice = dice.map { die ->
                if (die.handIndex == handIndex) die.copy(value = value) else die
            },
            delta = delta
        )

    private fun BattlePlayerRowView.withDiceAndDelta(
        dice: List<BattleDieView>,
        delta: Int
    ): BattlePlayerRowView = copy(
        dice = dice,
        dieTotal = dieTotal + delta,
        total = total + delta
    )
}
