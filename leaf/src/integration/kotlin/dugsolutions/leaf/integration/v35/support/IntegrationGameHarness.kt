package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecution
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Path

/**
 * Real-engine integration fixture for v35.
 *
 * The harness uses the same production Koin modules and GameFactory as the
 * application. It only adds test orchestration around that graph: CSV loading,
 * scenario construction, snapshots, and Chronicle access. Rules are never
 * mocked or reimplemented here.
 */
class IntegrationGameHarness(
    val scenario: GameScenario = GameScenario(),
    dataRoot: Path = IntegrationCatalog.defaultDataRoot()
) : AutoCloseable {

    private val application =
        koinApplication {
            modules(appModules)
        }

    private val koin = application.koin

    val catalog: IntegrationCatalog =
        IntegrationCatalog(
            plantRegistry = koin.get<PlantCardRegistry>(),
            plantManager = koin.get<PlantCardManager>(),
            wispRegistry = koin.get<WispCardRegistry>(),
            wispManager = koin.get<WispCardManager>(),
            roundRegistry = koin.get<RoundCardRegistry>(),
            roundManager = koin.get<RoundCardManager>(),
            dataRoot = dataRoot
        ).load()

    private val gameFactory: GameFactory =
        koin.get()

    val roundCoordinator: RoundCoordinator =
        koin.get()

    val gameRunner: GameRunner =
        koin.get()

    private val exactRoundCards =
        scenario.exactRoundNames?.map(catalog::requireRound)

    private val exactWispCards =
        scenario.exactWispNames?.map(catalog::requireWisp)

    /** Randomizer actually owned by this Game. */
    val randomizer: Randomizer

    val game: Game =
        scenario.randomizerFactory?.invoke()?.let { scripted ->
            gameFactory(
                config = scenario.toGameConfig(catalog),
                randomizer = scripted,
                exactRoundCards = exactRoundCards,
                exactWispCards = exactWispCards
            )
        } ?: gameFactory(
            config = scenario.toGameConfig(catalog),
            exactRoundCards = exactRoundCards,
            exactWispCards = exactWispCards
        )

    init {
        randomizer = game.randomizer
    }

    /**
     * Executes one production Round through RoundCoordinator.
     *
     * This targeted helper intentionally does not change GameStatus; status
     * ownership remains with GameRunner. Use a fresh harness when calling
     * [runGame] after round-by-round assertions.
     */
    fun runNextRound(): RoundExecution? =
        roundCoordinator.executeNext(game)

    /** Runs a fresh scenario to completion through the production GameRunner. */
    fun runGame(): GameRunResult {
        check(game.status == GameStatus.READY && game.roundNumber == 0) {
            "runGame() requires a fresh harness. " +
                "Current status=${game.status}, roundNumber=${game.roundNumber}"
        }
        return gameRunner.run(game)
    }

    fun snapshot(): GameSnapshot =
        GameSnapshot.capture(game)

    fun chronicleEntries(): List<GameEntry> =
        game.chronicle.entries

    override fun close() {
        application.close()
    }
}
