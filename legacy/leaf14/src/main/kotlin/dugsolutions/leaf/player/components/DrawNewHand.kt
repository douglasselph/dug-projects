package dugsolutions.leaf.player.components

import dugsolutions.leaf.common.Commons.HAND_SIZE
import dugsolutions.leaf.player.Player

class DrawNewHand {

    data class Result(
        val reshuffleNeeded: Boolean
    )

    operator fun invoke(player: Player, preferredCardCount: Int): Result {
        while (player.handSize < HAND_SIZE) {
            val needsMoreCards = player.cardsInHand.size < preferredCardCount
            
            if (needsMoreCards) {
                // Try to draw a card first
                val cardResult = player.drawCard()
                if (cardResult.reshuffleNeeded) {
                    return Result(reshuffleNeeded = true)
                }
                
                if (cardResult.cardId != null) {
                    // Successfully drew a card, continue
                    continue
                } else {
                    // No card available, try drawing a die instead
                    val dieResult = player.drawDie()
                    if (dieResult.reshuffleNeeded) {
                        return Result(reshuffleNeeded = true)
                    }
                    
                    if (dieResult.die == null) {
                        // Nothing left to draw
                        break
                    }
                    // Successfully drew a die, continue
                }
            } else {
                // Try to draw a die first
                val dieResult = player.drawDie()
                if (dieResult.reshuffleNeeded) {
                    return Result(reshuffleNeeded = true)
                }
                
                if (dieResult.die != null) {
                    // Successfully drew a die, continue
                    continue
                } else {
                    // No die available, try drawing a card instead
                    val cardResult = player.drawCard()
                    if (cardResult.reshuffleNeeded) {
                        return Result(reshuffleNeeded = true)
                    }
                    
                    if (cardResult.cardId == null) {
                        // Nothing left to draw
                        break
                    }
                    // Successfully drew a card, continue
                }
            }
        }
        return Result(reshuffleNeeded = false)
    }
}
