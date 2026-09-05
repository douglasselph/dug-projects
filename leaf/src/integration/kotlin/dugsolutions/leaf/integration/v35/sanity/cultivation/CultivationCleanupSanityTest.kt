package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.giveButterfly
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.givePendingMulch
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CultivationCleanupSanityTest {

    @Test
    fun `Cleanup moves every remaining Hand die to Discard`() {
        cultivationHarness().use { harness ->
            harness.setPlayerDice(
                1,
                hand = listOf(
                    DieSpec(DieSides.D4, 4),
                    DieSpec(DieSides.D6, 5)
                ),
                discard = listOf(DieSpec(DieSides.D8, 7))
            )
            harness.setPlayerDice(2, hand = listOf(DieSpec(DieSides.D10, 9)))
            harness.revealNextRound()

            val result = harness.runCultivationCleanup()
            val snapshot = harness.snapshot()

            assertTrue(snapshot.player(1).hand.isEmpty())
            assertEquals(listOf(7, 4, 5), snapshot.player(1).discard.map { it.value })
            assertTrue(snapshot.player(2).hand.isEmpty())
            assertEquals(listOf(9), snapshot.player(2).discard.map { it.value })
            assertEquals(2, result.players.single { it.playerId == PlayerId(1) }.discardedDice)
            assertEquals(1, result.players.single { it.playerId == PlayerId(2) }.discardedDice)

            val cleanup = ChronicleQueries.cleanupsFor(harness.chronicleEntries(), PlayerId(1)).single()
            assertEquals(2, cleanup.discardedDice)
        }
    }

    @Test
    fun `Cleanup refreshes Plant Creature and Butterflies when every graft is face down`() {
        cultivationHarness().use { harness ->
            harness.graftPlant(1, "Root Four More", faceUp = false)
            harness.graftPlant(1, "Root Recall", faceUp = false)
            harness.giveButterfly(1, Butterfly.GREEN, faceUp = false)
            harness.revealNextRound()

            val result = harness.runCultivationCleanup()
            val snapshot = harness.snapshot()

            assertTrue(snapshot.player(1).plants.all { it.faceUp })
            assertTrue(snapshot.player(1).butterflies.single().faceUp)
            assertTrue(result.players.single { it.playerId == PlayerId(1) }.refreshed)
            assertEquals(1, ChronicleQueries.refreshesFor(harness.chronicleEntries(), PlayerId(1)).size)
        }
    }

    @Test
    fun `Cleanup does not refresh a mixed face-up face-down Creature`() {
        cultivationHarness().use { harness ->
            val first = harness.graftPlant(1, "Root Four More", faceUp = true)
            val second = harness.graftPlant(1, "Root Recall", faceUp = false)
            harness.giveButterfly(1, Butterfly.RED, faceUp = false)
            harness.revealNextRound()

            val result = harness.runCultivationCleanup()
            val snapshot = harness.snapshot()

            assertTrue(snapshot.player(1).plants.single { it.id == first.id.value }.faceUp)
            assertFalse(snapshot.player(1).plants.single { it.id == second.id.value }.faceUp)
            assertFalse(snapshot.player(1).butterflies.single().faceUp)
            assertFalse(result.players.single { it.playerId == PlayerId(1) }.refreshed)
            assertTrue(ChronicleQueries.refreshesFor(harness.chronicleEntries(), PlayerId(1)).isEmpty())
        }
    }

    @Test
    fun `Cleanup clears temporary Critter values and normalizes pending Mulch`() {
        cultivationHarness().use { harness ->
            val player = harness.game.players.first()
            player.critterValues.setForRound(Critter.BEE, 4)
            player.critterValues.setForRound(Critter.WORM, 5)
            harness.givePendingMulch(1, DieSides.D8)
            harness.revealNextRound()

            harness.runCultivationCleanup()
            val snapshot = harness.snapshot().player(1)

            assertEquals(Critter.BEE.baseValue, snapshot.beeValue)
            assertEquals(Critter.WORM.baseValue, snapshot.wormValue)
            assertEquals(0, snapshot.pendingMulch)
            assertEquals(1, snapshot.mulch)
            assertEquals(DieSides.D8, snapshot.mulchTokens.single().storedDieSides)
        }
    }

    @Test
    fun `Main Action usage is round-local and resets to action one and two next round`() {
        val first = ScriptedDecisionDirector().apply {
            repeat(2) { finishBuildWithWater() }
        }
        val second = ScriptedDecisionDirector().apply {
            repeat(2) { finishBuildWithWater() }
        }
        // With empty Hands, Buy Order is a full tie at the end of each round.
        // Script only those two D20 tie breakers per round.
        val randomizer = ScriptedRandomizer().rolls(20, 1, 20, 1)
        IntegrationGameHarness(
            GameScenario(
                numPlayers = 2,
                roundSetup = GameRoundSetup.Ordered(2, 0),
                exactRoundNames = listOf("Resource_Water_Mulch", "Resource_Water_Mulch"),
                exactWispNames = emptyList(),
                randomizerFactory = { randomizer },
                decisionFactories = listOf(first.singleGameFactory(), second.singleGameFactory())
            )
        ).use { harness ->
            // No opening dice means the only random calls are the explicitly
            // scripted Buy Order D20 tie breakers.
            harness.setPlayerDice(1)
            harness.setPlayerDice(2)

            harness.runNextRound()
            harness.runNextRound()

            val p1Actions = ChronicleQueries.mainActionsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(listOf(1, 2, 1, 2), p1Actions.map { it.actionNumber })
            val p2Actions = ChronicleQueries.mainActionsFor(harness.chronicleEntries(), PlayerId(2))
            assertEquals(listOf(1, 2, 1, 2), p2Actions.map { it.actionNumber })
            assertEquals(4, harness.snapshot().player(1).water)
            assertEquals(4, harness.snapshot().player(2).water)
            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }
}
