package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.Commons.HAND_SIZE
import dugsolutions.leaf.player.Player
import kotlin.math.max
import kotlin.math.min


fun Player.drawNewHand(preferredCardCount: Int) {
    val spaceLeft = HAND_SIZE - handSize
    if (spaceLeft <= 0) return

    // If supply is low, take everything from supply first
    if (cardsInSupplyCount + diceInSupplyCount < spaceLeft) {
        // Draw all remaining cards from supply
        repeat(cardsInSupplyCount) { drawCard() }
        // Draw all remaining dice from supply
        repeat(diceInSupplyCount) { drawDie() }

        // Calculate remaining space after taking all supply
        val remainingSpace = spaceLeft - (cardsInSupplyCount + diceInSupplyCount)
        if (remainingSpace <= 0) {
            return
        }
        // If we haven't met preferredCardCount, try to draw more cards
        val cardsStillNeeded = preferredCardCount - cardsInHand.size
        if (cardsStillNeeded > 0) {
            repeat(minOf(cardsStillNeeded, remainingSpace)) { drawCard() }
        }

        // Fill remaining space with dice
        val spaceLeftAfterCards = HAND_SIZE - handSize
        repeat(spaceLeftAfterCards) { drawDie() }
    } else {
        // Normal case - supply is plentiful
        val cardsLeft = max(0, preferredCardCount - cardsInHand.size)
        val cardsLeftToDraw = min(spaceLeft, cardsLeft)

        // Draw cards first
        repeat(cardsLeftToDraw) { drawCard() }
        // Then draw dice to fill remaining space
        val diceLeft = max(0, HAND_SIZE - handSize)
        repeat(diceLeft) { drawDie() }

        // If we still have space, then fill the remaining space with more cards.
        val spaceLeftAfterCardsAndDice = HAND_SIZE - handSize
        repeat(spaceLeftAfterCardsAndDice) { drawCard() }
    }
}
