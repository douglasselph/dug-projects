package dugsolutions.leaf.player.decisions.ui

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.acquire.domain.ChoiceCard
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.player.decisions.core.DecisionAcquireSelect

class DecisionAcquireSelectSuspend : DecisionAcquireSelect {

    private val channel = DecisionSuspensionChannel<DecisionAcquireSelect.BuyItem>()
    private var possibleCardsStash: List<ChoiceCard> = emptyList()
    private var possibleDiceStash: List<ChoiceDie> = emptyList()

    // region DecisionBestCardPurchase

    override suspend operator fun invoke(
        possibleCards: List<ChoiceCard>,
        possibleDice: List<ChoiceDie>
    ): DecisionAcquireSelect.BuyItem {
        possibleCardsStash = possibleCards
        possibleDiceStash = possibleDice
        val cards = possibleCards.map { it.card }
        val dice = possibleDice.map { it.die }
        onBestPurchase(cards, dice)
        return channel.waitForDecision()
    }

    // endregion DecisionBestCardPurchase

    // region public

    var onBestPurchase: (possibleCards: List<GameCard>, possibleDice: List<Die>) -> Unit = { _, _ -> }

    fun provide(card: GameCard) {
        val choice = possibleCardsStash.find { it.card == card }
        require(choice != null)
        channel.provideDecision(DecisionAcquireSelect.BuyItem.Card(choice))
    }

    fun provide(die: Die) {
        val choice = possibleDiceStash.find { it.die == die }
        require(choice != null)
        channel.provideDecision(DecisionAcquireSelect.BuyItem.Die(choice))
    }

    // endregion public
}
