package dugsolutions.leaf.v14.player.components

import dugsolutions.leaf.v14.cards.domain.CardID
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.domain.DrawCardResult
import dugsolutions.leaf.v14.player.domain.DrawDieResult
import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.common.Commons

class DrawNewHand {

    sealed class ResultInstance {
        data class WasCard(val result: DrawCardResult) : ResultInstance()
        data class WasDie(val result: DrawDieResult) : ResultInstance()
    }

    operator fun invoke(player: Player, preferredCardCount: Int): List<ResultInstance> {
        val result = mutableListOf<ResultInstance>()
        while (player.handSize < Commons.HAND_SIZE) {
            if (player.cardsInHand.isEmpty()) {
                if (!addTo(result, player.drawCard())) {
                    // Nothing left
                    break
                }
            } else {
                val preferredCardsLeftToDraw = preferredCardCount - player.cardsInHand.size
                if (preferredCardsLeftToDraw > 0) {
                    if (!addTo(result, player.drawCardWithoutResupply())) {
                        if (!addTo(result, player.drawDieWithoutResupply())) {
                            if (!addTo(result, player.drawCard())) {
                                // Nothing left
                                break
                            }
                        }
                    }
                } else {
                    if (!addTo(result, player.drawDie())) {
                        if (!addTo(result, player.drawCard())) {
                            // Nothing left
                            break
                        }
                    }
                }
            }
        }
        return result
    }

    private fun addTo(result: MutableList<ResultInstance>, cardId: CardID?): Boolean {
        cardId ?: return false
        result.add(ResultInstance.WasCard(DrawCardResult(cardId)))
        return true
    }

    private fun addTo(result: MutableList<ResultInstance>, cardResult: DrawCardResult): Boolean {
        cardResult.cardId ?: return false
        result.add(ResultInstance.WasCard(cardResult))
        return true
    }

    private fun addTo(result: MutableList<ResultInstance>, die: Die?): Boolean {
        die ?: return false
        result.add(ResultInstance.WasDie(DrawDieResult(die)))
        return true
    }

    private fun addTo(result: MutableList<ResultInstance>, dieResult: DrawDieResult): Boolean {
        dieResult.die ?: return false
        result.add(ResultInstance.WasDie(dieResult))
        return true
    }
}
