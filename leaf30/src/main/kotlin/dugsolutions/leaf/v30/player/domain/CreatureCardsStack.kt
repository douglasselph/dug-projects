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

    fun replaceFirst(
        predicate: (CreatureCard) -> Boolean,
        replacement: (CreatureCard) -> CreatureCard
    ): Boolean {
        val index = cards.indexOfFirst(predicate)
        if (index < 0) return false
        cards[index] = replacement(cards[index])
        return true
    }

    fun removeFirst(predicate: (CreatureCard) -> Boolean): CreatureCard? {
        val index = cards.indexOfFirst(predicate)
        if (index < 0) return null
        return cards.removeAt(index)
    }

    fun replaceAll(replacement: (CreatureCard) -> CreatureCard) {
        cards.indices.forEach { index ->
            cards[index] = replacement(cards[index])
        }
    }

    fun clear() {
        cards.clear()
    }
}
