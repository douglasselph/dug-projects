package dugsolutions.leaf.grove.scenario

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.grove.domain.CardOperations
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketDiceConfig
import dugsolutions.leaf.grove.domain.MarketStackID

class ScenarioBasicConfig(
    cardOperations: CardOperations
) : ScenarioBase(cardOperations) {

    operator fun invoke(numPlayers: Int): MarketConfig {

        val roots = getGameCards(FlourishType.ROOT)
        val vines = getGameCards(FlourishType.VINE)
        val flowers = getGameCards(FlourishType.FLOWER)

        val numRoot = numPlayers * 2
        val numVine = numPlayers + 5
        val numFlower = numPlayers + 1
        val numDice = numPlayers + 6

        require(roots.size >= 3) { "Not enough root cards defined. Found only ${roots.size}" }
        require(flowers.size >= 3) { "Not enough flowers cards defined. Found only ${flowers.size}" }

        val allVines = vines.plus(vines).shuffled()
        require(allVines.size >= numVine) { "Not enough vine cards defined. Found only ${vines.size}" }

        return MarketConfig(
            stacks = listOf(
                getMarketStackConfig(MarketStackID.ROOT_1, listOf(roots[0]), numRoot),
                getMarketStackConfig(MarketStackID.ROOT_2, listOf(roots[1]), numRoot),
                getMarketStackConfig(MarketStackID.ROOT_3, listOf(roots[2]), numRoot),
                getMarketStackConfig(MarketStackID.FLOWER_1, listOf(flowers[0]), numFlower),
                getMarketStackConfig(MarketStackID.FLOWER_2, listOf(flowers[1]), numFlower),
                getMarketStackConfig(MarketStackID.FLOWER_3, listOf(flowers[2]), numFlower),
                getMarketStackConfig(MarketStackID.WILD, allVines.cards, 1),
            ),
            dice = listOf(
                MarketDiceConfig(DieSides.D4, numDice),
                MarketDiceConfig(DieSides.D6, numDice),
                MarketDiceConfig(DieSides.D8, numDice),
                MarketDiceConfig(DieSides.D10, numDice),
                MarketDiceConfig(DieSides.D12, numDice),
                MarketDiceConfig(DieSides.D20, numDice),
            )
        )
    }

}
