package dugsolutions.leaf.integration.v35.chronicle

import dugsolutions.leaf.integration.v35.support.ChronicleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChronicleQuerySanityTest {

    @Test
    fun `Chronicle helpers query one deterministic Round without parsing text`() {
        val randomizer = ScriptedRandomizer().rolls(
            4, 4, 4,
            3, 3, 3,
            6, 6,
            5, 5
        )
        val p1 = ScriptedDecisionDirector().apply {
            buy.thenPurchase { BuyChoice.Done }
        }
        val p2 = ScriptedDecisionDirector().apply {
            buy.thenPurchase { BuyChoice.Done }
        }
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(1, 0),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            exactWispNames = listOf("Wisp_Award_VP"),
            decisionFactories = listOf(
                p1.singleGameFactory(),
                p2.singleGameFactory()
            ),
            randomizerFactory = { randomizer }
        )

        IntegrationGameHarness(scenario).use { harness ->
            harness.runNextRound()
            val entries = harness.chronicleEntries()
            val roundEntries = ChronicleQueries.entriesForRound(entries, 1)

            assertTrue(roundEntries.first() is GameEntry.RoundRevealed)
            assertTrue(roundEntries.last() is GameEntry.RoundCompleted)
            assertEquals(
                listOf(4, 4, 4, 6, 6),
                ChronicleQueries.dieRollsFor(entries, PlayerId(1)).map { it.value }
            )
            assertEquals(
                listOf(3, 3, 3, 5, 5),
                ChronicleQueries.dieRollsFor(entries, PlayerId(2)).map { it.value }
            )
            assertEquals(2, ChronicleQueries.mainActionsFor(entries, PlayerId(1)).size)
            assertEquals(2, ChronicleQueries.mainActionsFor(entries, PlayerId(2)).size)

            ChronicleAssertions.assertRoundLifecycle(
                entries = entries,
                roundNumber = 1,
                cardName = "Resource_Compost_Mulch",
                cardType = RoundCardType.CULTIVATION
            )
            ChronicleAssertions.assertCount<GameEntry.RoundRevealed>(entries, 1)
            ChronicleAssertions.assertCount<GameEntry.RoundCompleted>(entries, 1)
            ChronicleAssertions.assertNoMarkers(entries)
            ChronicleAssertions.assertSequenceContinuous(entries)

            p1.assertExhausted()
            p2.assertExhausted()
            randomizer.assertExhausted()
        }
    }
}
