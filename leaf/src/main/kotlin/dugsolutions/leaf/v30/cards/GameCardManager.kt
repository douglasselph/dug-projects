package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.di.GameCardsFactory
import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCardID
import dugsolutions.leaf.v30.cards.domain.GameCards

class GameCardManager(
    private val gameCardsFactory: GameCardsFactory,
    private val checkGameCardNames: CheckGameCardNames = CheckGameCardNames()
) {
    private var cards: Map<GameCardID, GameCard> = emptyMap()

    fun loadCards(cardRegistry: GameCardRegistry) {
        cards = cardRegistry.getAllCards().associateBy { it.id }
        checkGameCardNames(getAllCards())
    }

    fun loadCards(incoming: List<GameCard>) {
        val reset = mutableMapOf<GameCardID, GameCard>()
        for (card in incoming) {
            reset[card.id] = card
        }
        cards = reset
        checkGameCardNames(getAllCards())
    }

    fun getCard(id: GameCardID): GameCard? = cards[id]

    fun getCard(name: String): GameCard? = cards.values.firstOrNull { it.name.lowercase() == name.lowercase() }

    fun getCardsByIds(ids: List<GameCardID>): List<GameCard> =
        ids.mapNotNull { getCard(it) }

    fun getGameCardsByType(type: CardType): GameCards {
        val cardsOfType = cards.values.filter { it.type == type }
        return gameCardsFactory(cardsOfType)
    }

    fun getCardsByType(type: CardType): List<GameCard> {
        return cards.values.filter { it.type == type }
    }

    fun getAllCards(): GameCards {
        return gameCardsFactory(cards.values.toList())
    }

} 
