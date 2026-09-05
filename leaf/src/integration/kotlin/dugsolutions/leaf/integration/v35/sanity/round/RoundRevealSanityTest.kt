package dugsolutions.leaf.integration.v35.sanity.round

import dugsolutions.leaf.integration.v35.support.ChronicleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.SnapshotAssertions
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoundRevealSanityTest {

    @Test
    fun `reveal step exposes the exact next Round without executing any Round actions`() {
        val randomizer = ScriptedRandomizer()
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 0
            ),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            exactWispNames = listOf("Wisp_Award_VP"),
            randomizerFactory = { randomizer }
        )

        IntegrationGameHarness(scenario).use { harness ->
            val before = harness.snapshot()

            val reveal = harness.revealNextRound()

            assertNotNull(reveal)
            val revealed = requireNotNull(reveal)
            val after = harness.snapshot()
            val entries = harness.chronicleEntries()

            assertEquals(1, revealed.roundNumber)
            assertEquals("Resource_Compost_Mulch", revealed.card.name)
            assertEquals(RoundCardType.CULTIVATION, revealed.card.type)

            SnapshotAssertions.assertCurrentRound(
                snapshot = after,
                name = "Resource_Compost_Mulch",
                type = RoundCardType.CULTIVATION,
                firstEffect = GameEffect.UPGRADE_DIE_FROM_HAND,
                secondEffect = GameEffect.MULCH_DIE_FROM_HAND
            )
            SnapshotAssertions.assertRoundDrawPile(after)
            SnapshotAssertions.assertRoundWasRevealedWithoutPlayerMutation(before, after)

            ChronicleAssertions.assertOnlyRevealRecorded(
                entries = entries,
                roundNumber = 1,
                cardName = "Resource_Compost_Mulch",
                cardType = RoundCardType.CULTIVATION
            )
            ChronicleAssertions.assertDoesNotContain<GameEntry.DieRolled>(entries)
            ChronicleAssertions.assertDoesNotContain<GameEntry.RoundCompleted>(entries)
            ChronicleAssertions.assertNoMarkers(entries)
            ChronicleAssertions.assertSequenceContinuous(entries)
            assertEquals(entries, ChronicleQueries.entriesForRound(entries, 1))

            // Proves Reveal did not silently consume any random values.
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `a revealed Round can be resumed through the same production coordinator`() {
        val randomizer = ScriptedRandomizer().rolls(
            // Opening Draw: P1 then P2.
            4, 4, 4,
            3, 3, 3,
            // Two baseline Main Draws per player.
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
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 1,
                battleRounds = 0
            ),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            exactWispNames = listOf("Wisp_Award_VP"),
            decisionFactories = listOf(
                p1.singleGameFactory(),
                p2.singleGameFactory()
            ),
            randomizerFactory = { randomizer }
        )

        IntegrationGameHarness(scenario).use { harness ->
            val reveal = harness.revealNextRound()
            assertNotNull(reveal)

            val execution = harness.executeRevealedRound()
            val entries = harness.chronicleEntries()

            assertEquals(1, execution.roundNumber)
            assertEquals("Resource_Compost_Mulch", execution.card.name)
            ChronicleAssertions.assertRoundLifecycle(
                entries = entries,
                roundNumber = 1,
                cardName = "Resource_Compost_Mulch",
                cardType = RoundCardType.CULTIVATION
            )
            assertEquals(
                "Resource_Compost_Mulch",
                ChronicleQueries.roundReveal(entries, 1)?.cardName
            )
            assertEquals(
                "Resource_Compost_Mulch",
                ChronicleQueries.roundCompletion(entries, 1)?.cardName
            )
            assertTrue(ChronicleQueries.entriesForRound(entries, 1).isNotEmpty())
            ChronicleAssertions.assertNoMarkers(entries)
            ChronicleAssertions.assertSequenceContinuous(entries)

            p1.assertExhausted()
            p2.assertExhausted()
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `harness refuses to reveal another Round while one is still pending`() {
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(
                cultivationRounds = 2,
                battleRounds = 0
            ),
            exactRoundNames = listOf(
                "Resource_Compost_Mulch",
                "Resource_Sunlight_Water"
            ),
            exactWispNames = listOf("Wisp_Award_VP")
        )

        IntegrationGameHarness(scenario).use { harness ->
            harness.revealNextRound()

            val error = assertThrows(IllegalStateException::class.java) {
                harness.revealNextRound()
            }

            assertTrue(error.message.orEmpty().contains("already been revealed"))
            assertEquals(1, harness.snapshot().roundNumber)
            SnapshotAssertions.assertRoundDrawPile(
                harness.snapshot(),
                "Resource_Sunlight_Water"
            )
        }
    }
}
