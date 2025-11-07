package dugsolutions.leaf.grove

import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.list.GameCardIDs
import dugsolutions.leaf.common.domain.Butterfly
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.grove.domain.GroveStacks
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.grove.domain.MarketStackType
import dugsolutions.leaf.grove.local.GameCardsUseCase
import dugsolutions.leaf.player.components.ButterflyManager
import dugsolutions.leaf.player.components.InsectManager
import dugsolutions.leaf.player.components.VPManager
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides

class Grove(
    private val stacks: GroveStacks,
    private val butterflyManager: ButterflyManager,
    private val vpManager: VPManager,
    private val gameCardsUseCase: GameCardsUseCase,
    private val insectManager: InsectManager
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
        insectManager.add(Token.Worm, config.numBugs)
        insectManager.add(Token.Bee, config.numBugs)
        insectManager.add(Token.Ladybug, config.numBugs)
        insectManager.add(Token.Aphid, config.numBugs)
    }

    // Query methods for available items
    fun getTopShowingCards(): List<GameCard> = stacks.getTopShowingCards()

    fun getAvailableDiceSides(): List<DieSides> = stacks.getAvailableDiceSides()

    fun getDiceQuantity(sides: Int): Int = stacks.getDiceQuantity(sides)

    fun getAvailableInsects(): List<Token> = insectManager.all

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

    fun addButterfly(butterfly: Butterfly) = butterflyManager.add(butterfly)
    fun removeButterfly(butterfly: Butterfly) = butterflyManager.remove(butterfly)
    fun has(butterfly: Butterfly) = butterflyManager.has(butterfly)

    fun removeInsect(token: Token) = insectManager.remove(token)

    fun setVP(count: Int) {
        vpManager.count = count
    }

    fun getVP(): Int {
        if (vpManager.count > 0) {
            vpManager.count--
            return 1
        }
        return 0
    }

    val readyForBattlePhase: Boolean
        get() {
            return stacks.getStacksByType(MarketStackType.WISP).size == 0
        }
}
