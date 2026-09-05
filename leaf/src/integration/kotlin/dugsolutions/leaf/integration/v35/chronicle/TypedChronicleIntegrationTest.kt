package dugsolutions.leaf.integration.v35.chronicle

import dugsolutions.leaf.integration.v35.support.ChronicleAssertions
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.GameRoundSetup
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypedChronicleIntegrationTest {

    @Test
    fun `real cultivation round records structured Chronicle entries`() {
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 0
            ),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            seed = 54321L
        )

        IntegrationGameHarness(scenario).use { harness ->
            harness.runNextRound()

            val entries = harness.chronicleEntries()

            ChronicleAssertions.assertContains<GameEntry.RoundRevealed>(entries) {
                it.roundNumber == 1 && it.cardName == "Resource_Compost_Mulch"
            }
            ChronicleAssertions.assertContains<GameEntry.DieRolled>(entries) {
                it.playerId.value == 1
            }
            ChronicleAssertions.assertContains<GameEntry.MainAction>(entries) {
                it.phase == ChroniclePhase.CULTIVATION && it.playerId.value == 1
            }
            ChronicleAssertions.assertContains<GameEntry.BuyOrder>(entries)
            ChronicleAssertions.assertContains<GameEntry.Cleanup>(entries) {
                it.phase == ChroniclePhase.CULTIVATION && it.playerId.value == 1
            }
            ChronicleAssertions.assertContains<GameEntry.RoundCompleted>(entries) {
                it.roundNumber == 1
            }

            assertTrue(
                entries.none { it is GameEntry.Marker },
                "Production round flow should use typed Chronicle entries, not Marker"
            )
            ChronicleAssertions.assertSequenceContinuous(entries)
        }
    }
}
