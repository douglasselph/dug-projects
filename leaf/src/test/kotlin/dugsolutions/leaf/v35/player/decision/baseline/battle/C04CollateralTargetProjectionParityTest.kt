package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** All reported activation values come from the very same per-target projector. */
class C04CollateralTargetProjectionParityTest {
    private val actor = PlayerId(1)
    private val rival = PlayerId(2)

    @Test fun `Bloom Backflip activation maximum equals its best own die projection`() {
        checkParity(GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW)
    }

    @Test fun `Gust of Petals compound reroll activation maximum equals its best die projection`() {
        checkParity(GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW)
    }

    private fun checkParity(effect: GameEffect) {
        val scorer = BattleCollateralActivationPriority()
        val context = context()
        val candidates = scorer.targetProjections(context, effect)
        assertTrue(candidates.isNotEmpty())
        assertEquals(candidates.maxOf { it.tacticalValue }, scorer.bestTacticalValue(context, effect))
        assertNotNull(scorer.score(context, effect, 90))
        for (candidate in candidates) {
            val index = assertNotNull(candidate.handIndex)
            assertEquals(listOf(candidate), scorer.targetProjections(context, effect, setOf(index)))
        }
        assertTrue(scorer.targetProjections(context, effect, emptySet()).isEmpty())
    }

    private fun context(): DecisionContext {
        val row = StrikeRow.TOP
        val own = listOf(BattleDieView(0, 20, 3), BattleDieView(1, 8, 4))
        val theirs = listOf(BattleDieView(0, 12, 8))
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                id = actor, hand = listOf(DieView(0, 20, 3), DieView(1, 8, 4))
            )),
            battle = BattleView(playerOrder = listOf(actor, rival), rows = listOf(
                BattleRowView(row, false, listOf(
                    BattlePlayerRowView(actor, row, own, emptyList(), 7, 0, 7, false),
                    BattlePlayerRowView(rival, row, theirs, emptyList(), 8, 0, 8, false)
                ))
            ))
        )
    }
}
