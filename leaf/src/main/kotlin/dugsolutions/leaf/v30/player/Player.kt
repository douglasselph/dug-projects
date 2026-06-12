package dugsolutions.leaf.v30.player

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.common.Butterflies
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Critters
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.common.Tokens
import dugsolutions.leaf.v30.player.decision.baseline.DecisionDirectorBaseline
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.domain.Creature
import dugsolutions.leaf.v30.player.domain.CreatureCard
import dugsolutions.leaf.v30.player.domain.OutOfDiceException
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.wisp.domain.WispCard
import dugsolutions.leaf.v30.wisp.domain.WispCards
import java.util.concurrent.atomic.AtomicInteger

class Player(
    val decisionDirector: DecisionDirector = DecisionDirectorBaseline(),
    private val chronicle: Chronicle = GameChronicle(),
    val id: Int = nextId()
) {
    companion object {
        private val nextPlayerId = AtomicInteger(1)

        private fun nextId(): Int {
            return nextPlayerId.getAndIncrement()
        }
    }

    private val _creature: Creature = Creature()
    private val _diceSupply = Dice()
    private val _diceHand = Dice()
    private val _diceDiscard = Dice()
    private val _critters = Critters()
    private val _tokens = Tokens()
    private val _butterflies = Butterflies()
    private val _wispCards = mutableListOf<WispCard>()
    private var _vp = 0

    val diceSupply: Dice
        get() = Dice(_diceSupply.dice)

    var diceHand: Dice
        get() = Dice(_diceHand.dice)
        set(value) {
            _diceHand.set(value.dice)
        }

    val diceDiscard: Dice
        get() = Dice(_diceDiscard.dice)

    val critters: List<Critter>
        get() = _critters.all

    val waterTokenCount: Int
        get() = _tokens.waterCount

    val mulchTokens: List<Token>
        get() = _tokens.mulchTokens

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

    val creatureCards: List<CreatureCard>
        get() = _creature.cards

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

    fun flipCreatureCardFaceUp(card: GameCard): Boolean {
        return _creature.faceUp(card)
    }

    fun flipAllCreatureCardsFaceUp() {
        _creature.faceUpAll()
    }

    fun flipItOrSnipIt() {
        val selected = decisionDirector.chooseFlipOrSnipCard(
            Decision.ChooseFlipOrSnipCard(
                player = this,
                creatureCards = creatureCards
            )
        )
        val wasFaceUp = selected.isFaceUp
        val changed = if (wasFaceUp) {
            _creature.faceDown(selected)
        } else {
            _creature.remove(selected)
        }
        if (!changed) return
        chronicle(
            Moment.WoundCard(
                player = this,
                card = selected,
                wasFlipped = wasFaceUp,
                wasLost = !wasFaceUp
            )
        )
    }

    fun addDieToSupply(die: Die) {
        _diceSupply.add(die)
    }

    fun addDieToDiscard(die: Die) {
        _diceDiscard.add(die)
    }

    fun addDieToHand(die: Die) {
        _diceHand.add(die)
    }

    fun removeDieFromHand(die: Die): Boolean {
        return _diceHand.remove(die)
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

    fun rerollDie(die: Die): Die? {
        val target = _diceHand.dice.firstOrNull { it == die } ?: return null
        target.roll()
        return target
    }

    fun raiseDie(
        die: Die,
        amount: Int
    ): Die? {
        val target = _diceHand.dice.firstOrNull { it == die } ?: return null
        target.adjustBy(amount)
        return target
    }

    fun discardHandDice() {
        _diceDiscard.addAll(_diceHand.dice)
        _diceHand.clear()
    }

    fun drawHighestDieFromDiscard(): Die? {
        return _diceDiscard.drawHighest()
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

    fun replaceCritter(
        from: Critter,
        to: Critter
    ): Int {
        return _critters.replace(from, to)
    }

    fun add(token: Token) {
        _tokens.add(token)
    }

    fun remove(token: Token): Boolean {
        return _tokens.pull(token) != null
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
