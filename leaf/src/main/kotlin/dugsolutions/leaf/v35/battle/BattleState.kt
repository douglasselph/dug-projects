package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.BattleDiePlacement
import dugsolutions.leaf.v35.battle.domain.BattleGrid
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.Randomizer

/**
 * Mutable state that exists for one Battle Round.
 *
 * The ordered Players are the round's left-to-right Grid/Battle order. Player
 * and dice ownership remain in the normal Game/Player graph; [grid] adds only
 * Battle-specific location and committed-Critter state.
 */
class BattleState(
    playersInBattleOrder: List<Player>
) {
    val playersInBattleOrder: List<Player> =
        playersInBattleOrder.toList()

    init {
        require(this.playersInBattleOrder.size in 2..4) {
            "BattleState requires 2 to 4 players: ${this.playersInBattleOrder.size}"
        }
        require(
            this.playersInBattleOrder.map { it.id }.distinct().size ==
                this.playersInBattleOrder.size
        ) {
            "BattleState player IDs must be unique: ${this.playersInBattleOrder.map { it.id }}"
        }
    }

    val playerIdsInBattleOrder: List<PlayerId>
        get() = playersInBattleOrder.map { it.id }

    val grid: BattleGrid =
        BattleGrid(playerIdsInBattleOrder)

    fun player(playerId: PlayerId): Player =
        stateNotNull(
            playersInBattleOrder.firstOrNull { it.id == playerId },
            context = "BattleState"
        ) {
            "Player ${playerId.value} is not participating in this Battle Round"
        }

    /**
     * Performs only the initial dice-location portion of Rank and Place.
     * Gnomes/UI are intentionally absent from the simulator state.
     */
    fun placeInitialHands(): List<BattleDiePlacement> =
        playersInBattleOrder.flatMap { player ->
            grid.placeInitialHand(player)
        }

    companion object {
        /** Rank the supplied Game players and create fresh per-round Battle state. */
        fun create(
            players: List<Player>,
            randomizer: Randomizer
        ): BattleState =
            BattleState(
                BattleOrder.determine(
                    players = players,
                    randomizer = randomizer
                )
            )
    }
}
