package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.decisions.core.DecisionBestCardPurchase

class DecisionBestCardPurchaseSuspend : DecisionBestCardPurchase {

    private val drawCountChannel = DecisionSuspensionChannel<GameCard>()

    // region DecisionBestCardPurchase

    override suspend fun invoke(possibleCards: List<GameCard>): GameCard {
        onBestCardPurchase(possibleCards)
        return drawCountChannel.waitForDecision()
    }

    // endregion DecisionBestCardPurchase

    // region public

    var onBestCardPurchase: (possibleCards: List<GameCard>) -> Unit = {}

    fun provide(card: GameCard) {
        drawCountChannel.provideDecision(card)
    }

    // endregion public
}
