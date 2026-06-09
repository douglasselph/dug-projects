package dugsolutions.leaf.v30.player.components

import dugsolutions.leaf.v30.cards.domain.GameCard

class Creature {

    val left = CreatureCardsStack()
    val right = CreatureCardsStack()

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
