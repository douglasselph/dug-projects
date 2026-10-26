package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The real downstream Human Baseline chooseDie must realize the activation projector's best target. */
class C04CollateralEndToEndTargetChoiceTest {
    private val actor = PlayerId(1)
    private val rival = PlayerId(2)

    @Test fun `Bloom Backflip downstream target matches complete activation projection`() {
        checkRealSelection(GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW)
    }

    @Test fun `Gust of Petals downstream target matches compound expected activation projection`() {
        checkRealSelection(GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW)
    }

    private fun checkRealSelection(effect: GameEffect) {
        val context = context()
        val options = listOf(EffectDieChoice(0, 20, 3), EffectDieChoice(1, 8, 4))
        val scorer = BattleCollateralActivationPriority()
        val projections = scorer.targetProjections(context, effect, options.map { it.index }.toSet())
        assertEquals(2, projections.size, "Both legal dice must have a complete tactical projection")
        val best = projections.maxOf { kotlin.math.round(it.tacticalValue).toInt() }
        val expected = projections.filter { kotlin.math.round(it.tacticalValue).toInt() == best }.map { it.handIndex }.toSet()
        assertTrue(expected.isNotEmpty())
        val strategy = HumanBaselineEffectStrategy()
        val chosen = strategy.chooseDie(ChooseEffectDieRequest(effect, options, context))
        assertTrue(chosen.index in expected, "Downstream selection must maximize the same complete projection")
        assertEquals(projections.maxOf { it.tacticalValue }, scorer.bestTacticalValue(context, effect))
    }

    private fun context(): DecisionContext {
        val row = StrikeRow.TOP
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                id = actor, hand = listOf(DieView(0, 20, 3), DieView(1, 8, 4))
            )),
            battle = BattleView(playerOrder = listOf(actor, rival), rows = listOf(
                BattleRowView(row, false, listOf(
                    BattlePlayerRowView(actor, row, listOf(BattleDieView(0, 20, 3), BattleDieView(1, 8, 4)), emptyList(), 7, 0, 7, false),
                    BattlePlayerRowView(rival, row, listOf(BattleDieView(0, 12, 8)), emptyList(), 8, 0, 8, false)
                ))
            ))
        )
    }
}
