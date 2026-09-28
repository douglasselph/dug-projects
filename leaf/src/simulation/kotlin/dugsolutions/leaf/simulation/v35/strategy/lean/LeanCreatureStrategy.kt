package dugsolutions.leaf.simulation.v35.strategy.lean

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.buy.*
import dugsolutions.leaf.v35.player.decision.cultivation.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/** Strict research strategy for the minimal-Plant/high-dice feasibility experiment. */
object LeanCreatureStrategy {
    const val VINE_9 = "Vine_09_01"
    const val VINE_11 = "Vine_11_04"
    const val FLOWER_17 = "Flower_17_04"
    val targetPlantNames = setOf(VINE_9, VINE_11, FLOWER_17)

    fun decisionFactory(): PlayerDecisionFactory = object : PlayerDecisionFactory {
        override fun create(): DecisionDirector = create(StrategyRandomizer.create())

        override fun create(strategyRandomizer: StrategyRandomizer): DecisionDirector =
            create(strategyRandomizer, DecisionReasoningSink.NONE)

        override fun create(
            strategyRandomizer: StrategyRandomizer,
            reasoningSink: DecisionReasoningSink
        ): DecisionDirector {
            val baseline = DecisionDirector.humanBaseline(strategyRandomizer, reasoningSink)
            return baseline.copy(
                buy = LeanBuyStrategy(baseline.buy),
                cultivation = LeanCultivationStrategy(baseline.cultivation)
            )
        }
    }
}

internal class LeanBuyStrategy(private val baseline: BuyStrategy) : BuyStrategy {
    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice {
        val creature = request.context.self.board.creature
        val vineCount = creature.count { it.name == LeanCreatureStrategy.VINE_9 || it.name == LeanCreatureStrategy.VINE_11 }
        val flowerCount = creature.count { it.name == LeanCreatureStrategy.FLOWER_17 }

        val wantedPlants = request.options.filterIsInstance<BuyItem.Plant>().filter { item ->
            when (item.card.name) {
                LeanCreatureStrategy.VINE_9, LeanCreatureStrategy.VINE_11 -> vineCount < 2
                LeanCreatureStrategy.FLOWER_17 -> flowerCount < 2
                else -> false
            }
        }
        if (wantedPlants.isNotEmpty()) {
            return baseline.choosePurchase(
                ChoosePurchaseRequest(wantedPlants, request.context, request.purchasesMadeThisBuy, wantedPlants)
            )
        }

        val ownedDiceCount = with(request.context.self.board) {
            supply.size + hand.size + discard.size + mulch.count { it.storedDieSides != null } +
                pendingMulch.count { it.storedDieSides != null }
        }
        val minimumPurchasedDieSides = if (ownedDiceCount >= 7) 12 else 10
        val highDice = request.options.filterIsInstance<BuyItem.Die>().filter {
            it.sides.value >= minimumPurchasedDieSides
        }
        if (highDice.isEmpty()) return BuyChoice.Done
        return baseline.choosePurchase(
            ChoosePurchaseRequest(highDice, request.context, request.purchasesMadeThisBuy, highDice)
        )
    }

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment = baseline.choosePayment(request)
}

/** Prefer Compost whenever it is a legal Main Action and any Hand die is below D12. */
internal class LeanCultivationStrategy(private val baseline: CultivationStrategy) : CultivationStrategy {
    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        if (request.mainActionsRemaining > 0 && request.context.self.board.hand.any { it.sides < 12 }) {
            val compost = request.legalChoices.firstOrNull { choice ->
                when (choice) {
                    is CultivationAction.Main -> when (choice.action) {
                        CultivationMainAction.RoundEffect1 -> request.roundCard.firstEffect.effect == GameEffect.UPGRADE_DIE_FROM_HAND
                        CultivationMainAction.RoundEffect2 -> request.roundCard.secondEffect.effect == GameEffect.UPGRADE_DIE_FROM_HAND
                        else -> false
                    }
                    else -> false
                }
            }
            if (compost != null) return compost
        }
        return baseline.chooseAction(request)
    }
}
