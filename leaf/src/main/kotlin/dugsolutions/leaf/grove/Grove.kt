package dugsolutions.leaf.grove

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.grove.domain.GameCardsUseCase
import dugsolutions.leaf.grove.domain.GroveStacks
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketStackID

class Grove(
    private val stacks: GroveStacks,
    private val gameCardsUseCase: GameCardsUseCase
) {

    // TODO: Unit test
    fun setup(config: MarketConfig) {
        stacks.clearAll()
        for (stackConfig in config.stacks) {
            stackConfig.cards?.let {
                stacks.add(stackConfig.which, gameCardsUseCase(stackConfig.cards))
                stacks.shuffle(stackConfig.which)
            }
        }
        for (diceConfig in config.dice) {
            stacks.addDie(count = diceConfig.count, sides = diceConfig.sides.value)
        }
    }

    // Query methods for available items
    fun getTopShowingCards(): List<GameCard> = stacks.getTopShowingCards()

    fun getAvailableDiceSides(): List<Int> = stacks.getAvailableDiceSides()

    fun getDiceQuantity(sides: Int): Int = stacks.getDiceQuantity(sides)

    fun removeCard(cardId: CardID) {
        stacks.removeTopShowingCardOf(cardId)
    }

    fun addDie(die: Die) {
        stacks.addDie(die.sides)
    }

    fun removeDie(die: Die) {
        stacks.removeDie(die.sides)
    }

    fun hasDie(sides: Int): Boolean {
       return stacks.hasDie(sides)
    }

    fun getCardsFor(type: MarketStackID): GameCardIDs? {
        return stacks[type]
    }

    val readyForBattlePhase: Boolean
        get() {
            val exhaustedStacks = MarketStackID.entries.count { stackId ->
                stacks[stackId]?.isEmpty() ?: true
            }
            return exhaustedStacks >= Commons.EXHAUSTED_STACK_COUNT
        }
}
