package dugsolutions.leaf.integration

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.market.domain.MarketConfig
import dugsolutions.leaf.market.domain.MarketDiceConfig
import dugsolutions.leaf.market.domain.MarketStackID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for the Game class.
 * These tests use real components instead of mocks to verify the game's behavior.
 */
class TestSproutingHusk {

    companion object {
        private const val TEST_DIR = "drawCard"
        private const val CARD_UNDER_TEST = "Sprouting Husk"
    }

    private lateinit var gameIntegrationCore: CoreSupport

    @BeforeEach
    fun setup() {
        gameIntegrationCore = CoreSupport(TEST_DIR)
        gameIntegrationCore.setup(
            marketConfig = { core -> marketConfig(core) },
            PUTSeedlings = { core -> putSeedlings(core) },
            otherSeedlings = { core -> otherSeedlings(core) }
        )
    }

    @AfterEach
    fun tearDown() {
        gameIntegrationCore.tearDown()
    }

    @Test
    fun testBaseSingle() = runBlocking {
        gameIntegrationCore.testSingle()
    }

    @Test
    fun testBase100Games() = runBlocking {
        gameIntegrationCore.test100()
    }

    @Test
    fun testBase10000Games() = runBlocking {
        gameIntegrationCore.test10000()
    }

    private fun otherSeedlings(core: CoreSupport): GameCards = with(core) {
        val seedling1 = getCard(CoreSupport.BaseCard.BASE_SEEDLING_1)
        val seedling2 = getCard(CoreSupport.BaseCard.BASE_SEEDLING_2)
        return gameCardsFactory(List(2) { seedling1 } + List(2) { seedling2 })
    }

    private fun putSeedlings(core: CoreSupport): GameCards = with(core) {
        val seedling = getCard(CARD_UNDER_TEST)
        return gameCardsFactory(List(4) { seedling })
    }

    private fun marketConfig(core: CoreSupport): MarketConfig = with(core) {
        val root1 = getCard(CoreSupport.BaseCard.BASE_ROOT)
        val vine1 = getCard(CoreSupport.BaseCard.BASE_VINE)
        val canopy1 = getCard(CoreSupport.BaseCard.BASE_CANOPY)
        val bloom1 = getCard(CoreSupport.BaseCard.BASE_BLOOM)
        return MarketConfig(
            stacks = listOf(
                getMarketStackConfig(MarketStackID.ROOT_1, List(8) { root1 }),
                getMarketStackConfig(MarketStackID.CANOPY_1, List(8) { canopy1 }),
                getMarketStackConfig(MarketStackID.VINE_1, List(8) { vine1 }),
                getMarketStackConfig(MarketStackID.BLOOM_1, List(4) { bloom1 })
            ),
            dice = listOf(
                MarketDiceConfig(DieSides.D4, 4),
                MarketDiceConfig(DieSides.D6, 4),
                MarketDiceConfig(DieSides.D8, 6),
                MarketDiceConfig(DieSides.D10, 6),
                MarketDiceConfig(DieSides.D12, 6),
                MarketDiceConfig(DieSides.D20, 4),
            ),
            bonusDie = listOf(DieSides.D20, DieSides.D12)
        )
    }

}
