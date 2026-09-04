package dugsolutions.leaf.integration.v35.sanity.setup

import dugsolutions.leaf.integration.v35.support.ChronicleAssertions
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntegrationHarnessSmokeTest {

    @Test
    fun `harness builds and executes through real v35 production graph`() {
        val harness = IntegrationGameHarness(
            scenario = GameScenario(
                numPlayers = 2,
                roundSetup = GameRoundSetup.Ordered(
                    cultivationRounds = 1,
                    battleRounds = 0
                ),
                seed = 12345L
            )
        )

        try {
            val before = harness.snapshot()

            assertEquals(2, before.players.size)
            assertEquals(9, before.grove.plantStacks.size)
            assertEquals(3, before.player(1).supply.count { it.sides == 4 })
            assertEquals(3, before.player(1).supply.count { it.sides == 6 })
            assertEquals(9, before.grove.graftBed.getValue(DieSides.D6))
            assertTrue(harness.catalog.allPlants.isNotEmpty())
            assertTrue(harness.catalog.allWisps.isNotEmpty())
            assertTrue(harness.catalog.allRounds.isNotEmpty())
            assertNotNull(harness.roundCoordinator)
            assertNotNull(harness.gameRunner)

            val execution = harness.runNextRound()
            assertNotNull(execution)
            assertEquals(1, harness.game.roundNumber)

            val entries = harness.chronicleEntries()
            ChronicleAssertions.assertContainsMarker(
                entries,
                "ROUND_REVEALED",
                "number=1"
            )
            ChronicleAssertions.assertContainsMarker(
                entries,
                "ROUND_COMPLETED",
                "number=1"
            )
            ChronicleAssertions.assertSequenceContinuous(entries)
        } finally {
            harness.close()
        }
    }
}
