package dugsolutions.leaf.v30.chronicle.decision

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.decision.domain.Decision

interface DecisionCostEvaluator {
    operator fun invoke(input: Decision.ChooseCritter): DecisionCount
    operator fun invoke(input: Decision.ChooseMainActionCultivation): DecisionCount
    operator fun invoke(input: Decision.ChooseMainActionBattle): DecisionCount
    operator fun invoke(input: Decision.ChooseItemsToBuy): DecisionCount
    operator fun invoke(input: Decision.ChooseCardsToRefreshWithWorms): DecisionCount
}

class DecisionCostEvaluatorBaseline : DecisionCostEvaluator {

    override fun invoke(input: Decision.ChooseCritter): DecisionCount {
        return DecisionCount(
            playerId = input.player.id,
            type = DecisionCountType.CHOOSE_CRITTER,
            count = input.availableCritters.distinct().count { it == Critter.BEE || it == Critter.WORM }
        )
    }

    override fun invoke(input: Decision.ChooseMainActionCultivation): DecisionCount {
        return DecisionCount(
            playerId = input.player.id,
            type = DecisionCountType.CHOOSE_MAIN_ACTION,
            count = 0
        )
    }

    override fun invoke(input: Decision.ChooseMainActionBattle): DecisionCount {
        return DecisionCount(
            playerId = input.player.id,
            type = DecisionCountType.CHOOSE_MAIN_ACTION,
            count = 0
        )
    }

    override fun invoke(input: Decision.ChooseItemsToBuy): DecisionCount {
        return DecisionCount(
            playerId = input.player.id,
            type = DecisionCountType.CHOOSE_ITEMS_TO_BUY,
            count = 0
        )
    }

    override fun invoke(input: Decision.ChooseCardsToRefreshWithWorms): DecisionCount {
        return DecisionCount(
            playerId = input.player.id,
            type = DecisionCountType.CHOOSE_CARDS_TO_REFRESH_WITH_WORMS,
            count = input.player.creatureCards.count { it.isFaceDown }
        )
    }
}
