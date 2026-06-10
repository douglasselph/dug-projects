package dugsolutions.leaf.v30.player

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Butterflies
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Critters
import dugsolutions.leaf.v30.player.decision.baseline.DecisionDirectorBaseline
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.domain.Creature
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.player.domain.OutOfDiceException
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispCards

class Player(
    val decisionDirector: DecisionDirector = DecisionDirectorBaseline()
) {
    private val _creature: Creature = Creature()
    private val _diceSupply = Dice()
    private val _diceHand = Dice()
    private val _diceDiscard = Dice()
    private val _critters = Critters()
    private val _butterflies = Butterflies()
    private val _wispCards = mutableListOf<WispCard>()
    private var _vp = 0

    val diceSupply: Dice
        get() = Dice(_diceSupply.dice)

    val diceHand: Dice
        get() = Dice(_diceHand.dice)

    val diceDiscard: Dice
        get() = Dice(_diceDiscard.dice)

    val critters: List<Critter>
        get() = _critters.all

    val butterflies: List<Butterfly>
        get() = _butterflies.all

    val wispCards: WispCards
        get() = WispCards(_wispCards)

    val vp: Int
        get() = _vp

    val creatureLeftCards: List<CreatureCard>
        get() = _creature.leftCards

    val creatureRightCards: List<CreatureCard>
        get() = _creature.rightCards

    val isCreatureLeftEmpty: Boolean
        get() = _creature.isLeftEmpty

    val isCreatureRightEmpty: Boolean
        get() = _creature.isRightEmpty

    fun getCreatureLeftCard(index: Int): CreatureCard? {
        return _creature.getLeft(index)
    }

    fun getCreatureRightCard(index: Int): CreatureCard? {
        return _creature.getRight(index)
    }

    fun addCardLeft(card: GameCard) {
        _creature.addLeft(card)
    }

    fun addCardRight(card: GameCard) {
        _creature.addRight(card)
    }

    fun addCardToCreature(card: GameCard) {
        addCardLeft(card)
    }

    fun addCardToCreature(card: CreatureCard) {
        _creature.addLeft(card)
    }

    fun flipCreatureCardFaceDown(card: GameCard): Boolean {
        return _creature.faceDown(card)
    }

    fun addDieToSupply(die: Die) {
        _diceSupply.add(die)
    }

    fun addDieToDiscard(die: Die) {
        _diceDiscard.add(die)
    }

    fun addDiceToSupply(dice: List<Die>) {
        _diceSupply.addAll(dice)
    }

    fun drawDie(): Die? {
        val die = _diceSupply.drawLowest() ?: return null
        _diceHand.add(die)
        return die
    }

    fun drawDiceWithRefresh(): Die {
        drawDie()?.let { return it }

        _diceSupply.addAll(_diceDiscard.dice)
        _diceDiscard.clear()

        return drawDie() ?: throw OutOfDiceException()
    }

    fun rollDice() {
        _diceHand.roll()
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

    fun addCritter(critter: Critter) {
        _critters.add(critter)
    }

    fun removeCritter(critter: Critter): Boolean {
        return _critters.remove(critter)
    }

    fun addButterfly(butterfly: Butterfly) {
        _butterflies.add(butterfly)
    }

    fun removeButterfly(butterfly: Butterfly): Boolean {
        return _butterflies.remove(butterfly)
    }

    fun addWispCard(card: WispCard) {
        _wispCards.add(card)
    }

    fun removeWispCard(card: WispCard): Boolean {
        return _wispCards.remove(card)
    }

    fun addVp(amount: Int) {
        require(amount > 0) { "VP amount must be positive: $amount" }
        _vp += amount
    }

    fun resetVp() {
        _vp = 0
    }

}
