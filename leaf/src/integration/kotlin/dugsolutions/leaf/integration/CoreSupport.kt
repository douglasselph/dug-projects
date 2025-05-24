package dugsolutions.leaf.integration

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.report.GenerateGameSummary
import dugsolutions.leaf.chronicle.report.WriteChronicleResults
import dugsolutions.leaf.chronicle.report.WriteGameResults
import dugsolutions.leaf.chronicle.report.ReportGameSummaries
import dugsolutions.leaf.chronicle.report.WriteGameSummaries
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryConfig
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.di.gameModule
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketCardConfig
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.domain.MarketDiceConfig
import dugsolutions.leaf.grove.domain.MarketStackConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.tool.CardRegistry
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.tool.RandomizerDefault
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get

/**
 * Integration tests for the Game class.
 * These tests use real components instead of mocks to verify the game's behavior.
 */
class CoreSupport(
    private val testDir: String
) : KoinTest {
    companion object {
        private const val TEST_CARD_LIST = Commons.TEST_CARD_LIST
    }

    private lateinit var cardManager: CardManager
    private lateinit var dieFactory: DieFactory
    private lateinit var grove: Grove
    private lateinit var game: Game
    private lateinit var cardRegistry: CardRegistry
    private lateinit var chronicle: GameChronicle
    private lateinit var runGame: RunGame
    private lateinit var writeGameResults: WriteGameResults
    private lateinit var writeChronicleResults: WriteChronicleResults
    private lateinit var reportGameSummaries: ReportGameSummaries
    private lateinit var generateGameSummary: GenerateGameSummary
    private lateinit var writeGameSummaries: WriteGameSummaries
    private lateinit var randomizer: Randomizer
    private lateinit var marketConfig: MarketConfig
    private lateinit var playerUnderTestSeedlings: GameCards
    private lateinit var otherSeedlings: GameCards

    lateinit var gameCardsFactory: GameCardsFactory

    fun setup(
        marketConfig: (core: CoreSupport) -> MarketConfig,
        PUTSeedlings: (core: CoreSupport) -> GameCards,
        otherSeedlings: (core: CoreSupport) -> GameCards
    ) {
        // Stop Koin if it's already running to avoid conflicts
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
        // Set the flag to use uniform dice before starting Koin
        DieFactoryConfig.useUniformDice = true

        // Start Koin with test modules
        startKoin {
            modules(
                gameModule,  // Include the main game module
            )
        }

        // Get the dependencies after Koin is started
        randomizer = get()
        cardManager = get()
        dieFactory = get()
        grove = get()
        game = get()
        cardRegistry = get()
        chronicle = get()
        gameCardsFactory = get()
        runGame = get()
        writeChronicleResults = get()
        writeGameResults = get()
        writeGameSummaries = get()
        generateGameSummary = get()
        reportGameSummaries = get()

        cardRegistry.loadFromCsv(TEST_CARD_LIST)
        cardManager.loadCards(cardRegistry)

        this.marketConfig = marketConfig(this)
        this.playerUnderTestSeedlings = PUTSeedlings(this)
        this.otherSeedlings = otherSeedlings(this)
    }

    fun tearDown() {
        // Clean up Koin after each test
        stopKoin()
    }

    fun testSingle() = runBlocking {
        // Arrange
        val testName = "single"
        (randomizer as RandomizerDefault).seed = 24
        baseSetup()
        // Act - properly collect from the Flow
        runGame().collect { gameEvent ->
            // You can optionally process game events here if needed
            println("Game event: $gameEvent")
            writeGameResults.update(testDir, testName)
        }
        writeGameResults.finish(testDir, testName)
    }

    fun test100() = runBlocking {
        // Arrange
        val numGames = 100
        val baseTestName = "run"
        val baseSeed = 42L
        val summaries = mutableListOf<GameSummary>()

        // Run multiple games with different seeds
        for (i in 0 until numGames) {
            val testName = "${baseTestName}_%02d".format(i)
            val seed = baseSeed + i

            // Set a unique seed for this game
            (randomizer as RandomizerDefault).seed = seed

            // Setup the game
            baseSetup()

            // Run the game and collect results
            runGame().collect { gameEvent ->
                println("Game $i event: $gameEvent")
                writeGameResults.update(testDir, testName)
            }
            summaries.add(generateGameSummary())

            // Complete the report for this game
            writeGameResults.finish(testDir, testName)

            println("Completed game $i with seed $seed")
        }
        writeGameSummaries(testDir, baseTestName + "_report_$numGames", summaries)
    }

    fun test10000() = runBlocking {
        // Arrange
        val numGames = 10000
        val baseTestName = "run_report_$numGames"
        val baseSeed = numGames.toLong()
        val summaries = mutableListOf<GameSummary>()

        // Run multiple games with different seeds
        for (i in 0 until numGames) {
            val seed = baseSeed + i
            val printFlag = i % 1000 == 0

            // Set a unique seed for this game
            (randomizer as RandomizerDefault).seed = seed

            // Setup the game
            baseSetup()

            // Run the game and collect results
            runGame().collect { gameEvent ->
                if (printFlag) {
                    println("Game $i event: $gameEvent")
                }
            }
            summaries.add(generateGameSummary())

            if (printFlag) {
                println("Completed game $i with seed $seed")
            }
        }
        writeGameSummaries(testDir, baseTestName, summaries)
    }

    // region Support

    private fun baseSetup() {
        grove.setup(marketConfig)
        var playerUnderTest = true
        game.setup(
            Game.Config(
                numPlayers = 2,
                dieFactory = dieFactory,
                setup = { _, player ->
                    if (playerUnderTest) {
                        playerUnderTest = false
                        player.setupInitialDeck(playerUnderTestSeedlings)
                    } else {
                        player.setupInitialDeck(otherSeedlings)
                    }
                },
            )
        )
    }

    fun getCards(type: FlourishType): GameCards {
        return gameCardsFactory(cardManager.getCardsByType(type)).sortByCost()
    }

    fun getCard(name: String): GameCard {
        return requireNotNull(cardManager.getCard(name)) { "$name not found" }
    }

    fun getMarketStackConfig(
        which: MarketStackID,
        cards: List<GameCard>
    ): MarketStackConfig {
        val stack = mutableListOf<MarketCardConfig>()
        for (card in cards) {
            stack.add(MarketCardConfig(card, 8))
        }
        return MarketStackConfig(which, stack)
    }

    fun marketBasicConfig(numPlayers: Int): MarketConfig {

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
            ),
            bonusDie = listOf(DieSides.D20, DieSides.D12, DieSides.D10, DieSides.D8, DieSides.D6, DieSides.D4).take(numPlayers)
        )
    }

    // endregion Support
} 
