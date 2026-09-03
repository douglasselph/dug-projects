package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.v35.wisp.domain.WispCards

/**
 * Mutable per-game Wisp draw deck.
 *
 * WispCardManager stores one definition per CSV row. reset() expands each
 * definition into its physical number of copies, shuffles them, and creates
 * the draw pile.
 */
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
        val expanded = wispCardManager.getAllCards().cards
            .flatMap { card ->
                List(card.quantity) { card }
            }

        drawPile = WispCards(
            randomizer.shuffled(expanded)
        )
    }

    fun draw(): WispCard? {
        val card = drawPile.getOrNull(0) ?: return null
        drawPile = WispCards(drawPile.cards.drop(1))
        return card
    }
}
