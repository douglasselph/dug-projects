package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class RaiseDiePlus1AndDoubleMatchingDiceCultivation(
    private val chronicle: Chronicle
) {

    operator fun invoke(
        player: Player,
        card: GameCard,
        target: Dice
    ) {
        val requested = target.dice
        if (requested.size !in 2..3) {
            warn(player, card)
            return
        }
        val hand = player.diceHand.dice.toMutableList()
        val dice = findMatchingDice(hand, requested)
        if (dice == null) {
            warn(player, card)
            return
        }

        // Target interpretation:
        // 2 dice matching: double both matching dice, then raise the first die by 1.
        // 2 dice not matching: raise the first die by 1, then both dice must match and are doubled.
        // 3 dice: first two dice must already match and are doubled; the third die is raised by 1.
        val success = when (dice.size) {
            2 -> resolveTwoDice(player, card, dice)
            3 -> resolveThreeDice(player, card, dice)
            else -> false
        }
        if (!success) return
        player.diceHand = Dice(hand)
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Raised one die by 1 and doubled matching dice in hand",
                dice = Dice(dice)
            )
        )
    }

    private fun resolveTwoDice(
        player: Player,
        card: GameCard,
        dice: List<Die>
    ): Boolean {
        val first = dice[0]
        val second = dice[1]
        if (first.value == second.value) {
            double(first)
            double(second)
            first.adjustBy(1)
            return true
        } else {
            first.adjustBy(1)
            if (first.value != second.value) {
                warn(player, card)
                return false
            }
            double(first)
            double(second)
            return true
        }
    }

    private fun resolveThreeDice(
        player: Player,
        card: GameCard,
        dice: List<Die>
    ): Boolean {
        val first = dice[0]
        val second = dice[1]
        val third = dice[2]
        if (first.value != second.value) {
            warn(player, card)
            return false
        }
        double(first)
        double(second)
        third.adjustBy(1)
        return true
    }

    private fun findMatchingDice(
        available: List<Die>,
        requested: List<Die>
    ): List<Die>? {
        val consumed = mutableSetOf<Int>()
        return requested.map { target ->
            val index = available.indices.firstOrNull { index ->
                index !in consumed && available[index] == target
            } ?: return null
            consumed.add(index)
            available[index]
        }
    }

    private fun double(die: Die) {
        die.adjustTo(die.value * 2)
    }

    private fun warn(
        player: Player,
        card: GameCard
    ) {
        chronicle(
            Moment.Warning(
                player = player,
                type = WarningType.DOUBLE_MATCHING_DICE_INVALID_TARGET,
                card = card
            )
        )
    }
}
