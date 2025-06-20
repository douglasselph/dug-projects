package dugsolutions.leaf.grove

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.GameCardIDs
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.grove.local.GameCardsUseCase
import dugsolutions.leaf.grove.domain.GroveStacks
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.grove.domain.MarketStackType

class Grove(
    private val stacks: GroveStacks,
    private val gameCardsUseCase: GameCardsUseCase
) {

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

    fun repairWild() {
        val wild1 = stacks[MarketStackID.WILD_1] ?: return
        val wild2 = stacks[MarketStackID.WILD_2] ?: return
        if (wild1.isEmpty() && wild2.size > 1) {
            wild2.removeTop()?.let { cardId -> wild1.add(cardId) }
        } else if (wild2.isEmpty() && wild1.size > 1) {
            wild1.removeTop()?.let { cardId -> wild2.add(cardId) }
        }
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
