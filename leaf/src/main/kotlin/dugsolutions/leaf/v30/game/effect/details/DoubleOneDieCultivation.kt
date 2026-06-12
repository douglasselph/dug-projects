package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice

class DoubleOneDieCultivation(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = (target as? ExecuteTarget.PlayerDie)?.dice?.firstDie
        if (targetDie == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return
        }
        if (!player.diceHand.hasDie(targetDie)) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }
        val raised = player.raiseDie(
            die = targetDie,
            amount = targetDie.value
        ) ?: return
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Doubled one hand die",
                dice = Dice(listOf(raised))
            )
        )
    }
}
