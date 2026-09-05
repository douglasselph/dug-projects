package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.BattleStrikeResolutionResult
import dugsolutions.leaf.v35.battle.DoomResult
import dugsolutions.leaf.v35.battle.domain.BattleCritterPlacement
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecution
import dugsolutions.leaf.v35.game.round.RoundReveal
import dugsolutions.leaf.v35.game.round.battle.BattleActionLoopResult
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupResult
import dugsolutions.leaf.v35.game.round.battle.BattleRankAndPlaceResult
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.game.round.cultivation.CultivationBuildActionsResult
import dugsolutions.leaf.v35.game.round.cultivation.CultivationCleanupResult
import dugsolutions.leaf.v35.game.round.cultivation.CultivationRound
import dugsolutions.leaf.v35.game.buy.BuyPhaseResult
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.tokens.Critter
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

    val cultivationRound: CultivationRound =
        koin.get()

    val battleRound: BattleRound =
        koin.get()

    private val exactRoundCards =
        scenario.exactRoundNames?.map(catalog::requireRound)

    private val exactWispCards =
        scenario.exactWispNames?.map(catalog::requireWisp)

    /** Randomizer actually owned by this Game. */
    val randomizer: Randomizer

    private var pendingReveal: RoundReveal? = null
    private var activeBattleState: BattleState? = null

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
    fun runNextRound(): RoundExecution? {
        check(pendingReveal == null) {
            "A Round has already been revealed but not executed: ${pendingReveal?.card?.name}"
        }
        return roundCoordinator.executeNext(game)
    }

    /**
     * Executes only the production reveal step. No Draw, action, Strike, Doom,
     * or Cleanup work occurs until [executeRevealedRound] is called.
     */
    fun revealNextRound(): RoundReveal? {
        check(pendingReveal == null) {
            "A Round has already been revealed but not executed: ${pendingReveal?.card?.name}"
        }
        return roundCoordinator.revealNext(game).also { reveal ->
            pendingReveal = reveal
        }
    }

    /** Completes the Round most recently returned by [revealNextRound]. */
    fun executeRevealedRound(): RoundExecution {
        val reveal = checkNotNull(pendingReveal) {
            "No revealed Round is pending execution"
        }
        return roundCoordinator.executeRevealed(game, reveal).also {
            pendingReveal = null
        }
    }


    /** Executes only Cultivation Step 2 against the currently revealed card. */
    fun runCultivationOpeningDraw(): Map<PlayerId, Int> {
        requirePendingCultivationReveal()
        return cultivationRound.executeOpeningDraw(game)
    }

    /** Executes only Cultivation Step 3 against the currently revealed card. */
    fun runCultivationBuildActions(): CultivationBuildActionsResult {
        val reveal = requirePendingCultivationReveal()
        return cultivationRound.executeBuildActions(game, reveal.card)
    }

    /** Executes only Cultivation Step 4 against the current game state. */
    fun runCultivationBuy(): BuyPhaseResult {
        requirePendingCultivationReveal()
        return cultivationRound.executeBuy(game)
    }

    /** Executes only Cultivation Step 5 against the current game state. */
    fun runCultivationCleanup(): CultivationCleanupResult {
        requirePendingCultivationReveal()
        return cultivationRound.executeCleanup(game)
    }

    private fun requirePendingCultivationReveal(): RoundReveal {
        val reveal = checkNotNull(pendingReveal) {
            "A Cultivation Round must be revealed before executing a Cultivation step"
        }
        check(reveal.card.type == RoundCardType.CULTIVATION) {
            "Current revealed Round is not Cultivation: ${reveal.card.type}"
        }
        return reveal
    }

    /** Executes only Battle Step 2 against the currently revealed card. */
    fun runBattleOpeningDraw(): Map<PlayerId, Int> {
        requirePendingBattleReveal()
        check(activeBattleState == null) {
            "Battle Rank/Place has already created live Battle state"
        }
        return battleRound.executeOpeningDraw(game)
    }

    /** Executes only Battle Step 3 and retains the transient BattleState. */
    fun runBattleRankAndPlace(): BattleRankAndPlaceResult {
        requirePendingBattleReveal()
        check(activeBattleState == null) {
            "Battle Rank/Place has already been executed"
        }
        return battleRound.executeRankAndPlace(game).also {
            activeBattleState = it.battleState
        }
    }

    /** Executes Battle Steps 4-5 against the retained Battle state. */
    fun runBattleActions(): BattleActionLoopResult {
        val reveal = requirePendingBattleReveal()
        return battleRound.executeActions(
            game = game,
            roundCard = reveal.card,
            battleState = requireActiveBattleState()
        )
    }

    /** Executes Battle Step 6 against the retained Battle state. */
    fun runBattleStrikes(): BattleStrikeResolutionResult {
        requirePendingBattleReveal()
        return battleRound.executeStrikes(game, requireActiveBattleState())
    }

    /** Executes Battle Step 7 against the retained Battle state. */
    fun runBattleDoom(): DoomResult {
        requirePendingBattleReveal()
        return battleRound.executeDoom(game, requireActiveBattleState())
    }

    /** Executes Battle Step 8 and discards the harness's transient Battle state. */
    fun runBattleCleanup(): BattleCleanupResult {
        requirePendingBattleReveal()
        val state = requireActiveBattleState()
        return battleRound.executeCleanup(game, state).also {
            activeBattleState = null
        }
    }

    /** Immutable snapshot of the currently active stepwise Battle Grid. */
    fun battleSnapshot(): BattleSnapshot =
        BattleSnapshot.capture(requireActiveBattleState())

    fun battleSnapshotOrNull(): BattleSnapshot? =
        activeBattleState?.let(BattleSnapshot::capture)

    /** Test-state setup helper: commit one owned Critter to the live Battle Grid. */
    fun placeBattleCritterForSetup(
        playerId: Int,
        row: StrikeRow,
        critter: Critter
    ): BattleCritterPlacement {
        val player = game.players.single { it.id.value == playerId }
        return requireActiveBattleState().grid.placeCritter(
            player = player,
            row = row,
            critter = critter
        )
    }

    private fun requirePendingBattleReveal(): RoundReveal {
        val reveal = checkNotNull(pendingReveal) {
            "A Battle Round must be revealed before executing a Battle step"
        }
        check(reveal.card.type == RoundCardType.BATTLE) {
            "Current revealed Round is not Battle: ${reveal.card.type}"
        }
        return reveal
    }

    private fun requireActiveBattleState(): BattleState =
        checkNotNull(activeBattleState) {
            "Battle Rank/Place must be executed before this Battle step"
        }

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
