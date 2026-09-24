package dugsolutions.leaf.integration.v35.sanity.cultivation

import dugsolutions.leaf.integration.v35.support.decision.ScriptedDecisionDirector
import dugsolutions.leaf.integration.v35.support.giveCritter
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * R1-D real-engine seam coverage for Human Baseline Critter Reward.
 *
 * This deliberately enters through the production Cultivation opening draw:
 * RollResolver constructs the live DecisionContext, asks the player's real
 * Human Baseline RewardStrategy, validates the answer, removes the chosen
 * Critter from the Grove, and adds it to the player.
 */
class HumanBaselineCritterRewardIntegrationTest {

    @Test
    fun `opening draw reward uses live Human Baseline protected Critter policy`() {
        val mechanicalRandomizer = ScriptedRandomizer().rolls(1, 3, 3, 4, 4, 4)
        val humanBaseline = ScriptedDecisionDirector(fallback = DecisionDirector.humanBaseline())

        cultivationHarness(
            randomizer = mechanicalRandomizer,
            first = humanBaseline
        ).use { harness ->
            // At 2 Bees / 0 Worms the approved protected minimum says the
            // next legal Critter reward should be a Worm.
            harness.giveCritter(1, Critter.BEE, count = 2)
            val groveWormsBeforeReward = harness.snapshot().grove.worms
            harness.revealNextRound()

            harness.runCultivationOpeningDraw()

            val snapshot = harness.snapshot()
            assertEquals(2, snapshot.player(1).bees)
            assertEquals(1, snapshot.player(1).worms)
            assertEquals(groveWormsBeforeReward - 1, snapshot.grove.worms)

            mechanicalRandomizer.assertExhausted()
        }
    }
}
