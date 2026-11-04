package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupResult
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** One completely settled v38 Strike, independent of how it was triggered. */
data class FinishedStrike(
    val resolution: StrikeResolution,
    val doom: DoomResult,
    val cleanup: BattleCleanupResult
)

/**
 * The single v38 Resolve → Doom → Cleanup path for ordinary Engagements and
 * Wisp's Resolve. Only the Strike row is reclaimed; the final Battle Refresh
 * is deliberately left to the caller after all three rows are completed.
 */
class StrikeFinishCoordinator {
    fun finish(game: Game, battle: BattleState, row: StrikeRow,
               decisionContext: (Player) -> DecisionContext): FinishedStrike {
        stateCheck(!battle.grid.isRowClosed(row), context = "StrikeFinishCoordinator") {
            "Strike already finished: $row"
        }
        val resolution = StrikeResolver(WoundResolver(
            grove = game.grove, chronicle = game.chronicle,
            decisionContext = decisionContext
        )).resolveRow(game, battle, row)
        battle.lockResolvedRow(row, game.assetProvenance)
        val doom = DoomResolver().executeRow(game, battle, row)
        val cleanup = BattleCleanupCoordinator(RefreshResolver(game.chronicle))
            .executeRow(game, battle, row)
        return FinishedStrike(resolution, doom, cleanup).also {
            battle.recordFinishedStrike(row, it)
        }
    }
}
