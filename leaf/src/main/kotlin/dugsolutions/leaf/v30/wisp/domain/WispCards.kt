package dugsolutions.leaf.v30.wisp.domain

import dugsolutions.leaf.v30.random.Randomizer

data class WispCards(
    private val incoming: List<WispCard>
) : Iterable<WispCard> {

    val cards: List<WispCard> = incoming.toList()

    override fun iterator(): Iterator<WispCard> = cards.iterator()

    val size: Int
        get() = cards.size

    operator fun get(index: Int): WispCard {
        return cards[index]
    }

    fun take(n: Int): WispCards {
        return create(cards.take(n))
    }

    operator fun plus(other: WispCards): WispCards {
        return create(cards + other.cards)
    }

    fun filter(predicate: (WispCard) -> Boolean): WispCards {
        return create(cards.filter(predicate))
    }

    fun getOrNull(index: Int): WispCard? {
        return cards.getOrNull(index)
    }

    val cardIds: List<WispCardID>
        get() = cards.map { it.id }

    fun shuffled(randomizer: Randomizer): WispCards {
        return create(randomizer.shuffled(cards))
    }

    private fun create(cards: List<WispCard>): WispCards {
        return WispCards(cards)
    }
}
