package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.common.Critter

interface DecisionDirector {

    fun chooseCritter(input: Decision.ChooseCritter): Critter

    fun chooseMainActionCultivation(input: Decision.ChooseMainActionCultivation): MainAction

    fun chooseMainActionBattle(input: Decision.ChooseMainActionBattle): MainAction

    fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy

    fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh
}
