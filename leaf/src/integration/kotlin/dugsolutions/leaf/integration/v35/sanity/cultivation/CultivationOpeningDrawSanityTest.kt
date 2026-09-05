package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.DieSpec
import dugsolutions.leaf.integration.v35.support.setPlayerDice
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CultivationOpeningDrawSanityTest {

    @Test
    fun `opening draw uses lowest dice and resolves exact roll rewards`() {
        val randomizer = ScriptedRandomizer().rolls(4, 1, 3, 2, 4, 2)
        cultivationHarness(
            randomizer = randomizer,
            wispNames = listOf("Wisp_Award_VP", "Wisp_Award_VP2")
        ).use { harness ->
            harness.revealNextRound()

            val counts = harness.runCultivationOpeningDraw()
            val snapshot = harness.snapshot()

            assertEquals(mapOf(PlayerId(1) to 3, PlayerId(2) to 3), counts)
            assertEquals(listOf(4, 1, 3), snapshot.player(1).hand.map { it.value })
            assertEquals(listOf(2, 4, 2), snapshot.player(2).hand.map { it.value })
            assertTrue(snapshot.player(1).hand.all { it.dieSides == DieSides.D4 })
            assertTrue(snapshot.player(2).hand.all { it.dieSides == DieSides.D4 })
            assertTrue(snapshot.player(1).supply.all { it.dieSides == DieSides.D6 })
            assertTrue(snapshot.player(2).supply.all { it.dieSides == DieSides.D6 })

            assertEquals(1, snapshot.player(1).bees)
            assertEquals(0, snapshot.player(1).worms)
            assertEquals(listOf("Wisp_Award_VP", "Wisp_Award_VP2"), snapshot.player(2).wisps)
            assertEquals(8, snapshot.grove.bees)
            assertEquals(0, snapshot.grove.wispCardsRemaining)

            val entries = harness.chronicleEntries()
            val rolls = entries.filterIsInstance<GameEntry.DieRolled>()
            assertEquals(
                listOf(
                    PlayerId(1) to 4,
                    PlayerId(1) to 1,
                    PlayerId(1) to 3,
                    PlayerId(2) to 2,
                    PlayerId(2) to 4,
                    PlayerId(2) to 2
                ),
                rolls.map { it.playerId to it.value }
            )
            assertTrue(rolls.all { it.sides == 4 && it.reason == RollReason.DRAW })

            val p1Rewards = ChronicleQueries.rollRewardsFor(entries, PlayerId(1))
            assertEquals(1, p1Rewards.size)
            assertEquals(RollRewardKind.CRITTER_GAINED, p1Rewards.single().kind)
            assertEquals(Critter.BEE, p1Rewards.single().critter)

            val p2Rewards = ChronicleQueries.rollRewardsFor(entries, PlayerId(2))
            assertEquals(listOf(RollRewardKind.WISP_GAINED, RollRewardKind.WISP_GAINED), p2Rewards.map { it.kind })
            assertEquals(listOf("Wisp_Award_VP", "Wisp_Award_VP2"), p2Rewards.map { it.wispName })

            randomizer.assertExhausted()
        }
    }

    @Test
    fun `opening draw refills an empty supply from discard and still draws lowest sides first`() {
        val randomizer = ScriptedRandomizer().rolls(6, 5, 8)
        cultivationHarness(randomizer = randomizer).use { harness ->
            harness.setPlayerDice(
                playerId = 1,
                discard = listOf(
                    DieSpec(DieSides.D8),
                    DieSpec(DieSides.D6),
                    DieSpec(DieSides.D6)
                )
            )
            harness.setPlayerDice(playerId = 2)
            harness.revealNextRound()

            val counts = harness.runCultivationOpeningDraw()
            val snapshot = harness.snapshot()

            assertEquals(3, counts.getValue(PlayerId(1)))
            assertEquals(0, counts.getValue(PlayerId(2)))
            assertEquals(listOf(6, 6, 8), snapshot.player(1).hand.map { it.sides })
            assertEquals(listOf(6, 5, 8), snapshot.player(1).hand.map { it.value })
            assertTrue(snapshot.player(1).supply.isEmpty())
            assertTrue(snapshot.player(1).discard.isEmpty())

            val opening = harness.chronicleEntries().filterIsInstance<GameEntry.OpeningDrawCompleted>()
            assertEquals(listOf(PlayerId(1) to 3, PlayerId(2) to 0), opening.map { it.playerId to it.count })
            randomizer.assertExhausted()
        }
    }
}
