package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.TrashResolver
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides

/** Immutable snapshot of one die Trashed by Doom. */
data class DoomedDie(
    val playerId: PlayerId,
    val row: StrikeRow,
    val sides: DieSides,
    val value: Int,
    val returnedToGraftBed: Boolean
)

data class DoomResult(
    val dice: List<DoomedDie>
) {
    val count: Int
        get() = dice.size

    val valuesTrashed: List<Int>
        get() = dice.map { it.value }.distinct()
}

/**
 * Executes Battle Step 7.
 *
 * Doom Trashes every die showing the lowest value. If that removed fewer than
 * two dice total, it continues with the next-lowest value, always taking the
 * complete value group. When fewer than two dice exist, it Trashes everything
 * available and stops.
 *
 * The universal v35 Trash rule applies: every Doomed die leaves the game.
 * D4 return is specific to Upgrade replacement, not Trash.
 */
class DoomResolver(
    private val trashResolver: TrashResolver = TrashResolver()
) {
    companion object {
        private const val MINIMUM_DOOMED_DICE = 2
    }

    fun execute(
        game: Game,
        battleState: BattleState
    ): DoomResult {
        val byValue =
            battleState.grid.diePlacements
                .groupBy { it.die.value }
                .toSortedMap()

        if (byValue.isEmpty()) {
            return DoomResult(emptyList())
        }

        val doomedPlacements = mutableListOf<dugsolutions.leaf.v35.battle.domain.BattleDiePlacement>()

        for ((_, placements) in byValue) {
            doomedPlacements += placements
            if (doomedPlacements.size >= MINIMUM_DOOMED_DICE) {
                break
            }
        }

        val results = doomedPlacements.map { placement ->
            val player = battleState.player(placement.playerId)
            val value = placement.die.value
            val sides = DieSides.from(placement.die.sides)

            val removed = battleState.grid.removeDie(placement.die)
            stateCheck(
                removed != null &&
                    removed.playerId == placement.playerId &&
                    removed.row == placement.row,
                context = "DoomResolver"
            ) {
                "Doom die lost its expected Grid placement: $placement"
            }

            val trash = trashResolver.trashDieFromHand(
                game = game,
                player = player,
                die = placement.die
            )

            DoomedDie(
                playerId = placement.playerId,
                row = placement.row,
                sides = sides,
                value = value,
                returnedToGraftBed = trash.returnedToGraftBed
            )
        }

        game.chronicle.record(
            Moment.Marker(
                "DOOM count=${results.size} values=" +
                    results.map { it.value }.distinct().joinToString(",")
            )
        )

        return DoomResult(results)
    }
}
