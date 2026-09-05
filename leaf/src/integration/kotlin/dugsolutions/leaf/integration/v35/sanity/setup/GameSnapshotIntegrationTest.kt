package dugsolutions.leaf.integration.v35.sanity.setup

import dugsolutions.leaf.integration.v35.support.DieSnapshot
import dugsolutions.leaf.integration.v35.support.GameScenario
import dugsolutions.leaf.integration.v35.support.IntegrationGameHarness
import dugsolutions.leaf.integration.v35.support.SnapshotAssertions
import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class GameSnapshotIntegrationTest {

    @Test
    fun `before snapshot stays unchanged when live game mutates`() {
        val randomizer = ScriptedRandomizer()
        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(1, 0),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            exactWispNames = listOf("Wisp_Award_VP"),
            randomizerFactory = { randomizer }
        )

        IntegrationGameHarness(scenario).use { harness ->
            val before = harness.snapshot()
            val livePlayer = harness.game.players.first()

            livePlayer.addVp(3)
            harness.game.grove.graftBed.take(DieSides.D6)
            randomizer.rolls(4)
            livePlayer.dice.supply.first().roll()

            val after = harness.snapshot()

            assertEquals(0, before.player(1).vp)
            assertEquals(9, before.grove.graftBed.getValue(DieSides.D6))
            assertEquals(1, before.player(1).supply.first().value)

            assertEquals(3, after.player(1).vp)
            assertEquals(8, after.grove.graftBed.getValue(DieSides.D6))
            assertEquals(4, after.player(1).supply.first().value)
            assertNotEquals(before, after)
            randomizer.assertExhausted()
        }
    }

    @Test
    fun `snapshot helpers make before and after full round assertions readable`() {
        val randomizer = ScriptedRandomizer().rolls(
            // Opening Draw: P1 then P2.
            4, 4, 4,
            3, 3, 3,
            // Two baseline Main Draws per player.
            6, 6,
            5, 5
        )

        val p1Decisions = ScriptedDecisionDirector().apply {
            buy.thenPurchase { BuyChoice.Done }
        }
        val p2Decisions = ScriptedDecisionDirector().apply {
            buy.thenPurchase { BuyChoice.Done }
        }

        val scenario = GameScenario(
            numPlayers = 2,
            roundSetup = GameRoundSetup.Ordered(1, 0),
            exactRoundNames = listOf("Resource_Compost_Mulch"),
            exactWispNames = listOf("Wisp_Award_VP"),
            decisionFactories = listOf(
                p1Decisions.singleGameFactory(),
                p2Decisions.singleGameFactory()
            ),
            randomizerFactory = { randomizer }
        )

        IntegrationGameHarness(scenario).use { harness ->
            val before = harness.snapshot()
            SnapshotAssertions.assertDiceCounts(
                before.player(1).supply,
                mapOf(DieSides.D4 to 3, DieSides.D6 to 3),
                "P1 initial Supply"
            )

            harness.runNextRound()

            val after = harness.snapshot()

            assertEquals(1, after.roundNumber)
            assertEquals("Resource_Compost_Mulch", after.currentRoundName)
            SnapshotAssertions.assertDiceCounts(
                after.player(1).supply,
                mapOf(DieSides.D6 to 1),
                "P1 remaining Supply"
            )
            SnapshotAssertions.assertDice(
                actual = after.player(1).discard,
                expected = listOf(
                    DieSnapshot(4, 4),
                    DieSnapshot(4, 4),
                    DieSnapshot(4, 4),
                    DieSnapshot(6, 6),
                    DieSnapshot(6, 6)
                ),
                label = "P1 Discard"
            )
            SnapshotAssertions.assertPlayerResources(
                after.player(1),
                bees = 0,
                worms = 0,
                water = 0,
                mulch = 0,
                wisps = 0,
                plants = 0
            )

            SnapshotAssertions.assertDiceCounts(
                after.player(2).supply,
                mapOf(DieSides.D6 to 1),
                "P2 remaining Supply"
            )
            SnapshotAssertions.assertDice(
                actual = after.player(2).discard,
                expected = listOf(
                    DieSnapshot(4, 3),
                    DieSnapshot(4, 3),
                    DieSnapshot(4, 3),
                    DieSnapshot(6, 5),
                    DieSnapshot(6, 5)
                ),
                label = "P2 Discard"
            )

            p1Decisions.assertExhausted()
            p2Decisions.assertExhausted()
            randomizer.assertExhausted()
        }
    }
}
