package dugsolutions.leaf.v35.game.operation

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
 * Shared normal one-step Upgrade rule.
 *
 * This first API covers a die upgraded from Hand into Discard, which is the
 * normal Compost destination. Later effects that say "use now" or skip
 * unavailable sizes can extend this resolver without duplicating the D4-return
 * and Graft-Bed rules.
 */
class UpgradeResolver {

    fun nextNormalStep(sides: DieSides): DieSides? =
        when (sides) {
            DieSides.D4 -> DieSides.D6
            DieSides.D6 -> DieSides.D8
            DieSides.D8 -> DieSides.D10
            DieSides.D10 -> DieSides.D12
            DieSides.D12 -> DieSides.D20
            DieSides.D20 -> null
        }

    fun canUpgradeNormalStep(
        game: Game,
        die: Die
    ): Boolean {
        val from = DieSides.from(die.sides)
        val to = nextNormalStep(from) ?: return false
        return game.grove.graftBed.has(to)
    }

    fun upgradeFromHandToDiscard(
        game: Game,
        player: Player,
        die: Die
    ): UpgradeResolution {
        val current = player.dice.hand.firstOrNull { it == die }
        check(current != null) {
            "Upgrade die is not in player Hand: $die"
        }

        val from = DieSides.from(current.sides)
        val to = checkNotNull(nextNormalStep(from)) {
            "D20 cannot be upgraded by a normal one-step Upgrade"
        }
        check(game.grove.graftBed.has(to)) {
            "Next Upgrade size is unavailable in Graft Bed: $to"
        }

        /*
         * All expected failures have been validated above. The following
         * mutations are therefore checked invariants rather than decision-time
         * branches.
         */
        check(player.dice.removeFromHand(current) != null) {
            "Validated Upgrade die could not be removed from Hand: $current"
        }
        check(game.grove.graftBed.take(to)) {
            "Validated Upgrade die became unavailable in Graft Bed: $to"
        }

        if (from == DieSides.D4) {
            game.grove.graftBed.returnD4()
        }

        val replacement = game.dieFactory(to)
        player.dice.addToDiscard(replacement)

        game.chronicle.record(
            Moment.Marker(
                "UPGRADE player=${player.id.value} from=$from to=$to destination=DISCARD"
            )
        )

        return UpgradeResolution(
            from = from,
            to = to,
            replacement = replacement
        )
    }
}
