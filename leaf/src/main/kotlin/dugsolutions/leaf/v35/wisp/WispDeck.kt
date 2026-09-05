package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.v35.wisp.domain.WispCards

/**
 * Mutable per-game Wisp draw deck.
 *
 * WispCardManager stores one definition per CSV row. reset() normally expands
 * each definition into its physical number of copies and shuffles them. An
 * exact reset order may instead be supplied for deterministic scenarios.
 */
class WispDeck(
    private val wispCardManager: WispCardManager,
    private val randomizer: Randomizer,
    private val exactResetCards: List<WispCard>? = null
) {

    private var drawPile = WispCards(emptyList())

    val cards: WispCards
        get() = drawPile

    val remaining: Int
        get() = drawPile.size

    val isEmpty: Boolean
        get() = remaining == 0

    fun reset() {
        exactResetCards?.let {
            setupExact(it)
            return
        }

        val expanded = wispCardManager.getAllCards().cards
            .flatMap { card ->
                List(card.quantity) { card }
            }

        drawPile = WispCards(
            randomizer.shuffled(expanded)
        )
    }

    /**
     * Installs an exact draw order without consulting [randomizer].
     *
     * The first item in [cards] is the next Wisp drawn. A scenario may use a
     * partial deck, but may not request more physical copies of a card than
     * its CSV quantity provides.
     */
    fun setupExact(cards: List<WispCard>) {
        validatePhysicalCopies(cards)
        drawPile = WispCards(cards.toList())
    }

    fun draw(): WispCard? {
        val card = drawPile.getOrNull(0) ?: return null
        drawPile = WispCards(drawPile.cards.drop(1))
        return card
    }

    private fun validatePhysicalCopies(cards: List<WispCard>) {
        cards.groupingBy { it.name.trim().lowercase() }
            .eachCount()
            .forEach { (name, requested) ->
                val definition = requireNotNull(wispCardManager.getCard(name)) {
                    "Exact Wisp deck contains unknown card: $name"
                }
                require(requested <= definition.quantity) {
                    "Exact Wisp deck requests too many copies of ${definition.name}: " +
                        "requested=$requested, available=${definition.quantity}"
                }
            }

        cards.forEach { card ->
            val definition = requireNotNull(wispCardManager.getCard(card.name)) {
                "Exact Wisp deck contains unknown card: ${card.name}"
            }
            require(card == definition) {
                "Exact Wisp deck card does not match catalog definition: ${card.name}"
            }
        }
    }
}
