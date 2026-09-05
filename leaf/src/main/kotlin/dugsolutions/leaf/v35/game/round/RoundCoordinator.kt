package dugsolutions.leaf.v35.game.round

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType

data class RoundReveal(
    val roundNumber: Int,
    val card: RoundCard
)

data class RoundExecution(
    val roundNumber: Int,
    val card: RoundCard
)

/** Coordinates reveal, type dispatch, and completion for one Round. */
class RoundCoordinator(
    private val cultivation: RoundExecutor,
    private val battle: RoundExecutor
) {

    /**
     * Reveals exactly one Round card and records the typed Chronicle event.
     *
     * This is the production reveal step used by [executeNext]. It is exposed
     * separately so deterministic integration scenarios can stop immediately
     * after reveal and inspect the real game state before any Round executor
     * performs Draw/Place/Action work.
     */
    fun revealNext(game: Game): RoundReveal? {
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

        return RoundReveal(
            roundNumber = roundNumber,
            card = card
        )
    }

    /**
     * Executes a card previously returned by [revealNext] and records Round
     * completion. The guards keep step-wise integration tests from executing a
     * stale reveal after the Game has advanced to another card.
     */
    fun executeRevealed(
        game: Game,
        reveal: RoundReveal
    ): RoundExecution {
        check(game.roundNumber == reveal.roundNumber) {
            "Cannot execute stale Round reveal ${reveal.roundNumber}; " +
                "game is at Round ${game.roundNumber}"
        }
        check(game.currentRound === reveal.card) {
            "Cannot execute a Round card that is not the currently revealed card: " +
                reveal.card.name
        }

        when (reveal.card.type) {
            RoundCardType.CULTIVATION -> cultivation.execute(game, reveal.card)
            RoundCardType.BATTLE -> battle.execute(game, reveal.card)
        }

        game.chronicle.record(
            Moment.RoundCompleted(
                roundNumber = reveal.roundNumber,
                cardName = reveal.card.name,
                cardType = reveal.card.type
            )
        )

        return RoundExecution(
            roundNumber = reveal.roundNumber,
            card = reveal.card
        )
    }

    fun executeNext(game: Game): RoundExecution? =
        revealNext(game)?.let { reveal ->
            executeRevealed(game, reveal)
        }
}
