package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.giveButterfly
import dugsolutions.leaf.integration.v35.support.giveCritter
import dugsolutions.leaf.integration.v35.support.giveNextWisp
import dugsolutions.leaf.integration.v35.support.giveStoredMulch
import dugsolutions.leaf.integration.v35.support.giveWater
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.player
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CultivationSupportSanityTest {

    @Test
    fun `Water reroll spends Water rerolls the chosen Hand die and records support`() {
        val first = scriptedSupportPlayer { it is SupportAction.UseWaterReroll }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val randomizer = ScriptedRandomizer().rolls(4)

        cultivationHarness(randomizer, first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 5)))
            harness.giveWater(1)
            harness.revealNextRound()

            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(4, snapshot.player(1).hand.single().value)
            // Water was spent, returned to the Grove, then two Water round effects were used.
            assertEquals(2, snapshot.player(1).water)
            assertEquals(5, snapshot.grove.water)

            val rerolls = ChronicleQueries.dieRollsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(1, rerolls.size)
            assertEquals(RollReason.ROLL, rerolls.single().reason)
            assertEquals(SupportActionKind.WATER_REROLL, supportKinds(harness).single())
            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Water refresh turns face-down Plant and Butterfly face up`() {
        val first = scriptedSupportPlayer { it == SupportAction.UseWaterRefresh }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            val plant = harness.graftPlant(1, "Root Four More", faceUp = false)
            harness.giveButterfly(1, Butterfly.GREEN, faceUp = false)
            harness.giveWater(1)
            harness.revealNextRound()

            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertTrue(snapshot.player(1).plants.single { it.id == plant.id.value }.faceUp)
            assertTrue(snapshot.player(1).butterflies.single { it.butterfly == Butterfly.GREEN }.faceUp)
            assertEquals(SupportActionKind.WATER_REFRESH, supportKinds(harness).single())
            assertEquals(1, ChronicleQueries.refreshesFor(harness.chronicleEntries(), PlayerId(1)).size)
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `stored Mulch rolls its stored die into Hand and returns token to Grove`() {
        val first = scriptedSupportPlayer { it is SupportAction.UseMulch }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val randomizer = ScriptedRandomizer().rolls(7)

        cultivationHarness(randomizer, first = first, second = second).use { harness ->
            harness.giveStoredMulch(1, DieSides.D8)
            harness.revealNextRound()

            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertTrue(snapshot.player(1).hand.any { it.dieSides == DieSides.D8 && it.value == 7 })
            assertEquals(0, snapshot.player(1).mulch)
            assertEquals(9, snapshot.grove.mulch)
            assertEquals(SupportActionKind.MULCH, supportKinds(harness).single())
            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Worm flip spends Worm and flips selected grafted Plant`() {
        val first = ScriptedDecisionDirector()
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            val plant = harness.graftPlant(1, "Root Four More", faceUp = true)
            harness.giveCritter(1, Critter.WORM)
            first.cultivation.thenSupport {
                it is SupportAction.UseWormFlip && it.cardId == plant.id
            }
            first.finishBuildWithWater()
            harness.revealNextRound()

            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertFalse(snapshot.player(1).plants.single().faceUp)
            assertEquals(0, snapshot.player(1).worms)
            assertEquals(9, snapshot.grove.worms)
            assertEquals(SupportActionKind.WORM_FLIP, supportKinds(harness).single())
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Butterfly reroll uses scripted keep choice and flips Butterfly face down`() {
        val first = scriptedSupportPlayer { it is SupportAction.UseButterfly }.apply {
            support.thenButterflyRoll { ButterflyRollChoice.REROLLED }
        }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val randomizer = ScriptedRandomizer().rolls(6)

        cultivationHarness(randomizer, first = first, second = second).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D6, 3)))
            harness.giveButterfly(1, Butterfly.PURPLE)
            harness.revealNextRound()

            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(6, snapshot.player(1).hand.single().value)
            assertFalse(snapshot.player(1).butterflies.single().faceUp)
            assertEquals(SupportActionKind.BUTTERFLY, supportKinds(harness).single())
            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Wisp support plays real Wisp effect then removes Wisp from hand`() {
        val first = scriptedSupportPlayer { it is SupportAction.PlayWisp }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(
            first = first,
            second = second,
            wispNames = listOf("Wisp_Award_VP")
        ).use { harness ->
            val card = harness.giveNextWisp(1)
            assertEquals("Wisp_Award_VP", card.name)
            harness.revealNextRound()

            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(1, snapshot.player(1).vp)
            assertTrue(snapshot.player(1).wisps.isEmpty())
            assertEquals(SupportActionKind.WISP, supportKinds(harness).single())
            assertTrue(
                ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
                    .any { it.effect == GameEffect.GAIN_ONE_VP && it.sourceName == "Wisp_Award_VP" }
            )
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    private fun scriptedSupportPlayer(
        predicate: (SupportAction) -> Boolean
    ): ScriptedDecisionDirector =
        ScriptedDecisionDirector().apply {
            cultivation.thenSupport(predicate)
            finishBuildWithWater()
        }

    private fun supportKinds(harness: dugsolutions.leaf.integration.v35.support.IntegrationGameHarness): List<SupportActionKind> =
        ChronicleQueries.supportActionsFor(harness.chronicleEntries(), PlayerId(1)).map { it.action }
}
