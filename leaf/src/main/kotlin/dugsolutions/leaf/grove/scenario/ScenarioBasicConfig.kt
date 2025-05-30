package dugsolutions.leaf.grove.scenario

import dugsolutions.leaf.cards.GetCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketDiceConfig
import dugsolutions.leaf.grove.domain.MarketStackID

class ScenarioBasicConfig(
    getCards: GetCards,
) : ScenarioBase(getCards) {

    operator fun invoke(numPlayers: Int): MarketConfig {

        val roots = getCards(FlourishType.ROOT)
        val vines = getCards(FlourishType.VINE)
        val canopies = getCards(FlourishType.CANOPY)
        val flowers = getCards(FlourishType.FLOWER)

        val numCards = numPlayers * 3
        val numWild = numPlayers * 2
        val numFlowers = numPlayers + 2

        // Create joint cards from roots[2,3], vines[2,3], and canopies[2,3]
        val joints = (listOf(
            roots[2], roots[3],  // Third and fourth root cards
            vines[2], vines[3],  // Third and fourth vine cards
            canopies[2], canopies[3]  // Third and fourth canopy cards
        ).flatMap { card -> List(numWild) { card } }).shuffled()

        return MarketConfig(
            stacks = listOf(
                getMarketStackConfig(MarketStackID.ROOT_1, List(numCards) { roots[0] }),
                getMarketStackConfig(MarketStackID.ROOT_2, List(numCards) { roots[1] }),
                getMarketStackConfig(MarketStackID.CANOPY_1, List(numCards) { canopies[0] }),
                getMarketStackConfig(MarketStackID.CANOPY_2, List(numCards) { canopies[1] }),
                getMarketStackConfig(MarketStackID.VINE_1, List(numCards) { vines[0] }),
                getMarketStackConfig(MarketStackID.VINE_2, List(numCards) { vines[1] }),
                getMarketStackConfig(MarketStackID.FLOWER_1, List(numFlowers) { flowers[0] }),
                getMarketStackConfig(MarketStackID.FLOWER_2, List(numFlowers) { flowers[1] }),
                getMarketStackConfig(MarketStackID.FLOWER_3, List(numFlowers) { flowers[2] }),
                getMarketStackConfig(MarketStackID.JOINT_RCV, joints)
            ),
            dice = listOf(
                MarketDiceConfig(DieSides.D4, numPlayers * 2),
                MarketDiceConfig(DieSides.D6, numPlayers * 2),
                MarketDiceConfig(DieSides.D8, numPlayers * 3),
                MarketDiceConfig(DieSides.D10, numPlayers * 3),
                MarketDiceConfig(DieSides.D12, numPlayers * 2),
                MarketDiceConfig(DieSides.D20, numPlayers * 2),
            )
        )
    }

}
