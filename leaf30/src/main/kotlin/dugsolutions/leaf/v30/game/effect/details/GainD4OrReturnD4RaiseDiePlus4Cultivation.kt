package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory

class GainD4OrReturnD4RaiseDiePlus4Cultivation(
    private val chronicle: Chronicle,
    private val dieFactory: DieFactory
) {

    private companion object {
        const val D4_SIDES = 4
        const val D4_MAX_VALUE = 4
        const val RAISE_AMOUNT = 4
    }

    operator fun invoke(
        grove: Grove,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDice = target?.dice?.diceInOrder.orEmpty()
        if (targetDice.isEmpty()) {
            gainD4(grove, player, card)
        } else {
            returnD4AndRaiseDie(grove, player, card, targetDice.first())
        }
    }

    private fun gainD4(
        grove: Grove,
        player: Player,
        card: GameCard
    ) {
        if (!grove.remove(DieSides.D4)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_UNAVAILABLE, card = card))
            return
        }
        val d4 = dieFactory(DieSides.D4).adjustTo(D4_MAX_VALUE)
        player.addDieToHand(d4)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Gained a D4 from the Grove into hand with value $D4_MAX_VALUE",
                dice = Dice(listOf(d4))
            )
        )
    }

    private fun returnD4AndRaiseDie(
        grove: Grove,
        player: Player,
        card: GameCard,
        targetDie: Die
    ) {
        val d4 = player.diceHand.dice
            .filter { it.sides == D4_SIDES }
            .minByOrNull { it.value }
        if (d4 == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return
        }
        if (!player.removeDieFromHand(d4)) {
            chronicle(Moment.Warning(player = player, type = WarningType.UPGRADE_DIE_NOT_FOUND, card = card))
            return
        }
        grove.add(DieSides.D4)
        val raised = player.raiseDie(targetDie, RAISE_AMOUNT)
        if (raised == null) {
            chronicle(Moment.Warning(player = player, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
            return
        }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Returned the lowest D4 from hand to the Grove and raised a hand die by $RAISE_AMOUNT",
                dice = Dice(listOf(d4, raised))
            )
        )
    }

}
