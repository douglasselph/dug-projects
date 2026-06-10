package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.common.Critter

interface DecisionDirector {

    fun chooseCritter(input: Decision.ChooseCritter): Critter

    fun chooseMainAction(input: Decision.ChooseMainAction): MainAction

    fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy

    fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh
}
