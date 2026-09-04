package dugsolutions.leaf.v35.game.round

import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.round.domain.RoundCard

/** Executes all rules for one already-revealed Round card. */
fun interface RoundExecutor {
    fun execute(
        game: Game,
        roundCard: RoundCard
    )
}
