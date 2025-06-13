package dugsolutions.leaf.grove.scenario

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketDiceConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.local.CardOperations

class ScenarioBasicConfig(
    cardOperations: CardOperations
) : ScenarioBase(cardOperations) {

    operator fun invoke(numPlayers: Int): MarketConfig {

        val roots = getGameCards(FlourishType.ROOT)
        val vines = getGameCards(FlourishType.VINE)
        val canopies = getGameCards(FlourishType.CANOPY)
        val flowers = getGameCards(FlourishType.FLOWER)

        val numCards = numPlayers * 3
        val numWild = numPlayers - 1
        val numFlowers = numPlayers + 2

        require(roots.size > 3) { "Not enough root cards defined. Found only ${roots.size}" }
        require(vines.size > 3) { "Not enough vine cards defined. Found only ${vines.size}" }
        require(canopies.size > 3) { "Not enough canopy cards defined. Found only ${canopies.size}" }
        require(flowers.size > 3) { "Not enough flowers cards defined. Found only ${flowers.size}" }

        // Create joint cards from roots[2,3], vines[2,3], and canopies[2,3]
        val joints = listOf(
            roots[2], roots[3],         // Third and fourth root cards
            vines[2], vines[3],         // Third and fourth vine cards
            canopies[2], canopies[3]    // Third and fourth canopy cards
        )
        return MarketConfig(
            stacks = listOf(
                getMarketStackConfig(MarketStackID.ROOT_1, listOf(roots[0]), numCards),
                getMarketStackConfig(MarketStackID.ROOT_2, listOf(roots[1]), numCards),
                getMarketStackConfig(MarketStackID.CANOPY_1, listOf(canopies[0]), numCards),
                getMarketStackConfig(MarketStackID.CANOPY_2, listOf(canopies[1]), numCards),
                getMarketStackConfig(MarketStackID.VINE_1, listOf(vines[0]), numCards),
                getMarketStackConfig(MarketStackID.VINE_2, listOf(vines[1]), numCards),
                getMarketStackConfig(MarketStackID.FLOWER_1, listOf(flowers[0]), numFlowers),
                getMarketStackConfig(MarketStackID.FLOWER_2, listOf(flowers[1]), numFlowers),
                getMarketStackConfig(MarketStackID.FLOWER_3, listOf(flowers[2]), numFlowers),
                getMarketStackConfig(MarketStackID.WILD_1, joints, numWild),
                getMarketStackConfig(MarketStackID.WILD_2, joints, numWild)
            ),
            dice = listOf(
                MarketDiceConfig(DieSides.D4, numPlayers * 2),
                MarketDiceConfig(DieSides.D6, numPlayers * 4),
                MarketDiceConfig(DieSides.D8, numPlayers * 5),
                MarketDiceConfig(DieSides.D10, numPlayers * 4),
                MarketDiceConfig(DieSides.D12, numPlayers * 4),
                MarketDiceConfig(DieSides.D20, numPlayers * 4),
            )
        )
    }

}
