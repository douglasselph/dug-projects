package dugsolutions.leaf.integration.v35.sanity.effect

import dugsolutions.leaf.integration.v35.sanity.battle.battleHarness
import dugsolutions.leaf.integration.v35.sanity.cultivation.cultivationHarness
import dugsolutions.leaf.integration.v35.sanity.cultivation.finishBuildWithWater
import dugsolutions.leaf.integration.v35.support.BattleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.giveNextWisp
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * One real-engine scenario per broad effect family.
 *
 * Individual effect corner cases belong in unit tests. These scenarios prove
 * the production round/action graph can actually reach representative effects
 * through real Plant/Wisp play and leave coherent Player/Grid/Chronicle state.
 */
class EffectFamilySanityTest {

    @Test
    fun `simple Set effect reaches real Plant activation and changes chosen die`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = p1, second = p2).use { harness ->
            harness.setPlayerDice(
                playerId = 1,
                hand = listOf(
                    DieSpec(DieSides.D6, 5),
                    DieSpec(DieSides.D8, 1)
                )
            )
            val rootKindred = harness.graftPlant(1, "Root Kindred", faceUp = true)

            p1.effect.thenDiePair { request ->
                request.legalChoices.first {
                    it.source.value == 5 && it.target.value == 1
                }
            }
            p1.cultivation.thenMain(CultivationMainAction.ActivatePlant(rootKindred))
            p1.cultivation.thenMain(CultivationMainAction.RoundEffect1)
            p1.cultivation.thenDone()

            harness.revealNextRound()
            harness.runCultivationBuildActions()

            assertEquals(listOf(5, 5), harness.snapshot().player(1).hand.map { it.value })
            assertEquals(
                GameEffect.SET_DIE_TO_MATCH_ANOTHER,
                plantEffectsFor(harness, 1).single().effect
            )
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `draw replacement effect discards chosen die and rolls replacement through real resolver`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val randomizer = ScriptedRandomizer().rolls(7)

        cultivationHarness(randomizer = randomizer, first = p1, second = p2).use { harness ->
            harness.setPlayerDice(
                playerId = 1,
                supply = listOf(DieSpec(DieSides.D8)),
                hand = listOf(DieSpec(DieSides.D6, 5))
            )
            harness.graftPlant(1, "Berry Important")
            val tulip = harness.graftPlant(1, "Transplant Tulip", faceUp = true)

            p1.effect.thenDie { it.legalChoices.single() }
            p1.cultivation.thenMain(CultivationMainAction.ActivatePlant(tulip))
            p1.cultivation.thenMain(CultivationMainAction.RoundEffect1)
            p1.cultivation.thenDone()

            harness.revealNextRound()
            harness.runCultivationBuildActions()
            val player = harness.snapshot().player(1)

            assertEquals(listOf(8), player.hand.map { it.sides })
            assertEquals(listOf(7), player.hand.map { it.value })
            assertEquals(listOf(6), player.discard.map { it.sides })
            assertEquals(listOf(5), player.discard.map { it.value })
            assertEquals(
                GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
                plantEffectsFor(harness, 1).single().effect
            )
            assertEquals(
                listOf(7),
                ChronicleQueries.dieRollsFor(harness.chronicleEntries(), PlayerId(1)).map { it.value }
            )
            randomizer.assertExhausted()
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `resource gain effect takes Water from Grove through Plant activation`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector().apply { finishBuildWithWater() }
        val randomizer = ScriptedRandomizer().rolls(4)

        cultivationHarness(randomizer = randomizer, first = p1, second = p2).use { harness ->
            val rootWell = harness.graftPlant(1, "Root Well", faceUp = true)

            p1.cultivation.thenMain(CultivationMainAction.ActivatePlant(rootWell))
            p1.cultivation.thenMain(CultivationMainAction.Draw)
            p1.cultivation.thenDone()

            harness.revealNextRound()
            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot()

            assertEquals(1, snapshot.player(1).water)
            assertEquals(
                GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
                plantEffectsFor(harness, 1).single().effect
            )
            randomizer.assertExhausted()
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `row manipulation effect reaches Battle Grid and reduces only chosen opposing row`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector()

        battleHarness(decisions = listOf(p1, p2)).use { harness ->
            harness.setPlayerDice(1, hand = d8Hand(6, 5, 4))
            harness.setPlayerDice(2, hand = d8Hand(5, 4, 3))
            val vine = harness.graftPlant(1, "Vine and Punishment", faceUp = true)

            p1.effect.thenStrikeRow { StrikeRow.TOP }
            p1.battle.thenFirstMain(BattleMainAction.ActivatePlant(vine))
            p1.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
            p2.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            p2.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))

            harness.revealNextRound()
            harness.runBattleRankAndPlace()
            harness.runBattleActions()
            val battle = harness.battleSnapshot()

            BattleAssertions.assertDieValues(battle, 2, StrikeRow.TOP, 2)
            BattleAssertions.assertDieValues(battle, 2, StrikeRow.MIDDLE, 4)
            BattleAssertions.assertDieValues(battle, 2, StrikeRow.BOTTOM, 3)
            assertEquals(
                GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3,
                plantEffectsFor(harness, 1).single().effect
            )
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `cross player manipulation Wisp swaps ownership and Battle location together`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector()

        battleHarness(
            wispNames = listOf("Wisp_Swap_Die"),
            decisions = listOf(p1, p2)
        ).use { harness ->
            harness.setPlayerDice(1, hand = d8Hand(6, 5, 4))
            harness.setPlayerDice(2, hand = d8Hand(5, 4, 3))
            val pollenTheft = harness.giveNextWisp(1)

            p1.effect.thenCrossPlayerSwap { request ->
                request.legalChoices.first {
                    it.ownDie.row == StrikeRow.TOP &&
                        it.opponentDie.ownerId == PlayerId(2) &&
                        it.opponentDie.row == StrikeRow.BOTTOM
                }
            }
            p1.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            p1.battle.thenTurn(
                BattleTurnAction.Support(
                    BattleSupportAction.Shared(
                        SupportAction.PlayWisp(pollenTheft)
                    )
                )
            )
            p1.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
            p2.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            p2.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))

            harness.revealNextRound()
            harness.runBattleRankAndPlace()
            harness.runBattleActions()
            val battle = harness.battleSnapshot()

            BattleAssertions.assertDieValues(battle, 1, StrikeRow.TOP, 3)
            BattleAssertions.assertDieValues(battle, 2, StrikeRow.BOTTOM, 6)
            assertTrue(harness.snapshot().player(1).wisps.isEmpty())
            val effect = ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
                .single { it.effect == GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE }
            assertEquals(EffectSourceKind.WISP, effect.sourceKind)
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `recursive Vine and Again reuses spent Root through same top level executor`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector().apply { finishBuildWithWater() }

        cultivationHarness(first = p1, second = p2).use { harness ->
            harness.setPlayerDice(1, hand = listOf(DieSpec(DieSides.D8, 2)))
            harness.graftPlant(1, "Root Four More")
            val vineAndAgain = harness.graftPlant(1, "Vine and Again", faceUp = true)

            p1.effect.thenPlantEffect { request ->
                request.legalChoices.single { it.cardName == "Root_05_02" }
            }
            p1.effect.thenDie { it.legalChoices.single() }
            p1.cultivation.thenMain(CultivationMainAction.ActivatePlant(vineAndAgain))
            p1.cultivation.thenMain(CultivationMainAction.RoundEffect1)
            p1.cultivation.thenDone()

            harness.revealNextRound()
            harness.runCultivationBuildActions()
            val snapshot = harness.snapshot().player(1)

            assertEquals(6, snapshot.hand.single().value)
            assertTrue(snapshot.plants.all { !it.faceUp })
            assertEquals(
                listOf(
                    GameEffect.RAISE_DIE_PLUS_4,
                    GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
                ),
                plantEffectsFor(harness, 1).map { it.effect }
            )
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `immediate Wisp gained from roll reward executes before opening draw continues`() {
        val p1 = ScriptedDecisionDirector().apply {
            effect.thenOptionalDie { null }
        }
        val p2 = ScriptedDecisionDirector()
        val randomizer = ScriptedRandomizer().rolls(
            2, // P1 first opening Draw: gains Wispquake
            4, // immediate Wispquake rerolls that D4
            3, 4, // P1 remaining opening dice
            4, 4, 4 // P2 opening dice
        )

        cultivationHarness(
            randomizer = randomizer,
            wispNames = listOf("Wisp_Quake"),
            first = p1,
            second = p2
        ).use { harness ->
            harness.revealNextRound()
            harness.runCultivationOpeningDraw()
            val snapshot = harness.snapshot()

            assertEquals(listOf(4, 3, 4), snapshot.player(1).hand.map { it.value })
            assertEquals(listOf(4, 4, 4), snapshot.player(2).hand.map { it.value })
            assertTrue(snapshot.player(1).wisps.isEmpty())
            assertEquals(0, snapshot.grove.wispDrawPile.size)

            val immediateReward = ChronicleQueries.rollRewardsFor(
                harness.chronicleEntries(),
                PlayerId(1)
            ).single { it.kind == RollRewardKind.WISP_PLAYED_IMMEDIATELY }
            assertEquals("Wisp_Quake", immediateReward.wispName)

            val effect = ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
                .single { it.effect == GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN }
            assertEquals(EffectSourceKind.WISP, effect.sourceKind)
            assertFalse("Wisp_Quake" in snapshot.player(1).wisps)

            randomizer.assertExhausted()
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    private fun d8Hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D8, it) }

    private fun plantEffectsFor(
        harness: dugsolutions.leaf.integration.v35.support.IntegrationGameHarness,
        playerId: Int
    ) =
        ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(playerId))
            .filter { it.sourceKind == EffectSourceKind.PLANT }
}
