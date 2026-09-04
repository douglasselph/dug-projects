package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides

data class UpgradeResolution(
    val from: DieSides,
    val to: DieSides,
    val replacement: Die
)

/**
 * Shared Upgrade rules.
 *
 * This resolver owns the size ladder, Graft Bed availability, replacement of
 * the old die, D4 return, and the destination of the new die. Card handlers
 * remain responsible for interpreting special instructions such as "skip
 * missing sizes" or "use the gained die now" and for rolling that die when
 * appropriate.
 */
class UpgradeResolver {

    private enum class Destination {
        HAND,
        DISCARD
    }

    fun nextNormalStep(sides: DieSides): DieSides? =
        when (sides) {
            DieSides.D4 -> DieSides.D6
            DieSides.D6 -> DieSides.D8
            DieSides.D8 -> DieSides.D10
            DieSides.D10 -> DieSides.D12
            DieSides.D12 -> DieSides.D20
            DieSides.D20 -> null
        }

    fun largerSizes(sides: DieSides): List<DieSides> {
        val ladder = listOf(
            DieSides.D4,
            DieSides.D6,
            DieSides.D8,
            DieSides.D10,
            DieSides.D12,
            DieSides.D20
        )
        val index = ladder.indexOf(sides)
        return if (index < 0 || index == ladder.lastIndex) {
            emptyList()
        } else {
            ladder.drop(index + 1)
        }
    }

    /** Larger sizes that are currently available in the shared Graft Bed. */
    fun availableLargerSizes(
        game: Game,
        sides: DieSides
    ): List<DieSides> =
        largerSizes(sides).filter { game.grove.graftBed.has(it) }

    /**
     * Returns the Nth currently available larger size, where step 1 is the
     * first available larger size. Missing sizes are skipped.
     */
    fun availableStep(
        game: Game,
        sides: DieSides,
        step: Int
    ): DieSides? {
        require(step >= 1) { "Available Upgrade step must be >= 1: $step" }
        return availableLargerSizes(game, sides).getOrNull(step - 1)
    }

    fun canUpgradeNormalStep(
        game: Game,
        die: Die
    ): Boolean {
        val from = DieSides.from(die.sides)
        val to = nextNormalStep(from) ?: return false
        return game.grove.graftBed.has(to)
    }

    fun canUpgradeAvailableSteps(
        game: Game,
        die: Die,
        steps: Int
    ): Boolean =
        availableStep(
            game = game,
            sides = DieSides.from(die.sides),
            step = steps
        ) != null

    fun upgradeFromHandToDiscard(
        game: Game,
        player: Player,
        die: Die
    ): UpgradeResolution {
        val from = DieSides.from(die.sides)
        val to = stateNotNull(nextNormalStep(from)) {
            "D20 cannot be upgraded by a normal one-step Upgrade"
        }
        return upgradeFromHand(
            game = game,
            player = player,
            die = die,
            to = to,
            destination = Destination.DISCARD
        )
    }

    /**
     * Replaces a Hand die with a specific larger available size and leaves the
     * new die in Hand. The caller may then roll/use it according to the effect.
     */
    fun upgradeFromHandToHand(
        game: Game,
        player: Player,
        die: Die,
        to: DieSides
    ): UpgradeResolution =
        upgradeFromHand(
            game = game,
            player = player,
            die = die,
            to = to,
            destination = Destination.HAND
        )

    private fun upgradeFromHand(
        game: Game,
        player: Player,
        die: Die,
        to: DieSides,
        destination: Destination
    ): UpgradeResolution {
        val current = player.dice.hand.firstOrNull { it === die }
        stateCheck(current != null) {
            "Upgrade die is not in player Hand: $die"
        }

        val from = DieSides.from(current.sides)
        stateCheck(to in largerSizes(from)) {
            "Upgrade target must be larger than source: from=$from to=$to"
        }
        stateCheck(game.grove.graftBed.has(to)) {
            "Upgrade size is unavailable in Graft Bed: $to"
        }

        /* Validate every expected failure before mutating any game state. */
        stateCheck(player.dice.removeExactFromHand(current) != null) {
            "Validated Upgrade die could not be removed from Hand: $current"
        }
        stateCheck(game.grove.graftBed.take(to)) {
            "Validated Upgrade die became unavailable in Graft Bed: $to"
        }

        if (from == DieSides.D4) {
            game.grove.graftBed.returnD4()
        }

        val replacement = game.dieFactory(to)
        when (destination) {
            Destination.HAND -> player.dice.addToHand(replacement)
            Destination.DISCARD -> player.dice.addToDiscard(replacement)
        }

        game.chronicle.record(
            Moment.Marker(
                "UPGRADE player=${player.id.value} from=$from to=$to " +
                    "destination=${destination.name}"
            )
        )

        return UpgradeResolution(
            from = from,
            to = to,
            replacement = replacement
        )
    }
}
