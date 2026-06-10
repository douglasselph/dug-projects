package dugsolutions.leaf.v30.round.domain

import dugsolutions.leaf.v30.random.Randomizer

data class RoundCards(
    private val incoming: List<RoundCard>
) : Iterable<RoundCard> {

    val cards: List<RoundCard> = incoming.toList()

    override fun iterator(): Iterator<RoundCard> = cards.iterator()

    val size: Int
        get() = cards.size

    operator fun get(index: Int): RoundCard {
        return cards[index]
    }

    fun take(n: Int): RoundCards {
        return create(cards.take(n))
    }

    operator fun plus(other: RoundCards): RoundCards {
        return create(cards + other.cards)
    }

    fun filter(predicate: (RoundCard) -> Boolean): RoundCards {
        return create(cards.filter(predicate))
    }

    fun getOrNull(index: Int): RoundCard? {
        return cards.getOrNull(index)
    }

    val cardIds: List<RoundCardID>
        get() = cards.map { it.id }

    fun shuffled(randomizer: Randomizer): RoundCards {
        return create(randomizer.shuffled(cards))
    }

    private fun create(cards: List<RoundCard>): RoundCards {
        return RoundCards(cards)
    }
}
