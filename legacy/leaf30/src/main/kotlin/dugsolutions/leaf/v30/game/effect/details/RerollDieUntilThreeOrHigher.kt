package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class RerollDieUntilThreeOrHigher(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = target?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.REROLL_TARGET_MISSING, card = card))
            return
        }
        if (!scope.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.REROLL_DIE_NOT_FOUND, card = card))
            return
        }
        val rerolled = rerollUntilThreeOrHigher(targetDie) { die -> scope.reroll(die) }
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Rerolled one die in ${scope.locationDescription} until it was $MIN_REROLL_VALUE or higher",
                dice = Dice(listOf(rerolled))
            )
        )
    }

    private fun rerollUntilThreeOrHigher(
        initial: Die,
        reroll: (Die) -> Die?
    ): Die {
        var current = initial
        repeat(MAX_REROLL_ATTEMPTS) {
            current = reroll(current) ?: throw MainActionException("Reroll target die was not found")
            if (current.value >= MIN_REROLL_VALUE) return current
        }
        throw MainActionException("Reroll did not reach $MIN_REROLL_VALUE after $MAX_REROLL_ATTEMPTS attempts")
    }

    private companion object {
        const val MIN_REROLL_VALUE = 3
        const val MAX_REROLL_ATTEMPTS = 10
    }
}
