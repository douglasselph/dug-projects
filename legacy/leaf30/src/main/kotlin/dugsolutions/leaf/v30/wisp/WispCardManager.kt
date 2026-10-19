package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.wisp.di.WispCardsFactory
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispCardID
import dugsolutions.leaf.v30.wisp.domain.WispCards

class WispCardManager(
    private val wispCardsFactory: WispCardsFactory
) {
    private var cards: Map<WispCardID, WispCard> = emptyMap()

    fun loadCards(cardRegistry: WispCardRegistry) {
        cards = cardRegistry.getAllCards().associateBy { it.id }
    }

    fun loadCards(incoming: List<WispCard>) {
        cards = incoming.associateBy { it.id }
    }

    fun getCard(id: WispCardID): WispCard? = cards[id]

    fun getCard(name: String): WispCard? = cards.values.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun getCardsByIds(ids: List<WispCardID>): List<WispCard> {
        return ids.mapNotNull { getCard(it) }
    }

    fun getAllCards(): WispCards {
        return wispCardsFactory(cards.values.toList())
    }
}
