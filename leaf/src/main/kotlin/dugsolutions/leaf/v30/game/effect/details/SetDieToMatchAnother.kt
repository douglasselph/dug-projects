package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice

class SetDieToMatchAnother(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDice = target?.dice?.diceInOrder.orEmpty()
        require(targetDice.size == TARGET_DICE_COUNT) {
            "Set die to match another requires exactly $TARGET_DICE_COUNT dice"
        }
        // Target dice order is significant: [0] is the source die to copy from, [1] is the die to change.
        val sourceDie = scope.findDie(targetDice[0])
            ?: throw MainActionException("Set die source was not found in ${scope.locationDescription}")
        val targetDie = scope.findDie(targetDice[1])
            ?: throw MainActionException("Set die target was not found in ${scope.locationDescription}")

        val changed = scope.setValue(targetDie, sourceDie.value)
            ?: throw MainActionException("Set die target was not found in ${scope.locationDescription}")
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Set one die in ${scope.locationDescription} to match another die",
                dice = Dice(listOf(sourceDie, changed))
            )
        )
    }

    private companion object {
        const val TARGET_DICE_COUNT = 2
    }
}
