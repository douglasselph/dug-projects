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
            Moment.RoundRevealed(
                roundNumber = roundNumber,
                cardName = card.name,
                cardType = card.type,
                firstEffect = card.firstEffect.effect,
                secondEffect = card.secondEffect.effect
            )
        )

        when (card.type) {
            RoundCardType.CULTIVATION -> cultivation.execute(game, card)
            RoundCardType.BATTLE -> battle.execute(game, card)
        }

        game.chronicle.record(
            Moment.RoundCompleted(
                roundNumber = roundNumber,
                cardName = card.name,
                cardType = card.type
            )
        )

        return RoundExecution(
            roundNumber = roundNumber,
            card = card
        )
    }

}
