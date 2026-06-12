package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice

class RaiseDiePlus1PerGraftedRootOrVine(
    private val chronicle: Chronicle
) {
    private companion object {
        val GRAFTED_TYPES = setOf(CardType.ROOT, CardType.VINE)
    }

    operator fun invoke(
        scope: DieEffectScope,
        player: Player,
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

        val amount = player.creatureCards.count { creatureCard -> creatureCard.card.type in GRAFTED_TYPES }
        val raised = scope.raise(targetDie, amount) ?: return
        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Raised one die in ${scope.locationDescription} by $amount for grafted roots and vines",
                dice = Dice(listOf(raised))
            )
        )
    }
}
