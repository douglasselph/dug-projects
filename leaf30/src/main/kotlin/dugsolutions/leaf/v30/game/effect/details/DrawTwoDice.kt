package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class DrawTwoDice(
    private val chronicle: Chronicle
) {
    private companion object {
        const val NUM_DICE = 2
    }

    operator fun invoke(
        player: Player,
        card: GameCard,
        placeDie: (index: Int, die: Die) -> Boolean
    ): List<Die> {
        val placed = mutableListOf<Die>()
        repeat(NUM_DICE) { index ->
            val die = player.drawDie()
            if (die == null) {
                chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
                return@repeat
            }
            if (placeDie(index, die)) {
                placed.add(die)
            } else {
                chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, card = card))
            }
        }
        if (placed.isNotEmpty()) {
            chronicle(
                Moment.GameCardEffect(
                    player = player,
                    card = card,
                    effect = card.effect,
                    detail = "Drew ${placed.size} dice",
                    dice = Dice(placed)
                )
            )
        }
        return placed
    }
}
