package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.BattleStrikeResolutionResult
import dugsolutions.leaf.v35.battle.DoomResolver
import dugsolutions.leaf.v35.battle.DoomResult
import dugsolutions.leaf.v35.battle.StrikeResolver
import dugsolutions.leaf.v35.battle.domain.BattleDiePlacement
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
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

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
 * A fresh [BattleState] is created for every Battle Round and deliberately
 * discarded after cleanup. Player/Game state is durable; Grid state is not.
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
        require(roundCard.type == RoundCardType.BATTLE) {
            "BattleRound requires a Battle Round card: ${roundCard.type}"
        }

        /*
         * BattleState does not exist until after opening Draws establish the
         * Rank order. Capture it so immediate Wisps gained during later rolls
         * receive the live Grid state, while opening-draw immediate Wisps still
         * execute correctly before Rank and Place.
         */
        var currentBattleState: BattleState? = null

        val rollResolver = RollResolver(
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
                        battleState = currentBattleState
                    )
                )
            }
        )

        // Step 2 — Draw and Roll 3 for every player.
        val openingDrawCounts = linkedMapOf<PlayerId, Int>()
        game.players.forEach { player ->
            var count = 0
            repeat(3) {
                if (rollResolver.draw(player) != null) count++
            }
            openingDrawCounts[player.id] = count
            game.chronicle.record(
                Moment.Marker(
                    "BATTLE_OPENING_DRAW_COMPLETE player=${player.id.value} count=$count"
                )
            )
        }

        // Step 3 — Rank players, create the Grid, and place opening Hands.
        val battleState = BattleState.create(
            players = game.players,
            randomizer = game.randomizer
        )
        currentBattleState = battleState
        val initialPlacements = battleState.placeInitialHands()

        game.chronicle.record(
            Moment.Marker(
                "BATTLE_RANK_PLACE order=" +
                    battleState.playerIdsInBattleOrder.joinToString(",") { it.value.toString() } +
                    " dice=${initialPlacements.size}"
            )
        )

        val refreshResolver = RefreshResolver(game.chronicle)
        val supportActionExecutor = SupportActionExecutor(
            rollResolver = rollResolver,
            refreshResolver = refreshResolver,
            effectExecutor = effectExecutor
        )

        // Steps 4-5 — Main Actions and Support passes.
        val actions = BattleActionCoordinator(
            rollResolver = rollResolver,
            effectExecutor = effectExecutor,
            supportActionExecutor = supportActionExecutor
        ).execute(
            game = game,
            roundCard = roundCard,
            battleState = battleState
        )

        // Step 6 — normal remaining Strikes. Closed rows are skipped here.
        val strikes = StrikeResolver(
            woundResolver = WoundResolver(
                grove = game.grove,
                chronicle = game.chronicle
            )
        ).resolveAll(
            game = game,
            battleState = battleState
        )

        // Step 7 — Doom only the dice still present after Strike effects.
        val doom = DoomResolver().execute(
            game = game,
            battleState = battleState
        )

        // Step 8 — reclaim survivors, return Critters, reset round state, Refresh.
        val cleanup = BattleCleanupCoordinator(
            refreshResolver = refreshResolver
        ).execute(
            game = game,
            battleState = battleState
        )

        return BattleRoundResult(
            openingDrawCounts = openingDrawCounts.toMap(),
            battleOrder = battleState.playerIdsInBattleOrder,
            initialPlacements = initialPlacements.toList(),
            actions = actions,
            strikes = strikes,
            doom = doom,
            cleanup = cleanup
        )
    }
}
