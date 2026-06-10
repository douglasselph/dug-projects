package dugsolutions.leaf.v30.wisp

import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispCards

class WispDeck(
    private val wispCardManager: WispCardManager,
    private val randomizer: Randomizer
) {
    private var drawPile = WispCards(emptyList())

    val cards: WispCards
        get() = drawPile

    val remaining: Int
        get() = drawPile.size

    val isEmpty: Boolean
        get() = remaining == 0

    fun reset() {
        val expanded = wispCardManager.getAllCards()
            .flatMap { card -> List(card.quantity) { card } }
        drawPile = WispCards(expanded).shuffled(randomizer)
    }

    fun draw(): WispCard? {
        val card = drawPile.getOrNull(0) ?: return null
        drawPile = WispCards(drawPile.cards.drop(1))
        return card
    }
}
