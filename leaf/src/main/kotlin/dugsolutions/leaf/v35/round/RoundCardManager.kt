package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCards
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Application-wide catalog of Round card definitions.
 *
 * Physical copies are not expanded here. RoundDeck expands each definition
 * according to its quantity when constructing a game deck.
 */
class RoundCardManager {

    private var cardsByName: Map<String, RoundCard> = emptyMap()

    fun loadCards(cardRegistry: RoundCardRegistry) {
        loadCards(cardRegistry.getAllCards())
    }

    fun loadCards(incoming: List<RoundCard>) {
        val reset = linkedMapOf<String, RoundCard>()

        incoming.forEach { card ->
            val key = card.name.normalizedName()

            require(key !in reset) {
                "Duplicate Round card name '${card.name}'"
            }

            reset[key] = card
        }

        cardsByName = reset
    }

    fun getCard(name: String): RoundCard? =
        cardsByName[name.normalizedName()]

    fun getCardsByType(type: RoundCardType): List<RoundCard> =
        cardsByName.values.filter { it.type == type }

    fun getRoundCardsByType(type: RoundCardType): RoundCards =
        RoundCards(getCardsByType(type))

    fun getAllCards(): RoundCards =
        RoundCards(cardsByName.values.toList())

    val size: Int
        get() = cardsByName.size

    fun clear() {
        cardsByName = emptyMap()
    }

    private fun String.normalizedName(): String =
        trim().lowercase()
}
