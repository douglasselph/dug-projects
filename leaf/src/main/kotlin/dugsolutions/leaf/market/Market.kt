package dugsolutions.leaf.market

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.market.domain.GameCardsUseCase
import dugsolutions.leaf.market.domain.MarketConfig
import dugsolutions.leaf.market.domain.MarketStackID
import dugsolutions.leaf.market.local.MarketStacks

class Market(
    private val stacks: MarketStacks,
    private val gameCardsUseCase: GameCardsUseCase

) {

    fun setup(config: MarketConfig) {
        stacks.clearAll()
        for (stackConfig in config.stacks) {
            stackConfig.cards?.let {
                stacks.add(stackConfig.which, gameCardsUseCase(stackConfig.cards))
            }
            stackConfig.cards2?.let { gameCards ->
                stacks.add(stackConfig.which, gameCards)
            }
        }
        for (diceConfig in config.dice) {
            stacks.addDie(count = diceConfig.count, sides = diceConfig.sides.value)
        }
        stacks.setBonusDice(config.bonusDie)
    }

    // Query methods for available items
    fun getTopShowingCards(): List<GameCard> = stacks.getTopShowingCards()

    fun getAvailableDiceSides(): List<Int> = stacks.getAvailableDiceSides()

    fun removeCard(cardId: CardID) {
        stacks.removeTopShowingCardOf(cardId)
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

    val useNextBonusDie: Die?
        get() = stacks.useNextBonusDie()

    val hasCards: Boolean
        get() = MarketStackID.entries.any { stacks[it]?.cardIds?.isNotEmpty() ?: false }

    val hasDice: Boolean
        get() = DieSides.entries.any { stacks.getDiceQuantity(it.value) > 0 }
}