package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player

interface DecisionBestCardPurchase {

    operator fun invoke(possibleCards: List<GameCard>): GameCard

}
