package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.BattleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleDoomSanityTest {

    @Test
    fun `doom trashes exactly two dice when two share the lowest value`() {
        battleHarness().use { harness ->
            harness.setPlayerDice(1, hand = hand(4, 3, 1))
            harness.setPlayerDice(2, hand = hand(6, 5, 1))
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleDoom()

            assertEquals(2, result.count)
            assertEquals(listOf(1), result.valuesTrashed)
            assertEquals(listOf(1, 1), result.dice.map { it.value })
            assertTrue(result.dice.all { !it.returnedToGraftBed })
            assertEquals(2, harness.snapshot().player(1).hand.size)
            assertEquals(2, harness.snapshot().player(2).hand.size)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.BOTTOM)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 2, StrikeRow.BOTTOM)

            val doom = ChronicleQueries.doom(harness.chronicleEntries()).single()
            assertEquals(2, doom.count)
            assertEquals(listOf(1), doom.valuesTrashed)
            assertEquals(2, harness.chronicleEntries().filterIsInstance<GameEntry.TrashDie>().size)
        }
    }

    @Test
    fun `doom continues through next-lowest complete group until at least two dice are trashed`() {
        battleHarness().use { harness ->
            harness.setPlayerDice(1, hand = hand(5, 2, 1))
            harness.setPlayerDice(2, hand = hand(6, 4, 2))
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleDoom()

            assertEquals(3, result.count)
            assertEquals(listOf(1, 2), result.valuesTrashed)
            assertEquals(listOf(1, 2, 2), result.dice.map { it.value }.sorted())
            assertEquals(1, harness.snapshot().player(1).hand.size)
            assertEquals(2, harness.snapshot().player(2).hand.size)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.TOP, 5)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.MIDDLE)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.BOTTOM)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 2, StrikeRow.TOP, 6)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 2, StrikeRow.MIDDLE, 4)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 2, StrikeRow.BOTTOM)
        }
    }

    private fun hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D6, it) }
}
