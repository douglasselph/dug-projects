package dugsolutions.leaf.v30.player

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.player.components.Creature
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class Player(
    val creature: Creature = Creature()
) {
    private val _diceSupply = Dice()
    private val _diceHand = Dice()
    private val _diceDiscard = Dice()

    val diceSupply: Dice
        get() = Dice(_diceSupply.dice)

    val diceHand: Dice
        get() = Dice(_diceHand.dice)

    val diceDiscard: Dice
        get() = Dice(_diceDiscard.dice)

    fun addCardLeft(card: GameCard) {
        creature.addLeft(card)
    }

    fun addCardRight(card: GameCard) {
        creature.addRight(card)
    }

    fun addDieToSupply(die: Die) {
        _diceSupply.add(die)
    }

    fun addDiceToSupply(dice: List<Die>) {
        _diceSupply.addAll(dice)
    }

    fun drawDie(): Die? {
        val die = _diceSupply.drawLowest() ?: return null
        _diceHand.add(die)
        return die
    }

    fun discardHandDice() {
        _diceDiscard.addAll(_diceHand.dice)
        _diceHand.clear()
    }

    fun clearDice() {
        _diceSupply.clear()
        _diceHand.clear()
        _diceDiscard.clear()
    }

}
