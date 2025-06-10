package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class BattlePhaseTransition(
    private val bestFlowerCards: BestFlowerCards,
    private val matchingBloomCard: MatchingBloomCard,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(players: List<Player>) {
        players.forEach { player -> invoke(player) }
    }

    private suspend operator fun invoke(player: Player) {

        val bestMatchingFlowerCards = bestFlowerCards(player)
        val bestMatchingBloomCards = bestMatchingFlowerCards.map { matchingBloomCard(it) }

        if (bestMatchingBloomCards.isNotEmpty()) {
            if (bestMatchingBloomCards.size == 1) {
                bestMatchingBloomCards[0]?.let { card ->
                    player.addCardToSupply(card.id)
                    player.addCardToSupply(card.id)
                    chronicle(Moment.ACQUIRE_CARD(player, card))
                    chronicle(Moment.ACQUIRE_CARD(player, card))
                }
            } else {
                bestMatchingBloomCards[0]?.let { card ->
                    player.addCardToSupply(card.id)
                    chronicle(Moment.ACQUIRE_CARD(player, card))
                }
                bestMatchingBloomCards[1]?.let { card ->
                    player.addCardToSupply(card.id)
                    chronicle(Moment.ACQUIRE_CARD(player, card))
                }
            }
        }
        // Move all flower cards to supply
        player.floralCards.forEach {
            flowerCard -> player.addCardToSupply(flowerCard.id)
        }
        val trashed = player.trashSeedlingCards()
        player.clearFloralCards()
        player.reset()
        player.drawHand()

        chronicle(Moment.EVENT_BATTLE_TRANSITION(player, trashed))
    }
}
