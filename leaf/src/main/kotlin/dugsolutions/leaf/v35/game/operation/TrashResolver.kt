package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.TrashDestination
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides

data class TrashDieResolution(
    val sides: DieSides,
    /** True if the Trashed die was a D4 returned to the Graft Bed. */
    val returnedToGraftBed: Boolean
)

/**
 * Universal v37.03 rule for Trashing a player-owned die from the Dice Hand.
 *
 * A Trashed D4 returns to the Graft Bed; all larger Trashed dice leave the
 * game. Upgrade replacement has its own distinct, non-Trash rule.
 *
 * Battle Grid dice are still logically in PlayerDice.hand, so Battle/Doom can
 * reuse this same rule after separately removing the die's Grid placement.
 */
class TrashResolver {

    fun trashDieFromHand(
        game: Game,
        player: Player,
        die: Die,
        reason: String = "TRASH"
    ): TrashDieResolution {
        val current =
            player.dice.hand.firstOrNull {
                it === die
            }

        stateCheck(current != null) {
            "Trash die is not in player Hand: $die"
        }

        val sides =
            DieSides.from(current.sides)

        stateCheck(
            player.dice.removeExactFromHand(
                current
            ) != null
        ) {
            "Validated Trash die could not be removed from Hand: $current"
        }

        player.dice.recordRemoval(current, reason)

        val returnedToGraftBed = sides == DieSides.D4
        if (returnedToGraftBed) {
            game.grove.graftBed.returnD4()
        }

        game.chronicle.record(
            Moment.TrashDie(
                playerId = player.id,
                sides = sides,
                destination = if (returnedToGraftBed) {
                    TrashDestination.GRAFT_BED
                } else {
                    TrashDestination.OUT_OF_GAME
                }
            )
        )

        return TrashDieResolution(
            sides = sides,
            returnedToGraftBed = returnedToGraftBed
        )
    }
}
