package dugsolutions.leaf.v30.player.components

import dugsolutions.leaf.v30.cards.domain.GameCard

class Creature {

    private val left = CreatureCardsStack()
    private val right = CreatureCardsStack()

    val leftCards: List<CreatureCard>
        get() = left.all

    val rightCards: List<CreatureCard>
        get() = right.all

    val isLeftEmpty: Boolean
        get() = left.isEmpty

    val isRightEmpty: Boolean
        get() = right.isEmpty

    fun getLeft(index: Int): CreatureCard? {
        return left[index]
    }

    fun getRight(index: Int): CreatureCard? {
        return right[index]
    }

    fun addLeft(card: GameCard): CreatureCardsStack {
        return addLeft(CreatureCard(card))
    }

    fun addLeft(card: CreatureCard): CreatureCardsStack {
        return left.add(card)
    }

    fun addRight(card: GameCard): CreatureCardsStack {
        return addRight(CreatureCard(card))
    }

    fun addRight(card: CreatureCard): CreatureCardsStack {
        return right.add(card)
    }

}
