package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice

class DoubleOneDie(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return
        }
        if (!scope.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }
        val raised = scope.raise(
            die = targetDie,
            amount = targetDie.value
        ) ?: return
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Doubled one die in ${scope.locationDescription}",
                dice = Dice(listOf(raised))
            )
        )
    }
}
