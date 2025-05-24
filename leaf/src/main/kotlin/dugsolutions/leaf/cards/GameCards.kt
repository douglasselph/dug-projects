package dugsolutions.leaf.cards

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.getByType
import dugsolutions.leaf.tool.Randomizer


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
