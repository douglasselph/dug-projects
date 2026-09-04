package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.chronicle.Chronicle
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.round.domain.RoundCard

/**
 * Durable state graph for one isolated game.
 *
 * Game does not run itself. Future GameRunner / round coordinators execute
 * rules against this state and record outcomes through [chronicle].
 *
 * v35 deliberately has no separate Table aggregate. Grove + Players + the
 * other per-game state live directly here.
 */
class Game(
    val config: GameConfig,
    val grove: Grove,
    players: List<Player>,
    val chronicle: Chronicle,
    val roundDeck: RoundDeck,
    val randomizer: Randomizer
) {
    var status: GameStatus = GameStatus.READY
        private set

    val isComplete: Boolean
        get() = status == GameStatus.COMPLETE

    /**
     * Defensive structural view. The Player instances are the live mutable
     * player state for this Game.
     */
    val players: List<Player> =
        players.toList()

    /**
     * RoundDeck remains the single source of truth for the revealed card.
     */
    val currentRound: RoundCard?
        get() = roundDeck.top

    /**
     * Zero before the first reveal, then the 1-based number of the most
     * recently revealed Round.
     */
    val roundNumber: Int
        get() = config.roundSetup.totalRounds - roundDeck.remaining

    /**
     * Means the final configured card has been revealed. A future GameRunner
     * will own the stronger concept of "fully resolved and scored."
     */
    val hasRevealedFinalRound: Boolean
        get() = roundDeck.isEmpty

    internal fun start() {
        check(status == GameStatus.READY) {
            "Game can only start from READY: $status"
        }
        status = GameStatus.RUNNING
    }

    internal fun complete() {
        check(status == GameStatus.RUNNING) {
            "Game can only complete from RUNNING: $status"
        }
        status = GameStatus.COMPLETE
    }
}
