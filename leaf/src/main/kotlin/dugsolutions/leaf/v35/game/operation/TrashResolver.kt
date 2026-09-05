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
    /** Compatibility/result flag; v35 Trash never returns a die to the Graft Bed. */
    val returnedToGraftBed: Boolean
)

/**
 * Universal v35 rule for Trashing a player-owned die from the Dice Hand.
 *
 * Trash removes the die from the game regardless of size. In particular, a
 * Trashed D4 does NOT return to the Graft Bed. Returning a replaced D4 is an
 * Upgrade-specific rule owned by UpgradeResolver.
 *
 * Battle Grid dice are still logically in PlayerDice.hand, so Battle/Doom can
 * reuse this same rule after separately removing the die's Grid placement.
 */
class TrashResolver {

    fun trashDieFromHand(
        game: Game,
        player: Player,
        die: Die
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

        game.chronicle.record(
            Moment.TrashDie(
                playerId = player.id,
                sides = sides,
                destination = TrashDestination.OUT_OF_GAME
            )
        )

        return TrashDieResolution(
            sides = sides,
            returnedToGraftBed = false
        )
    }
}
