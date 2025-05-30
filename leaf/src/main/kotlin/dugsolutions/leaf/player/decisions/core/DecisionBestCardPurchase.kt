package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.components.GameCard

interface DecisionBestCardPurchase {

    operator fun invoke(possibleCards: List<GameCard>): GameCard

}
