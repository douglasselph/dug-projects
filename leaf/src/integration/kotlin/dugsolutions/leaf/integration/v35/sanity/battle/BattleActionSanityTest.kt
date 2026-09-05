package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.BattleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.giveCritter
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.BattleMainStage
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.game.round.battle.BattleMainActionStage
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleActionSanityTest {

    @Test
    fun `every player takes first main in battle order before any final main`() {
        val decisions = List(3) { ScriptedDecisionDirector() }
        decisions.forEach { director ->
            director.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            director.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
        }

        battleHarness(numPlayers = 3, decisions = decisions).use { harness ->
            harness.setPlayerDice(1, hand = hand(6, 5, 4))
            harness.setPlayerDice(2, hand = hand(5, 4, 3))
            harness.setPlayerDice(3, hand = hand(4, 3, 2))
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleActions()

            assertEquals(listOf(1, 2, 3), result.firstMainActions.map { it.playerId.value })
            assertTrue(result.firstMainActions.all { it.stage == BattleMainActionStage.FIRST })
            assertEquals(listOf(1, 2, 3), result.finalMainActions.map { it.playerId.value })
            assertTrue(result.finalMainActions.all { it.stage == BattleMainActionStage.FINAL })

            val mainEntries = harness.chronicleEntries()
                .filterIsInstance<dugsolutions.leaf.v35.chronicle.domain.GameEntry.MainAction>()
                .filter { it.phase == ChroniclePhase.BATTLE }
            assertEquals(
                listOf(
                    1 to BattleMainStage.FIRST,
                    2 to BattleMainStage.FIRST,
                    3 to BattleMainStage.FIRST,
                    1 to BattleMainStage.FINAL,
                    2 to BattleMainStage.FINAL,
                    3 to BattleMainStage.FINAL
                ),
                mainEntries.map { it.playerId.value to it.battleStage }
            )
            decisions.forEach { it.assertExhausted() }
        }
    }

    @Test
    fun `support passes repeat and final main removes player from later passes`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector()
        p1.battle.thenFirstMain(BattleMainAction.RoundEffect1)
        p2.battle.thenFirstMain(BattleMainAction.RoundEffect1)

        p1.battle.thenTurn(
            BattleTurnAction.Support(
                BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.TOP)
            )
        )
        p2.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
        p1.battle.thenTurn(
            BattleTurnAction.Support(
                BattleSupportAction.PlaceCritter(Critter.BEE, StrikeRow.MIDDLE)
            )
        )
        p1.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))

        battleHarness(decisions = listOf(p1, p2)).use { harness ->
            harness.setPlayerDice(1, hand = hand(6, 5, 4))
            harness.setPlayerDice(2, hand = hand(5, 4, 3))
            harness.giveCritter(1, Critter.BEE, count = 2)
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleActions()
            val battle = harness.battleSnapshot()

            assertEquals(listOf(1, 2), result.supportActions.map { it.passNumber })
            assertEquals(listOf(PlayerId(1), PlayerId(1)), result.supportActions.map { it.playerId })
            assertEquals(listOf(PlayerId(2), PlayerId(1)), result.finalMainActions.map { it.playerId })
            BattleAssertions.assertCritters(battle, 1, StrikeRow.TOP, Critter.BEE)
            BattleAssertions.assertCritters(battle, 1, StrikeRow.MIDDLE, Critter.BEE)
            BattleAssertions.assertCritters(battle, 1, StrikeRow.BOTTOM)
            assertEquals(0, harness.snapshot().player(1).bees)
            assertTrue(ChronicleQueries.supportActionsFor(harness.chronicleEntries(), PlayerId(2)).isEmpty())
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `main action draw places new die in scripted strike row`() {
        val randomizer = ScriptedRandomizer().rolls(7)
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector()
        p1.battle.thenFirstMain(BattleMainAction.Draw)
        p1.battle.thenPlacement(StrikeRow.BOTTOM)
        p1.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
        p2.battle.thenFirstMain(BattleMainAction.RoundEffect1)
        p2.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))

        battleHarness(randomizer = randomizer, decisions = listOf(p1, p2)).use { harness ->
            harness.setPlayerDice(
                1,
                supply = listOf(DieSpec(DieSides.D8)),
                hand = hand(5, 4, 3)
            )
            harness.setPlayerDice(2, hand = hand(4, 3, 2))
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            harness.runBattleActions()

            BattleAssertions.assertDieValues(harness.battleSnapshot(), 1, StrikeRow.BOTTOM, 3, 7)
            assertEquals(4, harness.snapshot().player(1).hand.size)
            assertEquals(
                listOf(7),
                ChronicleQueries.dieRollsFor(harness.chronicleEntries(), PlayerId(1)).map { it.value }
            )
            randomizer.assertExhausted()
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    private fun hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D8, it) }
}
