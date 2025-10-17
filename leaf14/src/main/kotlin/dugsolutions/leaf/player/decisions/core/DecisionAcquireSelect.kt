package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.common.domain.acquire.ChoiceCard
import dugsolutions.leaf.common.domain.acquire.ChoiceDie


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
