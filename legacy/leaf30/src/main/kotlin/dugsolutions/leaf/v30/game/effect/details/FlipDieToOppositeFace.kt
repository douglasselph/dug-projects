package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice

class FlipDieToOppositeFace(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.FLIP_TARGET_MISSING, card = card))
            return
        }
        val foundDie = scope.findDie(targetDie)
        if (foundDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.FLIP_DIE_NOT_FOUND, card = card))
            return
        }
        val flippedValue = if (foundDie.sides > MIN_FLIPPABLE_SIDES) {
            (foundDie.sides + 1) - foundDie.value
        } else {
            foundDie.value
        }
        val flipped = scope.setValue(foundDie, flippedValue)
        if (flipped == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.FLIP_DIE_NOT_FOUND, card = card))
            return
        }
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Flipped one die in ${scope.locationDescription} to its opposite face",
                dice = Dice(listOf(flipped))
            )
        )
    }

    private companion object {
        const val MIN_FLIPPABLE_SIDES = 4
    }
}
