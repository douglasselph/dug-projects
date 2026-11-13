package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.BattleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.giveNextWisp
import dugsolutions.leaf.integration.v35.support.graftPlant
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleStrikeSanityTest {

    @Test
    fun `single highest player wins each strike and receives two vp`() {
        battleHarness().use { harness ->
            harness.setPlayerDice(1, hand = hand(4, 3, 2))
            harness.setPlayerDice(2, hand = hand(3, 2, 1))
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleStrikes()

            assertEquals(3, result.strikes.size)
            assertTrue(result.strikes.all { it.winnerIds == listOf(PlayerId(1)) })
            assertTrue(result.strikes.all { it.wounds.isEmpty() })
            assertTrue(result.strikes.all { it.vpPerWinner == 2 })
            assertEquals(6, harness.snapshot().player(1).vp)
            assertEquals(0, harness.snapshot().player(2).vp)
        }
    }

    @Test
    fun `tied leaders all win when another player is lower`() {
        battleHarness(numPlayers = 3).use { harness ->
            harness.setPlayerDice(1, hand = hand(6, 5, 4))
            harness.setPlayerDice(2, hand = hand(6, 5, 4))
            harness.setPlayerDice(3, hand = hand(2, 2, 2))
            checkNotNull(harness.revealNextRound())
            // Exact tie between P1/P2 needs a D20. Instead force known tie ordering.
            (harness.randomizer as dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer)
                .rolls(20, 10)
            harness.runBattleRankAndPlace()

            val result = harness.runBattleStrikes()

            assertTrue(result.strikes.all { it.winnerIds.toSet() == setOf(PlayerId(1), PlayerId(2)) })
            assertTrue(result.strikes.all { it.vpPerWinner == 2 })
            assertEquals(6, harness.snapshot().player(1).vp)
            assertEquals(6, harness.snapshot().player(2).vp)
            assertEquals(0, harness.snapshot().player(3).vp)
        }
    }

    @Test
    fun `everyone tied produces no winner no wounds and no vp`() {
        val randomizer = dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer()
            .rolls(20, 10, 5)
        battleHarness(numPlayers = 3, randomizer = randomizer).use { harness ->
            harness.setPlayerDice(1, hand = hand(4, 3, 2))
            harness.setPlayerDice(2, hand = hand(4, 3, 2))
            harness.setPlayerDice(3, hand = hand(4, 3, 2))
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleStrikes()

            assertTrue(result.strikes.all { it.winnerIds.isEmpty() })
            assertTrue(result.strikes.all { it.wounds.isEmpty() })
            assertTrue(result.strikes.all { it.vpPerWinner == 0 })
            assertEquals(listOf(0, 0, 0), harness.snapshot().players.values.map { it.vp })
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `loser exactly five behind is wounded and winner gets wound bonus vp`() {
        battleHarness().use { harness ->
            harness.setPlayerDice(1, hand = hand(8, 4, 2))
            harness.setPlayerDice(2, hand = hand(3, 3, 1))
            harness.graftPlant(2, "Root Four More", faceUp = true)
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleStrikes()
            val top = result.strikes.single { it.row == StrikeRow.TOP }

            assertEquals(listOf(PlayerId(1)), top.winnerIds)
            assertEquals(listOf(PlayerId(2)), top.woundedPlayerIds)
            assertEquals(3, top.vpPerWinner)
            assertEquals(7, harness.snapshot().player(1).vp)
            assertFalse(harness.snapshot().player(2).plants.single().faceUp)
            assertEquals(1, ChronicleQueries.woundsFor(harness.chronicleEntries(), PlayerId(2)).size)
        }
    }

    @Test
    fun `loser four behind is not wounded`() {
        battleHarness().use { harness ->
            harness.setPlayerDice(1, hand = hand(7, 4, 2))
            harness.setPlayerDice(2, hand = hand(3, 3, 1))
            harness.graftPlant(2, "Root Four More", faceUp = true)
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            val result = harness.runBattleStrikes()
            val top = result.strikes.single { it.row == StrikeRow.TOP }

            assertTrue(top.wounds.isEmpty())
            assertEquals(2, top.vpPerWinner)
            assertTrue(harness.snapshot().player(2).plants.single().faceUp)
            assertTrue(ChronicleQueries.woundsFor(harness.chronicleEntries(), PlayerId(2)).isEmpty())
        }
    }

    @Test
    fun `root and scoot withdrawal removes player from that strike only`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector()
        battleHarness(decisions = listOf(p1, p2)).use { harness ->
            harness.setPlayerDice(1, hand = hand(8, 4, 3))
            harness.setPlayerDice(2, hand = hand(3, 3, 2))
            val rootAndScoot = harness.graftPlant(1, "Root & Scoot", faceUp = true)
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            p1.effect.thenDie { request ->
                request.legalChoices.first { it.value == 8 }
            }
            p1.effect.thenStrikeRow { StrikeRow.TOP }
            p1.battle.thenFirstMain(BattleMainAction.ActivatePlant(rootAndScoot))
            p2.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            p1.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
            p2.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))

            harness.runBattleActions()
            val afterEffect = harness.battleSnapshot()
            BattleAssertions.assertWithdrawn(afterEffect, 1, StrikeRow.TOP)
            BattleAssertions.assertOpen(afterEffect, StrikeRow.TOP)
            BattleAssertions.assertDieValues(afterEffect, 1, StrikeRow.TOP)
            assertEquals(1, harness.snapshot().player(1).discard.size)

            val result = harness.runBattleStrikes()
            val top = result.strikes.single { it.row == StrikeRow.TOP }
            assertEquals(listOf(PlayerId(2)), top.totals.map { it.playerId })
            assertEquals(listOf(PlayerId(2)), top.winnerIds)
            assertTrue(result.strikes.single { it.row == StrikeRow.MIDDLE }.totals.any { it.playerId == PlayerId(1) })
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    @Test
    fun `globally closed row is omitted from normal strike resolution`() {
        val p1 = ScriptedDecisionDirector()
        val p2 = ScriptedDecisionDirector()
        battleHarness(
            wispNames = listOf("Wisps_Last_Word"),
            decisions = listOf(p1, p2)
        ).use { harness ->
            harness.setPlayerDice(1, hand = hand(5, 4, 3))
            harness.setPlayerDice(2, hand = hand(4, 3, 2))
            val wisp = harness.giveNextWisp(1)
            checkNotNull(harness.revealNextRound())
            harness.runBattleRankAndPlace()

            p1.effect.thenStrikeRow { StrikeRow.TOP }
            p1.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            p2.battle.thenFirstMain(BattleMainAction.RoundEffect1)
            p1.battle.thenTurn(
                BattleTurnAction.Support(
                    BattleSupportAction.Shared(SupportAction.PlayWisp(wisp))
                )
            )
            p2.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))
            p1.battle.thenTurn(BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1))

            harness.runBattleActions()
            val closed = harness.battleSnapshot()
            BattleAssertions.assertClosed(closed, StrikeRow.TOP)
            BattleAssertions.assertDieValues(closed, 1, StrikeRow.TOP)
            BattleAssertions.assertDieValues(closed, 2, StrikeRow.TOP)

            val normal = harness.runBattleStrikes()
            assertEquals(listOf(StrikeRow.MIDDLE, StrikeRow.BOTTOM), normal.strikes.map { it.row })

            val chronicleRows = ChronicleQueries.strikes(harness.chronicleEntries()).map(GameEntry.StrikeResolved::row)
            assertEquals(listOf(StrikeRow.TOP, StrikeRow.MIDDLE, StrikeRow.BOTTOM), chronicleRows)
            p1.assertExhausted()
            p2.assertExhausted()
        }
    }

    private fun hand(vararg values: Int): List<DieSpec> =
        values.map { DieSpec(DieSides.D10, it) }
}
