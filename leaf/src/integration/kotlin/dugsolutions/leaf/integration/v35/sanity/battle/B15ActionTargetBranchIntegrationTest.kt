package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.BattleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.giveButterfly
import dugsolutions.leaf.integration.v35.support.giveCritter
import dugsolutions.leaf.integration.v35.support.giveNextWisp
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B15G-1 representative real-engine coverage.
 *
 * These scenarios deliberately keep First/Final Main orchestration scripted so
 * each test reaches the seam under audit, then leave the actor's Support choice
 * and all downstream target/result choices to the production Human Baseline.
 * Rules execution, Battle state mutation, and Chronicle recording are production.
 */
class B15ActionTargetBranchIntegrationTest {

    @Test
    fun `Human Baseline Bee support selects deterministic winning row and production engine records it`() {
        val p1 = humanBaselineActor()
        val p2 = scriptedOpponent()

        battleHarness(decisions = listOf(p1, p2)).use { harness ->
            harness.setPlayerDice(1, hand = d8Hand(4, 3, 2))
            harness.setPlayerDice(2, hand = d8Hand(5, 3, 2))
            harness.giveCritter(1, Critter.BEE)

            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()
            val result = harness.runBattleActions()

            assertEquals(listOf(PlayerId(1)), result.supportActions.map { it.playerId })
            BattleAssertions.assertCritters(harness.battleSnapshot(), 1, StrikeRow.TOP, Critter.BEE)
            assertEquals(0, harness.snapshot().player(1).bees)

            val support = ChronicleQueries.supportActionsFor(harness.chronicleEntries(), PlayerId(1)).single()
            assertEquals(ChroniclePhase.BATTLE, support.phase)
            assertEquals(SupportActionKind.CRITTER_BEE, support.action)
            assertEquals(StrikeRow.TOP, support.row)
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `Human Baseline Butterfly commits target before roll then keeps actual better result through production engine`() {
        val randomizer = ScriptedRandomizer().rolls(8)
        val p1 = humanBaselineActor()
        val p2 = scriptedOpponent()

        battleHarness(randomizer = randomizer, decisions = listOf(p1, p2)).use { harness ->
            harness.setPlayerDice(1, hand = d8Hand(4, 3, 2))
            harness.setPlayerDice(2, hand = d8Hand(5, 3, 2))
            harness.giveButterfly(1, Butterfly.GREEN)

            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()
            val result = harness.runBattleActions()

            assertEquals(listOf(PlayerId(1)), result.supportActions.map { it.playerId })
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.TOP, 8)
            assertTrue(!harness.snapshot().player(1).butterflies.single { it.butterfly == Butterfly.GREEN }.faceUp)
            assertEquals(
                listOf(8),
                ChronicleQueries.dieRollsFor(harness.chronicleEntries(), PlayerId(1)).map { it.value }
            )
            val support = ChronicleQueries.supportActionsFor(harness.chronicleEntries(), PlayerId(1)).single()
            assertEquals(SupportActionKind.BUTTERFLY, support.action)
            randomizer.assertExhausted()
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `Human Baseline Pollen Theft willingness and B13 target execute the same meaningful swap`() {
        val p1 = humanBaselineActor()
        val p2 = scriptedOpponent()

        battleHarness(
            wispNames = listOf("Wisp_Swap_Die"),
            decisions = listOf(p1, p2)
        ).use { harness ->
            harness.setPlayerDice(1, hand = d8Hand(4, 3, 2))
            harness.setPlayerDice(2, hand = d8Hand(5, 3, 2))
            harness.giveNextWisp(1)

            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()
            val result = harness.runBattleActions()

            assertEquals(listOf(PlayerId(1)), result.supportActions.map { it.playerId })
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.TOP, 5)
            BattleAssertions.assertDieValues(harness.battleSnapshot(), 2, StrikeRow.TOP, 4)
            assertTrue(harness.snapshot().player(1).wisps.isEmpty())

            val support = ChronicleQueries.supportActionsFor(harness.chronicleEntries(), PlayerId(1)).single()
            assertEquals(SupportActionKind.WISP, support.action)
            val effect = ChronicleQueries.effectsFor(harness.chronicleEntries(), PlayerId(1))
                .single { it.effect == GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE }
            assertEquals(EffectSourceKind.WISP, effect.sourceKind)
            assertEquals(ChroniclePhase.BATTLE, effect.phase)
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    private fun humanBaselineActor() =
        ScriptedDecisionDirector(fallback = DecisionDirector.humanBaseline()).apply {
            battle.thenFirstMain(BattleMainAction.RoundEffect1)
        }

    private fun scriptedOpponent() =
        ScriptedDecisionDirector().apply {
            battle.thenFirstMain(BattleMainAction.RoundEffect1)
            battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
        }

    private fun d8Hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D8, it) }
}
