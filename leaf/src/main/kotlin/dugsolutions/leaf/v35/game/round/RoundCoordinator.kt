package dugsolutions.leaf.v35.game.round

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

data class RoundExecution(
    val roundNumber: Int,
    val card: RoundCard
)

/** Coordinates reveal, type dispatch, and completion for one Round. */
class RoundCoordinator(
    private val cultivation: RoundExecutor,
    private val battle: RoundExecutor
) {

    fun executeNext(game: Game): RoundExecution? {
        val card = game.roundDeck.next() ?: return null
        val roundNumber = game.roundNumber

        game.chronicle.record(
            Moment.Marker(
                marker(
                    event = "ROUND_REVEALED",
                    roundNumber = roundNumber,
                    card = card
                )
            )
        )

        when (card.type) {
            RoundCardType.CULTIVATION -> cultivation.execute(game, card)
            RoundCardType.BATTLE -> battle.execute(game, card)
        }

        game.chronicle.record(
            Moment.Marker(
                marker(
                    event = "ROUND_COMPLETED",
                    roundNumber = roundNumber,
                    card = card
                )
            )
        )

        return RoundExecution(
            roundNumber = roundNumber,
            card = card
        )
    }

    private fun marker(
        event: String,
        roundNumber: Int,
        card: RoundCard
    ): String =
        "$event number=$roundNumber type=${card.type} card=${card.name}"
}
