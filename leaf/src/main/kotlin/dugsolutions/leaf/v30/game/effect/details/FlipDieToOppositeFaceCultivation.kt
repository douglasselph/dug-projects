package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class FlipDieToOppositeFaceCultivation(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = (target as? ExecuteTarget.PlayerDie)?.dice?.firstDie ?: run {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_TARGET_MISSING, card = card))
            return
        }
        val hand = player.diceHand
        var flippedDie: Die? = null
        for (index in 0 until hand.size) {
            val die = hand[index] ?: continue
            if (die == targetDie) {
                flippedDie = die.flip()
                break
            }
        }
        if (flippedDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.FLIP_DIE_NOT_FOUND, card = card))
            return
        }
        player.diceHand = hand
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Flipped a hand die to its opposite face",
                dice = Dice(listOf(flippedDie))
            )
        )
    }
}
