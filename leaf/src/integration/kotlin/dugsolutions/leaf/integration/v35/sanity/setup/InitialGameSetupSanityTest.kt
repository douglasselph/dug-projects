package dugsolutions.leaf.integration.v35.sanity.setup

import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationCatalog
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.SnapshotAssertions
import dugsolutions.leaf.v35.game.GameRoundSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class InitialGameSetupSanityTest {

    private val exactRounds = listOf(
        "Resource_Compost_Mulch",
        "Resource_Sunlight_Water"
    )

    private val exactWisps = listOf(
        "Wisp_Award_VP",
        "Wisp_Award_VP2"
    )

    @Test
    fun `real production setup creates the expected initial v35 game state`() {
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 2,
                battleRounds = 0
            ),
            exactRoundNames = exactRounds,
            exactWispNames = exactWisps
        )

        IntegrationGameHarness(scenario).use { harness ->
            val snapshot = harness.snapshot()

            SnapshotAssertions.assertReadyGame(
                snapshot = snapshot,
                expectedPlayerCount = 2,
                expectedRoundNames = exactRounds
            )
            snapshot.players.values.forEach(SnapshotAssertions::assertInitialPlayer)
            SnapshotAssertions.assertInitialGrove(
                grove = snapshot.grove,
                expectedPlantNames = IntegrationCatalog.FIRST_GAME_PLANT_NAMES,
                expectedWispNames = exactWisps
            )

            // This sanity test intentionally uses the real CSV catalogs.
            assertEquals(36, harness.catalog.allPlants.size, "v35 Plant definitions")
            assertEquals(13, harness.catalog.allWisps.size, "v35 Wisp definitions")
            assertEquals(12, harness.catalog.allRounds.size, "v35 Round definitions")
            assertEquals(exactRounds, snapshot.roundDrawPile.map { it.name })
            assertEquals(exactWisps, snapshot.grove.wispDrawPile.map { it.name })
            assertEquals(0, harness.chronicleEntries().size, "setup should not create gameplay Chronicle entries")
        }
    }

    @Test
    fun `separate harnesses own independent mutable game state`() {
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 0
            ),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            exactWispNames = listOf("Wisp_Award_VP")
        )

        IntegrationGameHarness(scenario).use { first ->
            IntegrationGameHarness(scenario).use { second ->
                val secondBefore = second.snapshot()

                first.game.players.first().addVp(4)
                first.game.grove.graftBed.take(dugsolutions.leaf.v35.random.die.DieSides.D6)

                val firstAfter = first.snapshot()
                val secondAfter = second.snapshot()

                assertEquals(4, firstAfter.player(1).vp)
                assertEquals(0, secondAfter.player(1).vp)
                assertEquals(secondBefore, secondAfter, "mutating one Game must not mutate another")
                assertNotSame(first.game, second.game)
                assertNotSame(first.game.players.first(), second.game.players.first())
                assertNotSame(first.game.players.first().decisions, second.game.players.first().decisions)
            }
        }
    }
}
