package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.player
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * R3-D real-engine seam coverage for Human Baseline Wound Resolution.
 *
 * The wound is produced by a real Strike.  The wounded player's production
 * Human Baseline strategy receives the live Battle DecisionContext from
 * BattleRound/WoundResolver and must preserve the Plant whose effect can turn
 * the visible losing TOP row into a win.
 */
class HumanBaselineWoundIntegrationTest {

    @Test
    fun `real Strike wound uses live Battle context to choose Flip victim`() {
        val wounded = ScriptedDecisionDirector(fallback = DecisionDirector.humanBaseline())
        val opponent = ScriptedDecisionDirector()

        battleHarness(decisions = listOf(wounded, opponent)).use { harness ->
            harness.setPlayerDice(1, hand = hand(4, 3, 2))
            harness.setPlayerDice(2, hand = hand(9, 3, 1))

            val vinesTheLimit = harness.graftPlant(1, "Vine_11_04", faceUp = true)
            val queensBlossom = harness.graftPlant(1, "Flower_17_04", faceUp = true)

            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleStrikes()
            val top = result.strikes.single { it.row == StrikeRow.TOP }

            assertEquals(listOf(PlayerId(2)), top.winnerIds)
            assertEquals(listOf(PlayerId(1)), top.woundedPlayerIds)

            // TOP is 4 vs 9. Vine's the Limit can set the D10 to 10 and turn
            // that visible loss into a win, while Queen's Blossom has no such
            // immediate grid realization. Human Baseline therefore preserves
            // Vine's the Limit and takes the wound on Queen's Blossom.
            assertTrue(harness.player(1).creature.get(vinesTheLimit.id)!!.isFaceUp)
            assertFalse(harness.player(1).creature.get(queensBlossom.id)!!.isFaceUp)

            wounded.assertExhausted()
            opponent.assertExhausted()
        }
    }

    private fun hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D10, it) }
}
