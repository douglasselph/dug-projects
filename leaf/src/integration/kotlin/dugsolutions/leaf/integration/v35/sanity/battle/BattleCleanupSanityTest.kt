package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.giveButterfly
import dugsolutions.leaf.integration.v35.support.giveCritter
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BattleCleanupSanityTest {

    @Test
    fun `cleanup reclaims surviving grid dice returns critters clears battle state and refreshes`() {
        battleHarness().use { harness ->
            harness.setPlayerDice(1, hand = hand(6, 5, 4))
            harness.setPlayerDice(2, hand = hand(5, 4, 3))
            harness.giveCritter(1, Critter.BEE)
            harness.graftPlant(1, "Root Four More", faceUp = false)
            harness.giveButterfly(1, Butterfly.GREEN, faceUp = false)
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()
            harness.placeBattleCritterForSetup(1, StrikeRow.TOP, Critter.BEE)

            val groveBeesBeforeCleanup = harness.snapshot().grove.critters.getValue(Critter.BEE)
            val result = harness.runBattleCleanup()
            val snapshot = harness.snapshot()

            assertEquals(6, result.totalDiscardedDice)
            assertEquals(1, result.totalReturnedCritters)
            assertEquals(listOf(PlayerId(1)), result.refreshedPlayers)
            assertTrue(snapshot.player(1).hand.isEmpty())
            assertTrue(snapshot.player(2).hand.isEmpty())
            assertEquals(listOf(6, 5, 4), snapshot.player(1).discard.map { it.value })
            assertEquals(listOf(5, 4, 3), snapshot.player(2).discard.map { it.value })
            assertEquals(groveBeesBeforeCleanup + 1, snapshot.grove.critters.getValue(Critter.BEE))
            assertTrue(snapshot.player(1).plants.single().faceUp)
            assertTrue(snapshot.player(1).butterflies.single { it.butterfly == Butterfly.GREEN }.faceUp)
            assertNull(harness.battleSnapshotOrNull())

            val cleanup = ChronicleQueries.cleanupsFor(harness.chronicleEntries(), PlayerId(1)).single()
            assertEquals(ChroniclePhase.BATTLE, cleanup.phase)
            assertEquals(3, cleanup.discardedDice)
            assertEquals(1, cleanup.returnedCritters)
            assertTrue(cleanup.refreshed)
            assertEquals(1, ChronicleQueries.refreshesFor(harness.chronicleEntries(), PlayerId(1)).size)
        }
    }

    private fun hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D6, it) }
}
