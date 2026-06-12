package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class RerollDieUntilThreeOrHigherCultivation(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = (target as? ExecuteTarget.PlayerDie)?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.REROLL_TARGET_MISSING, card = card))
            return
        }
        if (!player.diceHand.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = player, type = WarningType.REROLL_DIE_NOT_FOUND, card = card))
            return
        }
        val rerolled = rerollUntilThreeOrHigher(targetDie) { die -> player.rerollDie(die) }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Rerolled a hand die until it was $MIN_REROLL_VALUE or higher",
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
