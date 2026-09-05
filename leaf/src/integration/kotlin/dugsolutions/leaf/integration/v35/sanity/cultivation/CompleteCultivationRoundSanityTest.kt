package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.ChronicleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteCultivationRoundSanityTest {

    @Test
    fun `complete deterministic Cultivation round leaves exact Player Grove and Chronicle state`() {
        val first = ScriptedDecisionDirector().apply {
            cultivation.thenMain(CultivationMainAction.Draw)
            cultivation.thenMain(CultivationMainAction.RoundEffect1)
            cultivation.thenDone()
            effect.thenDie { request -> request.legalChoices.first { it.value == 3 } }
            buy.thenPurchasePlant("Root_05_02")
            buy.thenPayment { request ->
                BuyPayment(dice = listOf(request.availableDice.single { it.sides == 6 && it.value == 6 }))
            }
            buy.thenDone()
        }
        val second = ScriptedDecisionDirector().apply {
            cultivation.thenMain(CultivationMainAction.RoundEffect2)
            cultivation.thenMain(CultivationMainAction.RoundEffect2)
            cultivation.thenDone()
            buy.thenDone()
        }
        val randomizer = ScriptedRandomizer().rolls(
            4, 4, 3, // P1 opening D4s
            3, 3, 3, // P2 opening D4s
            6        // P1 Draw Main Action -> D6
        )

        cultivationHarness(
            randomizer = randomizer,
            roundName = "Resource_Sunlight_Water",
            first = first,
            second = second
        ).use { harness ->
            val before = harness.snapshot()
            harness.runNextRound()
            val after = harness.snapshot()

            assertEquals(0, before.roundNumber)
            assertEquals(1, after.roundNumber)
            assertEquals("Resource_Sunlight_Water", after.currentRound?.name)

            val p1 = after.player(1)
            assertEquals(2, p1.supply.size)
            assertTrue(p1.supply.all { it.dieSides == DieSides.D6 })
            assertTrue(p1.hand.isEmpty())
            assertEquals(listOf(4, 4, 4, 6), p1.discard.map { it.value }.sorted())
            assertEquals(listOf("Root_05_02"), p1.plants.map { it.name })
            assertTrue(p1.plants.single().faceUp, "new graft should refresh at Cleanup when all grafts are face down")

            val p2 = after.player(2)
            assertEquals(3, p2.supply.size)
            assertTrue(p2.supply.all { it.dieSides == DieSides.D6 })
            assertTrue(p2.hand.isEmpty())
            assertEquals(listOf(3, 3, 3), p2.discard.map { it.value })
            assertEquals(2, p2.water)

            assertEquals(5, after.grove.plantStacks.single { it.name == "Root_05_02" }.remaining)
            assertEquals(7, after.grove.water)

            val entries = harness.chronicleEntries()
            ChronicleAssertions.assertRoundLifecycle(
                entries,
                roundNumber = 1,
                cardName = "Resource_Sunlight_Water",
                cardType = RoundCardType.CULTIVATION
            )
            ChronicleAssertions.assertNoMarkers(entries)
            ChronicleAssertions.assertSequenceContinuous(entries)

            assertEquals(
                listOf(MainActionKind.DRAW, MainActionKind.ROUND_EFFECT_1),
                ChronicleQueries.mainActionsFor(entries, PlayerId(1)).map { it.action }
            )
            assertEquals(
                listOf(MainActionKind.ROUND_EFFECT_2, MainActionKind.ROUND_EFFECT_2),
                ChronicleQueries.mainActionsFor(entries, PlayerId(2)).map { it.action }
            )
            assertTrue(ChronicleQueries.effectsFor(entries, PlayerId(1)).any { it.effect == GameEffect.RAISE_DIE_PLUS_3 })
            assertEquals(1, ChronicleQueries.purchasesFor(entries, PlayerId(1)).size)
            assertTrue(entries.any { it is GameEntry.Graft && it.playerId == PlayerId(1) })
            assertTrue(entries.any { it is GameEntry.Refresh && it.playerId == PlayerId(1) })
            assertEquals(1, ChronicleQueries.cleanupsFor(entries, PlayerId(1)).size)
            assertEquals(1, ChronicleQueries.cleanupsFor(entries, PlayerId(2)).size)

            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }
}
