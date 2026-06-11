package dugsolutions.leaf.v30.player.domain

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

    val cards: List<CreatureCard>
        get() = leftCards + rightCards

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

    fun faceDown(card: GameCard): Boolean {
        return left.replaceFirst({ it.card == card }) { it.faceDown() } ||
            right.replaceFirst({ it.card == card }) { it.faceDown() }
    }

    fun faceDown(card: CreatureCard): Boolean {
        return left.replaceFirst({ it == card }) { it.faceDown() } ||
            right.replaceFirst({ it == card }) { it.faceDown() }
    }

    fun faceUp(card: GameCard): Boolean {
        return left.replaceFirst({ it.card == card }) { it.faceUp() } ||
            right.replaceFirst({ it.card == card }) { it.faceUp() }
    }

    fun faceUpAll() {
        left.replaceAll { it.faceUp() }
        right.replaceAll { it.faceUp() }
    }

    fun remove(card: CreatureCard): Boolean {
        return left.removeFirst { it == card } != null ||
            right.removeFirst { it == card } != null
    }

}
