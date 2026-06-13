package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.common.Token
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.table.Table

class RaiseDiePlus1AndGainWater(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        table: Table,
        player: Player,
        scope: DieEffectScope,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val raisedDice = RaiseDiePlus1(chronicle)(
            scope = scope,
            card = card,
            target = target
        )
        if (raisedDice.isEmpty()) return

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
}
