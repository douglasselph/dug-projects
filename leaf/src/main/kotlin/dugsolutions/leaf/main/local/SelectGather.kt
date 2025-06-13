package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.domain.SelectedItems

class SelectGather(
    private val cardManager: CardManager
) {

    operator fun invoke(mainGameDomain: MainGameDomain): SelectedItems = with(mainGameDomain) {
        val selectedHandCards = mutableListOf<GameCard>()
        val selectedFloralCards = mutableListOf<GameCard>()
        val selectedDice = mutableListOf<Die>()

        mainGameDomain.players.forEach { player ->
            player.handCards.forEach { card ->
                if (card.highlight == HighlightInfo.SELECTED) {
                    cardManager.getCard(card.name)?.let {
                        selectedHandCards.add(it)
                    }
                }
            }
            player.buddingStack.forEach { card ->
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
