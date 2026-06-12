package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.domain.CreatureCard

interface DecisionDirector {

    fun chooseCritter(input: Decision.ChooseCritter): Critter

    fun chooseMainCultivationAction(input: Decision.ChooseMainActionCultivation): ActionCultivation

    fun chooseMainBattleAction(input: Decision.ChooseMainActionBattle): ActionBattleMain

    fun chooseSupportBattleAction(input: Decision.ChooseMainActionBattle): ActionBattleSupport

    fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy

    fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh

    fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard
}
