package dugsolutions.leaf.v35.player.wisp

import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.v35.wisp.domain.WispCards

/**
 * Mutable collection of unplayed Wisp cards owned by one player.
 *
 * This is player-owned state and is intentionally separate from WispDeck,
 * which represents the shared Grove draw pile.
 *
 * WispHand does not enforce a maximum size. Any effect that requires a player
 * to discard/trash down to a limit is responsible for choosing and removing
 * those cards.
 */
class WispHand(
    cards: List<WispCard> = emptyList()
) : Iterable<WispCard> {

    private val heldCards = cards.toMutableList()

    override fun iterator(): Iterator<WispCard> =
        heldCards.toList().iterator()

    /**
     * Immutable collection view of the Wisps currently held.
     */
    val cards: WispCards
        get() = WispCards(heldCards.toList())

    val size: Int
        get() = heldCards.size

    val isEmpty: Boolean
        get() = heldCards.isEmpty()

    val isNotEmpty: Boolean
        get() = heldCards.isNotEmpty()

    fun add(card: WispCard): WispHand {
        heldCards.add(card)
        return this
    }

    fun addAll(cards: Iterable<WispCard>): WispHand {
        heldCards.addAll(cards)
        return this
    }

    /**
     * Removes the first matching Wisp card.
     *
     * Multiple physical copies of the same Wisp definition are intentionally
     * interchangeable.
     */
    fun remove(card: WispCard): Boolean =
        heldCards.remove(card)

    fun clear() {
        heldCards.clear()
    }
}
