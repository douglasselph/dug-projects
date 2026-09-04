package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.buy.BuyCoordinator
import dugsolutions.leaf.v35.game.buy.BuyPhaseResult
import dugsolutions.leaf.v35.game.operation.GraftResolver
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.round.RoundExecutor
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
        require(roundCard.type == RoundCardType.CULTIVATION) {
            "CultivationRound requires a Cultivation Round card: ${roundCard.type}"
        }

        val build = CultivationBuildCoordinator(
            rollResolver = RollResolver(
                grove = game.grove,
                chronicle = game.chronicle
            ),
            effectExecutor = effectExecutor
        ).execute(game, roundCard)

        val buy = BuyCoordinator(
            graftResolver = GraftResolver(game.chronicle),
            createDie = { sides -> game.dieFactory(sides) }
        ).execute(game)

        val cleanup = CultivationCleanupCoordinator(
            refreshResolver = RefreshResolver(game.chronicle)
        ).execute(game)

        return CultivationRoundResult(
            build = build,
            buy = buy,
            cleanup = cleanup
        )
    }
}
