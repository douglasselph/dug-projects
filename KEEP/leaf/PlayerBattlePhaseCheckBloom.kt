package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.player.Player

class PlayerBattlePhaseCheckBloom(
    private val grove: Grove
) : PlayerBattlePhaseCheck {

    companion object {
        private const val BLOOM_COUNT = Commons.BLOOM_COUNT
    }
    override fun isReady(player: Player): Boolean {
        return player.bloomCount >= BLOOM_COUNT
    }

    override fun giftTo(player: Player) {
        val remainingBlooms = BLOOM_COUNT - player.bloomCount
        for (bloom in 1..remainingBlooms) {
            getBloomCard()?.let { cardId ->
                grove.removeCard(cardId)
                player.addCardToCompost(cardId)
            }
        }
    }

    private fun getBloomCard(): CardID? {
        val cards1 = grove.getCardsFor(MarketStackID.BLOOM_1)
        if (cards1 != null && !cards1.isEmpty()) {
            return cards1.cardIds[0]
        }
        val cards2 = grove.getCardsFor(MarketStackID.BLOOM_2)
        if (cards2 != null && !cards2.isEmpty()) {
            return cards2.cardIds[0]
        }
        val cards3 = grove.getCardsFor(MarketStackID.BLOOM_3)
        if (cards3 != null && !cards3.isEmpty()) {
            return cards3.cardIds[0]
        }
        return null
    }

}
