package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice

/**
 * Projects the complete immediate Battle result of the two Transplant Tulip
 * own-die swap-pair decision shapes.
 *
 * The optional older Tulip swap simply exchanges the two legal live dice.
 * The current Tulip effect also raises [EffectDiePairChoice.source] by +2 after
 * the swap, so pair direction matters for that effect. Legal-pair generation
 * remains owned by the effect handler; this helper only evaluates an already
 * legal pair without mutating gameplay state.
 */
class BattleOwnDieSwapPairAnalyzer(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer =
        BattleOwnTotalChangeAnalyzer(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        effect: GameEffect,
        pair: EffectDiePairChoice
    ): BattleActionAnalysis<EffectDiePairChoice>? {
        val battle = context.battle ?: return null
        val actorId = context.self.id
        val sourceRow = battle.rows.firstOrNull { row ->
            row.forPlayer(actorId)?.dice?.any { it.handIndex == pair.source.index } == true
        }?.row ?: return null
        val targetRow = battle.rows.firstOrNull { row ->
            row.forPlayer(actorId)?.dice?.any { it.handIndex == pair.target.index } == true
        }?.row ?: return null
        if (sourceRow == targetRow) return null

        val raisedSourceValue = when (effect) {
            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE ->
                pair.source.value

            GameEffect.DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE ->
                pair.source.value + DieValueHeuristics.actualRaiseGain(
                    pair.source.sides,
                    pair.source.value,
                    2
                )

            else -> return null
        }

        val changes = linkedMapOf(
            sourceRow to (pair.target.value - pair.source.value).toDouble(),
            targetRow to (raisedSourceValue - pair.target.value).toDouble()
        )

        return ownTotalChangeAnalyzer(
            context = context,
            realization = pair,
            changesByRow = changes,
            mode = BattleAnalysisMode.DETERMINISTIC
        )
    }
}
