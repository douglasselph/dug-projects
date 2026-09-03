package dugsolutions.leaf.v35.plant.domain

import dugsolutions.leaf.v35.random.Randomizer

class PlantCardsStack(
    cards: List<PlantCard> = emptyList()
) : Iterable<PlantCard> {
    private val cards = cards.toMutableList()

    override fun iterator(): Iterator<PlantCard> = all.iterator()

    val size: Int
        get() = cards.size

    val isEmpty: Boolean
        get() = cards.isEmpty()

    val isNotEmpty: Boolean
        get() = cards.isNotEmpty()

    val all: PlantCards
        get() = PlantCards(cards)

    operator fun get(index: Int): PlantCard? {
        return cards.getOrNull(index)
    }

    fun add(card: PlantCard): PlantCardsStack {
        cards.add(card)
        return this
    }

    fun addAll(incoming: List<PlantCard>): PlantCardsStack {
        cards.addAll(incoming)
        return this
    }

    fun remove(card: PlantCard): Boolean {
        return cards.remove(card)
    }

    fun drawTop(): PlantCard? {
        if (cards.isEmpty()) return null
        return cards.removeAt(0)
    }

    fun clear() {
        cards.clear()
    }

    fun shuffle(randomizer: Randomizer): PlantCardsStack {
        val shuffled = randomizer.shuffled(cards)
        cards.clear()
        cards.addAll(shuffled)
        return this
    }

}
