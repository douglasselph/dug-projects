package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class SetDieUpToD12ToMax(
    private val chronicle: Chronicle
) {

    private companion object {
        const val MAX_ALLOWED_SIDES = 12
    }

    operator fun invoke(
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ): Die? {
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return null
        }
        if (targetDie.sides > MAX_ALLOWED_SIDES) {
            chronicle(
                Moment.GameCardEffect(
                    player = scope.actingPlayer,
                    card = card,
                    effect = card.effect,
                    detail = "Ignored D${targetDie.sides}; only dice up to D$MAX_ALLOWED_SIDES can be set to max",
                    dice = Dice(listOf(targetDie))
                )
            )
            return null
        }
        if (!scope.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return null
        }

        val maxed = scope.setValue(targetDie, targetDie.sides) ?: return null
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Set one die in ${scope.locationDescription} to its maximum value",
                dice = Dice(listOf(maxed))
            )
        )
        return maxed
    }
}
