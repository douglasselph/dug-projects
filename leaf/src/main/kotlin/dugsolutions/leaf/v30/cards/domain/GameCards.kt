package dugsolutions.leaf.v30.cards.domain

import dugsolutions.leaf.v30.random.Randomizer


data class GameCards(
    val cards: List<GameCard>
) : Iterable<GameCard> {

    override fun iterator(): Iterator<GameCard> = cards.iterator()

    val size: Int
        get() = cards.size

    operator fun get(index: Int): GameCard {
        return cards[index]
    }

    fun getByType(type: CardType): List<GameCard> =
        cards.getByType(type)

    fun sortByCost(): GameCards =
        create(
            cards.sortedWith(compareBy { it.cost })
        )

    fun take(n: Int): GameCards =
        create(cards.take(n))

    operator fun plus(other: GameCards): GameCards =
        create(cards + other.cards)

    fun filter(predicate: (GameCard) -> Boolean): GameCards =
        create(cards.filter(predicate))

    fun getOrNull(index: Int): GameCard? =
        cards.getOrNull(index)

    val cardIds: List<GameCardID>
        get() = cards.map { it.id }

    fun shuffled(randomizer: Randomizer): GameCards =
        create(randomizer.shuffled(cards))

    private fun create(cards: List<GameCard>): GameCards {
        return GameCards(cards)
    }
}
