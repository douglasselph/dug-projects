package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CultivationMainActionSanityTest {

    @Test
    fun `Draw Main Action reaches real draw resolver and records first action`() {
        val first = ScriptedDecisionDirector().apply {
            cultivation.thenMain(CultivationMainAction.Draw)
            cultivation.thenMain(CultivationMainAction.RoundEffect1)
            cultivation.thenDone()
        }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val randomizer = ScriptedRandomizer().rolls(4)

        cultivationHarness(randomizer, first = first, second = second).use { harness ->
            harness.revealNextRound()
            val result = harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(1, snapshot.player(1).hand.size)
            assertEquals(DieSides.D4, snapshot.player(1).hand.single().dieSides)
            assertEquals(4, snapshot.player(1).hand.single().value)
            assertEquals(1, snapshot.player(1).water)

            val p1 = result.actions.filter { it.playerId == PlayerId(1) }
            assertEquals(listOf(1, 2), p1.map { it.actionNumber })
            assertEquals(
                listOf(CultivationMainAction.Draw, CultivationMainAction.RoundEffect1),
                p1.map { it.action }
            )

            val chronicle = ChronicleQueries.mainActionsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(listOf(MainActionKind.DRAW, MainActionKind.ROUND_EFFECT_1), chronicle.map { it.action })
            randomizer.assertExhausted()
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Activate Plant Main Action executes card effect and spends the graft`() {
        val first = ScriptedDecisionDirector()
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(
                playerId = 1,
                hand = listOf(DieSpec(DieSides.D6, 1))
            )
            val grafted = harness.graftPlant(1, "Root Four More", faceUp = true)
            first.effect.thenDie { it.legalChoices.single() }
            first.cultivation.thenMain(CultivationMainAction.ActivatePlant(grafted))
            first.cultivation.thenMain(CultivationMainAction.RoundEffect1)
            first.cultivation.thenDone()

            harness.revealNextRound()
            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(5, snapshot.player(1).hand.single().value)
            assertFalse(snapshot.player(1).plants.single().faceUp)

            val effect = ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
                .single { it.effect == GameEffect.RAISE_DIE_PLUS_4 }
            assertEquals(EffectSourceKind.PLANT, effect.sourceKind)
            assertEquals("Root_05_02", effect.sourceName)

            val actions = ChronicleQueries.mainActionsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(MainActionKind.ACTIVATE_PLANT, actions.first().action)
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Round Effect 1 uses the revealed card first effect`() {
        val first = ScriptedDecisionDirector().apply {
            cultivation.thenMain(CultivationMainAction.RoundEffect1)
            cultivation.thenMain(CultivationMainAction.RoundEffect1)
            cultivation.thenDone()
        }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            harness.revealNextRound()
            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(2, snapshot.player(1).water)
            assertEquals(5, snapshot.grove.water)
            val effects = ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(listOf(GameEffect.GAIN_WATER_TOKEN, GameEffect.GAIN_WATER_TOKEN), effects.map { it.effect })
            assertTrue(effects.all { it.sourceKind == EffectSourceKind.ROUND })
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `Round Effect 2 uses the revealed card second effect and creates pending Mulch`() {
        val first = ScriptedDecisionDirector()
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            harness.setPlayerDice(
                playerId = 1,
                hand = listOf(DieSpec(DieSides.D6, 5), DieSpec(DieSides.D8, 7))
            )
            first.effect.thenDie { request -> request.legalChoices.first { it.sides == 6 } }
            first.cultivation.thenMain(CultivationMainAction.RoundEffect2)
            first.cultivation.thenMain(CultivationMainAction.RoundEffect1)
            first.cultivation.thenDone()

            harness.revealNextRound()
            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(listOf(8), snapshot.player(1).hand.map { it.sides })
            assertEquals(1, snapshot.player(1).pendingMulch)
            assertEquals(DieSides.D6, snapshot.player(1).pendingMulchTokens.single().storedDieSides)
            assertEquals(8, snapshot.grove.mulch)

            val actions = ChronicleQueries.mainActionsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(listOf(MainActionKind.ROUND_EFFECT_2, MainActionKind.ROUND_EFFECT_1), actions.map { it.action })
            val effects = ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
            assertEquals(GameEffect.MULCH_DIE_FROM_HAND, effects.first().effect)
            first.assertExhausted()
            second.assertExhausted()
        }
    }

    @Test
    fun `exactly two Main Actions are consumed and Done does not create a third`() {
        val first = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val second = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = first, second = second).use { harness ->
            harness.revealNextRound()
            val result = harness.runCultivationBuildActions()

            listOf(PlayerId(1), PlayerId(2)).forEach { id ->
                assertEquals(2, result.actions.count { it.playerId == id })
                assertEquals(listOf(1, 2), result.actions.filter { it.playerId == id }.map { it.actionNumber })
                assertEquals(2, ChronicleQueries.mainActionsFor(harness.chronicleEntries(), id).size)
            }
            assertTrue(result.supportActions.isEmpty())
            first.assertExhausted()
            second.assertExhausted()
        }
    }
}
