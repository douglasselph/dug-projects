package dugsolutions.leaf.v30.grove

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.common.Butterflies
import dugsolutions.leaf.v30.common.Butterfly
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.common.Critters
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.common.Tokens
import dugsolutions.leaf.v30.grove.domain.DiceStacks
import dugsolutions.leaf.v30.grove.domain.GroveCardStacks
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.wisp.WispDeck
import dugsolutions.leaf.v30.wisp.domain.WispCard

class Grove(
    val wispDeck: WispDeck
) {

    private companion object {
        const val CARDS_PER_STACK = 8
        const val CRITTERS_PER_TYPE = 9
        const val TOKENS_PER_TYPE = 8
        const val DICE_PER_STACK_TWO_PLAYERS = 7
        const val DICE_PER_STACK_THREE_PLAYERS = 8
        const val DICE_PER_STACK_FOUR_PLAYERS = 9
    }

    val diceStacks = DiceStacks()
    val cardStacks = GroveCardStacks()
    val critters = Critters()
    val tokens = Tokens()
    val butterflies = Butterflies()

     init {
        reset()
    }

    fun resetDice(numPlayers: Int) {
        val count = when (numPlayers) {
            2 -> DICE_PER_STACK_TWO_PLAYERS
            3 -> DICE_PER_STACK_THREE_PLAYERS
            4 -> DICE_PER_STACK_FOUR_PLAYERS
            else -> throw IllegalArgumentException("Unsupported player count: $numPlayers")
        }

        DieSides.entries.forEach { sides ->
            diceStacks.setCount(
                sides = sides,
                count = if (sides == DieSides.D4) 0 else count
            )
        }
    }

    fun setCard(card: GameCard) {
        cardStacks.reset(card, CARDS_PER_STACK)
    }

    fun setCards(cards: GameCards) {
        cards.forEach { card ->
            setCard(card)
        }
    }

    fun reset() {
        critters.clear()
        Critter.entries.forEach { critter ->
            critters.set(critter, CRITTERS_PER_TYPE)
        }

        tokens.reset()
        tokens.set(Token.WATER, TOKENS_PER_TYPE)
        DieSides.entries.forEach { sides ->
            tokens.set(Token.MULCH(sides), TOKENS_PER_TYPE)
        }

        butterflies.clear()
        Butterfly.entries.forEach { butterfly ->
            butterflies.add(butterfly)
        }

        wispDeck.reset()
    }

    fun resetWispDeck() {
        wispDeck.reset()
    }

    fun drawWispCard(): WispCard? {
        return wispDeck.draw()
    }

    fun count(critter: Critter): Int {
        return critters.count(critter)
    }

    fun has(critter: Critter): Boolean {
        return count(critter) > 0
    }

    fun add(critter: Critter) {
        critters.add(critter)
    }

    fun remove(critter: Critter): Boolean {
        return critters.remove(critter)
    }

    fun remove(sides: DieSides): Boolean {
        return diceStacks.remove(sides)
    }

    fun remove(card: GameCard): Boolean {
        return cardStacks.remove(card)
    }

    fun count(token: Token): Int {
        return tokens.count(token)
    }

    fun has(token: Token): Boolean {
        return tokens.has(token)
    }

    fun add(token: Token) {
        tokens.returnToken(token)
    }

    fun remove(token: Token): Token? {
        return tokens.pull(token)
    }

    fun has(butterfly: Butterfly): Boolean {
        return butterflies.all.contains(butterfly)
    }

    fun add(butterfly: Butterfly): Boolean {
        if (has(butterfly)) return false
        butterflies.add(butterfly)
        return true
    }

    fun remove(butterfly: Butterfly): Boolean {
        return butterflies.remove(butterfly)
    }

}
