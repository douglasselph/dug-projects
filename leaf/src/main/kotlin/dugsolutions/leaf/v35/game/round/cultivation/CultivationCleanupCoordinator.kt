package dugsolutions.leaf.v35.game.round.cultivation

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.player.PlayerId

data class CultivationPlayerCleanupResult(
    val playerId: PlayerId,
    val discardedDice: Int,
    val refreshed: Boolean
)

data class CultivationCleanupResult(
    val players: List<CultivationPlayerCleanupResult>
) {
    val totalDiscardedDice: Int
        get() = players.sumOf { it.discardedDice }

    val refreshedPlayers: List<PlayerId>
        get() = players.filter { it.refreshed }.map { it.playerId }
}

/** Executes Cultivation Step 5 cleanup for every player in seating order. */
class CultivationCleanupCoordinator(
    private val refreshResolver: RefreshResolver
) {

    fun execute(game: Game): CultivationCleanupResult {
        val results = game.players.map { player ->
            val discarded = player.dice.handSize
            player.dice.discardHand()

            val refreshed = refreshResolver.refreshIfReady(player)

            game.chronicle.record(
                Moment.Marker(
                    "CULTIVATION_CLEANUP player=${player.id.value} " +
                        "discarded=$discarded refreshed=$refreshed"
                )
            )

            CultivationPlayerCleanupResult(
                playerId = player.id,
                discardedDice = discarded,
                refreshed = refreshed
            )
        }

        return CultivationCleanupResult(
            players = results
        )
    }
}
