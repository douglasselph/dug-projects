package dugsolutions.leaf.market.scenario

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.market.domain.MarketStackID
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.market.domain.MarketCardConfig
import dugsolutions.leaf.market.domain.MarketConfig
import dugsolutions.leaf.market.domain.MarketDiceConfig
import dugsolutions.leaf.market.domain.MarketStackConfig

class ScenarioMarketCheap(
    private val market: Market,
    private val cardManager: CardManager,
) {

    companion object {
        private const val STANDARD_STACK_SIZE = 8
        private const val BLOOM_SIZE = 3
        private const val SNATCH = 2
    }

    operator fun invoke(numPlayers: Int) {
        // Get cards by type and sort by cost
        val rootCards = cardManager.getGameCardsByType(FlourishType.ROOT).sortByCost()
        val canopyCards = cardManager.getGameCardsByType(FlourishType.CANOPY).sortByCost()
        val vineCards = cardManager.getGameCardsByType(FlourishType.VINE).sortByCost()
        val bloomCards = cardManager.getGameCardsByType(FlourishType.BLOOM).sortByCost()

        // Track used cards to avoid duplicates in the reference set
        val usedCards = mutableSetOf<GameCard>()

        // Get two cards of each type
        val cheapestRoot = rootCards.take(SNATCH).also { usedCards.addAll(it.cards) }
        val cheapestCanopy = canopyCards.take(SNATCH).also { usedCards.addAll(it.cards) }
        val cheapestVine = vineCards.take(SNATCH).also { usedCards.addAll(it.cards) }

        // Get available cards for JOINT_RCV (2 from each type, excluding used cards)
        val availableForJoint = (
            rootCards.filter { it !in usedCards }.shuffled().take(SNATCH) +
                canopyCards.filter { it !in usedCards }.shuffled().take(SNATCH) +
                vineCards.filter { it !in usedCards }.shuffled().take(SNATCH)
            ).shuffled()

        // Get random BLOOM cards (excluding any that might have been used)
        val availableBloom = bloomCards
            .filter { it !in usedCards }
            .shuffled()
            .take(BLOOM_SIZE)

        // Fill MarketStacks with these cards.
        val bloomStackSize = numPlayers + 1
        market.setup(
            MarketConfig(
                stacks = listOf(
                    // ROOT stacks - 8 copies of each card
                    MarketStackConfig(
                        MarketStackID.ROOT_1, listOf(
                            MarketCardConfig(cheapestRoot[0], STANDARD_STACK_SIZE),
                        )
                    ),
                    MarketStackConfig(
                        MarketStackID.ROOT_2, listOf(
                            MarketCardConfig(cheapestRoot[1], STANDARD_STACK_SIZE),
                        )
                    ),
                    // CANOPY stacks - 8 copies of each card
                    MarketStackConfig(
                        MarketStackID.CANOPY_1, listOf(
                            MarketCardConfig(cheapestCanopy[0], STANDARD_STACK_SIZE),
                        )
                    ),
                    MarketStackConfig(
                        MarketStackID.CANOPY_2, listOf(
                            MarketCardConfig(cheapestCanopy[1], STANDARD_STACK_SIZE),
                        )
                    ),
                    // VINE stacks - 8 copies of each card
                    MarketStackConfig(
                        MarketStackID.VINE_1, listOf(
                            MarketCardConfig(cheapestVine[0], STANDARD_STACK_SIZE),
                        )
                    ),
                    MarketStackConfig(
                        MarketStackID.VINE_2, listOf(
                            MarketCardConfig(cheapestVine[1], STANDARD_STACK_SIZE),
                        )
                    ),
                    // JOINT_RCV stack - 2 copies of each selected card
                    MarketStackConfig(
                        MarketStackID.JOINT_RCV, cards2 = availableForJoint
                    ),
                    // BLOOM stacks - (numPlayers + 1) copies of each card
                    MarketStackConfig(
                        MarketStackID.BLOOM_1, listOf(
                            MarketCardConfig(availableBloom[0], bloomStackSize),
                        )
                    ),
                    MarketStackConfig(
                        MarketStackID.BLOOM_2, listOf(
                            MarketCardConfig(availableBloom[1], bloomStackSize),
                        )
                    ),
                    MarketStackConfig(
                        MarketStackID.BLOOM_3, listOf(
                            MarketCardConfig(availableBloom[2], bloomStackSize),
                        )
                    ),
                ),
                dice = listOf(
                    MarketDiceConfig(DieSides.D4, numPlayers),
                    MarketDiceConfig(DieSides.D6, numPlayers),
                    MarketDiceConfig(DieSides.D8, numPlayers),
                    MarketDiceConfig(DieSides.D10, numPlayers),
                    MarketDiceConfig(DieSides.D12, numPlayers),
                    MarketDiceConfig(DieSides.D20, numPlayers)
                ),
                bonusDie = listOf(
                    DieSides.D20,
                    DieSides.D12,
                    DieSides.D10,
                    DieSides.D8,
                    DieSides.D6,
                    DieSides.D4
                ).take(numPlayers)
            )
        )
    }

}
