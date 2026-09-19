package dugsolutions.leaf.v30.round

import dugsolutions.leaf.v30.round.di.RoundCardsFactory
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.round.domain.RoundCardID
import dugsolutions.leaf.v30.round.domain.RoundCards

class RoundCardManager(
    private val roundCardsFactory: RoundCardsFactory
) {
    private var cards: Map<RoundCardID, RoundCard> = emptyMap()

    fun loadCards(cardRegistry: RoundCardRegistry) {
        cards = cardRegistry.getAllCards().associateBy { it.id }
    }

    fun loadCards(incoming: List<RoundCard>) {
        cards = incoming.associateBy { it.id }
    }

    fun getCard(id: RoundCardID): RoundCard? = cards[id]

    fun getCard(name: String): RoundCard? = cards.values.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun getCardsByIds(ids: List<RoundCardID>): List<RoundCard> {
        return ids.mapNotNull { getCard(it) }
    }

    fun getAllCards(): RoundCards {
        return roundCardsFactory(cards.values.toList())
    }
}
