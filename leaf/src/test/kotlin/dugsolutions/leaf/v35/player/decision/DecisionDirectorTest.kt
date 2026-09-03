package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.placement.BaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.reward.BaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.decision.wound.BaselineWoundStrategy
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecisionDirectorTest {

    @Test
    fun baseline_usesBaselineStrategies() {
        val director = DecisionDirector.baseline()

        assertTrue(
            director.reward is BaselineRewardStrategy
        )
        assertTrue(
            director.wound is BaselineWoundStrategy
        )
        assertTrue(
            director.placement is BaselineCreaturePlacementStrategy
        )
    }

    @Test
    fun copy_canReplaceOneStrategyWithoutChangingOthers() {
        val baseline = DecisionDirector.baseline()

        val customReward = object : RewardStrategy {
            override fun chooseCritter(
                request: ChooseCritterRequest
            ): Critter =
                request.legalChoices.last()
        }

        val changed = baseline.copy(
            reward = customReward
        )

        assertTrue(changed.reward === customReward)
        assertTrue(changed.wound === baseline.wound)
        assertTrue(changed.placement === baseline.placement)
    }

    @Test
    fun twoDirectors_canUseDifferentStrategiesIndependently() {
        val first = DecisionDirector.baseline()

        val customReward = object : RewardStrategy {
            override fun chooseCritter(
                request: ChooseCritterRequest
            ): Critter =
                Critter.WORM
        }

        val second = DecisionDirector.baseline().copy(
            reward = customReward
        )

        val request = ChooseCritterRequest(
            legalChoices = listOf(
                Critter.BEE,
                Critter.WORM
            ),
            ownedCritters = emptyList()
        )

        assertEquals(
            Critter.BEE,
            first.reward.chooseCritter(request)
        )
        assertEquals(
            Critter.WORM,
            second.reward.chooseCritter(request)
        )
    }
}
