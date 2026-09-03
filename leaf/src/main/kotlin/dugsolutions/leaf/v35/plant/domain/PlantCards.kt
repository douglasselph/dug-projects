package dugsolutions.leaf.v35.plant.domain

import dugsolutions.leaf.v35.random.Randomizer


data class PlantCards(
    private val incoming: List<PlantCard>
) : Iterable<PlantCard> {

    val cards: List<PlantCard> = incoming.toList()

    override fun iterator(): Iterator<PlantCard> = cards.iterator()

    val size: Int
        get() = cards.size

    operator fun get(index: Int): PlantCard {
        return cards[index]
    }

    fun getByType(type: PlantType): List<PlantCard> =
        cards.getByType(type)

    fun sortByCost(): PlantCards =
        create(
            cards.sortedWith(compareBy { it.cost })
        )

    fun take(n: Int): PlantCards =
        create(cards.take(n))

    operator fun plus(other: PlantCards): PlantCards =
        create(cards + other.cards)

    fun filter(predicate: (PlantCard) -> Boolean): PlantCards =
        create(cards.filter(predicate))

    fun getOrNull(index: Int): PlantCard? =
        cards.getOrNull(index)

    fun shuffled(randomizer: Randomizer): PlantCards =
        create(randomizer.shuffled(cards))

    private fun create(cards: List<PlantCard>): PlantCards {
        return PlantCards(cards)
    }
}
