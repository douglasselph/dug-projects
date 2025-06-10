package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.random.Randomizer


data class GameCards(
    val cards: List<GameCard>,
    private val randomizer: Randomizer,
    private val costScore: CostScore
) : Iterable<GameCard> {

    override fun iterator(): Iterator<GameCard> = cards.iterator()

    val size: Int
        get() = cards.size

    operator fun get(index: Int): GameCard {
        return cards[index]
    }

    fun getByType(type: FlourishType): List<GameCard> =
        cards.getByType(type)

    fun sortByCost(): GameCards =
        create(
            cards.sortedWith(
                compareBy<GameCard> { costScore(it.cost) }
                    .thenBy { it.resilience }
            )
        )

    fun take(n: Int): GameCards =
        create(cards.take(n))

    operator fun plus(other: GameCards): GameCards =
        create(cards + other.cards)

    fun filter(predicate: (GameCard) -> Boolean): GameCards =
        create(cards.filter(predicate))

    fun getOrNull(index: Int): GameCard? =
        cards.getOrNull(index)

    val cardIds: List<CardID>
        get() = cards.map { it.id }

    fun shuffled(): GameCards =
        create(randomizer.shuffled(cards))

    private fun create(cards: List<GameCard>): GameCards {
        return GameCards(cards, randomizer, costScore)
    }
}
