package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/** Task 2: game-local state must not be retained by application-wide services. */
class GameIsolationLifecycleTest {
    @Test
    fun sameContainerAndRunner_executeFreshGamesWithoutSharingState() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val plantRegistry = koin.get<PlantCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
            }
            val wispRegistry = koin.get<WispCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
            }
            val roundRegistry = koin.get<RoundCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
            }
            koin.get<PlantCardManager>().loadCards(plantRegistry)
            koin.get<WispCardManager>().loadCards(wispRegistry)
            koin.get<RoundCardManager>().loadCards(roundRegistry)

            val plants = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantRegistry.getCard(it)) }
            val config = GameConfig.baseline(
                selectedPlantCards = plants,
                numPlayers = 2,
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 1, battleRounds = 1),
                seed = 12345L
            )
            val factory = koin.get<GameFactory>()
            val runner = koin.get<GameRunner>()
            val first = factory(config)
            val second = factory(config)
            assertNotSame(first.grove, second.grove)
            assertNotSame(first.grove.sharedTokenEconomy, second.grove.sharedTokenEconomy)
            assertNotSame(first.chronicle, second.chronicle)
            assertNotSame(first.roundDeck, second.roundDeck)
            assertNotSame(first.randomizer, second.randomizer)
            assertNotSame(first.dieFactory, second.dieFactory)
            first.players.zip(second.players).forEach { (a, b) -> assertNotSame(a, b) }
            assertTrue(second.chronicle.entries.isEmpty())
            val firstResult = runner.run(first)
            assertTrue(second.chronicle.entries.isEmpty(), "Playing first game changed second game chronicle")
            val secondResult = runner.run(second)
            assertEquals(firstResult, secondResult, "Same seeds and configs must replay independently")
            assertEquals(first.chronicle.entries, second.chronicle.entries)
            assertTrue(first.isComplete && second.isComplete)

            // Different seeded games share the application services but not a live graph.
            repeat(12) { index ->
                val independent = factory(GameConfig.baseline(
                    selectedPlantCards = plants,
                    numPlayers = 2,
                    roundSetup = GameRoundSetup.Ordered(cultivationRounds = 1, battleRounds = 1),
                    seed = 60000L + index
                ))
                runner.run(independent)
                assertTrue(independent.isComplete)
            }
        } finally {
            app.close()
        }
    }
}
