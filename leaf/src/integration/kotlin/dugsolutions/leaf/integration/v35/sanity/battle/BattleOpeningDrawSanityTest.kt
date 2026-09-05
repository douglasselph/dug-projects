package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleOpeningDrawSanityTest {

    @Test
    fun `opening draw uses lowest dice exact rolls and resolves rewards`() {
        val randomizer = ScriptedRandomizer().rolls(4, 1, 3, 2, 4, 2)
        battleHarness(
            randomizer = randomizer,
            wispNames = listOf("Wisp_Award_VP", "Wisp_Award_VP2")
        ).use { harness ->
            checkNotNull(harness.revealNextRound())

            val counts = harness.runBattleOpeningDraw()
            val snapshot = harness.snapshot()

            assertEquals(mapOf(PlayerId(1) to 3, PlayerId(2) to 3), counts)
            assertEquals(listOf(4, 1, 3), snapshot.player(1).hand.map { it.value })
            assertEquals(listOf(2, 4, 2), snapshot.player(2).hand.map { it.value })
            assertTrue(snapshot.player(1).hand.all { it.dieSides == DieSides.D4 })
            assertTrue(snapshot.player(2).hand.all { it.dieSides == DieSides.D4 })
            assertEquals(3, snapshot.player(1).supply.size)
            assertEquals(3, snapshot.player(2).supply.size)
            assertTrue(snapshot.player(1).supply.all { it.dieSides == DieSides.D6 })
            assertTrue(snapshot.player(2).supply.all { it.dieSides == DieSides.D6 })

            assertEquals(1, snapshot.player(1).critters.getValue(Critter.BEE))
            assertEquals(0, snapshot.player(1).wisps.size)
            assertEquals(listOf("Wisp_Award_VP", "Wisp_Award_VP2"), snapshot.player(2).wisps)

            val entries = harness.chronicleEntries()
            assertEquals(listOf(4, 1, 3), ChronicleQueries.dieRollsFor(entries, PlayerId(1)).map { it.value })
            assertEquals(listOf(2, 4, 2), ChronicleQueries.dieRollsFor(entries, PlayerId(2)).map { it.value })
            assertEquals(
                listOf(RollRewardKind.CRITTER_GAINED),
                ChronicleQueries.rollRewardsFor(entries, PlayerId(1)).map { it.kind }
            )
            assertEquals(
                listOf(RollRewardKind.WISP_GAINED, RollRewardKind.WISP_GAINED),
                ChronicleQueries.rollRewardsFor(entries, PlayerId(2)).map { it.kind }
            )
            assertEquals(
                listOf(3, 3),
                entries.filterIsInstance<GameEntry.OpeningDrawCompleted>()
                    .filter { it.phase == ChroniclePhase.BATTLE }
                    .map { it.count }
            )
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `opening draw refills supply from discard only when needed`() {
        val randomizer = ScriptedRandomizer().rolls(4, 5, 7)
        battleHarness(randomizer = randomizer).use { harness ->
            harness.setPlayerDice(
                playerId = 1,
                supply = listOf(DieSpec(DieSides.D4)),
                discard = listOf(
                    DieSpec(DieSides.D8),
                    DieSpec(DieSides.D6)
                )
            )
            // Keep player 2 from introducing unrelated random rolls.
            harness.setPlayerDice(playerId = 2)
            checkNotNull(harness.revealNextRound())

            harness.runBattleOpeningDraw()

            val p1 = harness.snapshot().player(1)
            assertEquals(listOf(DieSides.D4, DieSides.D6, DieSides.D8), p1.hand.map { it.dieSides })
            assertEquals(listOf(4, 5, 7), p1.hand.map { it.value })
            assertTrue(p1.supply.isEmpty())
            assertTrue(p1.discard.isEmpty())
            randomizer.assertExhausted()
        }
    }
}
