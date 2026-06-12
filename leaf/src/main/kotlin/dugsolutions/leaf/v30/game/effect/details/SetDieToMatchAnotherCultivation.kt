package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.domain.MainActionException
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.Die

class SetDieToMatchAnotherCultivation(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        player: Player,
        card: GameCard,
        target: ExecuteTarget?
    ) {
        val targetDice = (target as? ExecuteTarget.PlayerDie)?.dice?.diceInOrder.orEmpty()
        require(targetDice.size == TARGET_DICE_COUNT) {
            "Set die to match another requires exactly $TARGET_DICE_COUNT dice"
        }
        // Target dice order is significant: [0] is the source die to copy from, [1] is the die to change.
        val sourceRequest = targetDice[0]
        val targetRequest = targetDice[1]
        val hand = player.diceHand
        val sourceDie = findDie(hand, sourceRequest)
            ?: throw MainActionException("Set die source was not found in player hand")
        val targetDie = findDie(hand, targetRequest)
            ?: throw MainActionException("Set die target was not found in player hand")

        targetDie.adjustTo(sourceDie.value)
        player.diceHand = hand
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = card,
                effect = card.effect,
                detail = "Set one hand die to match another hand die",
                dice = Dice(listOf(sourceDie, targetDie))
            )
        )
    }

    private fun findDie(
        dice: Dice,
        target: Die
    ): Die? {
        for (index in 0 until dice.size) {
            val die = dice[index] ?: continue
            if (die == target) return die
        }
        return null
    }

    private companion object {
        const val TARGET_DICE_COUNT = 2
    }
}
