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
 * This test uses an equivalent set of cards in the market with identical player behaviors in order to:
 *  - verify the results at 50%
 *  - provide an example of a typical game flow
 *  - test game length and transition points.
 */
class TestBase {

    companion object {
        private const val TEST_DIR = "base"
        private const val NUM_PLAYERS = 2
    }

    private lateinit var gameIntegrationCore: CoreSupport

    @BeforeEach
    fun setup() {
        gameIntegrationCore = CoreSupport(TEST_DIR)
        gameIntegrationCore.setup(
            marketConfig = { core -> marketConfig(core) },
            PUTSeedlings = { core -> seedlings(core) },
            otherSeedlings = { core -> seedlings(core) }
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

    private fun seedlings(core: CoreSupport): GameCards = with(core) {
        return getCards(FlourishType.SEEDLING).take(4)
    }

    private fun marketConfig(core: CoreSupport): MarketConfig {
        return core.marketBasicConfig(NUM_PLAYERS)
    }

}
