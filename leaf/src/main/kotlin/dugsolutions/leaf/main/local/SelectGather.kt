package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.domain.SelectedItems

class SelectGather(
    private val cardManager: CardManager
) {

    operator fun invoke(mainDomain: MainDomain): SelectedItems = with(mainDomain) {
        val selectedHandCards = mutableListOf<GameCard>()
        val selectedFloralCards = mutableListOf<GameCard>()
        val selectedDice = mutableListOf<Die>()

        mainDomain.players.forEach { player ->
            player.handCards.forEach { card ->
                if (card.highlight == HighlightInfo.SELECTED) {
                    cardManager.getCard(card.name)?.let {
                        selectedHandCards.add(it)
                    }
                }
            }
            player.floralArray.forEach { card ->
                if (card.highlight == HighlightInfo.SELECTED) {
                    cardManager.getCard(card.name)?.let {
                        selectedFloralCards.add(it)
                    }
                }
            }
            player.handDice.values.forEach { die ->
                if (die.highlight == HighlightInfo.SELECTED) {
                    die.backingDie?.let { selectedDice.add(it) }
                }
            }
        }
        return SelectedItems(
            cards = selectedHandCards,
            floralCards = selectedFloralCards,
            dice = selectedDice
        )
    }
}
