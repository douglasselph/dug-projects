package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.BattleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleRankAndPlaceSanityTest {

    @Test
    fun `rank and place uses lexicographic hand strength and high to low rows`() {
        battleHarness(numPlayers = 3).use { harness ->
            harness.setPlayerDice(1, hand = hand(4, 3, 1))
            harness.setPlayerDice(2, hand = hand(4, 2, 2))
            harness.setPlayerDice(3, hand = hand(3, 3, 3))
            checkNotNull(harness.revealNextRound())

            val result = harness.runBattleRankAndPlace()
            val battle = harness.battleSnapshot()

            assertEquals(listOf(PlayerId(1), PlayerId(2), PlayerId(3)), result.battleOrder)
            BattleAssertions.assertOrder(battle, 1, 2, 3)
            BattleAssertions.assertDieValues(battle, 1, StrikeRow.TOP, 4)
            BattleAssertions.assertDieValues(battle, 1, StrikeRow.MIDDLE, 3)
            BattleAssertions.assertDieValues(battle, 1, StrikeRow.BOTTOM, 1)
            BattleAssertions.assertDieValues(battle, 2, StrikeRow.TOP, 4)
            BattleAssertions.assertDieValues(battle, 2, StrikeRow.MIDDLE, 2)
            BattleAssertions.assertDieValues(battle, 2, StrikeRow.BOTTOM, 2)
            BattleAssertions.assertDieValues(battle, 3, StrikeRow.TOP, 3)
            BattleAssertions.assertDieValues(battle, 3, StrikeRow.MIDDLE, 3)
            BattleAssertions.assertDieValues(battle, 3, StrikeRow.BOTTOM, 3)

            val chronicleOrder = ChronicleQueries.battleOrders(harness.chronicleEntries()).single()
            assertEquals(result.battleOrder, chronicleOrder.order)
            assertEquals(9, chronicleOrder.initialDiceCount)
        }
    }

    @Test
    fun `complete hand tie is broken by scripted d20 rolls`() {
        val randomizer = ScriptedRandomizer().rolls(7, 19, 12)
        battleHarness(numPlayers = 3, randomizer = randomizer).use { harness ->
            harness.setPlayerDice(1, hand = hand(4, 3, 2))
            harness.setPlayerDice(2, hand = hand(4, 3, 2))
            harness.setPlayerDice(3, hand = hand(4, 3, 2))
            checkNotNull(harness.revealNextRound())

            harness.runBattleRankAndPlace()

            BattleAssertions.assertOrder(harness.battleSnapshot(), 2, 3, 1)
            randomizer.assertExhausted()
        }
    }

    private fun hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D6, it) }
}
