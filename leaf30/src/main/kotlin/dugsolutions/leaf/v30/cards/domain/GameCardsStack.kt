package dugsolutions.leaf.v30.cards.domain

import dugsolutions.leaf.v30.random.Randomizer

class GameCardsStack(
    cards: List<GameCard> = emptyList()
) : Iterable<GameCard> {
    private val cards = cards.toMutableList()

    override fun iterator(): Iterator<GameCard> = all.iterator()

    val size: Int
        get() = cards.size

    val isEmpty: Boolean
        get() = cards.isEmpty()

    val isNotEmpty: Boolean
        get() = cards.isNotEmpty()

    val all: GameCards
        get() = GameCards(cards)

    val cardIds: List<GameCardID>
        get() = cards.map { it.id }

    operator fun get(index: Int): GameCard? {
        return cards.getOrNull(index)
    }

    fun add(card: GameCard): GameCardsStack {
        cards.add(card)
        return this
    }

    fun addAll(incoming: List<GameCard>): GameCardsStack {
        cards.addAll(incoming)
        return this
    }

    fun remove(card: GameCard): Boolean {
        return cards.remove(card)
    }

    fun remove(cardId: GameCardID): GameCard? {
        val index = cards.indexOfFirst { it.id == cardId }
        if (index < 0) return null
        return cards.removeAt(index)
    }

    fun drawTop(): GameCard? {
        if (cards.isEmpty()) return null
        return cards.removeAt(0)
    }

    fun clear() {
        cards.clear()
    }

    fun shuffle(randomizer: Randomizer): GameCardsStack {
        val shuffled = randomizer.shuffled(cards)
        cards.clear()
        cards.addAll(shuffled)
        return this
    }

}
