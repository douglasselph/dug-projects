package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.BattleStrikeResolutionResult
import dugsolutions.leaf.v35.battle.DoomResolver
import dugsolutions.leaf.v35.battle.DoomResult
import dugsolutions.leaf.v35.battle.StrikeResolver
import dugsolutions.leaf.v35.battle.domain.BattleDiePlacement
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.DecisionContextFactory
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

/** Immutable result of Battle Step 3, plus the live state used by later steps. */
data class BattleRankAndPlaceResult(
    val battleState: BattleState,
    val battleOrder: List<PlayerId>,
    val initialPlacements: List<BattleDiePlacement>
)

/** Immutable result of one complete Battle Round. */
data class BattleRoundResult(
    val openingDrawCounts: Map<PlayerId, Int>,
    val battleOrder: List<PlayerId>,
    val initialPlacements: List<BattleDiePlacement>,
    val actions: BattleActionLoopResult,
    val strikes: BattleStrikeResolutionResult,
    val doom: DoomResult,
    val cleanup: BattleCleanupResult
)

/**
 * Executes a complete v35 Battle Round after its Round card has been revealed.
 *
 * RoundCoordinator owns Step 1 (Reveal). This executor owns the remaining
 * Battle sequence:
 *
 * 2. Draw and Roll 3
 * 3. Rank and Place
 * 4-5. First Main, then Support/final Main loop
 * 6. Resolve open Strikes top-to-bottom
 * 7. Doom
 * 8. Cleanup
 *
 * The public step methods are production orchestration seams used by the
 * deterministic integration harness. [executeRound] composes those same
 * methods, so tests never run a parallel implementation of Battle rules.
 */
class BattleRound(
    private val effectExecutor: GameEffectExecutor
) : RoundExecutor {

    override fun execute(
        game: Game,
        roundCard: RoundCard
    ) {
        executeRound(game, roundCard)
    }

    fun executeRound(
        game: Game,
        roundCard: RoundCard
    ): BattleRoundResult {
        requireBattleCard(roundCard)

        val openingDrawCounts = executeOpeningDraw(game)
        val rank = executeRankAndPlace(game)
        val actions = executeActions(game, roundCard, rank.battleState)
        val strikes = executeStrikes(game, rank.battleState)
        val doom = executeDoom(game, rank.battleState)
        val cleanup = executeCleanup(game, rank.battleState)

        return BattleRoundResult(
            openingDrawCounts = openingDrawCounts,
            battleOrder = rank.battleOrder,
            initialPlacements = rank.initialPlacements,
            actions = actions,
            strikes = strikes,
            doom = doom,
            cleanup = cleanup
        )
    }

    /** Battle Step 2 — each player Draws and Rolls 3. */
    fun executeOpeningDraw(game: Game): Map<PlayerId, Int> {
        val rollResolver = createRollResolver(game, battleState = null)
        val openingDrawCounts = linkedMapOf<PlayerId, Int>()

        game.players.forEach { player ->
            var count = 0
            repeat(3) {
                if (rollResolver.draw(player) != null) count++
            }
            openingDrawCounts[player.id] = count
            game.chronicle.record(
                Moment.OpeningDrawCompleted(
                    phase = ChroniclePhase.BATTLE,
                    playerId = player.id,
                    count = count
                )
            )
        }

        return openingDrawCounts.toMap()
    }

    /** Battle Step 3 — establish Battle order and place current Hands. */
    fun executeRankAndPlace(game: Game): BattleRankAndPlaceResult {
        val battleState = BattleState.create(
            players = game.players,
            randomizer = game.randomizer
        )
        val placements = battleState.placeInitialHands()

        game.chronicle.record(
            Moment.BattleOrder(
                order = battleState.playerIdsInBattleOrder,
                initialDiceCount = placements.size
            )
        )

        return BattleRankAndPlaceResult(
            battleState = battleState,
            battleOrder = battleState.playerIdsInBattleOrder,
            initialPlacements = placements.toList()
        )
    }

    /** Battle Steps 4-5 — First Main, then Support/final Main passes. */
    fun executeActions(
        game: Game,
        roundCard: RoundCard,
        battleState: BattleState
    ): BattleActionLoopResult {
        requireBattleCard(roundCard)

        val rollResolver = createRollResolver(game, battleState)
        val refreshResolver = RefreshResolver(game.chronicle)
        val supportActionExecutor = SupportActionExecutor(
            rollResolver = rollResolver,
            refreshResolver = refreshResolver,
            effectExecutor = effectExecutor
        )

        return BattleActionCoordinator(
            rollResolver = rollResolver,
            effectExecutor = effectExecutor,
            supportActionExecutor = supportActionExecutor
        ).execute(
            game = game,
            roundCard = roundCard,
            battleState = battleState
        )
    }

    /** Battle Step 6 — resolve every currently open Strike top-to-bottom. */
    fun executeStrikes(
        game: Game,
        battleState: BattleState
    ): BattleStrikeResolutionResult =
        StrikeResolver(
            woundResolver = WoundResolver(
                grove = game.grove,
                chronicle = game.chronicle,
                decisionContext = { player ->
                    DecisionContextFactory.create(game, player, battleState)
                }
            )
        ).resolveAll(
            game = game,
            battleState = battleState
        )

    /** Battle Step 7 — Doom the lowest value groups until at least two dice die. */
    fun executeDoom(
        game: Game,
        battleState: BattleState
    ): DoomResult =
        DoomResolver().execute(
            game = game,
            battleState = battleState
        )

    /** Battle Step 8 — reclaim surviving dice/Critters and Refresh if ready. */
    fun executeCleanup(
        game: Game,
        battleState: BattleState
    ): BattleCleanupResult =
        BattleCleanupCoordinator(
            refreshResolver = RefreshResolver(game.chronicle)
        ).execute(
            game = game,
            battleState = battleState
        )

    private fun createRollResolver(
        game: Game,
        battleState: BattleState?
    ): RollResolver =
        RollResolver(
            grove = game.grove,
            chronicle = game.chronicle,
            immediateWispHandler = { player, card ->
                effectExecutor.execute(
                    GameEffectRequest(
                        game = game,
                        actor = player,
                        effect = card.effect,
                        source = GameEffectSource.Wisp(card),
                        phase = GameEffectPhase.BATTLE,
                        battleState = battleState
                    )
                )
            },
            decisionContext = { player ->
                DecisionContextFactory.create(game, player, battleState)
            }
        )

    private fun requireBattleCard(roundCard: RoundCard) {
        require(roundCard.type == RoundCardType.BATTLE) {
            "BattleRound requires a Battle Round card: ${roundCard.type}"
        }
    }
}
