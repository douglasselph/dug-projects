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
import dugsolutions.leaf.v30.random.die.Die

class RaiseDiePlus2PerVine(
    private val chronicle: Chronicle
) {

    private companion object {
        const val RAISE_PER_VINE = 2
    }

    operator fun invoke(
        scope: DieEffectScope,
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ): List<Die> {
        val targetDice = target?.dice?.diceInOrder.orEmpty()
        if (targetDice.isEmpty()) {
            chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_TARGET_MISSING, card = card))
            return emptyList()
        }

        val vineCount = player.creatureCards.count { it.card.type == CardType.VINE }
        if (vineCount <= 0) return emptyList()

        val raisedDice = mutableListOf<Die>()
        val raisesByIndex = distributeRaises(vineCount, targetDice.size)
        targetDice.forEachIndexed { index, die ->
            val raises = raisesByIndex[index]
            if (raises <= 0) return@forEachIndexed
            if (!scope.hasDie(die, index)) {
                chronicle(Moment.Warning(player = scope.actingPlayer, type = WarningType.RAISE_DIE_NOT_FOUND, card = card))
                return@forEachIndexed
            }
            scope.raise(die, raises * RAISE_PER_VINE, index)?.let { raisedDice.add(it) }
        }
        if (raisedDice.isEmpty()) return emptyList()

        chronicle(
            Moment.GameCardEffect(
                player = scope.actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Raised ${raisedDice.size} dice in ${scope.locationDescription} by +$RAISE_PER_VINE per vine from $vineCount vines",
                dice = Dice(raisedDice)
            )
        )
        return raisedDice
    }

    private fun distributeRaises(
        vineCount: Int,
        diceCount: Int
    ): List<Int> {
        if (diceCount <= 0) return emptyList()
        val raises = MutableList(diceCount) { 0 }
        repeat(vineCount) { index ->
            // If there are more vine raises than target dice, dump all remaining raises into the last die.
            val targetIndex = index.coerceAtMost(diceCount - 1)
            raises[targetIndex]++
        }
        return raises
    }
}
