package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.buy.BuyCoordinator
import dugsolutions.leaf.v35.game.buy.BuyPhaseResult
import dugsolutions.leaf.v35.game.operation.GraftResolver
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.SupportActionExecutor
import dugsolutions.leaf.v35.player.decision.context.DecisionContextFactory
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

data class CultivationRoundResult(
    val build: CultivationBuildResult,
    val buy: BuyPhaseResult,
    val cleanup: CultivationCleanupResult
)

/** Executes a complete v35 Cultivation Round after its Round card is revealed. */
class CultivationRound(
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
    ): CultivationRoundResult {
        requireCultivation(roundCard)

        val openingDrawCounts = executeOpeningDraw(game)
        val actions = executeBuildActions(game, roundCard)
        val buy = executeBuy(game)
        val cleanup = executeCleanup(game)

        return CultivationRoundResult(
            build = CultivationBuildResult(
                openingDrawCounts = openingDrawCounts,
                actions = actions.actions,
                supportActions = actions.supportActions
            ),
            buy = buy,
            cleanup = cleanup
        )
    }

    /** Production Step 2 seam used by deterministic integration scenarios. */
    fun executeOpeningDraw(game: Game): Map<PlayerId, Int> =
        components(game).build.executeOpeningDraw(game)

    /** Production Step 3 seam used by deterministic integration scenarios. */
    fun executeBuildActions(
        game: Game,
        roundCard: RoundCard
    ): CultivationBuildActionsResult {
        requireCultivation(roundCard)
        return components(game).build.executeActions(game, roundCard)
    }

    /** Production Step 4 seam used by deterministic integration scenarios. */
    fun executeBuy(game: Game): BuyPhaseResult =
        BuyCoordinator(
            graftResolver = GraftResolver(
                chronicle = game.chronicle,
                decisionContext = { player -> DecisionContextFactory.create(game, player) }
            ),
            createDie = { sides -> game.dieFactory(sides) }
        ).execute(game)

    /** Production Step 5 seam used by deterministic integration scenarios. */
    fun executeCleanup(game: Game): CultivationCleanupResult =
        CultivationCleanupCoordinator(
            refreshResolver = RefreshResolver(game.chronicle)
        ).execute(game)

    private fun components(game: Game): Components {
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
                        phase = GameEffectPhase.CULTIVATION
                    )
                )
            },
            decisionContext = { player -> DecisionContextFactory.create(game, player) }
        )
        val refreshResolver = RefreshResolver(game.chronicle)
        val supportActionExecutor = SupportActionExecutor(
            rollResolver = rollResolver,
            refreshResolver = refreshResolver,
            effectExecutor = effectExecutor
        )
        return Components(
            build = CultivationBuildCoordinator(
                rollResolver = rollResolver,
                effectExecutor = effectExecutor,
                supportActionExecutor = supportActionExecutor
            )
        )
    }

    private fun requireCultivation(roundCard: RoundCard) {
        require(roundCard.type == RoundCardType.CULTIVATION) {
            "CultivationRound requires a Cultivation Round card: ${roundCard.type}"
        }
    }

    private data class Components(
        val build: CultivationBuildCoordinator
    )
}
