package dugsolutions.leaf.v30.player.domain

class CreatureCardsStack(
    cards: List<CreatureCard> = emptyList()
) : Iterable<CreatureCard> {
    private val cards = cards.toMutableList()

    override fun iterator(): Iterator<CreatureCard> = all.iterator()

    val size: Int
        get() = cards.size

    val isEmpty: Boolean
        get() = cards.isEmpty()

    val isNotEmpty: Boolean
        get() = cards.isNotEmpty()

    val all: List<CreatureCard>
        get() = cards.toList()

    operator fun get(index: Int): CreatureCard? {
        return cards.getOrNull(index)
    }

    fun add(card: CreatureCard): CreatureCardsStack {
        cards.add(card)
        return this
    }

    fun clear() {
        cards.clear()
    }
}
