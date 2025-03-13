package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.common.Commons.BLOOM_COUNT
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.purchase.ManagePurchasedFloralTypes
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player

class SelectPossibleCards(
    private val market: Market,
    private val managePurchasedFloralTypes: ManagePurchasedFloralTypes
) {
    operator fun invoke(
        player: Player
    ): List<GameCard> {
        val canAcquireBloom = player.bloomCount < BLOOM_COUNT
        return market.getTopShowingCards()
            .filter { card ->
                if (managePurchasedFloralTypes.has(card.type)) {
                    false
                } else {
                    when (card.type) {
                        FlourishType.BLOOM -> canAcquireBloom
                        else -> true
                    }
                }
            }
    }
}
