package dugsolutions.leaf.v14.player.decisions.core

import dugsolutions.leaf.v14.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.v14.game.acquire.domain.ChoiceDie

interface DecisionAcquireSelect {

    sealed class BuyItem {

        data class Card(val item: ChoiceCard) : BuyItem()
        data class Die(val item: ChoiceDie) : BuyItem()
        data object None : BuyItem()
    }

    suspend operator fun invoke(
        possibleCards: List<ChoiceCard>,
        possibleDice: List<ChoiceDie>
    ): BuyItem

}
