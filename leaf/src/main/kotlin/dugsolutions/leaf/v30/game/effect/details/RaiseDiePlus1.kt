package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class RaiseDiePlus1(
    private val chronicle: Chronicle
) {
    private companion object {
        const val RAISE_AMOUNT = 1
    }

    operator fun invoke(
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ): List<Die> {
        val targetDice = target?.dice?.diceInOrder.orEmpty()
        if (targetDice.isEmpty()) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return emptyList()
        }

        val raisedDice = targetDice.mapIndexedNotNull { index, die ->
            if (!scope.hasDie(die, index)) {
                chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
                null
            } else {
                scope.raise(die, RAISE_AMOUNT, index)
            }
        }
        if (raisedDice.isEmpty()) return emptyList()

        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Raised ${raisedDice.size} dice in ${scope.locationDescription} by $RAISE_AMOUNT",
                dice = Dice(raisedDice)
            )
        )
        return raisedDice
    }
}
