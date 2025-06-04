package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.components.GameCard

interface DecisionBestCardPurchase {

    suspend operator fun invoke(possibleCards: List<GameCard>): GameCard

}
