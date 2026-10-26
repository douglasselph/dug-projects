package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.math.roundToInt

class BattleCollateralActivationPriorityTest {
    private val self = PlayerId(1)
    private val other = PlayerId(2)
    private val priority = BattleCollateralActivationPriority()

    @Test fun `Bloom top-level tactical value equals best legal die target projection`() {
        val ctx = context(row(StrikeRow.TOP, 3, 8), row(StrikeRow.MIDDLE, 7, 4))
        val effect = GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        val target = BattleOwnDieCollateralAnalyzer()
        val expected = listOf(0, 1).mapNotNull { index ->
            target(ctx, effect, index, index)?.tacticalValue
        }.maxOrNull()
        assertEquals(expected, priority.bestTacticalValue(ctx, effect))
        assertEquals(35 + assertNotNull(expected).roundToInt(), priority.score(ctx, effect, 50)?.total)
        assertEquals(ctx.battle, ctx.battle)
    }

    @Test fun `Gust top-level expected value equals best legal reroll target projection`() {
        val ctx = context(row(StrikeRow.TOP, 3, 8), row(StrikeRow.MIDDLE, 7, 4))
        val effect = GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        val target = BattleGustOfPetalsTargetAnalyzer()
        val expected = listOf(EffectDieChoice(0, 20, 3), EffectDieChoice(1, 20, 7))
            .mapNotNull { target(ctx, it, it.index)?.tacticalValue }.maxOrNull()
        assertEquals(expected, priority.bestTacticalValue(ctx, effect))
    }

    @Test fun `Vine Punishment top-level value equals best row projection`() {
        val ctx = context(row(StrikeRow.TOP, 3, 8), row(StrikeRow.MIDDLE, 7, 4))
        val effect = GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
        val target = BattleDeterministicStrikeRowTargetAnalyzer()
        val expected = listOf(StrikeRow.TOP, StrikeRow.MIDDLE)
            .mapNotNull { target(ctx, effect, it)?.tacticalValue }.maxOrNull()
        assertEquals(expected, priority.bestTacticalValue(ctx, effect))
    }

    @Test fun `projection does not apply outside Battle or to unrelated effects`() {
        assertNull(priority.bestTacticalValue(DecisionContext.EMPTY, GameEffect.RAISE_ANY_DIE_PLUS_1))
        assertNull(priority.bestTacticalValue(DecisionContext.EMPTY, GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW))
    }

    private fun context(vararg rows: BattleRowView): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
            id = self,
            hand = rows.flatMap { it.forPlayer(self)?.dice.orEmpty() }
                .sortedBy { it.handIndex }.map { DieView(it.handIndex, it.sides, it.value) }
        )),
        battle = BattleView(playerOrder = listOf(self, other), rows = rows.toList())
    )

    private fun row(row: StrikeRow, ownValue: Int, otherValue: Int): BattleRowView {
        val idx = if (row == StrikeRow.TOP) 0 else 1
        val actorDie = BattleDieView(idx, 20, ownValue)
        val opponentDie = BattleDieView(idx, 12, otherValue)
        return BattleRowView(row, false, listOf(
            BattlePlayerRowView(self, row, listOf(actorDie), emptyList(), ownValue, 0, ownValue, false),
            BattlePlayerRowView(other, row, listOf(opponentDie), emptyList(), otherValue, 0, otherValue, false)
        ))
    }
}
