package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.table.Table

class RaiseDiePlus1AndGainWaterCultivation(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        table: Table,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDie = target?.dice?.firstDie
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
            amount = RAISE_AMOUNT
        ) ?: return
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Raised a die by $RAISE_AMOUNT",
                dice = Dice(listOf(raised))
            )
        )
        val token = table.grove.remove(Token.WATER) ?: return
        player.add(token)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Gained a water token from the Grove",
                token = token
            )
        )
    }

    private companion object {
        const val RAISE_AMOUNT = 1
    }
}
