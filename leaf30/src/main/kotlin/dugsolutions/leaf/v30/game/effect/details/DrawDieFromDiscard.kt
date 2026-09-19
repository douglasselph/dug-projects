package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class DrawDieFromDiscard(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        player: Player,
        card: GameCard,
        placeDie: (Die) -> Boolean
    ): Die? {
        val die = player.diceDiscard.dice.maxByOrNull { it.sides }
        if (die == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return null
        }
        if (!player.removeDieFromDiscard(die)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return null
        }

        die.roll()
        if (!placeDie(die)) {
            player.addDieToDiscard(die)
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, card = card))
            return null
        }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Drew the highest-sided die from discard and used it now",
                dice = Dice(listOf(die))
            )
        )
        return die
    }
}
