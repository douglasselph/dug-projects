package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides

data class TrashDieResolution(
    val sides: DieSides,
    val returnedToGraftBed: Boolean
)

/**
 * Universal v35 rule for Trashing a player-owned die from the Dice Hand.
 *
 * - A Trashed D4 returns to the D4 space on the shared Graft Bed.
 * - A Trashed D6/D8/D10/D12/D20 leaves the game.
 *
 * Battle Grid dice are still logically in PlayerDice.hand, so Battle/Doom can
 * reuse this same rule later while separately removing the die's grid
 * placement.
 */
class TrashResolver {

    fun trashDieFromHand(
        game: Game,
        player: Player,
        die: Die
    ): TrashDieResolution {
        val current =
            player.dice.hand.firstOrNull {
                it == die
            }

        check(current != null) {
            "Trash die is not in player Hand: $die"
        }

        val sides =
            DieSides.from(current.sides)

        check(
            player.dice.removeFromHand(
                current
            ) != null
        ) {
            "Validated Trash die could not be removed from Hand: $current"
        }

        val returnedToGraftBed =
            sides == DieSides.D4

        if (returnedToGraftBed) {
            game.grove.graftBed.returnD4()
        }

        game.chronicle.record(
            Moment.Marker(
                "TRASH_DIE player=${player.id.value} sides=$sides " +
                    "destination=" +
                    if (returnedToGraftBed) {
                        "GRAFT_BED"
                    } else {
                        "OUT_OF_GAME"
                    }
            )
        )

        return TrashDieResolution(
            sides = sides,
            returnedToGraftBed =
                returnedToGraftBed
        )
    }
}
