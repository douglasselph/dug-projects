package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.v35.wisp.domain.WispCards

/**
 * Application-wide catalog of Wisp card definitions.
 *
 * Physical copies are not expanded here. WispDeck expands each definition
 * according to its quantity when a game deck is reset.
 */
class WispCardManager {

    private var cardsByName: Map<String, WispCard> = emptyMap()

    fun loadCards(cardRegistry: WispCardRegistry) {
        loadCards(cardRegistry.getAllCards())
    }

    fun loadCards(incoming: List<WispCard>) {
        val reset = linkedMapOf<String, WispCard>()

        incoming.forEach { card ->
            val key = card.name.normalizedName()

            require(key !in reset) {
                "Duplicate Wisp card name '${card.name}'"
            }

            reset[key] = card
        }

        cardsByName = reset
    }

    fun getCard(name: String): WispCard? =
        cardsByName[name.normalizedName()]

    fun getAllCards(): WispCards =
        WispCards(cardsByName.values.toList())

    val size: Int
        get() = cardsByName.size

    fun clear() {
        cardsByName = emptyMap()
    }

    private fun String.normalizedName(): String =
        trim().lowercase()
}
