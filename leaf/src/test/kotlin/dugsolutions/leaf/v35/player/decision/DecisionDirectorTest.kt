package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.buy.BaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.decision.cultivation.BaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.decision.placement.BaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.effect.BaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.reward.BaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.decision.support.BaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy
import dugsolutions.leaf.v35.player.decision.wound.BaselineWoundStrategy
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecisionDirectorTest {

    @Test
    fun baseline_usesBaselineStrategies() {
        val director = DecisionDirector.baseline()

        assertTrue(director.reward is BaselineRewardStrategy)
        assertTrue(director.wound is BaselineWoundStrategy)
        assertTrue(director.placement is BaselineCreaturePlacementStrategy)
        assertTrue(director.cultivation is BaselineCultivationStrategy)
        assertTrue(director.buy is BaselineBuyStrategy)
        assertTrue(director.support is BaselineSupportStrategy)
        assertTrue(director.effect is BaselineEffectStrategy)
    }

    @Test
    fun copy_canReplaceSupportWithoutChangingOtherStrategies() {
        val baseline = DecisionDirector.baseline()
        val custom = object : SupportStrategy {
            override fun chooseButterflyRoll(
                request: ChooseButterflyRollRequest
            ) = ButterflyRollChoice.REROLLED
        }

        val changed = baseline.copy(support = custom)

        assertTrue(changed.support === custom)
        assertTrue(changed.reward === baseline.reward)
        assertTrue(changed.wound === baseline.wound)
        assertTrue(changed.placement === baseline.placement)
        assertTrue(changed.cultivation === baseline.cultivation)
        assertTrue(changed.buy === baseline.buy)
        assertTrue(changed.effect === baseline.effect)
    }

    @Test
    fun copy_canReplaceBuyWithoutChangingOtherStrategies() {
        val baseline = DecisionDirector.baseline()
        val custom = object : BuyStrategy {
            override fun choosePurchase(request: ChoosePurchaseRequest) = BuyChoice.Done
            override fun choosePayment(request: ChoosePaymentRequest) = BuyPayment()
        }

        val changed = baseline.copy(buy = custom)

        assertTrue(changed.buy === custom)
        assertTrue(changed.reward === baseline.reward)
        assertTrue(changed.wound === baseline.wound)
        assertTrue(changed.placement === baseline.placement)
        assertTrue(changed.cultivation === baseline.cultivation)
        assertTrue(changed.support === baseline.support)
        assertTrue(changed.effect === baseline.effect)
    }

    @Test
    fun copy_canReplaceCultivationWithoutChangingOtherStrategies() {
        val baseline = DecisionDirector.baseline()
        val custom = object : CultivationStrategy {
            override fun chooseAction(
                request: ChooseCultivationActionRequest
            ): CultivationAction = request.legalChoices.last()
        }

        val changed = baseline.copy(cultivation = custom)

        assertTrue(changed.cultivation === custom)
        assertTrue(changed.reward === baseline.reward)
        assertTrue(changed.wound === baseline.wound)
        assertTrue(changed.placement === baseline.placement)
        assertTrue(changed.buy === baseline.buy)
        assertTrue(changed.support === baseline.support)
        assertTrue(changed.effect === baseline.effect)
    }

    @Test
    fun copy_canReplaceEffectWithoutChangingOtherStrategies() {
        val baseline = DecisionDirector.baseline()
        val custom = object : EffectStrategy {
            override fun chooseDie(
                request: ChooseEffectDieRequest
            ): EffectDieChoice = request.legalChoices.last()
        }

        val changed = baseline.copy(effect = custom)

        assertTrue(changed.effect === custom)
        assertTrue(changed.reward === baseline.reward)
        assertTrue(changed.wound === baseline.wound)
        assertTrue(changed.placement === baseline.placement)
        assertTrue(changed.cultivation === baseline.cultivation)
        assertTrue(changed.buy === baseline.buy)
        assertTrue(changed.support === baseline.support)
    }

    @Test
    fun copy_canReplaceOneStrategyWithoutChangingOthers() {
        val baseline = DecisionDirector.baseline()

        val customReward = object : RewardStrategy {
            override fun chooseCritter(
                request: ChooseCritterRequest
            ): Critter = request.legalChoices.last()
        }

        val changed = baseline.copy(reward = customReward)

        assertTrue(changed.reward === customReward)
        assertTrue(changed.wound === baseline.wound)
        assertTrue(changed.placement === baseline.placement)
        assertTrue(changed.support === baseline.support)
        assertTrue(changed.effect === baseline.effect)
    }

    @Test
    fun twoDirectors_canUseDifferentStrategiesIndependently() {
        val first = DecisionDirector.baseline()

        val customReward = object : RewardStrategy {
            override fun chooseCritter(
                request: ChooseCritterRequest
            ): Critter = Critter.WORM
        }

        val second = DecisionDirector.baseline().copy(
            reward = customReward
        )

        val request = ChooseCritterRequest(
            legalChoices = listOf(Critter.BEE, Critter.WORM),
            ownedCritters = emptyList()
        )

        assertEquals(Critter.BEE, first.reward.chooseCritter(request))
        assertEquals(Critter.WORM, second.reward.chooseCritter(request))
    }
}
