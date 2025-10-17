package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.cards.list.GameCards

class CardManager(
    private val gameCardsFactory: GameCardsFactory
) {
    private var cards: Map<CardID, GameCard> = emptyMap()

    fun loadCards(cardRegistry: CardRegistry) {
        cards = cardRegistry.getAllCards().associateBy { it.id }
    }

    fun loadCards(incoming: List<GameCard>) {
        val reset = mutableMapOf<CardID, GameCard>()
        for (card in incoming) {
            reset[card.id] = card
        }
        cards = reset
    }

    fun getCard(id: CardID): GameCard? = cards[id]

    fun getCard(name: String): GameCard? = cards.values.firstOrNull { it.name.lowercase() == name.lowercase() }

    fun getCardsByIds(ids: List<CardID>): List<GameCard> =
        ids.mapNotNull { getCard(it) }

    fun getGameCardsByType(type: FlourishType): GameCards {
        val cardsOfType = cards.values.filter { it.type == type }
        return gameCardsFactory(cardsOfType)
    }

    fun getCardsByType(type: FlourishType): List<GameCard> {
        return cards.values.filter { it.type == type }
    }

} 
