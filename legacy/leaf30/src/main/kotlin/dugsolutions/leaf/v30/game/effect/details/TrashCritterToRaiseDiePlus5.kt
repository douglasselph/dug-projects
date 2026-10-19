package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class TrashCritterToRaiseDiePlus5(
    private val chronicle: Chronicle
) {

    private companion object {
        const val RAISE_AMOUNT = 5
    }

    operator fun invoke(
        scope: DieEffectScope,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ): Die? {
        val critter = target?.critter?.firstOrNull()
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return null
        }
        if (!scope.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return null
        }
        if (critter == null || !player.removeCritter(critter)) {
            chronicle(
                Moment.GameCardEffect(
                    player = scope.actingPlayer,
                    card = card,
                    effect = card.effect,
                    detail = "Could not trash the requested critter to raise a die in ${scope.locationDescription}",
                    dice = Dice(listOf(targetDie)),
                    critter = critter
                )
            )
            return null
        }

        val raised = scope.raise(targetDie, RAISE_AMOUNT) ?: return null
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Trashed $critter to raise a die in ${scope.locationDescription} by $RAISE_AMOUNT",
                dice = Dice(listOf(raised)),
                critter = critter
            )
        )
        return raised
    }
}
