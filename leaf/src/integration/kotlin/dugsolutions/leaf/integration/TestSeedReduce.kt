package dugsolutions.leaf.integration

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.grove.domain.MarketConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for the Game class.
 * These tests use real components instead of mocks to verify the game's behavior.
 *
 * Test for 2 Players
 *
 * The difference is the Player Under Test will receive one specific Seedling Card.
 * The other player will have the normal distribution of Seedling cards.
 */
class TestSeedReduce {

    companion object {
        private const val TEST_DIR = "SeedReduce"
        private const val CARD_UNDER_TEST = "SeedReduce"
        private const val NUM_PLAYERS = 2
    }

    private lateinit var gameIntegrationCore: CoreSupport

    @BeforeEach
    fun setup() {
        gameIntegrationCore = CoreSupport(TEST_DIR)
        gameIntegrationCore.setup(
            marketConfig = { core -> marketConfig(core) },
            PUTSeedlings = { core -> PUTSeedlings(core) },
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

    private fun PUTSeedlings(core: CoreSupport): GameCards = with(core) {
        val seedling = getCard(CARD_UNDER_TEST)
        return gameCardsFactory(List(4) { seedling })
    }

    private fun otherSeedlings(core: CoreSupport): GameCards = with(core) {
        return getCards(FlourishType.SEEDLING).take(4)
    }

    private fun marketConfig(core: CoreSupport): MarketConfig {
        return core.marketBasicConfig(NUM_PLAYERS)
    }

}
